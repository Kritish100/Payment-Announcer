package com.example.payment

import android.content.*
import android.os.Bundle
import android.view.Gravity
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var logTextView: TextView

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            logTextView.text = intent?.getStringExtra("data") ?: "No Data"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        logTextView = TextView(this).apply {
            text = "Waiting for notification..."
            textSize = 18f
            gravity = Gravity.CENTER
            typeface = android.graphics.Typeface.MONOSPACE
            setPadding(40, 40, 40, 40)
        }

        setContentView(logTextView)

        registerReceiver(receiver, IntentFilter("NOTIFICATION_DUMP"), RECEIVER_EXPORTED)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }
}