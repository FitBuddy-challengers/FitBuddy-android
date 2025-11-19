package com.cookandroid.challengers.screen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.cookandroid.challengers.viewmodel.AiChatViewNewModel
import com.cookandroid.challengers.viewmodel.AiChatViewNewModelFactory
import com.cookandroid.challengers.api.RetrofitClient

class AiChatActivity : ComponentActivity() {

    private val viewModel: AiChatViewNewModel by viewModels {
        AiChatViewNewModelFactory(RetrofitClient.aiRoutineApi)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AiChatRouteNew(
                viewModel = viewModel,
                onExit = { finish() }
            )
        }
    }
}