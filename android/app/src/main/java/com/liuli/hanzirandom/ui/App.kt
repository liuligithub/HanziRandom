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

private val screenStackSaver = listSaver<SnapshotStateList<AppScreen>, String>(
    save = { stack -> stack.map { NavigationStackCodec.encode(it) } },
    restore = { saved ->
        saved
            .mapNotNull { NavigationStackCodec.decode(it) }
            .ifEmpty { listOf(AppScreen.Main) }
            .toMutableStateList()
    }
)

@Composable
fun HanziRandomApp(app: HanziApplication) {
    val stack = rememberSaveable(saver = screenStackSaver) {
        mutableStateListOf<AppScreen>(AppScreen.Main)
    }
    val stateHolder = rememberSaveableStateHolder()
    val reviewHost: ReviewSessionHost = viewModel()

    val pop = {
        if (stack.size > 1) {
            val removed = stack.removeAt(stack.lastIndex)
            if (removed is AppScreen.Review) reviewHost.release(removed.id)
            if (removed is AppScreen.Settings) stateHolder.removeState("settings")
        }
    }

    BackHandler(enabled = stack.size > 1) {
        pop()
    }

    when (val screen = stack.last()) {
        AppScreen.Main -> stateHolder.SaveableStateProvider("main") {
            MainTabs(
                app = app,
                onDetail = { stack.add(AppScreen.Detail(it)) },
                onReview = { stack.add(AppScreen.Review(it, System.nanoTime())) },
                onSettings = { stack.add(AppScreen.Settings) }
            )
        }

        AppScreen.Settings -> stateHolder.SaveableStateProvider("settings") {
            val factory = remember(app) { SettingsViewModel.factory(app) }
            val vm: SettingsViewModel = viewModel(factory = factory)
            val library by vm.library.collectAsStateWithLifecycle()
            val fontSize by vm.fontSize.collectAsStateWithLifecycle()
            val swipeEnabled by vm.swipeEnabled.collectAsStateWithLifecycle()
            SettingsScreen(
                library = library,
                fontSize = fontSize,
                swipeEnabled = swipeEnabled,
                onFontSizeChange = vm::setFontSize,
                onSwipeEnabledChange = vm::setSwipeEnabled,
                onResetRound = vm::resetRound,
                onAddCharacters = vm::addCharacters,
                onRemove = vm::remove,
                onExportString = vm::exportString,
                onDetail = { stack.add(AppScreen.Detail(it)) },
                onClose = pop
            )
        }

        is AppScreen.Detail -> CharacterDetailScreen(
            character = screen.character,
            onSpeak = { app.speechService.speak(it) },
            onClose = pop
        )

        is AppScreen.Review -> ReviewRoute(
            app = app,
            screen = screen,
            reviewHost = reviewHost,
            onClose = pop
        )
    }
}

@Composable
private fun MainTabs(
    app: HanziApplication,
    onDetail: (String) -> Unit,
    onReview: (List<String>) -> Unit,
    onSettings: () -> Unit
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
                    StudyTab(app, onOpenSettings = onSettings)
                }
            } else {
                tabStateHolder.SaveableStateProvider("tab-mistakes") {
                    MistakeBookTab(
                        app = app,
                        onDetail = onDetail,
                        onReview = onReview,
                        onOpenSettings = onSettings
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
    reviewHost: ReviewSessionHost,
    onClose: () -> Unit
) {
    val owner = remember(screen.id) {
        object : ViewModelStoreOwner {
            override val viewModelStore: ViewModelStore = reviewHost.storeFor(screen.id)
        }
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
