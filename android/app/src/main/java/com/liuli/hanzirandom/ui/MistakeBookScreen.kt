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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liuli.hanzirandom.data.MistakeEntry
import com.liuli.hanzirandom.ui.theme.HanziColors
import com.liuli.hanzirandom.ui.theme.HanziFontFamily
import com.liuli.hanzirandom.ui.theme.cardBackground

@Composable
fun MistakeBookScreen(
    entries: List<MistakeEntry>,
    onReview: (List<String>) -> Unit,
    onRemove: (String) -> Unit,
    onClear: () -> Unit,
    onDetail: (String) -> Unit,
    onOpenSettings: () -> Unit
) {
    var showClearDialog by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HanziColors.SoftYellow.copy(alpha = 0.25f))
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "错题本",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = HanziColors.AccentDeep,
                modifier = Modifier.testTag("mistakeBookHeader")
            )
            Spacer(Modifier.weight(1f))
            if (entries.isNotEmpty()) {
                IconButton(
                    onClick = { showClearDialog = true },
                    modifier = Modifier.testTag("clearMistakesButton")
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "清空错题本",
                        tint = HanziColors.AccentDeep
                    )
                }
            }
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier.testTag("settingsButton")
            ) {
                Icon(
                    Icons.Filled.Settings,
                    contentDescription = "设置",
                    tint = HanziColors.AccentDeep
                )
            }
        }

        if (entries.isEmpty()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .testTag("mistakeBookEmptyState"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Filled.StarBorder,
                    contentDescription = null,
                    tint = HanziColors.Accent.copy(alpha = 0.6f),
                    modifier = Modifier.size(56.dp)
                )
                Text(
                    text = "还没有错题",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = HanziColors.AccentDeep,
                    modifier = Modifier.padding(top = 16.dp)
                )
                Text(
                    text = "学习时点「加入错题本」\n错得多的会排在前面",
                    style = MaterialTheme.typography.bodyMedium,
                    color = HanziColors.TextPrimary.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .widthIn(max = 560.dp)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(CircleShape)
                            .clickable(role = Role.Button) { onReview(entries.map { it.char }) }
                            .testTag("reviewMistakesButton")
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = HanziColors.AccentDeep
                        )
                        Text(
                            text = "开始复习（${entries.size} 个）",
                            color = HanziColors.AccentDeep,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                }
                items(entries, key = { it.char }) { entry ->
                    MistakeRow(entry, onDetail = { onDetail(entry.char) }, onRemove = onRemove)
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("清空错题本？") },
            text = { Text("所有错题记录将被删除，且无法恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        onClear()
                    },
                    modifier = Modifier.testTag("confirmClearMistakesButton")
                ) {
                    Text("清空", color = HanziColors.Danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun MistakeRow(
    entry: MistakeEntry,
    onDetail: () -> Unit,
    onRemove: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onDetail)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .cardBackground(cornerRadius = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = entry.char,
                fontFamily = HanziFontFamily,
                fontSize = 48.sp,
                color = HanziColors.TextPrimary
            )
        }
        Box(
            modifier = Modifier
                .padding(start = 16.dp)
                .clip(CircleShape)
                .background(HanziColors.SoftYellow)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = "错 ${entry.count} 次",
                color = HanziColors.AccentDeep,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
        Spacer(Modifier.weight(1f))
        IconButton(onClick = { onRemove(entry.char) }) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = "删除 ${entry.char}",
                tint = HanziColors.AccentDeep
            )
        }
    }
}
