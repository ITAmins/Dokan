package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = BalanceBlue,
    onPrimary = Color.White,
    primaryContainer = BalanceBlueBg,
    onPrimaryContainer = BalanceNavy,
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

@Composable
fun DailyCashNotebookTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = NotebookTypography,
        content = content
    )
}
