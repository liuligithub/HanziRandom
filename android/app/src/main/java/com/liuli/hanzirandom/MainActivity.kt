package com.liuli.hanzirandom

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liuli.hanzirandom.ui.StudyMode
import com.liuli.hanzirandom.ui.StudyScreen
import com.liuli.hanzirandom.ui.StudyViewModel
import com.liuli.hanzirandom.ui.theme.HanziRandomTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )
        super.onCreate(savedInstanceState)
        val app = application as HanziApplication
        setContent {
            HanziRandomTheme {
                val vm: StudyViewModel = viewModel(factory = StudyViewModel.factory(app))
                val state by vm.state.collectAsStateWithLifecycle()
                val fontSize by vm.fontSize.collectAsStateWithLifecycle()
                val swipeEnabled by vm.swipeEnabled.collectAsStateWithLifecycle()
                val justAdded by vm.justAddedMistake.collectAsStateWithLifecycle()

                Scaffold { padding ->
                    Box(
                        modifier = Modifier
                            .padding(padding)
                            .fillMaxSize()
                    ) {
                        StudyScreen(
                            state = state,
                            fontSize = fontSize,
                            swipeEnabled = swipeEnabled,
                            justAddedMistake = justAdded,
                            mode = StudyMode.Learning,
                            onNext = vm::next,
                            onPrevious = vm::previous,
                            onReset = vm::reset,
                            onSpeak = vm::speak,
                            onAddMistake = vm::addMistake,
                            onRemoveMistake = {},
                            onOpenSettings = {},
                            onClose = {}
                        )
                    }
                }
            }
        }
    }
}
