package com.alienkb.handler.ui.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alienkb.handler.ui.components.AsciiArt
import com.alienkb.handler.ui.components.TerminalWindow
import com.alienkb.handler.ui.theme.HC
import com.alienkb.handler.ui.theme.Mono
import kotlinx.coroutines.launch

private data class Slide(
    val tag: String,
    val title: String,
    val body: String,
    val art: String,
    val accent: Color,
    val file: String,
)

private val slides = listOf(
    Slide(
        "01 / boot", "Meet Handler",
        "Your accessibility autopilot. It keeps your screen-time tracker running, and steps aside " +
            "the moment an app that hates accessibility opens.",
        Art.LOGO, HC.Mint, "~/handler",
    ),
    Slide(
        "02 / the problem", "Banks say no",
        "Banking apps refuse to open while a service like Curbox is on. Switching it off by hand " +
            "every time is slow, and forgetting to switch it back on breaks your tracking.",
        Art.PROBLEM, HC.Rose, "~/bank.apk",
    ),
    Slide(
        "03 / the fix", "Off before. On after.",
        "Pick your protected apps. Handler turns accessibility off right before they open, and back " +
            "on as soon as you leave or lock your phone. No more trips to Settings.",
        Art.FLOW, HC.Sky, "~/flow.sh",
    ),
    Slide(
        "04 / watchdog", "Xiaomi-proof",
        "HyperOS likes to quietly switch accessibility off after a day or two. Handler notices the " +
            "instant it happens and puts it back, then double-checks on boot, unlock and every 15 minutes.",
        Art.WATCHDOG, HC.Amber, "~/watchdog",
    ),
    Slide(
        "05 / setup", "One command, once",
        "Grant one permission with ADB from a computer (or Shizuku / LADB on the phone). It survives " +
            "reboots. Handler has no internet permission, so nothing ever leaves your phone.",
        Art.SETUP, HC.Violet, "~/terminal",
    ),
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val pager = rememberPagerState(pageCount = { slides.size })
    val scope = rememberCoroutineScope()
    val accent by animateColorAsState(slides[pager.currentPage].accent, tween(500), label = "accent")
    val last = pager.currentPage == slides.lastIndex

    Box(
        Modifier
            .fillMaxSize()
            .background(HC.Bg)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(accent.copy(alpha = 0.20f), Color.Transparent),
                        center = Offset(size.width / 2f, 0f),
                        radius = size.width,
                    ),
                    radius = size.width,
                    center = Offset(size.width / 2f, 0f),
                )
                val step = 22.dp.toPx()
                var y = step / 2
                while (y < size.height) {
                    var x = step / 2
                    while (x < size.width) {
                        drawCircle(Color.White.copy(alpha = 0.035f), radius = 1.2f, center = Offset(x, y))
                        x += step
                    }
                    y += step
                }
            },
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 22.dp, vertical = 10.dp),
        ) {
            Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("> handler_", fontFamily = Mono, color = accent, fontSize = 14.sp)
                Spacer(Modifier.weight(1f))
                if (!last) {
                    TextButton(onClick = onDone) { Text("Skip", color = HC.TextLo) }
                }
            }

            HorizontalPager(state = pager, modifier = Modifier.weight(1f).fillMaxWidth()) { page ->
                SlidePage(slides[page], active = pager.currentPage == page)
            }

            Row(
                Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                slides.indices.forEach { i ->
                    val selected = i == pager.currentPage
                    val w by animateDpAsState(if (selected) 26.dp else 8.dp, label = "dot")
                    Box(
                        Modifier
                            .padding(horizontal = 4.dp)
                            .height(8.dp)
                            .width(w)
                            .clip(CircleShape)
                            .background(if (selected) accent else HC.Stroke),
                    )
                }
            }

            Button(
                onClick = {
                    if (last) onDone() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = HC.Bg),
            ) {
                Text(if (last) "Let's set it up" else "Next", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun SlidePage(slide: Slide, active: Boolean) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
    ) {
        Text(slide.tag.uppercase(), fontFamily = Mono, fontSize = 11.sp, color = slide.accent, letterSpacing = 2.sp)
        Spacer(Modifier.height(12.dp))
        TerminalWindow(accent = slide.accent, title = slide.file) {
            AsciiArt(slide.art, slide.accent, Modifier.fillMaxWidth(), animate = active)
        }
        Spacer(Modifier.height(26.dp))
        Text(slide.title, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = HC.TextHi, lineHeight = 36.sp)
        Spacer(Modifier.height(10.dp))
        Text(slide.body, fontSize = 15.sp, lineHeight = 23.sp, color = HC.TextLo)
    }
}
