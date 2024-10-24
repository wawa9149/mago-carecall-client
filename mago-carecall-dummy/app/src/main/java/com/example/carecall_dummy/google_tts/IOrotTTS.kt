package com.example.carecall_dummy.google_tts
import android.content.Context
import android.speech.tts.UtteranceProgressListener

interface IOrotTTS {
    fun createTts(context: Context, listener: UtteranceProgressListener)
    fun start(msg: String?, utteranceId: String?) // 수정된 부분
    fun pause()
    fun clear()
}
