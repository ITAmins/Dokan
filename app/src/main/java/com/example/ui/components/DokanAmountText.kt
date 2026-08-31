package com.example.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.PdfExporter
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.TextPrimary

enum class AmountType {
    INCOME,
    EXPENSE,
    NEUTRAL,
    DUE
}

@Composable
fun DokanAmountText(
    amount: Double,
    modifier: Modifier = Modifier,
    type: AmountType = AmountType.NEUTRAL,
    prefix: String = "৳",
    style: TextStyle = MaterialTheme.typography.titleMedium,
    showSign: Boolean = false,
    fontSize: androidx.compose.ui.unit.TextUnit = 16.sp
) {
    val formattedNumber = try {
        PdfExporter.formatBengaliNumber(Math.abs(amount))
    } catch (e: Exception) {
        String.format("%.2f", Math.abs(amount))
    }

    val signStr = if (showSign) {
        when {
            amount > 0 -> "+"
            amount < 0 -> "-"
            else -> ""
        }
    } else ""

    val displayText = "$signStr$prefix$formattedNumber"

    val color = when (type) {
        AmountType.INCOME -> IncomeGreen
        AmountType.EXPENSE -> ExpenseRed
        AmountType.DUE -> ExpenseRed
        AmountType.NEUTRAL -> MaterialTheme.colorScheme.onSurface
    }

    Text(
        text = displayText,
        modifier = modifier,
        style = style.copy(
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = fontSize
        )
    )
}
