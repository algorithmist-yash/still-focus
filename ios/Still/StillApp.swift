import SwiftUI
import WebKit
import FamilyControls
import ManagedSettings
import DeviceActivity

@main struct StillApp: App {
 @StateObject private var model = FocusModel()
 var body: some Scene { WindowGroup { FocusWebView(model: model)
  .familyActivityPicker(isPresented: $model.showPicker, selection: $model.selection)
  .onChange(of: model.selection) { _ in model.saveSelection(); model.status() }
  .onReceive(NotificationCenter.default.publisher(for: UIApplication.didBecomeActiveNotification)) { _ in model.reconcile(); model.status() }
 } }
}

@MainActor final class FocusModel: NSObject, ObservableObject, WKScriptMessageHandler, WKNavigationDelegate {
 @Published var showPicker = false
 @Published var selection = FamilyActivitySelection()
 weak var web: WKWebView?
 private let store = ManagedSettingsStore(named: .init("still"))
 private let center = DeviceActivityCenter()
 override init() {
  super.init()
  if let data = UserDefaults.standard.data(forKey: "selection"), let saved = try? JSONDecoder().decode(FamilyActivitySelection.self, from: data) { selection = saved }
  reconcile()
 }
 func saveSelection() { if let data = try? JSONEncoder().encode(selection) { UserDefaults.standard.set(data, forKey: "selection") } }
 func reconcile() { if UserDefaults.standard.double(forKey: "endAt") <= Date().timeIntervalSince1970 { stop() } }
 func stop() { store.clearAllSettings(); center.stopMonitoring(); UserDefaults.standard.removeObject(forKey: "endAt") }
 func send(_ value: [String: Any]) { guard let data = try? JSONSerialization.data(withJSONObject: value), let json = String(data: data, encoding: .utf8) else { return }; web?.evaluateJavaScript("window.stillNativeUpdate && window.stillNativeUpdate(\(json))") }
 func status() { send(["platform": "ios", "authorized": AuthorizationCenter.shared.authorizationStatus == .approved, "count": selection.applicationTokens.count, "apps": []]) }
 func userContentController(_ userContentController: WKUserContentController, didReceive message: WKScriptMessage) {
  guard message.frameInfo.isMainFrame, let body = message.body as? String, let data = body.data(using: .utf8), let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any], let action = json["action"] as? String else { return }
  Task { @MainActor in
   do {
    switch action {
    case "status": reconcile(); status()
    case "setup":
     try await AuthorizationCenter.shared.requestAuthorization(for: .individual)
     showPicker = true
     status()
    case "stop": stop()
    case "start":
     guard AuthorizationCenter.shared.authorizationStatus == .approved else { throw FocusError.permission }
     guard let endMs = json["endAt"] as? Double else { throw FocusError.duration }
     let end = Date(timeIntervalSince1970: endMs / 1000)
     // DeviceActivity requires intervals of at least 15 minutes. Allow a small bridge delay.
     let seconds = end.timeIntervalSinceNow
     guard seconds >= 899 && seconds <= 10801 else { throw FocusError.duration }
     let calendar = Calendar.current
     let start = Date().addingTimeInterval(-2)
     let components: Set<Calendar.Component> = [.year, .month, .day, .hour, .minute, .second]
     let schedule = DeviceActivitySchedule(intervalStart: calendar.dateComponents(components, from: start), intervalEnd: calendar.dateComponents(components, from: end), repeats: false)
     center.stopMonitoring()
     try center.startMonitoring(.init("still-session"), during: schedule)
     store.shield.applications = selection.applicationTokens.isEmpty ? nil : selection.applicationTokens
     store.shield.applicationCategories = selection.categoryTokens.isEmpty ? nil : .specific(selection.categoryTokens)
     store.shield.webDomains = selection.webDomainTokens.isEmpty ? nil : selection.webDomainTokens
     UserDefaults.standard.set(end.timeIntervalSince1970, forKey: "endAt")
    default: break
    }
   } catch { if action == "start" { stop() }; send(["error": error.localizedDescription]) }
  }
 }
 func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) { status() }
 func webView(_ webView: WKWebView, decidePolicyFor navigationAction: WKNavigationAction, decisionHandler: @escaping (WKNavigationActionPolicy) -> Void) { decisionHandler(navigationAction.request.url?.isFileURL == true ? .allow : .cancel) }
 enum FocusError: LocalizedError {
  case permission, duration
  var errorDescription: String? { switch self { case .permission: return "Allow Screen Time access first."; case .duration: return "iOS blocking needs at least 15 minutes remaining. The timer can still run without blocking." } }
 }
}

struct FocusWebView: UIViewRepresentable {
 @ObservedObject var model: FocusModel
 func makeUIView(context: Context) -> WKWebView {
  let config = WKWebViewConfiguration()
  config.userContentController.add(model, name: "still")
  let view = WKWebView(frame: .zero, configuration: config)
  model.web = view; view.navigationDelegate = model
  view.isOpaque = false; view.backgroundColor = UIColor(red: 0.965, green: 0.969, blue: 0.953, alpha: 1)
  if let url = Bundle.main.url(forResource: "index", withExtension: "html", subdirectory: "www") { view.loadFileURL(url, allowingReadAccessTo: url.deletingLastPathComponent()) }
  return view
 }
 func updateUIView(_ uiView: WKWebView, context: Context) {}
}
