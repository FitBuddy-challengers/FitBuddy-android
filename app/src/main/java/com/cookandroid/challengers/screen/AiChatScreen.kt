package com.cookandroid.challengers.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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

//말풍선 ui
@Composable
fun BotBubbleShape() = RoundedCornerShape(
    topStart = 0.dp,   // 직각
    topEnd = 12.dp,
    bottomStart = 12.dp,
    bottomEnd = 12.dp
)

@Composable
fun UserBubbleShape() = RoundedCornerShape(
    topStart = 12.dp,
    topEnd = 0.dp,     // 직각
    bottomStart = 12.dp,
    bottomEnd = 12.dp
)

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

    // 새 메시지 오면 자동 스크롤
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
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
        },

        bottomBar = {
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
                    .imePadding()              // 키보드 위로 자연스레 이동 (핵심)
                    .navigationBarsPadding()   // 하단바와 충돌 방지
            )
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(Color(0xFFE7ECF5))
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

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp) //좌우 여백 16 추가.
            ) {
                items(messages) { msg ->
                    ChatBubble(msg, onOptionClick)
                }
            }
        }
    }
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
                        painter = painterResource(R.drawable.img_chatbot_cat_new),
                        contentDescription = "bot",
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(message.time, fontSize = 11.sp, color = Color(0xFF4F4F4F), fontFamily = Pretendard)
                }

                Spacer(Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .border(1.dp, Color(0xFFD6D6D6), BotBubbleShape())
                        .background(Color.White, BotBubbleShape())
                        .padding(16.dp)
                ) {
                    Text(message.text, fontSize = 16.sp, color = Color(0xFF292929), fontFamily = Pretendard)
                }

                if (message.options.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    QuickReplyButtons(options = message.options, onOptionSelected = onClickOption)
                }
            }
        }

        is ChatUiMessage.User -> {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {

                // 🔹 시간 — 말풍선 바로 위 오른쪽
                Text(
                    message.time,
                    fontSize = 11.sp,
                    color = Color(0xFF4F4F4F),
                    fontFamily = Pretendard,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 8.dp)
                )

                // 🔹 말풍선 — 시간과 겹치지 않도록 top padding 추가
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 16.dp)   // ← 시간과 말풍선 간격 확보!
                        .background(Color(0xFF4F80FF), UserBubbleShape())
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        message.text,
                        fontSize = 16.sp,
                        color = Color.White,
                        fontFamily = Pretendard
                    )
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
    var selected by remember { mutableStateOf<String?>(null) }

    Row(
        modifier = Modifier
            .wrapContentWidth()
            .padding(start = 0.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEach { option ->

            val isSelected = selected == option

            Button(
                onClick = {
                    selected = option
                    onOptionSelected(option)
                          },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = if (isSelected) Color(0xFF2777F0) else Color(0xFF4F4F4F)
                ),
                border = BorderStroke(
                    1.dp,
                    if (isSelected) Color(0xFF2777F0) else Color(0xFFD6D6D6)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                modifier = Modifier
                    .height(32.dp)
                    .wrapContentWidth()
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


