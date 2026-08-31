package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = DokanPurplePrimary,
    onPrimary = Color.White,
    primaryContainer = DokanPurpleBg,
    onPrimaryContainer = DokanPurpleDark,
    secondary = IncomeGreen,
    onSecondary = Color.White,
    secondaryContainer = IncomeGreenBg,
    onSecondaryContainer = IncomeGreen,
    tertiary = ExpenseRed,
    onTertiary = Color.White,
    tertiaryContainer = ExpenseRedBg,
    onTertiaryContainer = ExpenseRed,
    background = NotebookBackground,
    onBackground = TextPrimary,
    surface = NotebookPaper,
    onSurface = TextPrimary,
    surfaceVariant = NotebookPaperCream,
    onSurfaceVariant = TextSecondary,
    outline = NotebookCardBorder
)

private val DarkColorScheme = darkColorScheme(
    primary = DokanPurpleLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3B0764),
    onPrimaryContainer = Color(0xFFE9D5FF),
    secondary = IncomeGreenDark,
    onSecondary = Color(0xFF064E3B),
    secondaryContainer = Color(0xFF065F46),
    onSecondaryContainer = Color(0xFFA7F3D0),
    tertiary = ExpenseRedDark,
    onTertiary = Color(0xFF7F1D1D),
    tertiaryContainer = Color(0xFF991B1B),
    onTertiaryContainer = Color(0xFFFECACA),
    background = NotebookBackgroundDark,
    onBackground = TextPrimaryDark,
    surface = NotebookPaperDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = TextSecondaryDark,
    outline = NotebookCardBorderDark
)

@Composable
fun DokanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = NotebookTypography,
        shapes = AppShapes,
        content = content
    )
}

@Composable
fun DailyCashNotebookTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    DokanTheme(darkTheme = darkTheme, content = content)
}
