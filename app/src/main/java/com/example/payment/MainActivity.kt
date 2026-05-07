package com.example.payment

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

    private var isServiceRunning = false
    private var glowAnimator: ObjectAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // UI Initialization
        val pulseGlow = findViewById<View>(R.id.pulseGlow)
        val pulseContainer = findViewById<FrameLayout>(R.id.pulseContainer)
        val innerStatusText = findViewById<TextView>(R.id.innerStatusText)
        val btnToggle = findViewById<MaterialButton>(R.id.btnToggleService)

        // Set Default State (Standby)
        updateUI(isServiceRunning, pulseGlow, pulseContainer, innerStatusText, btnToggle)

        btnToggle.setOnClickListener {
            if (!isServiceRunning) {
                // User wants to START: Check permission first
                if (isNotificationServiceEnabled()) {
                    isServiceRunning = true
                    updateUI(isServiceRunning, pulseGlow, pulseContainer, innerStatusText, btnToggle)
                } else {
                    // Permission missing: Show the ACTIONABLE POPUP
                    showPermissionDialog()
                }
            } else {
                // User wants to STOP: Always allow stopping
                isServiceRunning = false
                updateUI(isServiceRunning, pulseGlow, pulseContainer, innerStatusText, btnToggle)
            }
        }

        // Checklist Item 1: Notification Access
        findViewById<Button>(R.id.btnFixNotification).setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        // Checklist Item 2: Payment App Verification
        findViewById<Button>(R.id.btnVerifyApps).setOnClickListener {
            showPaymentAppDialog()
        }

        // Support Buttons
        findViewById<MaterialButton>(R.id.btnWhatsApp).setOnClickListener {
            val url = "https://api.whatsapp.com/send?phone=97798XXXXXXXX"
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }

        findViewById<MaterialButton>(R.id.btnMail).setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:support@kriticrafts.com")
            }
            startActivity(intent)
        }

        // Privacy Dialog
        findViewById<TextView>(R.id.btnTerms).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Data Transparency & Privacy")
                .setMessage("PaySay runs entirely on your physical device. No payment data is uploaded or stored on any external server.")
                .setPositiveButton("I Understand", null)
                .show()
        }
    }

    private fun showPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("System Integration")
            .setMessage("To enable real-time payment announcements, PaySay requires access to notification alerts. Please toggle the access for PaySay in the following menu.")
            .setPositiveButton("ENABLE") { _, _ ->
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
            .setNegativeButton("CANCEL", null)
            .show()
    }

    private fun updateChecklistStatus() {
        val hasAccess = isNotificationServiceEnabled()
        
        // Notification Access UI Elements
        val notifIconBadge = findViewById<FrameLayout>(R.id.notifIconBadge) // You'll need to add this ID to your XML FrameLayout
        val notifIconText = findViewById<TextView>(R.id.notifIconText) // The "!" or "✓" text
        val btnEnable = findViewById<Button>(R.id.btnFixNotification)
    
        if (hasAccess) {
            // Success State (Green)
            notifIconBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#E8F5E9"))
            notifIconText.text = "✓"
            notifIconText.setTextColor(Color.parseColor("#4CAF50"))
            btnEnable.visibility = View.GONE // Hide button if already enabled
        } else {
            // Warning State (Soft Red/Orange)
            notifIconBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FFEBEE"))
            notifIconText.text = "!"
            notifIconText.setTextColor(Color.parseColor("#E57373"))
            btnEnable.visibility = View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        updateChecklistStatus()
    }

    private fun updateUI(running: Boolean, glow: View, container: View, text: TextView, btn: MaterialButton) {
        val activeColor = Color.parseColor("#4CAF50")
        val standbyColor = Color.parseColor("#E57373")
        val activeBg = Color.parseColor("#E8F5E9")
        val standbyBg = Color.parseColor("#FFEBEE")

        val strokeColor = if (running) activeColor else standbyColor
        val fillColor = if (running) activeBg else standbyBg

        // 1. Update Circle Appearance
        val background = container.background as GradientDrawable
        background.setColor(fillColor)
        background.setStroke(6, strokeColor)

        // 2. Update Glow Base Color
        glow.backgroundTintList = ColorStateList.valueOf(fillColor)

        // 3. Update Text Content
        text.text = if (running) "ACTIVE" else "STANDBY"
        text.setTextColor(strokeColor)

        // 4. Update Button State
        if (running) {
            btn.text = "STOP SERVICE"
            btn.setBackgroundColor(standbyColor)
            btn.setTextColor(Color.WHITE)
            btn.strokeWidth = 0
            startGlowPulse(glow)
        } else {
            btn.text = "START SERVICE"
            btn.setBackgroundColor(activeColor) // Always Green for START
            btn.strokeWidth = 0
            btn.setTextColor(Color.WHITE)
            stopGlowPulse(glow)
        }
    }

    private fun startGlowPulse(view: View) {
        glowAnimator?.cancel()
        val scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.5f)
        val scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.5f)
        val alpha = PropertyValuesHolder.ofFloat(View.ALPHA, 0.7f, 0.0f)

        glowAnimator = ObjectAnimator.ofPropertyValuesHolder(view, scaleX, scaleY, alpha).apply {
            duration = 1800
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    private fun stopGlowPulse(view: View) {
        glowAnimator?.cancel()
        view.alpha = 0f
    }

    private fun showPaymentAppDialog() {
        AlertDialog.Builder(this)
            .setTitle("Payment App Readiness")
            .setMessage("For PaySay to announce payments, please ensure notifications are enabled inside your eSewa, Fonepay, or Khalti app settings.\n\nWithout these notifications, the system cannot detect incoming payments.")
            .setPositiveButton("Check App Settings") { _, _ ->
                startActivity(Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val pkgName = packageName
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return flat != null && flat.contains(pkgName)
    }
}