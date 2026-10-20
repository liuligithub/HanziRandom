package com.liuli.hanzirandom.ui

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liuli.hanzirandom.domain.SessionState
import com.liuli.hanzirandom.ui.components.PillButton
import com.liuli.hanzirandom.ui.components.PillStyle
import com.liuli.hanzirandom.ui.components.TianZiGe
import com.liuli.hanzirandom.ui.theme.HanziColors
import com.liuli.hanzirandom.ui.theme.HanziFontFamily
import com.liuli.hanzirandom.ui.theme.cardBackground

enum class StudyMode { Learning, Review }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StudyScreen(
    state: SessionState,
    fontSize: Float,
    swipeEnabled: Boolean,
    justAddedMistake: Boolean,
    mode: StudyMode,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onReset: () -> Unit,
    onSpeak: (String) -> Unit,
    onAddMistake: (String) -> Unit,
    onRemoveMistake: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val haptic = { view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HanziColors.SoftYellow.copy(alpha = 0.25f)),
        contentAlignment = Alignment.TopCenter
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val compact = maxHeight < 560.dp ||
                LocalDensity.current.fontScale > 1.15f
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .then(
                        if (compact) {
                            Modifier.verticalScroll(scrollState)
                        } else {
                            Modifier.fillMaxHeight()
                        }
                    )
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (mode == StudyMode.Learning) "随机汉字" else "错题复习",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = HanziColors.AccentDeep
                )
                Spacer(Modifier.weight(1f))
                if (mode == StudyMode.Learning) {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = "设置",
                            tint = HanziColors.AccentDeep
                        )
                    }
                } else {
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "关闭",
                            tint = HanziColors.AccentDeep
                        )
                    }
                }
            }

            if (state.total > 0) {
                Text(
                    text = "第 ${state.position} / 共 ${state.total}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = HanziColors.AccentDeep.copy(alpha = 0.8f),
                    modifier = Modifier.testTag("progressLabel")
                )
                LinearProgressIndicator(
                    progress = {
                        state.drawnCount.toFloat() / maxOf(state.total, 1).toFloat()
                    },
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .width(240.dp)
                        .testTag("progressBar"),
                    color = HanziColors.AccentDeep,
                    trackColor = HanziColors.SoftYellow
                )
            }

            if (compact) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .padding(vertical = 12.dp)
                ) {
                    CharacterArea(
                        state = state,
                        fontSize = fontSize,
                        swipeEnabled = swipeEnabled,
                        mode = mode,
                        onNext = { haptic(); onNext() },
                        onPrevious = { haptic(); onPrevious() },
                        onOpenSettings = onOpenSettings,
                        onCopy = { current -> copyToClipboard(context, current) }
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    CharacterArea(
                        state = state,
                        fontSize = fontSize,
                        swipeEnabled = swipeEnabled,
                        mode = mode,
                        onNext = { haptic(); onNext() },
                        onPrevious = { haptic(); onPrevious() },
                        onOpenSettings = onOpenSettings,
                        onCopy = { current -> copyToClipboard(context, current) }
                    )
                }
            }

            if (state.current != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val current = state.current
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PillButton(
                            text = "读一读",
                            onClick = { onSpeak(current) },
                            modifier = Modifier.testTag("speakButton"),
                            style = PillStyle.Secondary,
                            icon = Icons.Filled.VolumeUp
                        )
                        PillButton(
                            text = if (justAddedMistake) "已加入 ✓" else "加入错题本",
                            onClick = { haptic(); onAddMistake(current) },
                            modifier = Modifier.testTag("addMistakeButton"),
                            style = PillStyle.Secondary,
                            icon = if (justAddedMistake) {
                                Icons.Filled.CheckCircle
                            } else {
                                Icons.Filled.Star
                            }
                        )
                    }
                    if (mode == StudyMode.Review) {
                        PillButton(
                            text = "删除错题",
                            onClick = { haptic(); onRemoveMistake(current) },
                            modifier = Modifier.testTag("deleteMistakeButton"),
                            style = PillStyle.Danger,
                            icon = Icons.Filled.Delete
                        )
                    }
                }
            }

            if (state.total > 0) {
                Spacer(Modifier.height(12.dp))
                BottomControls(
                    state = state,
                    mode = mode,
                    onPrevious = { haptic(); onPrevious() },
                    onNext = { haptic(); onNext() },
                    onReset = { haptic(); onReset() }
                )
            }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CharacterArea(
    state: SessionState,
    fontSize: Float,
    swipeEnabled: Boolean,
    mode: StudyMode,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onOpenSettings: () -> Unit,
    onCopy: (String) -> Unit
) {
    val current = state.current
    if (current == null && state.isFinished) {
        EmptyOrFinishedCard(mode, onOpenSettings)
        return
    }
    if (current == null) return

    var showMenu by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .cardBackground()
            .testTag("characterCard")
            .pointerInput(swipeEnabled, current) {
                var drag = 0f
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, amount ->
                        change.consume()
                        drag += amount
                    },
                    onDragEnd = {
                        if (swipeEnabled) {
                            val threshold = 40.dp.toPx()
                            if (drag < -threshold) onNext() else if (drag > threshold) onPrevious()
                        }
                        drag = 0f
                    },
                    onDragCancel = { drag = 0f }
                )
            }
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClickLabel = "下一个",
                onLongClickLabel = "复制",
                role = Role.Button,
                onClick = onNext,
                onLongClick = { showMenu = true }
            )
    ) {
        TianZiGe(Modifier.fillMaxSize())

        val cardSide = minOf(maxWidth.value, maxHeight.value)
        val fontScale = LocalDensity.current.fontScale.coerceAtLeast(0.1f)
        val effectiveSize = minOf(fontSize, cardSide * 0.72f) / fontScale
        Text(
            text = current,
            modifier = Modifier
                .align(Alignment.Center)
                .testTag("characterLabel"),
            fontFamily = HanziFontFamily,
            fontSize = effectiveSize.sp,
            color = HanziColors.TextPrimary
        )
        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(
                text = { Text("复制") },
                onClick = {
                    showMenu = false
                    onCopy(current)
                }
            )
        }
    }
}

@Composable
private fun EmptyOrFinishedCard(mode: StudyMode, onOpenSettings: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .cardBackground()
            .padding(40.dp),
        contentAlignment = Alignment.Center
    ) {
        if (mode == StudyMode.Review) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("🎉", fontSize = 48.sp)
                Text(
                    text = "全部复习完啦",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = HanziColors.AccentDeep
                )
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "请先在设置里添加汉字",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Gray
                )
                PillButton(text = "去添加汉字", onClick = onOpenSettings)
            }
        }
    }
}

@Composable
private fun BottomControls(
    state: SessionState,
    mode: StudyMode,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onReset: () -> Unit
) {
    val showReset = state.isFinished && mode == StudyMode.Learning && !state.canGoNext
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (showReset) {
            Text(
                text = "全部抽完",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = HanziColors.AccentDeep
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            PillButton(
                text = "上一个",
                onClick = onPrevious,
                modifier = Modifier.testTag("previousButton"),
                style = PillStyle.Secondary,
                enabled = state.canGoPrevious
            )
            if (showReset) {
                PillButton(
                    text = "重置",
                    onClick = onReset,
                    modifier = Modifier.testTag("resetButton"),
                    style = PillStyle.Primary
                )
            } else {
                PillButton(
                    text = "下一个",
                    onClick = onNext,
                    modifier = Modifier.testTag("nextButton"),
                    style = PillStyle.Primary,
                    enabled = state.canGoNext
                )
            }
        }
    }
}
