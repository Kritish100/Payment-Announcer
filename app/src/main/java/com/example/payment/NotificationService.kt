package com.example.payment

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.content.Intent
import android.util.Log

import android.media.MediaPlayer
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.content.Context

class NotificationService : NotificationListenerService() {

    private lateinit var announcer: PaymentAnnouncer

    override fun onCreate() {
        super.onCreate()
        announcer = PaymentAnnouncer(this)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {

        val packageName = sbn.packageName
        val extras = sbn.notification.extras
        val title = extras.getString("android.title") ?: ""
        val content = extras.getString("android.text") ?: ""

        // 1. Send the data to MainActivity for debugging
        val debugIntent = Intent("NOTIFICATION_DUMP")
        debugIntent.putExtra("data", "Package: $packageName\nTitle: $title\nContent: $content")
        sendBroadcast(debugIntent)

        // Filter for eSewa Business app or main eSewa app
        // val targetApp = sbn.packageName == "com.esewa.merchant" || sbn.packageName == "com.f1soft.esewa"
        val targetApp = true
        if (targetApp) {
            // val notificationContent = sbn.notification.extras.getString("android.text") ?: ""
            // val amount = extractAmount(notificationContent)
            
            // if (amount != null && amount > 0) {
            //     announcer.announce(amount)
            // }

            // TEST TEST TEST
            announcer.announce(250)
        }

        // playTestSound()
        
    }

    private fun extractAmount(text: String): Int? {
        val regex = "(?i)(?:NPR|Rs\\.?)\\s*([\\d,]+(?:\\.\\d{2})?)".toRegex()
        val match = regex.find(text)
        return match?.groupValues?.get(1)?.let { raw ->
            raw.replace(",", "").toDoubleOrNull()?.toInt()
        }
    }

    private fun playTestSound() {
        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // 1. Set Volume to Max (Stream Music is what Bluetooth speakers usually use)
        val originalVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC)
        am.setStreamVolume(AudioManager.STREAM_MUSIC, am.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0)

        // 2. Request Audio Focus (Pauses YouTube)
        val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build())
            .build()

        if (am.requestAudioFocus(focusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            val resId = resources.getIdentifier("initial", "raw", packageName)
            MediaPlayer.create(this, resId)?.apply {
                start()
                setOnCompletionListener { 
                    it.release()
                    // 3. Return Focus (Resumes YouTube) and restore Volume
                    am.abandonAudioFocusRequest(focusRequest)
                    am.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume, 0)
                }
            }
        }
    }
}