package com.still.focus
import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast

class FocusService : AccessibilityService() {
 private var lastBlocked = 0L
 override fun onAccessibilityEvent(event: AccessibilityEvent?) {
  val app = event?.packageName?.toString() ?: return
  val prefs = getSharedPreferences("focus", MODE_PRIVATE)
  val now = System.currentTimeMillis()
  if (now >= prefs.getLong("endAt", 0)) return
  if (app == packageName || app == "com.android.settings" || app == "com.android.systemui") return
  if (prefs.getStringSet("apps", emptySet())?.contains(app) == true && now - lastBlocked > 700) {
   lastBlocked = now
   performGlobalAction(GLOBAL_ACTION_HOME)
   Toast.makeText(this, "Still · This app is paused for your focus session.", Toast.LENGTH_SHORT).show()
  }
 }
 override fun onInterrupt() {}
}
