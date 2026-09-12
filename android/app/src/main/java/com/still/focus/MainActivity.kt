package com.still.focus

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.os.Build
import android.content.Intent
import android.content.ComponentName
import android.provider.Settings
import android.webkit.*
import android.view.WindowInsets
import android.widget.FrameLayout
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : Activity() {
 private lateinit var web: WebView
 private val session by lazy { FocusSession(this) }
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  web = WebView(this)
  web.settings.javaScriptEnabled = true
  web.settings.domStorageEnabled = true
  web.settings.allowFileAccess = true
  web.settings.allowContentAccess = false
  web.addJavascriptInterface(Bridge(), "StillAndroid")
  web.webViewClient = object : WebViewClient() {
   override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = true
   override fun onPageFinished(view: WebView, url: String) {
    status()
    // Native-only contact entry; shared website files are left unchanged.
    view.evaluateJavascript("""
     if (!document.getElementById('android-privacy')) {
      const button = document.createElement('button');
      button.id = 'android-privacy'; button.className = 'outline';
      button.textContent = 'Privacy & contact'; button.style.minHeight = '44px';
      button.onclick = () => window.StillAndroid.postMessage(JSON.stringify({action:'privacy'}));
      document.querySelector('main').appendChild(button);
     }
    """.trimIndent(), null)
   }
  }
  val content = FrameLayout(this)
  content.addView(web, FrameLayout.LayoutParams(-1, -1))
  content.setOnApplyWindowInsetsListener { view, insets ->
   if (Build.VERSION.SDK_INT >= 30) {
    val safe = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout() or WindowInsets.Type.ime())
    view.setPadding(safe.left, safe.top, safe.right, safe.bottom)
   } else {
    @Suppress("DEPRECATION")
    view.setPadding(insets.systemWindowInsetLeft, insets.systemWindowInsetTop, insets.systemWindowInsetRight, insets.systemWindowInsetBottom)
   }
   insets
  }
  setContentView(content)
  web.loadUrl("file:///android_asset/www/index.html")
 }
 override fun onResume() { super.onResume(); if (::web.isInitialized) status() }
 private fun enabled(): Boolean {
  val expected = ComponentName(this, FocusService::class.java)
  // Android may store either package/.Service or package/package.Service.
  // Compare parsed components, rather than their different string encodings.
  return Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
   ?.split(':')?.any { ComponentName.unflattenFromString(it) == expected } == true
 }
 private fun appList(): JSONArray {
  val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
  val protected = session.protectedPackages()
  val apps = packageManager.queryIntentActivities(intent, 0).filter { it.activityInfo.packageName !in protected }.distinctBy { it.activityInfo.packageName }.sortedBy { it.loadLabel(packageManager).toString() }
  return JSONArray().also { array -> apps.forEach { array.put(JSONObject().put("id", it.activityInfo.packageName).put("name", it.loadLabel(packageManager).toString()).put("color", "#60784e")) } }
 }
 private fun send(data: JSONObject) { web.evaluateJavascript("window.stillNativeUpdate && window.stillNativeUpdate($data)", null) }
 private fun status() {
  send(JSONObject().put("platform", "android").put("authorized", enabled()).put("apps", appList()))
  val explanation = JSONObject.quote("Enable Still in Accessibility settings. During focus sessions, selected apps are covered by a blocking screen with a countdown. Calls and essential system controls remain available. You can pause or end the session in Still.")
  web.evaluateJavascript("document.getElementById('blocking-explanation') && (document.getElementById('blocking-explanation').textContent = $explanation)", null)
 }
 inner class Bridge {
  @JavascriptInterface fun postMessage(raw: String) { runOnUiThread {
   try {
    val data = JSONObject(raw)
    when (data.getString("action")) {
     "status" -> status()
     "privacy" -> AlertDialog.Builder(this@MainActivity).setTitle("Privacy & contact")
      .setMessage("Still is developed by Yash Raj.\n\nSession names, timer history, stopwatch laps and app selections are stored on this device. There is no account or cross-device sync.\n\nWith your permission, Accessibility detects foreground app names to restrict selected apps. It does not read screen content, messages or typed text. This information is not sent to the developer.\n\nThe interface references Google Fonts; where network access is available, font loading may contact Google. Android and Google Play operate under their own privacy policies.\n\nYou can revoke Accessibility permission in Settings. Clearing Still’s app storage deletes its local data.\n\nDeveloper contact: yash.algorithmist@gmail.com\n\nFull Android privacy policy: github.com/algorithmist-yash/still-focus/blob/master/docs/android-privacy-policy.md")
      .setPositiveButton("Close", null).show()
     "setup" -> AlertDialog.Builder(this@MainActivity).setTitle("Allow focus blocking?").setMessage("Still uses Accessibility to detect the name of the app you open and show a blocking screen when that app is selected during a focus session. A countdown shows when access returns. Calls and essential system controls remain available. It never reads screen content, messages, or typed text, and this data stays on your device. You can turn it off in Settings at any time.").setNegativeButton("Cancel", null).setPositiveButton("Open settings") { _, _ -> startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }.show()
     "start" -> {
      check(enabled()) { "Enable Still in Accessibility settings first." }
      val end = data.getLong("endAt")
      require(end > System.currentTimeMillis() && end <= System.currentTimeMillis() + 10801000) { "Invalid session duration." }
      val allowed = appList(); val ids = (0 until allowed.length()).map { allowed.getJSONObject(it).getString("id") }.toSet()
      val chosen = data.getJSONArray("apps"); val safe = (0 until chosen.length()).map { chosen.getString(it) }.filter { it in ids }.toSet()
      session.start(safe, end)
     }
     "stop" -> session.stop()
    }
   } catch(e: Exception) { send(JSONObject().put("error", e.message ?: "Could not update blocking.")) }
  } }
 }
}
