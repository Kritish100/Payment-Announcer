package com.example.payment

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.ImageView
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

    private var isServiceRunning = false
    private var glowAnimator: ObjectAnimator? = null

    // 1. Define the package names for Nepalese Payment Apps
    private val paymentApps = mapOf(
        "eSewa" to "com.f1soft.esewa", // verified
        "Khalti" to "com.khalti", // verified
        "IME Pay" to "com.swifttechnology.imepay", // verified
        "IME Pay Merchant" to "com.swifttechnology.imepaymerchant",
        "Fonepay" to "com.f1soft.fonepay.user",
        "Fonepay Merchant" to "com.f1soft.fonepay.merchant",
        "Prabhu Pay" to "com.prabhu.prabhupay",

        "NIC Asia MoBank" to "com.f1soft.nicasiamobilebanking", // verified
        "Global Smart Plus" to "com.swifttechnology.globalsmart", // Verified
        "NIMB Smart" to "com.f1soft.megafonebank.activities.starter", // Verified
        "Digi Prabhu" to "com.f1soft.kistmobilebanking.activities.main", // Verified
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // --- UI Initialization ---
        val pulseGlow = findViewById<View>(R.id.pulseGlow)
        val pulseContainer = findViewById<FrameLayout>(R.id.pulseContainer)
        val innerStatusText = findViewById<TextView>(R.id.innerStatusText)
        val btnToggle = findViewById<MaterialButton>(R.id.btnToggleService)
        val btnConfigureApps = findViewById<Button>(R.id.btnVerifyApps) 
        val btnFixNotif = findViewById<Button>(R.id.btnFixNotification)

        val sharedPref = getSharedPreferences("PaySayPrefs", MODE_PRIVATE)
        isServiceRunning = sharedPref.getBoolean("is_active", false)

        // Set Default State (Standby)
        updateUI(isServiceRunning, pulseGlow, pulseContainer, innerStatusText, btnToggle)

        // --- Logic: Start/Stop Service ---
        btnToggle.setOnClickListener {
            val sharedPref = getSharedPreferences("PaySayPrefs", MODE_PRIVATE)

            if (!isServiceRunning) {
                // USER Clicked Start
                if (isNotificationServiceEnabled()) {
                    isServiceRunning = true
                    sharedPref.edit().putBoolean("is_active", true).apply()
                    updateUI(isServiceRunning, pulseGlow, pulseContainer, innerStatusText, btnToggle)
                    Toast.makeText(this, "PaySay: Monitoring Started", Toast.LENGTH_SHORT).show()
                } else {
                    showPermissionDialog()
                }
            } else {
                // User clicked STOP
                isServiceRunning = false
                sharedPref.edit().putBoolean("is_active", false).apply()
                updateUI(isServiceRunning, pulseGlow, pulseContainer, innerStatusText, btnToggle)
                Toast.makeText(this, "PaySay: Monitoring Stopped", Toast.LENGTH_SHORT).show()
            }
        }

        // --- Logic: Checklist Actions ---
        btnFixNotif.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        btnConfigureApps.setOnClickListener {
            showAppSelectionDialog()
        }

        // --- Support & Branding ---
        findViewById<MaterialButton>(R.id.btnWhatsApp).setOnClickListener {
            val url = "https://api.whatsapp.com/send?phone=97798XXXXXXXX"
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            } catch (e: Exception) {
                Toast.makeText(this, "WhatsApp not installed", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<MaterialButton>(R.id.btnMail).setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:support@kriticrafts.com")
            }
            startActivity(intent)
        }

        findViewById<TextView>(R.id.btnTerms).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Data Transparency & Privacy")
                .setMessage("PaySay runs entirely on your physical device. No payment data is uploaded or stored on any external server.")
                .setPositiveButton("I Understand", null)
                .show()
        }
    }

    // --- Permission & System Logic ---

    private fun isNotificationServiceEnabled(): Boolean {
        val pkgName = packageName
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return flat != null && flat.contains(pkgName)
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

    private fun isPackageInstalled(packageName: String): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    // private fun getDetectedPaymentApps(): Map<String, String> {
    //     val detected = mutableMapOf<String, String>()
    //     val pm = packageManager
        
    //     // Core prefixes that cover 99% of the Nepali market
    //     val trustedPrefixes = listOf(
    //         "com.f1soft",            // Covers eSewa and almost all MoBanks
    //         "com.khalti",            // Khalti
    //         "com.swifttechnology",   // IME Pay
    //         "com.prabhu.prabhupay",  // Prabhu Pay
    //         "com.android.shell"      // For your ADB testing
    //     )
    
    //     // Query all installed applications
    //     val allPackages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
    
    //     for (appInfo in allPackages) {
    //         val pkg = appInfo.packageName
            
    //         // Check if this package starts with any of our trusted strings
    //         if (trustedPrefixes.any { pkg.startsWith(it) }) {
    //             // Get the actual name of the app as shown on the home screen
    //             val label = pm.getApplicationLabel(appInfo).toString()
    //             detected[label] = pkg
    //         }
    //     }
    //     return detected.toSortedMap() // Sort alphabetically for a professional UI
    // }

    private fun showAppSelectionDialog() {
        val installedApps = paymentApps.filter { isPackageInstalled(it.value) }
        // val installedApps = getDetectedPaymentApps()
    
        if (installedApps.isEmpty()) {
            Toast.makeText(this, "No payment apps detected.", Toast.LENGTH_SHORT).show()
            return
        }
    
        // 1. Inflate the custom dialog container
        val inflater = LayoutInflater.from(this)
        val dialogView = inflater.inflate(R.layout.dialog_select_app, null)
        val container = dialogView.findViewById<LinearLayout>(R.id.appContainer)
    
        // 2. Create the dialog
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
    
        // 3. Dynamically add a row for each installed app
        for (app in installedApps) {
            val rowView = inflater.inflate(R.layout.item_payment_app, container, false)
            
            val nameText = rowView.findViewById<TextView>(R.id.appName)
            nameText.text = app.key
    
            // When a row is tapped:
            rowView.setOnClickListener {
                dialog.dismiss()
                navigateToNotificationSettings(app.key, app.value)
            }
    
            container.addView(rowView)
        }
    
        dialog.show()
    
        // 4. Optional: Force the dialog to be wide enough on small screens
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(), 
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }


    private fun navigateToNotificationSettings(appName: String, packageName: String) {
        Toast.makeText(this, "Opening $appName settings ...", Toast.LENGTH_LONG).show()

        val intent = Intent().apply {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                    putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                } else {
                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data = Uri.parse("package:$packageName")
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            } catch (e: Exception) {
                action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                data = Uri.parse("package:$packageName")
            }
        }

        Handler(Looper.getMainLooper()).postDelayed({
            try {
                startActivity(intent)
            } catch (e: Exception) {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            }
        }, 700)
    }

    // --- UI Updates & Animations ---

    private fun updateChecklistStatus() {
        val hasAccess = isNotificationServiceEnabled()
        val notifIconBadge = findViewById<FrameLayout>(R.id.notifIconBadge)
        val notifIconText = findViewById<TextView>(R.id.notifIconText)
        val btnEnable = findViewById<Button>(R.id.btnFixNotification)

        if (hasAccess) {
            notifIconBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#E8F5E9"))
            notifIconText.text = "✓"
            notifIconText.setTextColor(Color.parseColor("#4CAF50"))
            btnEnable.visibility = View.GONE
        } else {
            notifIconBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FFEBEE"))
            notifIconText.text = "!"
            notifIconText.setTextColor(Color.parseColor("#E57373"))
            btnEnable.visibility = View.VISIBLE
        }
    }

    private fun updateUI(running: Boolean, glow: View, container: View, text: TextView, btn: MaterialButton) {
        val activeColor = Color.parseColor("#4CAF50")
        val standbyColor = Color.parseColor("#E57373")
        val activeBg = Color.parseColor("#E8F5E9")
        val standbyBg = Color.parseColor("#FFEBEE")
        
        val strokeColor = if (running) activeColor else standbyColor
        val fillColor = if (running) activeBg else standbyBg

        val background = container.background as GradientDrawable
        background.setColor(fillColor)
        background.setStroke(6, strokeColor)

        glow.backgroundTintList = ColorStateList.valueOf(fillColor)
        text.text = if (running) "ACTIVE" else "STANDBY"
        text.setTextColor(strokeColor)

        if (running) {
            btn.text = "STOP SERVICE"
            btn.setBackgroundColor(standbyColor)
            startGlowPulse(glow)
        } else {
            btn.text = "START SERVICE"
            btn.setBackgroundColor(activeColor)
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

    override fun onResume() {
        super.onResume()
        updateChecklistStatus()
    }
}