package com.alienkb.handler.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alienkb.handler.ui.theme.HC
import com.alienkb.handler.ui.theme.Mono
import kotlinx.coroutines.delay

private val CardShape = RoundedCornerShape(22.dp)

@Composable
fun HCard(
    modifier: Modifier = Modifier,
    borderColor: Color = HC.Stroke,
    background: Color = HC.Surface1,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(background)
            .border(1.dp, borderColor, CardShape)
            .padding(18.dp),
        content = content,
    )
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        "// ${text.uppercase()}",
        modifier = modifier.padding(start = 4.dp, top = 6.dp),
        fontFamily = Mono,
        fontSize = 11.sp,
        letterSpacing = 1.5.sp,
        color = HC.TextLo,
    )
}

@Composable
fun Tag(text: String, color: Color, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(8.dp)
    Text(
        text,
        modifier = modifier
            .clip(shape)
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.35f), shape)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        color = color,
        fontFamily = Mono,
        fontSize = 10.sp,
    )
}

@Composable
fun PulseDot(color: Color, size: Dp = 9.dp) {
    val t = rememberInfiniteTransition(label = "pulse")
    val a by t.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
        label = "alpha",
    )
    Box(Modifier.size(size * 2.4f), contentAlignment = Alignment.Center) {
        Box(Modifier.size(size * 2.4f).clip(CircleShape).background(color.copy(alpha = a)))
        Box(Modifier.size(size).clip(CircleShape).background(color))
    }
}

@Composable
fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onChange(!checked) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = if (enabled) HC.TextHi else HC.TextLo, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Spacer(Modifier.size(2.dp))
            Text(subtitle, color = HC.TextLo, fontSize = 13.sp, lineHeight = 18.sp)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = HC.Bg,
                checkedTrackColor = HC.Mint,
                uncheckedThumbColor = HC.TextLo,
                uncheckedTrackColor = HC.Surface3,
                uncheckedBorderColor = HC.Stroke,
            ),
        )
    }
}

/**
 * Monospace ASCII art that scales itself to fit the width and can "type" itself in.
 * Hidden characters are drawn transparent so the layout never jumps.
 */
@Composable
fun AsciiArt(
    art: String,
    color: Color,
    modifier: Modifier = Modifier,
    animate: Boolean = false,
    maxSp: Float = 13f,
) {
    val lines = remember(art) { art.lines() }
    val cols = remember(art) { lines.maxOfOrNull { it.length }?.coerceAtLeast(1) ?: 1 }
    val total = art.length
    var shown by remember(art, animate) { mutableIntStateOf(if (animate) 0 else total) }
    LaunchedEffect(art, animate) {
        if (animate) {
            shown = 0
            while (shown < total) {
                delay(8)
                shown = (shown + 4).coerceAtMost(total)
            }
        }
    }
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val fontScale = LocalDensity.current.fontScale
        val size = (maxWidth.value / (cols * 0.62f) / fontScale).coerceIn(6f, maxSp)
        val text = buildAnnotatedString {
            withStyle(SpanStyle(color = color)) { append(art.substring(0, shown)) }
            withStyle(SpanStyle(color = Color.Transparent)) { append(art.substring(shown)) }
        }
        Text(
            text,
            style = TextStyle(
                fontFamily = Mono,
                fontSize = size.sp,
                lineHeight = (size * 1.2f).sp,
                shadow = Shadow(color = color.copy(alpha = 0.55f), blurRadius = 14f),
            ),
            softWrap = false,
            maxLines = lines.size,
        )
    }
}

@Composable
fun TerminalWindow(
    accent: Color,
    modifier: Modifier = Modifier,
    title: String = "~/handler",
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(HC.Surface1)
            .border(1.dp, accent.copy(alpha = 0.35f), shape),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(HC.Surface2)
                .padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            listOf(HC.Rose, HC.Amber, HC.Mint).forEach {
                Box(Modifier.size(9.dp).clip(CircleShape).background(it))
            }
            Spacer(Modifier.weight(1f))
            Text(title, fontFamily = Mono, fontSize = 11.sp, color = HC.TextLo)
        }
        Column(Modifier.padding(16.dp), content = content)
    }
}
