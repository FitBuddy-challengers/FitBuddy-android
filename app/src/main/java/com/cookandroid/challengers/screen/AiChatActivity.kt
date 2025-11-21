package com.cookandroid.challengers.screen

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.cookandroid.challengers.viewmodel.AiChatViewNewModel
import com.cookandroid.challengers.viewmodel.AiChatViewNewModelFactory
import com.cookandroid.challengers.api.RetrofitClient
import com.cookandroid.challengers.data.db.AppDatabase
import com.cookandroid.challengers.util.UserPreference

class AiChatActivity : AppCompatActivity() {

    private val viewModel: AiChatViewNewModel by viewModels {
        val prefs = UserPreference(this)
        val userId = prefs.getUserId()

        val db = AppDatabase.getDatabase(
            context = this,
            scope = lifecycleScope
        )
        val exercisePlanDao = db.exercisePlanDao()
        val planDetailDao = db.planDetailDao()
        val exerciseSetDao = db.exerciseSetDao()

        AiChatViewNewModelFactory(
            api = RetrofitClient.aiRoutineApi,
            userId = userId,
            exercisePlanDao = exercisePlanDao,
            planDetailDao = planDetailDao,
            exerciseSetDao = exerciseSetDao
        )
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