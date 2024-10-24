package com.example.carecall_dummy

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.os.Bundle
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.carecall_dummy.google_tts.OrotTTS
import com.google.protobuf.ByteString
import com.google.protobuf.Empty
import com.mago.carecall.grpc.lib.*
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.Metadata
import io.grpc.stub.MetadataUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flow

import android.content.Context


class AudioRecorderActivity : ComponentActivity() {

    // 오디오 녹음 관련 상수
    private val SAMPLE_RATE = 16000 // 16kHz
    private val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO // 모노
    private val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT // 16bit PCM
    private val BUFFER_SIZE = 8000

    // AcousticEchoCanceler 객체
    private var echoCanceler: AcousticEchoCanceler? = null

    // 오디오 녹음 관련 변수
    private var audioRecord: AudioRecord? = null

    // gRPC 관련 변수
    val channel: ManagedChannel = ManagedChannelBuilder.forAddress("222.109.76.229", 9190)
        .usePlaintext() // 보안이 없는 텍스트로 연결
        .build()

    val metadata = Metadata().apply {
        put(Metadata.Key.of("Content-Type", Metadata.ASCII_STRING_MARSHALLER), "application/grpc")
        put(Metadata.Key.of("AUTHORIZATION", Metadata.ASCII_STRING_MARSHALLER), "Bearer test-access-token-1234")
        put(Metadata.Key.of("uuid", Metadata.ASCII_STRING_MARSHALLER), "random-xxxxx") // 유니크 ID 값으로 대체
    }

    // 메타데이터가 추가된 스텁을 한 번만 생성
    var stub = MetadataUtils.attachHeaders(
        CareCallEventServiceGrpcKt.CareCallEventServiceCoroutineStub(channel),
        metadata
    )

    var dialogId: String = "" // 대화 ID
    var turnId: String = "" // 턴 ID

    // ViewModel 초기화
    private val mainViewModel: MainViewModel by viewModels()

    // TTS 인스턴스
    private lateinit var orotTTS: OrotTTS

    // Activity 초기화
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // AudioManager 설정 추가
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION  // 통신 모드 설정
        audioManager.isSpeakerphoneOn = true  // 스피커폰 활성화 (에코 방지)

        // TTS 초기화
        orotTTS = OrotTTS().apply {
            createTts(this@AudioRecorderActivity, object : UtteranceProgressListener() {

                override fun onStart(utteranceId: String?) {
                    mainViewModel.setTtsIsSpeaking(true) // TTS가 시작될 때 상태 변경
                }

                override fun onDone(utteranceId: String?) {
                    mainViewModel.setTtsIsSpeaking(false) // TTS가 끝났을 때 상태 변경
                }

                override fun onError(utteranceId: String?) {
                    mainViewModel.setTtsIsSpeaking(false)
                }
            })
        }

