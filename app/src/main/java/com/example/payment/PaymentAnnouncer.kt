package com.example.payment

import android.content.Context
import android.media.MediaPlayer
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.util.Log

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

    // fun announce(amount: Int, packageName: String) {
    //     // 1. Force Max Volume immediately
    //     val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    //     audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVol, 0)
        
    //     Log.d("PaySay", "Attempting to play test_sound at volume $maxVol")
    
    //     // 2. Clear any old players
    //     mediaPlayer?.release()
    //     mediaPlayer = null
    
    //     // 3. Play ONE static file
    //     val resId = context.resources.getIdentifier("ru", "raw", context.packageName)
        
    //     if (resId != 0) {
    //         mediaPlayer = MediaPlayer.create(context, resId)
    //         mediaPlayer?.setOnCompletionListener { 
    //             it.release()
    //             Log.d("PaySay", "Test sound finished playing.")
    //         }
    //         mediaPlayer?.start()
    //     } else {
    //         Log.e("PaySay", "CRITICAL: test_sound.mp3 NOT FOUND in res/raw")
    //     }
    // }

    // 2. Play the sequence
    fun announce(amount: Int, packageName: String) {
        // We attempt to get focus to duck other apps, 
        // but we don't check the return value. We play anyway.
        requestFocus()
        setMaxVolume() // Force full blast

        val soundList = mutableListOf<String>()


        // we will get rid of this audio, we don't need the audio. just make sure the apps are listed in configure
        //  and our app is enabled to read their notifications, thats it.
        when {
            packageName == "com.f1soft.esewa" -> soundList.add("intro_esewa")
            packageName == "com.khalti" -> soundList.add("intro_khalti")
            packageName == "com.prabhu.prabhupay" -> soundList.add("intro_prabhu_pay")
            packageName.contains("com.swifttechnology.imepay") -> soundList.add("intro_imepay")
            packageName.contains("com.f1soft") -> soundList.add("intro_fonepay")
        }

        // 2. Add the amount sounds (e.g., "received", "500", "rupees")
        soundList.addAll(getNepaliAudioPlaylist(amount))

        playlist = soundList.mapNotNull { name ->
            val id = context.resources.getIdentifier(name, "raw", context.packageName)
            if (id != 0) id else null
        }.toMutableList()

        if (playlist.isNotEmpty()) {
            currentIndex = 0
            playNext()
        }
    }

    // private fun playNext() {
    //     if (currentIndex < playlist.size) {
    //         // Reuse the player if possible to avoid the overhead of constant creation
    //         val nextPlayer = MediaPlayer.create(context, playlist[currentIndex])
    //         mediaPlayer?.stop()
    //         mediaPlayer?.release()
    //         mediaPlayer = nextPlayer
    //         mediaPlayer?.setOnCompletionListener {
    //             currentIndex++
    //             playNext()
    //         }
    //         mediaPlayer?.start()
    //     } else {
    //         mediaPlayer?.release()
    //         mediaPlayer = null
            
    //         // NOW it is safe to restore volume and focus
    //         restoreVolume()
    //         abandonFocus()
    //     }
    // }

    private fun playNext() {
        if (currentIndex < playlist.size) {
            // Log the current file being played for debugging
            Log.d("PaySay", "Playing index $currentIndex: Resource ID ${playlist[currentIndex]}")
            
            // 1. Release the PREVIOUS player before creating a new one
            mediaPlayer?.release()
            mediaPlayer = null
    
            try {
                mediaPlayer = MediaPlayer.create(context, playlist[currentIndex])
                
                if (mediaPlayer == null) {
                    Log.e("PaySay", "Failed to create MediaPlayer for ID: ${playlist[currentIndex]}")
                    currentIndex++
                    playNext()
                    return
                }
    
                mediaPlayer?.setOnCompletionListener {
                    it.release() // Release current one when finished
                    currentIndex++
                    playNext()
                }
                mediaPlayer?.start()
            } catch (e: Exception) {
                Log.e("PaySay", "Error playing audio: ${e.message}")
                currentIndex++
                playNext()
            }
        } else {
            mediaPlayer?.release()
            mediaPlayer = null
            restoreVolume()
            abandonFocus()
            Log.d("PaySay", "Playlist finished, volume restored.")
        }
    }
}