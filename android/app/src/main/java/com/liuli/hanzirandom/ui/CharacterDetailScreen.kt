package com.liuli.hanzirandom.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liuli.hanzirandom.ui.components.PillButton
import com.liuli.hanzirandom.ui.components.PillStyle
import com.liuli.hanzirandom.ui.components.TianZiGe
import com.liuli.hanzirandom.ui.theme.HanziColors
import com.liuli.hanzirandom.ui.theme.HanziFontFamily
import com.liuli.hanzirandom.ui.theme.cardBackground
import kotlinx.coroutines.delay

@Composable
fun CharacterDetailScreen(
    character: String,
    onSpeak: (String) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            delay(1500)
            copied = false
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
                .fillMaxHeight()
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(Modifier.weight(1f))
                TextButton(
                    onClick = onClose,
                    modifier = Modifier.testTag("detailCloseButton")
                ) {
                    Text(
                        text = "关闭",
                        color = HanziColors.AccentDeep,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            BoxWithConstraints(
                modifier = Modifier
                    .weight(3f)
                    .fillMaxWidth()
                    .cardBackground()
                    .clickable(role = Role.Button) { onSpeak(character) }
            ) {
                TianZiGe(Modifier.fillMaxSize())
                val fontScale = LocalDensity.current.fontScale.coerceAtLeast(0.1f)
                val size = minOf(200f, minOf(maxWidth.value, maxHeight.value) * 0.8f) / fontScale
                Text(
                    text = character,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .testTag("detailCharacterLabel"),
                    fontFamily = HanziFontFamily,
                    fontSize = size.sp,
                    color = HanziColors.TextPrimary
                )
            }

            Spacer(Modifier.height(32.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                PillButton(
                    text = "读一读",
                    onClick = { onSpeak(character) },
                    style = PillStyle.Secondary,
                    icon = Icons.AutoMirrored.Filled.VolumeUp
                )
                PillButton(
                    text = if (copied) "已复制" else "复制",
                    onClick = {
                        copyToClipboard(context, character)
                        copied = true
                    },
                    modifier = Modifier.testTag("detailCopyButton"),
                    style = PillStyle.Primary
                )
            }
        }
    }
}
