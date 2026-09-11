package com.still.focus

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.content.Intent
import android.content.ComponentName
import android.provider.Settings
import android.webkit.*
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : Activity() {
 private lateinit var web: WebView
 private val prefs get() = getSharedPreferences("focus", MODE_PRIVATE)
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
   override fun onPageFinished(view: WebView, url: String) { status() }
  }
  setContentView(web)
  web.loadUrl("file:///android_asset/www/index.html")
 }
 override fun onResume() { super.onResume(); if (::web.isInitialized) status() }
 private fun enabled(): Boolean {
  val name = ComponentName(this, FocusService::class.java).flattenToString()
  return Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)?.split(':')?.any { it.equals(name, true) } == true
 }
 private fun appList(): JSONArray {
  val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
  val home = packageManager.resolveActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)?.activityInfo?.packageName
  val protected = setOf(packageName, "com.android.settings", "com.android.systemui", "com.android.phone", "com.google.android.dialer", "com.android.dialer", home)
  val apps = packageManager.queryIntentActivities(intent, 0).filter { it.activityInfo.packageName !in protected }.distinctBy { it.activityInfo.packageName }.sortedBy { it.loadLabel(packageManager).toString() }
  return JSONArray().also { array -> apps.forEach { array.put(JSONObject().put("id", it.activityInfo.packageName).put("name", it.loadLabel(packageManager).toString()).put("color", "#60784e")) } }
 }
 private fun send(data: JSONObject) { web.evaluateJavascript("window.stillNativeUpdate && window.stillNativeUpdate($data)", null) }
 private fun status() { send(JSONObject().put("platform", "android").put("authorized", enabled()).put("apps", appList())) }
 inner class Bridge {
  @JavascriptInterface fun postMessage(raw: String) { runOnUiThread {
   try {
    val data = JSONObject(raw)
    when (data.getString("action")) {
     "status" -> status()
     "setup" -> AlertDialog.Builder(this@MainActivity).setTitle("Allow focus blocking?").setMessage("Still uses Accessibility to detect the name of the app you open and return you home when that app is selected during a focus session. It never reads screen content, messages, or typed text, and this data stays on your device. You can turn it off in Settings at any time.").setNegativeButton("Cancel", null).setPositiveButton("Open settings") { _, _ -> startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }.show()
     "start" -> {
      check(enabled()) { "Enable Still in Accessibility settings first." }
      val end = data.getLong("endAt")
      require(end > System.currentTimeMillis() && end <= System.currentTimeMillis() + 10801000) { "Invalid session duration." }
      val allowed = appList(); val ids = (0 until allowed.length()).map { allowed.getJSONObject(it).getString("id") }.toSet()
      val chosen = data.getJSONArray("apps"); val safe = (0 until chosen.length()).map { chosen.getString(it) }.filter { it in ids }.toSet()
      prefs.edit().putStringSet("apps", safe).putLong("endAt", end).apply()
     }
     "stop" -> prefs.edit().remove("endAt").apply()
    }
   } catch(e: Exception) { send(JSONObject().put("error", e.message ?: "Could not update blocking.")) }
  } }
 }
}
