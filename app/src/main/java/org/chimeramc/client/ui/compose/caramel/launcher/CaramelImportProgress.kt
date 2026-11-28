package org.chimeramc.client.ui.compose.caramel.launcher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import org.chimeramc.client.R
import org.chimeramc.client.ui.compose.caramel.C
import org.chimeramc.client.ui.compose.caramel.CaramelModal
import org.chimeramc.client.ui.compose.caramel.CaramelLauncherState
import org.chimeramc.client.ui.compose.caramel.T
import org.chimeramc.client.ui.compose.caramel.sp_

/**
 * The import dialog: a progress ring, the percentage in the pixel face, and the file being read.
 *
 * <p>Importing copies a whole game package and can take a minute on a phone, so it needs a modal
 * that cannot be dismissed by tapping outside it and that says how far along it is. The percentage
 * is drawn in Press Start 2P — the same face the splash uses — because it reads as a game loading
 * screen rather than as a system dialog.
 *
 * <p>[CaramelLauncherState.importProgress] is -1 when nothing is importing, which is the only state
 * that hides the dialog.
 */
@Composable
fun CaramelImportProgressModal(state: CaramelLauncherState) {
    if (state.importProgress < 0) return
    val percent = state.importProgress.coerceIn(0, 100)
    CaramelModal(true, {}, Modifier.size(360.dp, 260.dp)) {
        Column(
            Modifier.fillMaxWidth().size(360.dp, 260.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator(
                progress = { percent / 100f },
                color = C.Caramel,
                trackColor = C.sheen(0.12f),
                strokeWidth = 6.dp,
                modifier = Modifier.size(96.dp),
            )
            Text(
                "$percent%",
                style = T.Display.copy(
                    fontFamily = FontFamily(Font(R.font.pressstart2p)),
                    fontSize = 26.sp_,
                ),
            )
            Text(
                stringResource(R.string.caramel_import_instance_title),
                style = T.Label,
            )
            Text(
                stringResource(R.string.caramel_import_instance_wait),
                style = T.Caption,
            )
        }
    }
}
