package com.cookandroid.challengers.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cookandroid.challengers.R
import com.cookandroid.challengers.ui.theme.Pretendard
import java.text.SimpleDateFormat
import java.util.*

sealed class ChatUiMessage {
    data class Bot(val text: String, val time: String, val options: List<String> = emptyList()) : ChatUiMessage()
    data class User(val text: String, val time: String) : ChatUiMessage()
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(
    messages: List<ChatUiMessage>,
    onSend: (String) -> Unit,
    onOptionClick: (String) -> Unit,
    onBack: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val keyboardOpen = isKeyboardOpen()

    // 새로운 메시지가 오면 아래로 스크롤
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {

        // 🔵 상단바는 절대 안 움직임
        CenterAlignedTopAppBar(
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        painter = painterResource(R.drawable.btn_back_2),
                        contentDescription = "back",
                        tint = Color.Unspecified
                    )
                }
            },
            title = {
                Text(
                    text = "챗봇과 대화 중",
                    fontSize = 20.sp,
                    color = Color(0xFF292929),
                    fontFamily = Pretendard,
                    fontWeight = FontWeight.Bold
                )
            },
            modifier = Modifier.padding(top = 20.dp)
        )

        // 🔵 메시지 영역
        Box(
            modifier = Modifier
                .weight(1f) // 남는 공간만 차지 = 키보드 올라오면 자연스럽게 줄어듦
                .fillMaxWidth()
                .background(Color(0xFFE7ECF5))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {

                Text(
                    text = SimpleDateFormat("yyyy.MM.dd E", Locale.getDefault()).format(Date()),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    color = Color(0xFF4F4F4F),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    fontFamily = Pretendard,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(Modifier.height(8.dp))

                // ⭐ LazyColumn이 키보드를 피해 스크롤 가능하도록 설정
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(
                            WindowInsets.ime.only(WindowInsetsSides.Bottom)
                        )
                ) {
                    items(messages) { msg ->
                        ChatBubble(msg) { selected ->
                            onOptionClick(selected)
                        }
                    }
                }
            }
        }

        // 🔵 입력창 — 항상 아래 고정 + 키보드 위로 스무스하게 올라옴
        ChatInputBar(
            text = inputText,
            onTextChange = { inputText = it },
            onSend = {
                if (inputText.isNotBlank()) {
                    onSend(inputText)
                    inputText = ""
                }
            },
            modifier = Modifier
                .imePadding()           // 키보드 활성화 시 위로 딱 붙음
                .navigationBarsPadding() // 하단바 기본 패딩
        )
    }
}


// 🔍 키보드 열림 여부 감지
@Composable
fun isKeyboardOpen(): Boolean {
    val ime = WindowInsets.ime
    return ime.getBottom(LocalDensity.current) > 0
}


@Composable
fun ChatBubble(message: ChatUiMessage, onClickOption: (String) -> Unit = {}) {

    when (message) {
        is ChatUiMessage.Bot -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(R.drawable.temp_cat),
                        contentDescription = "bot",
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(message.time, fontSize = 11.sp, color = Color(0xFF9E9E9E), fontFamily = Pretendard)
                }

                Spacer(Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Text(message.text, fontSize = 16.sp, color = Color(0xFF292929), fontFamily = Pretendard)
                }

                if (message.options.isNotEmpty()) {
                    QuickReplyButtons(options = message.options, onOptionSelected = onClickOption)
                }
            }
        }

        is ChatUiMessage.User -> {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End
            ) {
                Text(message.time, fontSize = 11.sp, color = Color(0xFF9E9E9E), fontFamily = Pretendard)
                Spacer(Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .background(Color(0xFF4F80FF), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Text(message.text, fontSize = 16.sp, color = Color.White, fontFamily = Pretendard)
                }
            }
        }
    }
}


@Composable
fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        TextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFF7F7F7),
                unfocusedContainerColor = Color(0xFFF7F7F7),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            shape = RoundedCornerShape(12.dp),
            placeholder = { Text("내용을 입력하세요", fontFamily = Pretendard) },
            trailingIcon = {
                IconButton(onClick = onSend) {
                    Icon(
                        painter = painterResource(R.drawable.ic_home_chat_send),
                        contentDescription = "send",
                        tint = Color.Unspecified
                    )
                }
            },
            textStyle = LocalTextStyle.current.copy(fontFamily = Pretendard)
        )
    }
}


@Composable
fun QuickReplyButtons(
    options: List<String>,
    onOptionSelected: (String) -> Unit
) {
    var selectedOption by remember { mutableStateOf<String?>(null) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        options.forEach { option ->

            val isSelected = selectedOption == option

            Button(
                onClick = {
                    selectedOption = option
                    onOptionSelected(option)
                },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF4F4F4F)
                ),
                border = BorderStroke(
                    1.dp,
                    if (isSelected) Color(0xFF2777F0) else Color(0xFFD6D6D6)
                ),
                modifier = Modifier
                    .padding(end = 8.dp)
                    .height(34.dp)
            ) {
                Text(option, fontSize = 14.sp)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AiChatScreenPreview() {

    val sampleMessages = listOf(
        ChatUiMessage.Bot(
            text = "오늘도 운동 화이팅🔥\n원하시는 상담 내용을 선택해주세요!",
            time = "13:43",
            options = listOf("운동 스케줄 생성", "운동 상담")
        ),
        ChatUiMessage.User(
            text = "운동 스케줄 생성",
            time = "13:44"
        )
    )

    AiChatScreen(
        messages = sampleMessages,
        onSend = {},
        onOptionClick = {},
        onBack = {}
    )
}

@Preview(showBackground = true)
@Composable
fun QuickReplyButtonsPreview() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFE7ECF5))
            .padding(36.dp)
    ) {
        // 가짜 AI 말풍선
        Box(
            modifier = Modifier
                .background(Color.White, RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Text(
                text = "오늘도 운동 화이팅🔥\n원하시는 상담 내용을 선택해주세요!",
                fontFamily = Pretendard,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))  // 🔥 간격 조절

        // QuickReply 버튼 프리뷰
        QuickReplyButtons(
            options = listOf("운동 스케줄 생성", "운동 상담"),
            onOptionSelected = {}
        )
    }
}
