package com.llamadroid.domain.media

import android.content.Intent
import android.speech.RecognizerIntent
import android.content.Context
import androidx.activity.result.contract.ActivityResultContract
import java.util.Locale

/**
 * Android built-in speech recognition via RecognizerIntent.
 * Works offline on most devices with downloaded language pack.
 */
class SpeechInputContract : ActivityResultContract<Unit, String?>() {
    override fun createIntent(context: Context, input: Unit): Intent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your message...")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }

    override fun parseResult(resultCode: Int, intent: Intent?): String? {
        if (resultCode != android.app.Activity.RESULT_OK) return null
        val results = intent?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        return results?.firstOrNull()
    }
}
