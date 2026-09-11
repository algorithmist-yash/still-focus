import DeviceActivity
import ManagedSettings

final class FocusMonitor: DeviceActivityMonitor {
 override func intervalDidEnd(for activity: DeviceActivityName) {
  super.intervalDidEnd(for: activity)
  ManagedSettingsStore(named: .init("still")).clearAllSettings()
 }
}
