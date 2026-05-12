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
            packageName.contains("com.f1soft") -> true
            packageName.contains("com.khalti") -> true
            packageName.contains("com.swifttechnology") -> true
            packageName.contains("com.prabhu") -> true
            
            // 3. Testing
            packageName == "com.android.shell" -> true
            packageName == "com.mand.notitest" -> true
            
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
            val extras = sbn.notification.extras

            // 1. Pull from ALL possible text fields
            val title = extras.getCharSequence("android.title")?.toString() ?: ""
            val text = extras.getCharSequence("android.text")?.toString() ?: ""
            val bigText = extras.getCharSequence("android.bigText")?.toString() ?: ""

            // 2. Combine them to ensure we don't miss the number
            val fullContent = "$title $text $bigText"

            Log.d("PaySay", "Incoming from ${sbn.packageName}")
            Log.d("PaySay", "Full Content: $fullContent")

            // 3. Use your regex logic
            val amount = extractAmount(fullContent)

            if (amount != null && amount > 0) {
                Log.d("PaySay", "Success! Extracted Amount: $amount")
                announcer.announce(amount, sbn.packageName)
            } else {
                Log.d("PaySay", "Failed to extract amount from: $fullContent")
            }
        }
    }

    private fun extractAmount(text: String): Int? {
        // This looks for:
        // 1. Numbers following Rs. or NPR
        // 2. OR just standalone numbers that look like currency (e.g., 250 or 250.00)
        val regex = "(?i)(?:NPR|Rs\\.?|Received|Amount)\\s*([\\d,]+(?:\\.\\d{1,2})?)|([\\d,]{2,}(?:\\.\\d{1,2})?)".toRegex()
        val match = regex.find(text)
        
        // Check group 1 (with prefix) or group 2 (standalone number)
        val rawAmount = match?.groupValues?.get(1)?.takeIf { it.isNotEmpty() } 
                    ?: match?.groupValues?.get(2)

        return rawAmount?.replace(",", "")?.toDoubleOrNull()?.toInt()
    }
}