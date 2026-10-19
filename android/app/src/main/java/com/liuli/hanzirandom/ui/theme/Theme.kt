package com.liuli.hanzirandom.ui.theme

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.liuli.hanzirandom.R

object HanziColors {
    val Accent = Color(0xFFFF9E40)
    val AccentDeep = Color(0xFFF27326)
    val CardBackground = Color(0xFFFFFAF0)
    val SoftYellow = Color(0xFFFFEDC7)
    val Danger = Color(0xFFD13D2E)
    val DangerSoft = Color(0xFFFFEDE6)
    val TextPrimary = Color(0xFF3B2A1A)
}

val HanziCardCornerRadius = 28.dp

val CardGradient = Brush.linearGradient(listOf(Color.White, HanziColors.SoftYellow))
val ButtonGradient = Brush.horizontalGradient(
    listOf(HanziColors.Accent, HanziColors.AccentDeep)
)

val HanziFontFamily = FontFamily(Font(R.font.lxgw_wenkai_gb_lite_regular))

@Composable
fun HanziRandomTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = HanziColors.AccentDeep,
            onPrimary = Color.White,
            secondary = HanziColors.Accent,
            background = Color(0xFFFFFBF5),
            onBackground = HanziColors.TextPrimary,
            surface = HanziColors.CardBackground,
            onSurface = HanziColors.TextPrimary,
            error = HanziColors.Danger
        ),
        typography = Typography(),
        content = content
    )
}

fun Modifier.cardBackground(cornerRadius: Dp = HanziCardCornerRadius): Modifier {
    val shape = RoundedCornerShape(cornerRadius)
    return this
        .shadow(
            elevation = 10.dp,
            shape = shape,
            spotColor = HanziColors.Accent.copy(alpha = 0.35f),
            ambientColor = HanziColors.Accent.copy(alpha = 0.2f)
        )
        .clip(shape)
        .background(CardGradient)
        .border(2.dp, HanziColors.Accent.copy(alpha = 0.35f), shape)
}
