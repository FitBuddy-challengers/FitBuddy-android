package com.cookandroid.challengers.screen

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.cookandroid.challengers.viewmodel.AiChatViewNewModel
import com.cookandroid.challengers.api.RetrofitClient
import com.cookandroid.challengers.viewmodel.AiChatState
import com.cookandroid.challengers.viewmodel.AiChatViewNewModelFactory
import java.text.SimpleDateFormat
import java.util.*
import android.widget.DatePicker
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import com.cookandroid.challengers.ui.theme.Pretendard
import kotlinx.coroutines.delay

@Composable
fun AiChatRouteNew(
    viewModel: AiChatViewNewModel,
    onExit: () -> Unit
) {
    val activity = LocalContext.current as Activity

    val messages by viewModel.messages.collectAsState()
    val state by viewModel.state.collectAsState()

    // 달력 모달 표시 여부
    var showCalendar by remember { mutableStateOf(false) }

    // 첫 진입 = Welcome
    LaunchedEffect(Unit) {
        viewModel.loadWelcomeMessage()
    }

    // EXIT → Activity 종료
    LaunchedEffect(state) {
        if (state == AiChatState.EXIT) {
            activity.finish()
        }
    }

    // ASK_DATE → 1초 후 모달 띄우기
    LaunchedEffect(state == AiChatState.ASK_DATE) {
        if (state == AiChatState.ASK_DATE) {
            delay(600)   // 1초가 길면 0.6초 정도 추천
            showCalendar = true
        }
    }

    // 달력 모달
    if (showCalendar) {
        CalendarModal(
            onDateSelected = { date ->
                viewModel.onUserSend(date)
                showCalendar = false
            },
            onDismiss = { showCalendar = false }
        )
    }

    // 채팅 화면
    AiChatScreen(
        messages = messages,
        onSend = { viewModel.onUserSend(it) },
        onOptionClick = { viewModel.onUserSend(it) },
        onBack = { activity.finish() }
    )
}
// ================================================
// 커스텀 모달 캘린더
// ================================================
@Composable
fun CalendarModal(
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x80000000))
            .padding(30.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(20.dp))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // 🔵 상단 제목 (파란 박스 제거한 버전)
            Text(
                text = "운동 날짜를 선택해주세요!",
                color = Color(0xFF2777F0), // 파란색 텍스트
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = Pretendard,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            Spacer(Modifier.height(20.dp))

            // 📅 DatePicker 영역
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(330.dp)
                    .background(Color(0xFFF2F4F7), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                // 실기기에서는 실제 AndroidView(DatePicker)로 대체됨
                Text(
                    "DatePicker Preview",
                    color = Color.Gray,
                    fontFamily = Pretendard
                )
            }

            Spacer(Modifier.height(20.dp))

            // 🔘 버튼 영역
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {

                // ❌ 취소 버튼 — 테두리 2777F0 적용
                OutlinedButton(
                    onClick = onDismiss,
                    border = BorderStroke(1.dp, Color(0xFF2777F0)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.width(100.dp)
                ) {
                    Text(
                        "취소",
                        color = Color(0xFF2777F0),
                        fontFamily = Pretendard,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,

                    )
                }

                // 🔵 닫기 버튼
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2777F0)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.width(100.dp)
                ) {
                    Text("닫기", color = Color.White, fontFamily = Pretendard,fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,)
                }
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun CalendarModalPreview() {
    CalendarModal(onDateSelected = {}, onDismiss = {})
}
