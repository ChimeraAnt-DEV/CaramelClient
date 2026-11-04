package org.chimeramc.client.ui.compose.replay

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.chimeramc.client.core.replay.ReplayTimelineData
import org.chimeramc.client.core.replay.ReplayTimelineLayout
import org.chimeramc.client.ui.compose.theme.glowColors

/**
 * The cinematic timeline strip: a bar with a diamond per camera keyframe, a tick per marker and a
 * draggable playhead.
 *
 * <p>The spec asks for a keyframe timeline the user can place keyframes on. This is the strip: the
 * diamonds are the camera path's keyframes, the smaller ticks are the clip's markers, and the
 * playhead is the scrub position. Tapping or dragging the bar reports the time under the finger, and
 * a tap near a marker snaps to it, so a marker is reachable by touch.
 *
 * <p>The strip draws from an immutable [ReplayTimelineData] snapshot rather than the mutable path, so
 * adding a keyframe (which rebuilds the snapshot and hands over a new value) redraws the strip. All
 * the arithmetic is in the pure [ReplayTimelineLayout]; this composable only multiplies by its own
 * width and draws, so the snapping rule is unit-tested off-device.
 */
@Composable
fun ReplayTimelineStrip(
    data: ReplayTimelineData,
    onScrub: (Long) -> Unit,
    modifier: Modifier = Modifier,
    contentDescriptionText: String = "Timeline",
) {
    val colors = glowColors
    val track = colors.surfaceRaised
    val keyframeColor = colors.snowBlue
    val markerColor = colors.warning
    val playhead = colors.snowBlueLight
    val snapToleranceMs = 900L
    val durationMs = data.durationMs

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .semantics { contentDescription = contentDescriptionText }
            .pointerInput(data) {
                detectTapGestures { offset ->
                    val t = ReplayTimelineLayout.timeForX(offset.x, durationMs, size.width.toFloat())
                    onScrub(snap(data, t, snapToleranceMs))
                }
            }
            .pointerInput(data) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val t = ReplayTimelineLayout.timeForX(
                        change.position.x, durationMs, size.width.toFloat(),
                    )
                    onScrub(snap(data, t, snapToleranceMs))
                }
            },
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(44.dp)) {
            val w = size.width
            val h = size.height
            val centerY = h / 2f
            val barHeight = 6f

            // The track: a rounded bar across the middle.
            drawRoundRect(
                color = track,
                topLeft = Offset(0f, centerY - barHeight / 2f),
                size = Size(w, barHeight),
                cornerRadius = CornerRadius(barHeight / 2f),
            )

            // Marker ticks: short vertical lines below the track, so they read as events.
            for (f in data.markerFractions) {
                val x = f * w
                drawLine(
                    color = markerColor,
                    start = Offset(x, centerY + barHeight),
                    end = Offset(x, centerY + barHeight + 8f),
                    strokeWidth = 2f,
                )
            }

            // Keyframe diamonds on the track, so a placed keyframe is visible at a glance.
            for (f in data.keyframeFractions) {
                val x = f * w
                val r = 5f
                drawLine(keyframeColor, Offset(x, centerY - r), Offset(x + r, centerY), 3f)
                drawLine(keyframeColor, Offset(x + r, centerY), Offset(x, centerY + r), 3f)
                drawLine(keyframeColor, Offset(x, centerY + r), Offset(x - r, centerY), 3f)
                drawLine(keyframeColor, Offset(x - r, centerY), Offset(x, centerY - r), 3f)
            }

            // The playhead: a full-height line at the scrub position.
            val px = ReplayTimelineLayout.fraction(data.positionMs, durationMs) * w
            drawLine(
                color = playhead,
                start = Offset(px, 2f),
                end = Offset(px, h - 2f),
                strokeWidth = 3f,
            )
        }
    }
}

/**
 * The scrub time for a tap, snapped to the nearest marker within the tolerance.
 *
 * <p>Pure and small enough to inline, but kept out of the gesture lambda so the tap and drag paths
 * share exactly one snapping rule.
 */
private fun snap(data: ReplayTimelineData, timeMs: Long, toleranceMs: Long): Long {
    var best: Long? = null
    var bestDelta = Long.MAX_VALUE
    for (t in data.markerTimes) {
        val delta = kotlin.math.abs(t - timeMs)
        if (delta <= toleranceMs && delta < bestDelta) {
            bestDelta = delta
            best = t
        }
    }
    return best ?: timeMs
}

