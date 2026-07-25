package fr.leretourdelabete.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val NightInk = Color(0xFF07111D)
val NightBlue = Color(0xFF10263A)
val BloodRed = Color(0xFF9E2531)
val BloodRedBright = Color(0xFFCD4050)
val Bone = Color(0xFFF4E9D3)
val Parchment = Color(0xFFD7C7A6)
val Ash = Color(0xFF9AA8B5)
val MoonYellow = Color(0xFFE3C94F)
val GhoulGreen = Color(0xFF63B978)
val VioletStone = Color(0xFFA777D6)

private val colorScheme = darkColorScheme(
    primary = BloodRedBright,
    onPrimary = Color.White,
    primaryContainer = BloodRed,
    onPrimaryContainer = Bone,
    secondary = Parchment,
    onSecondary = NightInk,
    tertiary = MoonYellow,
    background = NightInk,
    onBackground = Bone,
    surface = Color(0xE60A1825),
    onSurface = Bone,
    surfaceVariant = Color(0xE61A2A38),
    onSurfaceVariant = Parchment,
    outline = Color(0xFF687784),
    error = Color(0xFFFF6B6B),
)

private val typography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 44.sp,
        lineHeight = 48.sp,
        letterSpacing = 0.5.sp,
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 38.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 32.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 23.sp,
        lineHeight = 28.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 24.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 17.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 15.sp,
        lineHeight = 21.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        letterSpacing = 0.2.sp,
    ),
)

@Composable
fun RetourBeteTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content,
    )
}
