package com.example.payment

import android.content.Context
import android.media.MediaPlayer
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build

class PaymentAnnouncer(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private var playlist = mutableListOf<Int>()
    private var currentIndex = 0

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var focusRequest: AudioFocusRequest? = null

    // To store the user's original volume setting
    private var originalVolume: Int = 0

    // 1. Logic to convert amount to a list of audio files
    private fun getNepaliAudioPlaylist(amount: Int): List<String> {
        val files = mutableListOf<String>()

        files.add("ru") 
        
        var n = amount

        if (n >= 100000) { files.add("num_${n / 100000}"); files.add("lakh"); n %= 100000 }
        if (n >= 1000) { files.add("num_${n / 1000}"); files.add("hajar"); n %= 1000 }
        if (n >= 100) { files.add("num_${n / 100}"); files.add("saya"); n %= 100 }
        if (n > 0) { files.add("num_$n") }

        files.add("prapta_bhayo")
        return files
    }

    private fun setMaxVolume() {
        originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) // Save current volume so we can be polite and change it back later
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) // Get the maximum possible volume for the device
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVol, 0)  // Set it to Max (the '0' flag ensures no system volume slider UI pops up)
    }

    private fun restoreVolume() {
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume, 0)  // Return the volume to what the merchant had it at before
    }

    // 1. Request Focus from the System
    private fun requestFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val playbackAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()

                focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAudioAttributes(playbackAttributes)
                    .setAcceptsDelayedFocusGain(false)
                    .setOnAudioFocusChangeListener { /* Focus change ignored to keep playing */ }
                    .build()

                audioManager.requestAudioFocus(focusRequest!!)
            } else {
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(
                    null, 
                    AudioManager.STREAM_MUSIC, 
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
                )
            }
        } catch (e: Exception) {
            // Silently fail - focus isn't critical for playback
        }
    }

    // 2. Abandon Focus when done
    private fun abandonFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(null)
            }
        } catch (e: Exception) {
            // Silently fail
        }
    }

    // 2. Play the sequence
    fun announce(amount: Int) {
        // We attempt to get focus to duck other apps, 
        // but we don't check the return value. We play anyway.
        requestFocus()
        setMaxVolume() // Force full blast

        val names = getNepaliAudioPlaylist(amount)
        playlist = names.mapNotNull { name ->
            val id = context.resources.getIdentifier(name, "raw", context.packageName)
            if (id != 0) id else null
        }.toMutableList()

        if (playlist.isNotEmpty()) {
            currentIndex = 0
            playNext()
        }
    }

    private fun playNext() {
        if (currentIndex < playlist.size) {
            // Reuse the player if possible to avoid the overhead of constant creation
            val nextPlayer = MediaPlayer.create(context, playlist[currentIndex])
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = nextPlayer
            mediaPlayer?.setOnCompletionListener {
                currentIndex++
                playNext()
            }
            mediaPlayer?.start()
        } else {
            mediaPlayer?.release()
            mediaPlayer = null
            
            restoreVolume()
            abandonFocus() // Give control back to other apps
        }
    }
}