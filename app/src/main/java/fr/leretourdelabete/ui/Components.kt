package fr.leretourdelabete.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.leretourdelabete.R
import fr.leretourdelabete.audio.AudioRouteState
import fr.leretourdelabete.model.NightColor
import fr.leretourdelabete.ui.theme.BloodRed
import fr.leretourdelabete.ui.theme.BloodRedBright
import fr.leretourdelabete.ui.theme.Bone
import fr.leretourdelabete.ui.theme.GhoulGreen
import fr.leretourdelabete.ui.theme.MoonYellow
import fr.leretourdelabete.ui.theme.NightInk
import fr.leretourdelabete.ui.theme.Parchment

enum class ActionTone {
    PRIMARY,
    SECONDARY,
    DANGER,
    YELLOW,
    GREEN,
    CALL,
}

@Composable
fun GameBackdrop(
    isDay: Boolean = false,
    nightColor: NightColor? = null,
    content: @Composable () -> Unit,
) {
    @DrawableRes val background = if (isDay) {
        R.drawable.bg_village_day
    } else {
        R.drawable.bg_village_night
    }
    val accent = when (nightColor) {
        NightColor.YELLOW -> MoonYellow
        NightColor.GREEN -> GhoulGreen
        null -> BloodRed
    }
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            NightInk.copy(alpha = if (isDay) 0.1f else 0.1f),
                            NightInk.copy(alpha = if (isDay) 0.18f else 0.24f),
                            NightInk.copy(alpha = if (isDay) 0.4f else 0.52f),
                        ),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(accent.copy(alpha = if (isDay) 0.01f else 0.025f)),
        )
        content()
    }
}

@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    containerAlpha: Float = 0.7f,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        color = NightInk.copy(alpha = containerAlpha.coerceIn(0f, 1f)),
        contentColor = Bone,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Parchment.copy(alpha = 0.32f),
        ),
    ) {
        Box(modifier = Modifier.padding(20.dp)) {
            content()
        }
    }
}

@Composable
fun LargeActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: ActionTone = ActionTone.PRIMARY,
    enabled: Boolean = true,
    @DrawableRes iconRes: Int? = null,
    showLabelWithIcon: Boolean = false,
) {
    val container = when (tone) {
        ActionTone.PRIMARY -> BloodRedBright
        ActionTone.SECONDARY -> Color(0xFF273D50)
        ActionTone.DANGER -> Color(0xFF6F1922)
        ActionTone.YELLOW -> MoonYellow
        ActionTone.GREEN -> GhoulGreen
        ActionTone.CALL -> Color.Black
    }
    val contentColor = if (tone == ActionTone.YELLOW) NightInk else Color.White
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = if (tone == ActionTone.CALL) 76.dp else 58.dp),
        shape = RoundedCornerShape(if (tone == ActionTone.CALL) 18.dp else 15.dp),
        border = if (tone == ActionTone.CALL) {
            androidx.compose.foundation.BorderStroke(3.dp, BloodRedBright)
        } else {
            null
        },
        contentPadding = if (tone == ActionTone.CALL) {
            PaddingValues(horizontal = 14.dp, vertical = 6.dp)
        } else {
            PaddingValues(horizontal = 22.dp, vertical = 14.dp)
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = contentColor,
            disabledContainerColor = Color(0xFF26313A),
            disabledContentColor = Color(0xFF7E8992),
        ),
    ) {
        if (iconRes != null) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = label,
                modifier = Modifier.size(if (tone == ActionTone.CALL) 64.dp else 28.dp),
                colorFilter = if (iconRes == R.drawable.ic_bluetooth) {
                    ColorFilter.tint(contentColor)
                } else {
                    null
                },
            )
        }
        if (iconRes == null || showLabelWithIcon) {
            if (iconRes != null) Spacer(Modifier.width(10.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun ToggleChoice(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = BloodRedBright,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) accent else Parchment.copy(alpha = 0.35f),
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) accent.copy(alpha = 0.2f) else NightInk.copy(alpha = 0.4f),
            contentColor = if (selected) Color.White else Parchment,
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(
            text = label,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun Tag(
    label: String,
    color: Color = Parchment,
    modifier: Modifier = Modifier,
) {
    Text(
        text = label,
        color = color,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.13f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(50))
            .padding(horizontal = 11.dp, vertical = 6.dp),
    )
}

@Composable
fun AudioRouteBadge(
    route: AudioRouteState,
    modifier: Modifier = Modifier,
) {
    Tag(
        label = if (route.external) "ENCEINTE · ${route.label}" else "TÉLÉPHONE · ${route.label}",
        color = if (route.external) GhoulGreen else Parchment,
        modifier = modifier,
    )
}

@Composable
fun StatusBanner(
    message: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = Color(0xF2291D20),
        contentColor = Bone,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            BloodRedBright.copy(alpha = 0.7f),
        ),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }
}

@Composable
fun ScreenTitle(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    centered: Boolean = false,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            color = Bone,
            textAlign = if (centered) TextAlign.Center else TextAlign.Start,
        )
        if (subtitle != null) {
            Spacer(Modifier.width(1.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Parchment,
                textAlign = if (centered) TextAlign.Center else TextAlign.Start,
            )
        }
    }
}

fun formatDuration(milliseconds: Long): String {
    val totalSeconds = (milliseconds.coerceAtLeast(0L) + 999L) / 1_000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%02d:%02d".format(minutes, seconds)
}

@Composable
fun NightColorLabel(
    color: NightColor?,
    modifier: Modifier = Modifier,
) {
    val label = when (color) {
        NightColor.YELLOW -> "NUIT JAUNE"
        NightColor.GREEN -> "NUIT VERTE"
        null -> "PREMIÈRE NUIT"
    }
    val accent = when (color) {
        NightColor.YELLOW -> MoonYellow
        NightColor.GREEN -> GhoulGreen
        null -> BloodRedBright
    }
    Tag(label = label, color = accent, modifier = modifier)
}

@Composable
fun InlineLabelValue(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = Parchment, style = MaterialTheme.typography.bodyMedium)
        Text(value, color = Bone, fontWeight = FontWeight.Bold)
    }
}
