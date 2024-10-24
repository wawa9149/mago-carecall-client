package com.example.carecall_dummy

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.carecall_dummy.ui.theme.Carecall_dummyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Carecall_dummyTheme {
                // A surface container using the 'background' color from the theme
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
                        StartScreen() // StartScreen 함수 호출
                    }
                }
            }
        }
    }
}

@Composable
fun StartScreen() {
    val context = LocalContext.current // 현재 컨텍스트를 가져옵니다.

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // png image
        Image(
            painter = painterResource(id = R.drawable.health_mago), // drawable에서 이미지 리소스 가져오기
            contentDescription = "My Image", // 이미지의 설명
            modifier = Modifier
                .size(240.dp) // 이미지 크기 설정
        )

        Button(
            shape = RoundedCornerShape(12.dp), // 모서리 둥글기 조절
            contentPadding = PaddingValues(16.dp),
            colors = ButtonDefaults.buttonColors(Color(0xFF80B3FF
        )), // 버튼 색상
            onClick = {
            // 버튼 클릭 시 AudioRecorderActivity로 이동
            val intent = Intent(context, AudioRecorderActivity::class.java)
            context.startActivity(intent)
        }
        ) {


            Text(text = "케어콜과 대화하기", color = Color.White) // 버튼 텍스트
        }
    }
}

@Composable
fun StartScreenPreview() {
    Carecall_dummyTheme {
        StartScreen()
    }
}
