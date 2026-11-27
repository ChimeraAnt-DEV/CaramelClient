package org.chimeramc.client.ui.compose.caramel.launcher

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.chimeramc.client.BuildConfig
import org.chimeramc.client.R
import org.chimeramc.client.launcher.core.about.AboutFacts
import org.chimeramc.client.launcher.core.about.AboutPeople
import org.chimeramc.client.ui.compose.caramel.C
import org.chimeramc.client.ui.compose.caramel.CaramelAvatarPicture
import org.chimeramc.client.ui.compose.caramel.CaramelDimens
import org.chimeramc.client.ui.compose.caramel.CaramelLauncherState
import org.chimeramc.client.ui.compose.caramel.CaramelModal
import org.chimeramc.client.ui.compose.caramel.GlassPanel
import org.chimeramc.client.ui.compose.caramel.caramelPrefs
import org.chimeramc.client.ui.compose.caramel.StatusLabel
import org.chimeramc.client.ui.compose.caramel.T
import org.chimeramc.client.ui.compose.caramel.sp_

/**
 * The About popup: who makes the client, its channel, when it started and the stories behind it.
 *
 * <p>The app menu's About item used to open the Accounts modal, so pressing "About" showed the
 * account list. This is the screen it should have shown. Every claim is drawn from a real source —
 * the roster in [AboutPeople] (whose avatars are the contributors' own GitHub pictures), the channel
 * and date in [AboutFacts], the version from the running build — rather than typed into the layout,
 * so the popup cannot drift from the project it describes.
 */
@Composable
fun CaramelAboutModal(state: CaramelLauncherState) {
    CaramelModal(state.aboutOpen, { state.aboutOpen = false }, Modifier.size(720.dp, 680.dp)) {
        Column(Modifier.fillMaxSize()) {
            CaramelModalHeader(
                stringResource(R.string.caramel_menu_about),
                stringResource(R.string.caramel_about_sub),
                { state.aboutOpen = false },
            )
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState())
                    .padding(horizontal = 32.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                CaramelAboutIdentity()
                CaramelAboutLinks(state)
                CaramelAboutPeople()
                CaramelAboutFunFacts()
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

/** The mark, the version and the date the client started. */
@Composable
private fun CaramelAboutIdentity() {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Image(
            painterResource(R.drawable.ic_caramel_logo),
            null,
            Modifier.size(84.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.app_name), style = T.Display.copy(fontSize = 22.sp_))
            if (BuildConfig.VERSION_NAME.isNotEmpty()) {
                StatusLabel(stringResource(R.string.changelog_version, BuildConfig.VERSION_NAME), C.Caramel)
            }
            Text(stringResource(R.string.caramel_about_made_on, AboutFacts.MADE_ON), style = T.Body)
        }
    }
}

/**
 * The channel and repository buttons: both open inside the launcher's own browser.
 *
 * <p>Each wears the real brand mark — the YouTube play badge and the GitHub Octocat — rather than a
 * generic globe and code brackets, because the mark is what identifies the destination.
 */
@Composable
private fun CaramelAboutLinks(state: CaramelLauncherState) {
    val channelTitle = stringResource(R.string.caramel_about_channel)
    val repositoryTitle = stringResource(R.string.caramel_about_repository)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        AboutLinkButton(channelTitle, R.drawable.ic_brand_youtube, Color(0xFFFF0033)) {
            state.webTitle = channelTitle
            state.webUrl = AboutFacts.YOUTUBE_URL
        }
        AboutLinkButton(repositoryTitle, R.drawable.ic_brand_github, C.Text) {
            state.webTitle = repositoryTitle
            state.webUrl = AboutFacts.REPOSITORY_URL
        }
    }
}

/** A glass pill carrying a brand glyph and its label. */
@Composable
private fun AboutLinkButton(label: String, brandRes: Int, tint: Color, onClick: () -> Unit) {
    GlassPanel(
        Modifier.height(38.dp)
            .clickable(remember { MutableInteractionSource() }, null, onClick = onClick),
        RoundedCornerShape(CaramelDimens.RadiusButton),
        elevation = 4.dp,
    ) {
        Row(
            Modifier.fillMaxHeight().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Image(
                painterResource(brandRes),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                colorFilter = ColorFilter.tint(tint),
            )
            Text(label, style = T.Label)
        }
    }
}

/** The roster: each person's own GitHub picture, their badge and their role. */
@Composable
private fun CaramelAboutPeople() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.caramel_about_people_title), style = T.Title)
        for (person in AboutPeople.all()) {
            GlassPanel(Modifier.fillMaxWidth(), RoundedCornerShape(18.dp), elevation = 6.dp) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    CaramelAvatarPicture(person.avatarUrl, 52)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                person.name,
                                style = T.Subtitle.copy(fontWeight = FontWeight.SemiBold),
                            )
                            StatusLabel(
                                if (person.role == AboutPeople.Role.OWNER) {
                                    stringResource(R.string.about_owner_badge)
                                } else {
                                    stringResource(R.string.about_lead_badge)
                                },
                                if (person.role == AboutPeople.Role.OWNER) C.Caramel else C.Blue,
                            )
                        }
                        Text(
                            if (person.role == AboutPeople.Role.OWNER) {
                                stringResource(R.string.about_owner_role)
                            } else {
                                stringResource(R.string.about_lead_role)
                            },
                            style = T.Body,
                        )
                    }
                }
            }
        }
    }
}

/**
 * The stories, one at a time.
 *
 * <p>A listed block of facts is a wall of text at the bottom of a modal nobody scrolls to. One fact
 * at a time, cross-faded on a timer, is read. The dots under it say how many there are, so the
 * rotation is visible rather than a surprise.
 */
@Composable
private fun CaramelAboutFunFacts() {
    val facts = remember { AboutFacts.funFactResIds() }
    if (facts.isEmpty()) return
    var index by remember { mutableIntStateOf(0) }
    val reduceMotion = caramelPrefs.motionScale == 0f
    LaunchedEffect(facts.size, reduceMotion) {
        if (reduceMotion) return@LaunchedEffect
        while (true) {
            delay(4600)
            index = (index + 1) % facts.size
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.caramel_about_fun_facts), style = T.Title)
        GlassPanel(Modifier.fillMaxWidth(), RoundedCornerShape(18.dp), elevation = 6.dp) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Crossfade(
                    targetState = index,
                    animationSpec = tween(if (reduceMotion) 0 else 520),
                    label = "about-fact",
                ) { i ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            Modifier.padding(top = 7.dp).size(5.dp)
                                .background(C.Caramel, CircleShape),
                        )
                        Text(
                            stringResource(facts[i % facts.size]),
                            style = T.Body,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    facts.indices.forEach { i ->
                        Box(
                            Modifier.size(if (i == index) 16.dp else 5.dp, 5.dp)
                                .background(
                                    if (i == index) C.Caramel else C.FillStrong,
                                    RoundedCornerShape(3.dp),
                                ),
                        )
                    }
                }
            }
        }
    }
}
