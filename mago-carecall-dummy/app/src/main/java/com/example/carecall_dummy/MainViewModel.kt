package com.example.carecall_dummy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val _ttsIsSpeaking = MutableStateFlow(false)
    val ttsIsSpeaking: StateFlow<Boolean> = _ttsIsSpeaking

    private val _recording = MutableStateFlow(false)
    val recording: StateFlow<Boolean> = _recording.asStateFlow()

    // TTS 텍스트 상태 추가
    private val _ttsText = MutableStateFlow<String>("")
    val ttsText: StateFlow<String> get() = _ttsText

    private val _speechResult = MutableStateFlow("")
    val speechResult: StateFlow<String> get() = _speechResult

    // TTS 시작 상태 업데이트
    fun setTtsIsSpeaking(isSpeaking: Boolean) {
        viewModelScope.launch {
            _ttsIsSpeaking.emit(isSpeaking)
        }
    }

    // TTS 텍스트 상태 업데이트 함수
    fun setTtsText(text: String) {
        _ttsText.value = text
    }

    fun setRecording(isRecording: Boolean) {
        _recording.value = isRecording
    }

    // 상태 업데이트 함수
    fun setSpeechResult(result: String) {
        _speechResult.value = result
    }
}
