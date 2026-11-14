package org.chimeramc.client.ui.compose.caramel

import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged

/** The pack's `onFocusChangedCompat`, used by [CaramelTextField]. */
fun Modifier.onFocusChangedCompat(block: (Boolean) -> Unit): Modifier = onFocusChanged { block(it.isFocused) }
