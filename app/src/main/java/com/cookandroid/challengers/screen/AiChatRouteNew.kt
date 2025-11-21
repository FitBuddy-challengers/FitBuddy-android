package com.cookandroid.challengers.screen

import android.app.Activity
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.cookandroid.challengers.R
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
import androidx.fragment.app.FragmentActivity
import com.cookandroid.challengers.ui.theme.Pretendard
import com.google.android.material.datepicker.MaterialDatePicker
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

    // ASK_DATE → 모달 띄우기
    LaunchedEffect(state == AiChatState.ASK_DATE) {
        if (state == AiChatState.ASK_DATE) {
            delay(600)

            val activity = activity as FragmentActivity
            val picker = MaterialDatePicker.Builder.dateRangePicker()
                //.setTheme(R.style.CustomCalendarTheme)
                .setTitleText("운동 기간을 선택해주세요!")
                .build()

            picker.addOnPositiveButtonClickListener { selection ->
                val start = selection.first
                val end = selection.second

                val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

                val startStr = format.format(Date(start))
                val endStr = format.format(Date(end))

                viewModel.onDateSelected(startStr, endStr)
            }

            picker.show(activity.supportFragmentManager, "date_picker")
        }
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
//@Composable
//fun CalendarModal(
//    onDateSelected: (String, String) -> Unit,
//    onDismiss: () -> Unit
//) {
//    val context = LocalContext.current
//
//    AndroidView(
//        modifier = Modifier.fillMaxSize(),
//        factory = { ctx ->
//            val picker = MaterialDatePicker.Builder.dateRangePicker()
//                .setTheme(R.style.CustomCalendarTheme)   // ★ 추가
//                .setTitleText("운동 기간을 선택해주세요!")
//                .build()
//
//            picker.addOnPositiveButtonClickListener { selection ->
//                val start = selection.first
//                val end = selection.second
//
//                val startStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
//                    .format(Date(start))
//                val endStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
//                    .format(Date(end))
//
//                onDateSelected(startStr, endStr)
//            }
//
//            picker.addOnDismissListener { onDismiss() }
//
//            picker.show(
//                (context as androidx.fragment.app.FragmentActivity).supportFragmentManager,
//                "date_range_picker"
//            )
//
//            View(context)
//        }
//    )
//}
//@Composable
//fun CalendarModal(
//    onDateSelected: (String) -> Unit,
//    onDismiss: () -> Unit
//) {
//    val context = LocalContext.current
//    val calendar = Calendar.getInstance()
//
//    var startDate by remember { mutableStateOf<String?>(null) }
//    var endDate by remember { mutableStateOf<String?>(null) }
//
//    Box(
//        modifier = Modifier
//            .fillMaxSize()
//            .background(Color(0x80000000))
//            .padding(30.dp),
//        contentAlignment = Alignment.Center
//    ) {
//
//        Column(
//            modifier = Modifier
//                .fillMaxWidth()
//                .background(Color.White, RoundedCornerShape(20.dp))
//                .padding(20.dp),
//            horizontalAlignment = Alignment.CenterHorizontally
//        ) {
//
//            Text(
//                text = "운동 날짜를 선택해주세요!",
//                color = Color(0xFF2777F0),
//                fontSize = 20.sp,
//                fontWeight = FontWeight.Bold,
//                fontFamily = Pretendard,
//                modifier = Modifier.padding(vertical = 12.dp)
//            )
//
//            Spacer(Modifier.height(20.dp))
//
//            // -----------------------------
//            // 📅 실제 DatePicker (AndroidView)
//            // -----------------------------
//            AndroidView(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(330.dp)
//                    .background(Color(0xFFF2F4F7), RoundedCornerShape(12.dp)),
//                factory = { ctx ->
//                    DatePicker(ctx).apply {
//                        // 초기 날짜
//                        val y = calendar.get(Calendar.YEAR)
//                        val m = calendar.get(Calendar.MONTH)
//                        val d = calendar.get(Calendar.DAY_OF_MONTH)
//                        init(y, m, d) { _, year, month, day ->
//                            val picked = "%04d-%02d-%02d".format(year, month + 1, day)
//
//                            // 첫 날짜 선택 → startDate 세팅
//                            if (startDate == null) {
//                                startDate = picked
//                            } else {
//                                endDate = picked
//                            }
//                        }
//                    }
//                }
//            )
//
//            Spacer(Modifier.height(20.dp))
//
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.SpaceEvenly
//            ) {
//                OutlinedButton(
//                    onClick = onDismiss,
//                    border = BorderStroke(1.dp, Color(0xFF2777F0)),
//                    shape = RoundedCornerShape(10.dp),
//                    modifier = Modifier.width(100.dp)
//                ) {
//                    Text("취소", color = Color(0xFF2777F0), fontFamily = Pretendard)
//                }
//
//                Button(
//                    onClick = {
//                        // startDate와 endDate가 둘 다 선택되어 있어야 함
//                        if (startDate != null && endDate != null) {
//                            onDateSelected("${startDate}/${endDate}")
//                        }
//                        onDismiss()
//                    },
//                    colors = ButtonDefaults.buttonColors(
//                        containerColor = Color(0xFF2777F0)
//                    ),
//                    shape = RoundedCornerShape(10.dp),
//                    modifier = Modifier.width(100.dp)
//                ) {
//                    Text("확인", color = Color.White, fontFamily = Pretendard)
//                }
//            }
//        }
//    }
//}




