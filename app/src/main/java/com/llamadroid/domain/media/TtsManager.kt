package com.llamadroid.domain.media

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@Singleton
class TtsManager @Inject constructor(private val context: Context) {
    private var tts: TextToSpeech? = null
    private var isReady = false

    fun init() {
        if (tts != null) return
        tts = TextToSpeech(context) { status ->
            isReady = (status == TextToSpeech.SUCCESS)
            tts?.language = Locale.getDefault()
        }
    }

    fun speak(text: String) {
        if (!isReady) { init(); return }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts_utterance")
    }

    fun stop() { tts?.stop() }

    fun shutdown() { tts?.stop(); tts?.shutdown(); tts = null; isReady = false }
}
