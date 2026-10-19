package com.liuli.hanzirandom.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liuli.hanzirandom.HanziApplication
import com.liuli.hanzirandom.ui.theme.HanziColors

sealed interface AppScreen {
    data object Main : AppScreen
    data class Detail(val character: String) : AppScreen
    data class Review(val characters: List<String>, val id: Long) : AppScreen
}

@Composable
fun HanziRandomApp(app: HanziApplication) {
    val stack = remember { mutableStateListOf<AppScreen>(AppScreen.Main) }
    val stateHolder = rememberSaveableStateHolder()

    BackHandler(enabled = stack.size > 1) {
        stack.removeAt(stack.lastIndex)
    }

    when (val screen = stack.last()) {
        AppScreen.Main -> stateHolder.SaveableStateProvider("main") {
            MainTabs(
                app = app,
                onDetail = { stack.add(AppScreen.Detail(it)) },
                onReview = { stack.add(AppScreen.Review(it, System.nanoTime())) }
            )
        }

        is AppScreen.Detail -> CharacterDetailScreen(
            character = screen.character,
            onSpeak = { app.speechService.speak(it) },
            onClose = { stack.removeAt(stack.lastIndex) }
        )

        is AppScreen.Review -> ReviewRoute(
            app = app,
            screen = screen,
            onClose = { stack.removeAt(stack.lastIndex) }
        )
    }
}

@Composable
private fun MainTabs(
    app: HanziApplication,
    onDetail: (String) -> Unit,
    onReview: (List<String>) -> Unit
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val itemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = HanziColors.AccentDeep,
        selectedTextColor = HanziColors.AccentDeep,
        indicatorColor = HanziColors.SoftYellow
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = HanziColors.CardBackground) {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Icon(Icons.Filled.School, contentDescription = null) },
                    label = { Text("学习") },
                    colors = itemColors,
                    modifier = Modifier.testTag("studyTab")
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Icon(Icons.Filled.Star, contentDescription = null) },
                    label = { Text("错题本") },
                    colors = itemColors,
                    modifier = Modifier.testTag("mistakesTab")
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (tab == 0) {
                StudyTab(app, onOpenSettings = {})
            } else {
                MistakeBookTab(
                    app = app,
                    onDetail = onDetail,
                    onReview = onReview,
                    onOpenSettings = {}
                )
            }
        }
    }
}

@Composable
private fun StudyTab(app: HanziApplication, onOpenSettings: () -> Unit) {
    val vm: StudyViewModel = viewModel(factory = StudyViewModel.factory(app))
    val state by vm.state.collectAsStateWithLifecycle()
    val fontSize by vm.fontSize.collectAsStateWithLifecycle()
    val swipeEnabled by vm.swipeEnabled.collectAsStateWithLifecycle()
    val justAdded by vm.justAddedMistake.collectAsStateWithLifecycle()

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
        onOpenSettings = onOpenSettings,
        onClose = {}
    )
}

@Composable
private fun MistakeBookTab(
    app: HanziApplication,
    onDetail: (String) -> Unit,
    onReview: (List<String>) -> Unit,
    onOpenSettings: () -> Unit
) {
    val vm: MistakeBookViewModel = viewModel(factory = MistakeBookViewModel.factory(app))
    val entries by vm.mistakes.collectAsStateWithLifecycle()

    MistakeBookScreen(
        entries = entries,
        onReview = onReview,
        onRemove = vm::remove,
        onClear = vm::clear,
        onDetail = onDetail,
        onOpenSettings = onOpenSettings
    )
}

@Composable
private fun ReviewRoute(
    app: HanziApplication,
    screen: AppScreen.Review,
    onClose: () -> Unit
) {
    val vm: ReviewViewModel = viewModel(
        key = "review-${screen.id}",
        factory = ReviewViewModel.factory(app, screen.characters)
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val justAdded by vm.justAddedMistake.collectAsStateWithLifecycle()
    val fontSize by app.settingsRepository.fontSize.collectAsStateWithLifecycle()
    val swipeEnabled by app.settingsRepository.swipeEnabled.collectAsStateWithLifecycle()

    StudyScreen(
        state = state,
        fontSize = fontSize,
        swipeEnabled = swipeEnabled,
        justAddedMistake = justAdded,
        mode = StudyMode.Review,
        onNext = vm::next,
        onPrevious = vm::previous,
        onReset = {},
        onSpeak = vm::speak,
        onAddMistake = vm::addMistake,
        onRemoveMistake = vm::removeMistake,
        onOpenSettings = {},
        onClose = onClose
    )
}
