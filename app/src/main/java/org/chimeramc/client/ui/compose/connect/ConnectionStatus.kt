package org.chimeramc.client.ui.compose.connect

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.chimeramc.client.ui.compose.theme.GlassSurface
import org.chimeramc.client.ui.compose.theme.glowColors

/**
 * What a connection step looks like on screen, derived from a [ConnectionStatusState].
 *
 * The mapping is pure so the copy and the button rules are unit-tested without a device: the two
 * things that were wrong in the original hang were "which step is it on" and "is there anything the
 * user can do", and both are decisions, not rendering.
 */
data class ConnectionStatusCopy(
    /** The headline: the step in flight, or the failure's step. */
    val title: String,
    /** The secondary line: elapsed time while busy, or the reason when it failed. */
    val detail: String,
    /** Whether to show the spinner. */
    val showSpinner: Boolean,
    /** Whether Cancel is offered. */
    val showCancel: Boolean,
    /** Whether Retry is offered. */
    val showRetry: Boolean,
    /** Whether the block is shown at all. */
    val visible: Boolean,
)

/**
 * The state the status block renders.
 *
 * [stepLabel] and the failure reason are already resolved to plain language by the state machine and
 * the error taxonomy, so this component never formats an error itself.
 */
data class ConnectionStatusState(
    val visible: Boolean,
    val stepLabel: String,
    val elapsedSeconds: Long,
    val failed: Boolean,
    val retryable: Boolean,
    val cancelled: Boolean,
    /** The failure reason, already user-facing. Empty while busy. */
    val reason: String = "",
) {
    /** The copy and the affordances for this state. */
    fun copy(): ConnectionStatusCopy {
        if (!visible) {
            return ConnectionStatusCopy("", "", false, false, false, false)
        }
        if (cancelled) {
            return ConnectionStatusCopy(
                title = "Cancelled",
                detail = "",
                showSpinner = false,
                showCancel = false,
                showRetry = true,
                visible = true,
            )
        }
        if (failed) {
            return ConnectionStatusCopy(
                title = stepLabel,
                detail = reason.ifEmpty { "Something went wrong." },
                showSpinner = false,
                showCancel = false,
                showRetry = retryable,
                visible = true,
            )
        }
        return ConnectionStatusCopy(
            title = stepLabel,
            detail = "— ${elapsedSeconds}s",
            showSpinner = true,
            showCancel = true,
            showRetry = false,
            visible = true,
        )
    }
}

/**
 * The connection status block.
 *
 * It sits over the Launch screen while a join is in flight, and it is the one thing on that screen
 * that changes while the network is working. Three rules make it the opposite of the old hang:
 *
 * * **It always says something.** A step has a label and an elapsed timer, so "still working" is
 *   visible rather than inferred from an absence.
 * * **It always offers a way out.** A busy step offers Cancel; a failed step offers Retry when the
 *   failure is retryable and nothing when it is not, so a Retry button is never a lie.
 * * **It is never the only thing.** The block is a strip, not a full-screen overlay, so the Launch
 *   screen stays usable underneath and a user can still see the instance they are joining.
 */
@Composable
fun ConnectionStatus(
    state: ConnectionStatusState,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val copy = state.copy()
    if (!copy.visible) return

    GlassSurface(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (copy.showSpinner) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(18.dp)
                            .semantics { contentDescription = "Working" },
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(12.dp))
                }
                Column {
                    Text(
                        text = copy.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (state.failed) glowColors.danger else glowColors.textPrimary,
                    )
                    if (copy.detail.isNotEmpty()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = copy.detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = glowColors.textSecondary,
                        )
                    }
                }
            }
            Row {
                if (copy.showCancel) {
                    TextButton(onClick = onCancel) { Text("Cancel") }
                }
                if (copy.showRetry) {
                    TextButton(onClick = onRetry) { Text("Retry") }
                }
            }
        }
    }
}
