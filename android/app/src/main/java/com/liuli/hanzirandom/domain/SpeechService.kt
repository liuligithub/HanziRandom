package com.liuli.hanzirandom.domain

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Shared TextToSpeech wrapper. Chinese voice missing/init failure degrades
 * silently to no audio, matching the iOS spec.
 */
class SpeechService(context: Context) {
    private var tts: TextToSpeech? = null
    private var ready = false

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.SIMPLIFIED_CHINESE)
                ready = tts != null &&
                    result != TextToSpeech.LANG_MISSING_DATA &&
                    result != TextToSpeech.LANG_NOT_SUPPORTED
            }
        }
    }

    fun speak(text: String) {
        if (!ready || text.isEmpty()) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "hanzi-${System.nanoTime()}")
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ready = false
    }
}
