package org.chimeramc.client.ui.compose.caramel

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** The loaders the launcher can offer a version. */
enum class CaramelLoader {
    /** Vanilla Bedrock. */
    Bedrock,

    /** The Rust runtime. */
    Rust,

    /** The lightning-loader path. */
    Lightning,
}

/**
 * The glyph for a loader, drawn rather than shipped as an asset so it scales cleanly at any size.
 *
 * The Bedrock block is a deterministic 12x12 grey mosaic: the noise is seeded from a constant, so
 * the same block is drawn on every recomposition and every launch. A random seed would make the
 * icon flicker between frames.
 */
@Composable
fun CaramelLoaderGlyph(loader: CaramelLoader, modifier: Modifier = Modifier.size(28.dp)) {
    when (loader) {
        CaramelLoader.Bedrock -> Canvas(modifier) {
            val cells = 12
            val cell = size.width / cells
            val random = Random(7)
            for (column in 0 until cells) {
                for (row in 0 until cells) {
                    val grey = if (random.nextFloat() > 0.15f) {
                        listOf(52, 64, 78, 92, 108, 124).random(random)
                    } else {
                        listOf(28, 34).random(random)
                    }
                    drawRect(
                        Color(grey, grey, grey + 6),
                        Offset(column * cell, row * cell),
                        Size(cell + 0.5f, cell + 0.5f),
                    )
                }
            }
            drawRoundRect(
                Color(0xFF14141A),
                Offset.Zero,
                size,
                CornerRadius(2.dp.toPx()),
                Stroke(1.5.dp.toPx()),
            )
        }

        CaramelLoader.Rust -> Box(modifier, Alignment.Center) {
            Canvas(Modifier.matchParentSize()) {
                val centre = Offset(size.width / 2, size.height / 2)
                val radius = size.width * 0.36f
                val rust = Color(0xFFDEA584)
                for (tooth in 0 until 12) {
                    val angle = tooth * PI / 6
                    drawCircle(
                        rust,
                        size.width * 0.085f,
                        Offset(
                            centre.x + (radius + size.width * 0.1f) * cos(angle).toFloat(),
                            centre.y + (radius + size.width * 0.1f) * sin(angle).toFloat(),
                        ),
                    )
                }
                drawCircle(rust, radius + size.width * 0.04f, centre)
                drawCircle(Color(0xFF221612), radius * 0.72f, centre)
            }
            Text("R", style = CaramelType.Title.copy(color = Color(0xFFDEA584), fontSize = 12.sp))
        }

        CaramelLoader.Lightning -> Canvas(modifier) {
            val width = size.width
            val height = size.height
            val bolt = Path().apply {
                moveTo(0.56f * width, 0.06f * height)
                lineTo(0.19f * width, 0.56f * height)
                lineTo(0.46f * width, 0.56f * height)
                lineTo(0.40f * width, 0.94f * height)
                lineTo(0.81f * width, 0.40f * height)
                lineTo(0.54f * width, 0.40f * height)
                close()
            }
            drawPath(bolt, CaramelColors.Yellow)
        }
    }
}

/** A selectable loader tile with a check badge when it is the chosen one. */
@Composable
fun CaramelLoaderTile(
    loader: CaramelLoader,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(11.dp)
    Box(modifier.size(44.dp)) {
        Box(
            Modifier
                .size(44.dp)
                .background(Color.White.copy(if (selected) 0.12f else 0.07f), shape)
                .border(
                    if (selected) 1.6.dp else 1.dp,
                    if (selected) CaramelColors.Green else Color.White.copy(0.14f),
                    shape,
                )
                .clickable(remember { MutableInteractionSource() }, null, onClick = onClick),
            Alignment.Center,
        ) {
            CaramelLoaderGlyph(loader, Modifier.size(28.dp))
        }
        if (selected) {
            Box(
                Modifier
                    .offset((-5).dp, (-5).dp)
                    .size(14.dp)
                    .background(CaramelColors.Green, CircleShape)
                    .border(1.5.dp, CaramelColors.Navy, CircleShape),
                Alignment.Center,
            ) {
                Icon(Icons.Rounded.Check, null, Modifier.size(9.dp), CaramelColors.OnGreen)
            }
        }
    }
}
