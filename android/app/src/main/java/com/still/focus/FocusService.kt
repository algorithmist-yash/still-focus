package com.still.focus

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class FocusService : AccessibilityService(), SharedPreferences.OnSharedPreferenceChangeListener {
 private val handler = Handler(Looper.getMainLooper())
 private lateinit var session: FocusSession
 private var overlay: View? = null
 private var countdown: TextView? = null
 private var blockedPackage: String? = null
 private val windows get() = getSystemService(WINDOW_SERVICE) as WindowManager
 private val tick = object : Runnable {
  override fun run() {
   if (overlay == null) return
   val left = session.remaining()
   if (left <= 0) { dismiss(); return }
   val seconds = (left + 999) / 1000
   countdown?.text = "%02d:%02d remaining".format(seconds / 60, seconds % 60)
   handler.postDelayed(this, 1000)
  }
 }
 override fun onServiceConnected() {
  super.onServiceConnected()
  session = FocusSession(this)
  session.prefs.registerOnSharedPreferenceChangeListener(this)
 }
 override fun onAccessibilityEvent(event: AccessibilityEvent?) {
  if (!::session.isInitialized || event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
  val app = event.packageName?.toString() ?: return
  if (app == packageName) {
   if (event.className?.toString() == MainActivity::class.java.name) dismiss()
   return // Ignore events from our own overlay.
  }
  if (!session.blocks(app)) { dismiss(); return }
  if (overlay != null && blockedPackage == app) return
  showBlock(app)
 }
 private fun showBlock(app: String) {
  dismiss(); blockedPackage = app
  val label = try { packageManager.getApplicationLabel(packageManager.getApplicationInfo(app, 0)).toString() } catch (_: Exception) { "This app" }
  val panel = LinearLayout(this).apply {
   orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
   setPadding(dp(28), dp(40), dp(28), dp(40)); setBackgroundColor(Color.rgb(24, 37, 30))
   importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
  }
  fun text(value: String, size: Float, color: Int) = TextView(this).apply {
   text = value; textSize = size; setTextColor(color); gravity = Gravity.CENTER
   setPadding(0, dp(12), 0, dp(12))
  }
  panel.addView(text("Still · Focus mode", 18f, Color.rgb(214, 235, 173)))
  panel.addView(text("$label is paused", 28f, Color.WHITE))
  panel.addView(text("You selected this app as a distraction. It will be available when your focus timer ends.", 16f, Color.LTGRAY))
  countdown = text("", 24f, Color.WHITE)
  panel.addView(countdown)
  panel.addView(Button(this).apply {
   text = "Go to home screen"
   setOnClickListener { if (performGlobalAction(GLOBAL_ACTION_HOME)) dismiss() }
  })
  panel.addView(Button(this).apply {
   text = "Return to Still"
   setOnClickListener {
    try {
     startActivity(Intent(this@FocusService, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
     dismiss()
    } catch (_: Exception) { if (performGlobalAction(GLOBAL_ACTION_HOME)) dismiss() }
   }
  })
  panel.addView(text("Calls and essential system controls remain available. You can pause or end your session in Still.", 14f, Color.LTGRAY))
  val params = WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
   WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.OPAQUE)
  val scroll = ScrollView(this).apply { isFillViewport = true; addView(panel) }
  try { windows.addView(scroll, params); overlay = scroll; handler.post(tick) }
  catch (_: RuntimeException) { blockedPackage = null; countdown = null; performGlobalAction(GLOBAL_ACTION_HOME) }
 }
 private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
 private fun dismiss() {
  handler.removeCallbacks(tick)
  overlay?.let { try { windows.removeView(it) } catch (_: RuntimeException) {} }
  overlay = null; countdown = null; blockedPackage = null
 }
 override fun onSharedPreferenceChanged(prefs: SharedPreferences?, key: String?) {
  handler.post { if (blockedPackage?.let { session.blocks(it) } != true) dismiss() }
 }
 override fun onInterrupt() { dismiss() }
 override fun onDestroy() {
  if (::session.isInitialized) session.prefs.unregisterOnSharedPreferenceChangeListener(this)
  dismiss(); super.onDestroy()
 }
}
