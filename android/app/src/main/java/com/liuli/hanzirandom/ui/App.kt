package com.liuli.hanzirandom.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.liuli.hanzirandom.HanziApplication
import com.liuli.hanzirandom.ui.theme.HanziColors

sealed interface AppScreen {
    data object Main : AppScreen
    data class Detail(val character: String) : AppScreen
    data class Review(val characters: List<String>, val id: Long) : AppScreen
}

private const val ROUTE_SEPARATOR = "\u001F"

private val screenStackSaver = listSaver<SnapshotStateList<AppScreen>, String>(
    save = { stack -> stack.map { encodeScreen(it).joinToString(ROUTE_SEPARATOR) } },
    restore = { saved ->
        saved
            .map { decodeScreen(it.split(ROUTE_SEPARATOR)) }
            .ifEmpty { listOf(AppScreen.Main) }
            .toMutableStateList()
    }
)

private fun encodeScreen(screen: AppScreen): List<String> = when (screen) {
    AppScreen.Main -> listOf("main")
    is AppScreen.Detail -> listOf("detail", screen.character)
    is AppScreen.Review -> listOf("review", screen.id.toString(), *screen.characters.toTypedArray())
}

private fun decodeScreen(parts: List<String>): AppScreen = when (parts.firstOrNull()) {
    "main" -> AppScreen.Main
    "detail" -> parts.getOrNull(1)?.let { AppScreen.Detail(it) } ?: AppScreen.Main
    "review" -> {
        val id = parts.getOrNull(1)?.toLongOrNull()
        if (id != null) AppScreen.Review(parts.drop(2), id) else AppScreen.Main
    }
    else -> AppScreen.Main
}

@Composable
fun HanziRandomApp(app: HanziApplication) {
    val stack = rememberSaveable(saver = screenStackSaver) {
        mutableStateListOf<AppScreen>(AppScreen.Main)
    }
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
    val tabStateHolder = rememberSaveableStateHolder()
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
                tabStateHolder.SaveableStateProvider("tab-study") {
                    StudyTab(app, onOpenSettings = {})
                }
            } else {
                tabStateHolder.SaveableStateProvider("tab-mistakes") {
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
    val owner = remember(screen.id) {
        object : ViewModelStoreOwner {
            override val viewModelStore = ViewModelStore()
        }
    }
    DisposableEffect(owner) {
        onDispose { owner.viewModelStore.clear() }
    }
    val vm: ReviewViewModel = viewModel(
        viewModelStoreOwner = owner,
        key = "review-${screen.id}",
        factory = ReviewViewModel.factory(app, screen.characters)
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val justAdded by vm.justAddedMistake.collectAsStateWithLifecycle()
    val fontSize by app.settingsRepository.fontSize.collectAsStateWithLifecycle()
    val swipeEnabled by app.settingsRepository.swipeEnabled.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
    ) {
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
}
