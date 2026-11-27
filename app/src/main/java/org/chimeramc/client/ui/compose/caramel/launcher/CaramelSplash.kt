package org.chimeramc.client.ui.compose.caramel.launcher

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.chimeramc.client.R
import org.chimeramc.client.launcher.core.splash.SplashShow
import org.chimeramc.client.ui.compose.caramel.C
import org.chimeramc.client.ui.compose.caramel.T
import org.chimeramc.client.ui.compose.caramel.sp_

/**
 * The cold-start show: Minecraft key art, cross-fading, under the Caramel lockup.
 *
 * <p>The show is the clock. The activity drives [elapsedMs] from a real frame callback and hands
 * over when [SplashShow.finishedAt] says so, so the art is up for the promised twenty seconds
 * rather than for however long the disk happened to take. The warm-up runs beside it; a slow
 * install no longer stretches the screen and a fast one no longer cuts it short.
 *
 * <p>All timing arithmetic lives in [SplashShow] (pure, unit-tested). This composable only reads
 * the answers, which is why it has no durations of its own.
 */
@Composable
fun CaramelSplashScreen(
    art: List<Int>,
    show: SplashShow,
    preparing: String,
    onFinished: () -> Unit,
    onSkip: () -> Unit = onFinished,
) {
    val elapsedMs = rememberShowClock(show, onFinished)
    val outgoing = show.indexAt(elapsedMs)
    val fade = show.fadeAt(elapsedMs)
    val incoming = show.incomingAt(elapsedMs)
    val push = show.slideProgressAt(elapsedMs)
    val exit = exitAlpha(elapsedMs, show)

    Box(Modifier.fillMaxSize().background(C.Abyss).alpha(exit)) {
        Slide(art.getOrNull(outgoing), push)
        if (incoming >= 0) Slide(art.getOrNull(incoming), 0f, fade)

        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to C.Abyss.copy(alpha = 0.78f),
                    0.42f to C.Abyss.copy(alpha = 0.30f),
                    1f to C.Abyss.copy(alpha = 0.90f),
                ),
            ),
        )

        Column(
            Modifier.fillMaxSize().padding(28.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painterResource(R.drawable.ic_caramel_logo),
                    contentDescription = stringResource(R.string.splash_logo_desc),
                    modifier = Modifier.size(52.dp),
                )
                Column(Modifier.padding(start = 12.dp)) {
                    Text(stringResource(R.string.app_name), style = T.Display.copy(fontSize = 22.sp_))
                    Text(preparing, style = T.Body)
                }
                androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                // The show is the clock, but it is also twenty seconds: a returning user gets to
                // leave it. Skipping runs the same hand-over the timeline would, so nothing that
                // waits for the launcher (the version warm-up, the share intent) is dropped.
                androidx.compose.material3.TextButton(onClick = onSkip) {
                    Text(stringResource(R.string.caramel_skip), style = T.Label.copy(color = C.Text))
                }
            }
            Column {
                Text(stringResource(R.string.splash_show_tagline), style = T.Caption)
                Box(
                    Modifier
                        .padding(top = 10.dp)
                        .fillMaxWidth(0.42f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(C.FillStrong),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(show.progressAt(elapsedMs))
                            .height(3.dp)
                            .background(C.Caramel),
                    )
                }
            }
        }
    }
}

/**
 * Runs the show's clock off real frames and reports when it ends.
 *
 * <p>The elapsed time is measured from the first frame's timestamp rather than accumulated per
 * frame, so a dropped frame does not slow the show down and a long stall does not make it drift.
 * The callback is read through [rememberUpdatedState] because the effect must not restart when the
 * caller re-creates the lambda.
 */
@Composable
private fun rememberShowClock(show: SplashShow, onFinished: () -> Unit): Long {
    var elapsedMs by remember(show) { mutableLongStateOf(0L) }
    val finished = rememberUpdatedState(onFinished)
    LaunchedEffect(show) {
        val start = withFrameNanos { it }
        while (true) {
            withFrameNanos { now ->
                val elapsed = (now - start) / 1_000_000L
                elapsedMs = if (elapsed < 0L) 0L else elapsed
            }
            if (show.finishedAt(elapsedMs)) break
        }
        finished.value.invoke()
    }
    return elapsedMs
}

/**
 * One slide, full-bleed, pushed in slowly so a still photograph is not perfectly frozen.
 *
 * @param push 0..1 through the slide; drives the slow scale.
 * @param alpha drawn opacity, so the incoming slide can be cross-faded over the outgoing one.
 */
@Composable
private fun Slide(art: Int?, push: Float, alpha: Float = 1f) {
    if (art == null) return
    val scale = 1.06f + 0.10f * push.coerceIn(0f, 1f)
    Image(
        painterResource(art),
        contentDescription = stringResource(R.string.splash_background_desc),
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha.coerceIn(0f, 1f)
            },
    )
}

/**
 * The screen's own fade at the very end of the show, so the hand-over to the launcher reads as one
 * motion rather than a hard cut between two activities.
 */
private fun exitAlpha(elapsedMs: Long, show: SplashShow): Float {
    val exitMs = 420L
    val left = show.totalMs() - elapsedMs
    if (left >= exitMs) return 1f
    if (left <= 0L) return 0f
    return left.toFloat() / exitMs.toFloat()
}
