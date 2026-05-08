package com.example.payment

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.content.Intent
import android.content.ComponentName
import android.os.Build
import android.util.Log
import android.content.Context
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager

class NotificationService : NotificationListenerService() {

    private lateinit var announcer: PaymentAnnouncer

    // Replace the targetPackages set with this logic
    private fun isTargetPackage(packageName: String): Boolean {
        return when {
            // 1. The F1Soft Ecosystem (Esewa, Fonepay & most banks)
            packageName.contains("com.f1soft") -> true
            
            // 2. Major Wallets not on F1Soft
            packageName.contains("com.khalti") -> true
            packageName.contains("com.swifttechnology") -> true
            packageName.contains("com.prabhu.prabhupay") -> true
            
            // 3. Testing
            packageName == "com.android.shell" -> true
            
            else -> false
        }
    }

    override fun onCreate() {
        super.onCreate()
        announcer = PaymentAnnouncer(this)
    }

    /**
     * This is the "Secret Sauce" for background persistence.
     * Tells Android to restart this service if it gets killed for memory.
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    /**
     * Called when the system successfully connects to your listener.
     */
    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d("PaySay", "Notification Listener Connected")
    }

    /**
     * If the system disconnects the listener (common on some Chinese OEMs),
     * this tries to request a re-bind silently.
     */
    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            requestRebind(ComponentName(this, NotificationService::class.java))
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        
        // 1. Read the switch state from SharedPreferences
        val sharedPref = getSharedPreferences("PaySayPrefs", android.content.Context.MODE_PRIVATE)
        val isActive = sharedPref.getBoolean("is_active", false) // Default to false if not set

        // 2. If the user pressed STOP, we exit immediately
        if (!isActive) {
            Log.d("PaySay", "Service is technically running but user set state to STOPPED. Ignoring.")
            return 
        }

        // 2. Check if the app is in our target list
        if (isTargetPackage(sbn.packageName)) {
            
            val packageName = sbn.packageName
            val extras = sbn.notification.extras
            val content = extras.getString("android.text") ?: ""
            
            Log.d("PaySay", "Notification from: $packageName Content: $content")
            // val amount = extractAmount(content)
            // TEST TEST TEST
            val amount = 250
            
            if (amount != null && amount > 0) {
                // Real payment detected!
                announcer.announce(amount, packageName)
            } else {
                // If parsing fails, you can announce a generic "Payment Received" 
                // or just log it for debugging during your test.
                Log.d("PaySay", "Matched $packageName but no amount found.")
            }
        }
    }

    private fun extractAmount(text: String): Int? {
        val regex = "(?i)(?:NPR|Rs\\.?)\\s*([\\d,]+(?:\\.\\d{2})?)".toRegex()
        val match = regex.find(text)
        return match?.groupValues?.get(1)?.let { raw ->
            raw.replace(",", "").toDoubleOrNull()?.toInt()
        }
    }
}