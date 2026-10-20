package com.liuli.hanzirandom.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.liuli.hanzirandom.ui.theme.ButtonGradient
import com.liuli.hanzirandom.ui.theme.HanziColors

enum class PillStyle { Primary, Secondary, Danger }

@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: PillStyle = PillStyle.Primary,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    val background: Brush = when (style) {
        PillStyle.Primary -> ButtonGradient
        PillStyle.Secondary -> SolidColor(HanziColors.SoftYellow)
        PillStyle.Danger -> SolidColor(HanziColors.DangerSoft)
    }
    val contentColor = when (style) {
        PillStyle.Primary -> Color.White
        PillStyle.Secondary -> HanziColors.AccentDeep
        PillStyle.Danger -> HanziColors.Danger
    }
    val borderColor = when (style) {
        PillStyle.Primary -> Color.Transparent
        PillStyle.Secondary -> HanziColors.Accent.copy(alpha = 0.5f)
        PillStyle.Danger -> HanziColors.Danger.copy(alpha = 0.4f)
    }
    val horizontalPadding = if (style == PillStyle.Primary) 28.dp else 24.dp
    val verticalPadding = if (style == PillStyle.Primary) 14.dp else 12.dp
    val textSize = if (style == PillStyle.Primary) 22.sp else 18.sp

    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.4f)
            .clip(CircleShape)
            .background(background)
            .border(1.5.dp, borderColor, CircleShape)
            .heightIn(min = 48.dp)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = text,
                color = contentColor,
                fontSize = textSize,
                fontWeight = if (style == PillStyle.Primary) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