        // Compose UI 설정
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
//                                        Color(0xB3000000), // rgba(0, 0, 0, 0.7)
//                                        Color(0xE6000000)  // rgba(0, 0, 0, 0.9)
                                        Color(0xFF80B3FF),
                                        Color(0xFFD2FFC2)
                                    ),
                                )
                            )
                    ) {
                        AudioRecorderScreen() // AudioRecorderScreen 함수 호출
                    }
                }
            }
        }
    }

    @Composable
    fun AudioRecorderScreen() {
        // TTS 실행 중인지 여부
        val ttsIsSpeaking by mainViewModel.ttsIsSpeaking.collectAsState()
        // 녹음 중인지 여부를 ViewModel에서 가져옴
        val recording by mainViewModel.recording.collectAsState()
        // TTS 텍스트를 ViewModel에서 가져옴
        val ttsText by mainViewModel.ttsText.collectAsState()
        // speechResult 관찰
        val speechResult by mainViewModel.speechResult.collectAsState()


        // 애니메이션 상태 관리
        val infiniteTransition = rememberInfiniteTransition()
        val rotationAngle by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1500, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        )

        // 화면이 처음 전환될 때 EventRequest 실행
        LaunchedEffect(Unit) {
            CoroutineScope(Dispatchers.IO).launch {
                sendEventRequest(EventRequest.ActionEvent.DIALOG_START)
            }
        }

        // Column에 Modifier 추가하여 중앙 정렬
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = ttsText, textAlign = TextAlign.Center, color = Color.White, fontSize = 20.sp)  // TTS 텍스트 출력

            Row(
                modifier = Modifier.padding(25.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 녹음 중일 때 이미지 회전 애니메이션 적용
                if (recording) {
                    Image(
                        painter = painterResource(id = R.drawable.health), // 회전시킬 이미지 리소스
                        contentDescription = null,
                        modifier = Modifier
                            .size(30.dp) // 이미지 크기
                            .rotate(rotationAngle) // 회전 애니메이션 적용
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // 음성 텍스트 결과 출력
                if (speechResult.isNotEmpty()) {
                    Text(text = speechResult, color = Color.Black, fontSize = 16.sp)
                }
            }

            // 구분선
            Spacer(modifier = Modifier.height(16.dp))

            Button(shape = RoundedCornerShape(12.dp), // 모서리 둥글기 조절
                contentPadding = PaddingValues(16.dp),
                colors = ButtonDefaults.buttonColors(Color(0xFF80B3FF
                )), // 버튼 색상
                onClick = {
                    if (!checkPermissions()) {
                        requestPermissions()
                    } else {
                        if (!recording) {
                            startRecording()
                            mainViewModel.setRecording(true) // ViewModel에서 상태 변경
                        } else {
                            stopRecording()
                            mainViewModel.setRecording(false) // ViewModel에서 상태 변경
                        }
                    }
                },
//                enabled = !ttsIsSpeaking  // TTS 실행 중일 때 버튼 비활성화
            ) {
                Text(text = if (recording) "대화 그만하기" else "대화 시작하기")
            }

            Spacer(modifier = Modifier.height(25.dp))

//            Button(
//                onClick = {
//                    CoroutineScope(Dispatchers.IO).launch {
//                        sendEventRequest(EventRequest.ActionEvent.DIALOG_END)
//                        Log.d("AudioRecorder", "Dialog ended")
//                        stopRecording() // 녹음 중지
//                        orotTTS.pause() // TTS 일시 중지
//                        orotTTS.clear() // TTS 초기화
//                    }
//                },
//            ) {
//                Text(text = "대화 종료하기")
//            }

//            if (ttsIsSpeaking) {
//                Text(text = "TTS가 실행 중입니다.", color = Color.White)
//            }
        }
    }

    private fun checkPermissions(): Boolean {
        val result = ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.RECORD_AUDIO)
        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            REQUEST_AUDIO_PERMISSION_CODE
        )
    }

    // 오디오 녹음 및 서버로 전송
    private fun startRecording() {
        // 오디오 녹음 권한 확인
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        // 오디오 녹음 초기화
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,  // 음성 통신을 위해 최적화된 소스
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            BUFFER_SIZE
        )

        // 오디오 녹음 초기화 실패 시 종료
        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            Log.e("AudioRecorder", "AudioRecord initialization failed")
            return
        } else {
            Log.d("AudioRecorder", "AudioRecord initialized successfully")
        }

        if (AcousticEchoCanceler.isAvailable()) {
            val audioSessionId = audioRecord!!.audioSessionId
            val echoCanceler = AcousticEchoCanceler.create(audioSessionId)
            echoCanceler?.enabled = true

            println("Echo Canceller is enabled.")
        } else {
            println("Echo Canceller is not supported on this device.")
        }

        // 오디오 녹음 시작
        audioRecord?.startRecording()
        mainViewModel.setRecording(true) // ViewModel에서 상태 변경
        Log.d("AudioRecorder", "Recording started")

        // EventRequest 실행
        sendEventRequest(EventRequest.ActionEvent.TURN_START)
    }

    // EventRequest 응답 받아서 TTS 출력
    private fun sendEventRequest(actionEvent: EventRequest.ActionEvent) {
        if (!isChannelAvailable()) {
            Log.e("gRPC Error", "gRPC channel is already closed")
            return
        }

        // EventRequest 메시지 생성
        val eventRequest = EventRequest.newBuilder()
            .setEvent(actionEvent)
            .setDialogId(dialogId)
            .build()

        Log.d("AudioRecorder", "Sending EventRequest: event=${eventRequest.event}")

        try {
            CoroutineScope(Dispatchers.IO).launch {
                // 서버로 EventRequest 전송
            val response: EventResponse = stub.eventRequest(eventRequest)

            // 서버에서 받은 dialogId를 저장
            dialogId = response.dialogId
            turnId = response.turnId

            Log.d("AudioRecorder", "Received EventResponse: action=${actionEvent} dialogId=${response.dialogId}, turnId=${response.turnId}")

            if (response.greeting.text.isEmpty()) {
                Log.d("AudioRecorder", "No greeting text received")
            } else {
                Log.d("AudioRecorder", "Received greeting text: ${response.greeting.text}")

                // 서버에서 받은 greeting 텍스트로 TTS 실행
                orotTTS.start(response.greeting.text, "greeting")
                mainViewModel.setTtsText(response.greeting.text)  // TTS 텍스트를 ViewModel에 저장
            }

                if (actionEvent == EventRequest.ActionEvent.TURN_START) {
                    // 오디오 스트림 전송 및 응답 처리 시작
                    startAudioStream()
                    // DeliveryStream 처리 시작
                    startDeliveryStream()
                }
        }
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Error sending EventRequest: ${e.message}")
        }
    }

    private fun isChannelAvailable(): Boolean {
        return !channel.isShutdown && !channel.isTerminated
    }

    // 오디오 스트림 전송 및 서버 응답 처리
    private fun startAudioStream() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val requestFlow = flow {
                    while (mainViewModel.recording.value) {
                        val buffer = ByteArray(BUFFER_SIZE)
                        val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                        if (read > 0) {
                            Log.d("AudioRecorder", "Audio data read: $read bytes")
                            Log.d("AudioRecorder", "Audio data: ${buffer.contentToString()}")
                            val audioChunkRequest = AudioStreamRequest.newBuilder()
                                .setDialogId(dialogId)
                                .setTurnId(turnId)
                                .setContent(ByteString.copyFrom(buffer, 0, read))
                                .build()
                            emit(audioChunkRequest)
                        }
                    }
                }

                // gRPC를 통해 오디오 데이터를 서버로 스트리밍
                stub.audioStream(requestFlow).collect { response ->
                    Log.d("AudioRecorder", "Received audio stream response: ${response.status}")
                    when (response.status) {
                        AudioStreamResponse.Status.PAUSE -> Log.d("AudioRecorder", "Audio stream paused")
                        AudioStreamResponse.Status.END -> stopRecording()
                        else -> Log.d("AudioRecorder", "Unknown response status: ${response.status}")
                    }
                }
            } catch (e: Exception) {
                Log.e("AudioRecorder", "Error during audio stream: ${e.message}")
            }
        }
    }

    // DeliveryStream 처리
    private fun startDeliveryStream() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("DeliveryStream", "Starting DeliveryStream")
                stub.deliveryStream(Empty.getDefaultInstance()).collect { response ->
                    Log.d("DeliveryStream", "Received response: ${response.action}")
                    handleDeliveryResponse(response)
                }
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    private fun handleError(exception: Exception) {
        Log.e("DeliveryStream", "Error occurred", exception)
    }

    private fun handleDeliveryResponse(response: DeliveryResponse) {

        Log.d("DeliveryStream", "Received DeliveryResponse: action=${response.action}")

        when (response.action) {
            DeliveryResponse.Action.SPEECH_TO_TEXT -> {
                // Handle speech-to-text action
                val speechContent = response.speech
                // 상태 업데이트
                mainViewModel.setSpeechResult(speechContent.text) // ViewModel에 텍스트 저장
                println("Received speech: ${speechContent.text}")
            }
            DeliveryResponse.Action.DIALOG -> {
                // Handle dialog action
                val dialogContent = response.dialog
                println("Dialog Stage: ${dialogContent.stage}, Turn: ${dialogContent.turn}, Dialog: ${dialogContent.response.text}")

                // 서버에서 받은 dialog 텍스트로 TTS 실행
                orotTTS.start(dialogContent.response.text, "dialog")
                mainViewModel.setTtsText(dialogContent.response.text)  // TTS 텍스트를 ViewModel에 저장
            }
            DeliveryResponse.Action.EMOTION_RECOGNITION -> {
                // Handle emotion recognition
                val emotionContent = response.emotion
                println("Emotions - Neutral: ${emotionContent.neutral}, Happiness: ${emotionContent.happiness}, Sadness: ${emotionContent.sadness}, Anger: ${emotionContent.angry}, Surprise: ${emotionContent.surprise}")
            }
            DeliveryResponse.Action.DEMENTIA_DETECTION -> {
                // Handle dementia detection
                val dementiaContent = response.dementia
                println("Dementia Level: ${dementiaContent.dementia}")
            }
            DeliveryResponse.Action.DEPRESSION_DETECTION -> {
                // Handle depression detection
                val depressionContent = response.depression
                println("Depression - Negative: ${depressionContent.negative}, Positive: ${depressionContent.positive}")
            }
            else -> {
                println("Unknown action: ${response.action}")
            }
        }
    }

    // TTS 시작을 녹음이 멈췄을 때 실행
    private fun stopRecording() {
        if (mainViewModel.recording.value) {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            mainViewModel.setRecording(false) // ViewModel에서 상태 변경
            Log.d("AudioRecorder", "Recording stopped")

            // Echo Canceller 해제
            echoCanceler?.release()
            echoCanceler = null

            CoroutineScope(Dispatchers.IO).launch {
                sendEventRequest(EventRequest.ActionEvent.TURN_END)
                Log.d("AudioRecorder", "Turn ended")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        channel.shutdown() // gRPC 채널 종료
        audioRecord?.release() // 오디오 녹음 종료
        orotTTS.pause() // TTS 일시 중지
        orotTTS.clear() // TTS 초기화

        // AcousticEchoCanceler 해제
        echoCanceler?.release()
        echoCanceler = null
        Log.d("AudioRecorder", "gRPC channel closed")
    }

    companion object {
        const val REQUEST_AUDIO_PERMISSION_CODE = 1
    }
}
