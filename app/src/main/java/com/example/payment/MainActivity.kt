package com.example.payment

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Initialize UI Elements
        val statusIndicator = findViewById<View>(R.id.statusIndicator)
        val statusText = findViewById<TextView>(R.id.statusText)
        val btnStart = findViewById<MaterialButton>(R.id.btnStartService)
        val btnStop = findViewById<MaterialButton>(R.id.btnStopService)
        
        val btnFixNotification = findViewById<Button>(R.id.btnFixNotification)
        val btnVerifyApps = findViewById<Button>(R.id.btnVerifyApps)
        
        val btnWhatsApp = findViewById<MaterialButton>(R.id.btnWhatsApp)
        val btnMail = findViewById<MaterialButton>(R.id.btnMail)
        val btnTerms = findViewById<TextView>(R.id.btnTerms)

        // 2. START Service Logic
        btnStart.setOnClickListener {
            // Change Circle to Emerald Green
            statusIndicator.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#2E7D32"))
            
            // Update Text to Active Green
            statusText.text = "ACTIVE"
            statusText.setTextColor(Color.parseColor("#2E7D32"))
            
            // NOTE: Here is where you will later start your NotificationService
            // val intent = Intent(this, NotificationService::class.java)
            // startService(intent)
        }

        // 3. STOP Service Logic
        btnStop.setOnClickListener {
            // Change Circle back to Standby Gray
            statusIndicator.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#9E9E9E"))
            
            // Update Text to Standby Gray
            statusText.text = "STANDBY"
            statusText.setTextColor(Color.parseColor("#9E9E9E"))
            
            // NOTE: Here is where you will later stop your NotificationService
            // val intent = Intent(this, NotificationService::class.java)
            // stopService(intent)
        }

        // 4. System Checklist Redirects
        btnFixNotification.setOnClickListener {
            // Opens Android Settings to allow this app to read notifications
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            startActivity(intent)
        }

        btnVerifyApps.setOnClickListener {
            // Opens App Info list so users can check eSewa/Fonepay notification settings
            val intent = Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)
            startActivity(intent)
        }

        // 5. Support Buttons
        btnWhatsApp.setOnClickListener {
            val phoneNumber = "97798XXXXXXXX" // Replace with your support number
            val url = "https://api.whatsapp.com/send?phone=$phoneNumber"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        }

        btnMail.setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:support@kriticrafts.com")
                putExtra(Intent.EXTRA_SUBJECT, "PaySay Support Request")
            }
            startActivity(intent)
        }

        // 6. Data Transparency & Privacy Dialog
        btnTerms.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Data Transparency & Privacy")
                .setMessage("PaySay is designed with a 'Local-First' architecture. All payment data is processed entirely on your physical device. \n\n• Zero Data Upload\n• No Cloud Storage\n• 100% Privacy by Default")
                .setPositiveButton("I Understand", null)
                .show()
        }
    }
}