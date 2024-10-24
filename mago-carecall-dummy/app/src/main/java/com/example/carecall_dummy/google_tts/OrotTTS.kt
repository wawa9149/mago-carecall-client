package com.example.carecall_dummy.google_tts

import android.content.Context
import android.media.AudioAttributes
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.*

class OrotTTS : IOrotTTS {

    private val params = Bundle()
    private var tts: TextToSpeech? = null
    private var messageQueue: Queue<Pair<String, String>> = LinkedList() // 메시지와 utteranceId의 쌍


    override fun createTts(context: Context, listener: UtteranceProgressListener) {
        tts = TextToSpeech(context) { state ->
            if (state == TextToSpeech.SUCCESS) {
                tts?.apply {
                    language = Locale.KOREAN
                    setOnUtteranceProgressListener(listener)
                }
            }
        }
    }

    // 두 매개변수를 받는 start 메서드
    override fun start(msg: String?, utteranceId: String?) {
        Log.d("TTS", "TTS started")

        tts?.let {
            if (it.isSpeaking) {
                it.stop()
            }

            Log.d("TTS", "TTS is speaking: $msg")

            msg?.let { content ->
                val id = utteranceId ?: UUID.randomUUID().toString() // null일 경우 기본값 설정
                messageQueue.offer(Pair(content, id))
                speakNext()
            }
        }
    }

    // 다음 메시지를 말하는 함수
    private fun speakNext() {
        val nextMessage = messageQueue.poll()
        nextMessage?.let { (content, id) ->
            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, id) // utteranceId 설정
            tts?.speak(content, TextToSpeech.QUEUE_FLUSH, params, id) // utteranceId 전달
        }
    }

    override fun pause() {
        tts?.stop()
    }

    override fun clear() {
        tts?.stop()
        tts = null
    }
}
