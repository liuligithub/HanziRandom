package com.liuli.hanzirandom.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liuli.hanzirandom.data.SettingsRepository
import com.liuli.hanzirandom.ui.theme.HanziColors
import com.liuli.hanzirandom.ui.theme.HanziFontFamily
import com.liuli.hanzirandom.ui.theme.cardBackground
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    library: List<String>,
    fontSize: Float,
    swipeEnabled: Boolean,
    onFontSizeChange: (Float) -> Unit,
    onSwipeEnabledChange: (Boolean) -> Unit,
    onResetRound: () -> Unit,
    onAddCharacters: (String) -> Int,
    onRemove: (String) -> Unit,
    onExportString: () -> String,
    onDetail: (String) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var preview by remember { mutableFloatStateOf(fontSize) }
    var input by rememberSaveable { mutableStateOf("") }
    var copiedAll by remember { mutableStateOf(false) }
    var copyToken by remember { mutableIntStateOf(0) }

    fun showToast(message: String) {
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(fontSize) {
        preview = fontSize
    }

    LaunchedEffect(copyToken) {
        if (copyToken > 0) {
            delay(1500)
            copiedAll = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HanziColors.SoftYellow.copy(alpha = 0.25f))
            .safeDrawingPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "设置",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = HanziColors.AccentDeep
                )
                Spacer(Modifier.weight(1f))
                TextButton(
                    onClick = onClose,
                    modifier = Modifier.testTag("settingsDoneButton")
                ) {
                    Text("完成", color = HanziColors.AccentDeep, fontWeight = FontWeight.Bold)
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .cardBackground()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "字号",
                                fontWeight = FontWeight.Bold,
                                color = HanziColors.TextPrimary
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                text = preview.toInt().toString(),
                                fontWeight = FontWeight.Bold,
                                color = HanziColors.AccentDeep
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(84.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "永",
                                fontFamily = HanziFontFamily,
                                fontSize = minOf(preview, 72f).sp,
                                color = HanziColors.TextPrimary
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("小", fontSize = 12.sp, color = HanziColors.TextPrimary.copy(alpha = 0.6f))
                            Slider(
                                value = preview,
                                onValueChange = { preview = it },
                                onValueChangeFinished = { onFontSizeChange(preview) },
                                valueRange = SettingsRepository.FONT_SIZE_MIN..SettingsRepository.FONT_SIZE_MAX,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp)
                                    .semantics {
                                        contentDescription = "字号"
                                        stateDescription = preview.toInt().toString()
                                    }
                                    .testTag("fontSizeSlider")
                            )
                            Text("大", fontSize = 12.sp, color = HanziColors.TextPrimary.copy(alpha = 0.6f))
                        }
                    }
                }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .cardBackground()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "手势",
                            fontWeight = FontWeight.Bold,
                            color = HanziColors.TextPrimary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "左右滑动切换",
                                color = HanziColors.TextPrimary,
                                fontSize = 15.sp
                            )
                            Spacer(Modifier.weight(1f))
                            Switch(
                                checked = swipeEnabled,
                                onCheckedChange = onSwipeEnabledChange,
                                modifier = Modifier
                                    .semantics { contentDescription = "左右滑动切换" }
                                    .testTag("swipeSwitch")
                            )
                        }
                    }
                }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .cardBackground()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "抽卡",
                            fontWeight = FontWeight.Bold,
                            color = HanziColors.TextPrimary
                        )
                        TextButton(
                            onClick = onResetRound,
                            modifier = Modifier.testTag("resetRoundButton")
                        ) {
                            Icon(
                                Icons.Filled.Shuffle,
                                contentDescription = null,
                                tint = HanziColors.AccentDeep,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "重新开始抽卡",
                                color = HanziColors.AccentDeep,
                                modifier = Modifier.padding(start = 6.dp)
                            )
                        }
                        Text(
                            text = "用最新的字库重新洗牌开始一轮。新加的字会立即加入还没抽完的这一轮；如果本轮已经抽完，重新开始后生效。",
                            fontSize = 12.sp,
                            color = HanziColors.TextPrimary.copy(alpha = 0.6f)
                        )
                    }
                }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .cardBackground()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "添加汉字（可粘贴整段）",
                            fontWeight = FontWeight.Bold,
                            color = HanziColors.TextPrimary
                        )
                        OutlinedTextField(
                            value = input,
                            onValueChange = { input = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp)
                                .testTag("charactersInput"),
                            placeholder = { Text("输入或粘贴汉字…") }
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = {
                                    val added = onAddCharacters(input)
                                    input = ""
                                    showToast(
                                        if (added > 0) "已添加 $added 个汉字" else "没有新增汉字"
                                    )
                                },
                                enabled = input.isNotEmpty(),
                                modifier = Modifier.testTag("addCharactersButton")
                            ) {
                                Text("添加")
                            }
                        }
                    }
                }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .cardBackground()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "导出全部",
                            fontWeight = FontWeight.Bold,
                            color = HanziColors.TextPrimary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    copyToClipboard(context, onExportString())
                                    copiedAll = true
                                    copyToken++
                                },
                                enabled = library.isNotEmpty(),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("copyAllButton")
                            ) {
                                Text(
                                    text = if (copiedAll) "已复制" else "复制到剪贴板",
                                    fontSize = 13.sp
                                )
                            }
                            OutlinedButton(
                                onClick = { shareText(context, onExportString()) },
                                enabled = library.isNotEmpty(),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("shareAllButton")
                            ) {
                                Text("分享/存到备忘录", fontSize = 13.sp)
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "字库（${library.size}）",
                        fontWeight = FontWeight.Bold,
                        color = HanziColors.TextPrimary,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )
                }

                items(library, key = { it }) { character ->
                    CharacterRow(
                        character = character,
                        onCopy = {
                            copyToClipboard(context, character)
                            showToast("已复制 $character")
                        },
                        onRemove = { onRemove(character) },
                        onDetail = { onDetail(character) }
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CharacterRow(
    character: String,
    onCopy: () -> Unit,
    onRemove: () -> Unit,
    onDetail: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value -> value == SwipeToDismissBoxValue.EndToStart }
    )

    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
            onRemove()
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(HanziColors.Danger, MaterialTheme.shapes.medium)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = null,
                    tint = Color.White
                )
            }
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(HanziColors.CardBackground)
                .clickable(
                    role = Role.Button,
                    onClickLabel = "查看详情",
                    onClick = onDetail
                )
                .testTag("libraryRow-$character")
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = character,
                fontFamily = HanziFontFamily,
                fontSize = 22.sp,
                color = HanziColors.TextPrimary
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onCopy) {
                Icon(
                    Icons.Filled.ContentCopy,
                    contentDescription = "复制 $character",
                    tint = HanziColors.AccentDeep
                )
            }
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "删除 $character",
                    tint = HanziColors.AccentDeep
                )
            }
        }
    }
}
