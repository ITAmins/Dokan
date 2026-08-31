package com.example.ui.dailycash

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.PdfExporter
import com.example.ui.components.*
import com.example.ui.theme.*
import java.util.Calendar

@Composable
fun DailyCashScreen(
    viewModel: DailyCashViewModel,
    onNavigate: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showEditSabekDialog by remember { mutableStateOf(false) }
    var showEditCountedCashDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DokanTopBar(
                title = "দৈনিক ক্যাশ হিসাব",
                subtitle = "${uiState.activeDateKey} (${uiState.activeDayOfWeek})",
                onNavigationClick = { onNavigate("open_drawer") },
                onCalculatorClick = { onNavigate("note_counter") },
                onSyncClick = { viewModel.loadData() }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Date Switcher
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, NotebookCardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = { viewModel.moveToPreviousDay() }) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "পূর্ববর্তী", tint = DokanPurplePrimary)
                        }

                        Row(
                            modifier = Modifier.clickable {
                                val cal = Calendar.getInstance()
                                DatePickerDialog(
                                    context,
                                    { _, y, m, d -> viewModel.setDate(y, m, d) },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = DokanPurplePrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${uiState.activeDateKey} (${uiState.activeDayOfWeek})",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                            )
                        }

                        IconButton(onClick = { viewModel.moveToNextDay() }) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "পরবর্তী", tint = DokanPurplePrimary)
                        }
                    }
                }
            }

            // Cash Inflows Card (নগদ আগমন)
            item {
                DokanCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(IncomeGreenBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = IncomeGreen, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "ক্যাশ প্রাপ্তি / জমা (Cash In)",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        CashRowItem(
                            label = "সাবেক ক্যাশ (সকালের শুরু)",
                            amount = uiState.openingCash,
                            isEditable = true,
                            onEditClick = { showEditSabekDialog = true }
                        )

                        Divider(modifier = Modifier.padding(vertical = 8.dp), color = NotebookCardBorder.copy(alpha = 0.5f))

                        CashRowItem(
                            label = "নগদ বিক্রয় (ক্যাশ সেল)",
                            amount = uiState.cashSales
                        )

                        Divider(modifier = Modifier.padding(vertical = 8.dp), color = NotebookCardBorder.copy(alpha = 0.5f))

                        CashRowItem(
                            label = "বাকি আদায় (কাস্টমার জমা)",
                            amount = uiState.bakiCollection
                        )

                        Divider(modifier = Modifier.padding(vertical = 8.dp), color = NotebookCardBorder)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "মোট প্রাপ্ত ক্যাশ তহবিল",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            DokanAmountText(
                                amount = uiState.openingCash + uiState.cashSales + uiState.bakiCollection,
                                type = AmountType.INCOME,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }

            // Cash Outflows Card (ক্যাশ প্রদান / খরচ)
            item {
                DokanCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(ExpenseRedBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "ক্যাশ প্রদান / ব্যয় (Cash Out)",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        CashRowItem(
                            label = "পণ্য ক্রয় (মাল কেনা স্টক)",
                            amount = uiState.totalPurchases
                        )

                        Divider(modifier = Modifier.padding(vertical = 8.dp), color = NotebookCardBorder.copy(alpha = 0.5f))

                        CashRowItem(
                            label = "দোকান পরিচালনা খরচ",
                            amount = uiState.totalOperatingExpenses
                        )

                        Divider(modifier = Modifier.padding(vertical = 8.dp), color = NotebookCardBorder.copy(alpha = 0.5f))

                        CashRowItem(
                            label = "বাড়ির খরচ / উত্তোলন",
                            amount = uiState.totalHomeExpenses
                        )

                        Divider(modifier = Modifier.padding(vertical = 8.dp), color = NotebookCardBorder)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "সর্বমোট ক্যাশ ব্যয়",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            DokanAmountText(
                                amount = uiState.totalCashOutflow,
                                type = AmountType.EXPENSE,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }

            // Closing Cash & Difference Card
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = DokanPurpleDark
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "হিসাব অনুযায়ী সমাপনী ক্যাশ",
                                    color = Color(0xFFDDD6FE),
                                    fontSize = 12.5.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "৳ ${PdfExporter.formatBengaliNumber(uiState.expectedClosingCash)}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 24.sp
                                )
                            }

                            Button(
                                onClick = { showEditCountedCashDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Calculate, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("নোট গণনা", color = Color.White, fontSize = 12.sp)
                                }
                            }
                        }

                        Divider(color = Color.White.copy(alpha = 0.15f), modifier = Modifier.padding(vertical = 12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "গোনা ক্যাশ: ৳${PdfExporter.formatBengaliNumber(uiState.actualAvailableCash)}",
                                color = Color(0xFFE9D5FF),
                                fontSize = 13.sp
                            )

                            if (uiState.actualAvailableCash > 0) {
                                val discrepancy = uiState.actualAvailableCash - uiState.expectedClosingCash
                                val isSurplus = discrepancy >= 0
                                Text(
                                    text = if (isSurplus) "অমিল: +৳${PdfExporter.formatBengaliNumber(discrepancy)} (বাড়তি)" else "অমিল: -৳${PdfExporter.formatBengaliNumber(Math.abs(discrepancy))} (ঘাটতি)",
                                    color = if (isSurplus) IncomeGreenLight else ExpenseRedLight,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog for Sabek Cash
    if (showEditSabekDialog) {
        CashInputDialog(
            title = "সাবেক ক্যাশ আপডেট",
            currentValue = uiState.openingCash,
            onDismiss = { showEditSabekDialog = false },
            onConfirm = {
                viewModel.setSabekCash(it)
                showEditSabekDialog = false
            }
        )
    }

    // Dialog for Actual Counted Cash
    if (showEditCountedCashDialog) {
        CashInputDialog(
            title = "গোনা সমাপনী ক্যাশ আপডেট",
            currentValue = uiState.actualAvailableCash,
            onDismiss = { showEditCountedCashDialog = false },
            onConfirm = {
                viewModel.setAvailableCash(it)
                showEditCountedCashDialog = false
            }
        )
    }
}

@Composable
private fun CashRowItem(
    label: String,
    amount: Double,
    isEditable: Boolean = false,
    onEditClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            if (isEditable && onEditClick != null) {
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "সম্পাদনা",
                    tint = DokanPurplePrimary,
                    modifier = Modifier
                        .size(14.dp)
                        .clickable(onClick = onEditClick)
                )
            }
        }

        DokanAmountText(
            amount = amount,
            type = AmountType.NEUTRAL,
            fontSize = 14.5.sp
        )
    }
}

@Composable
private fun CashInputDialog(
    title: String,
    currentValue: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var amountStr by remember { mutableStateOf(if (currentValue > 0) currentValue.toString() else "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title, fontWeight = FontWeight.Bold) },
        text = {
            DokanAmountInput(
                value = amountStr,
                onValueChange = { amountStr = it },
                label = "টাকার পরিমাণ (৳)"
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(amountStr.toDoubleOrNull() ?: 0.0) }) {
                Text("নিশ্চিত", fontWeight = FontWeight.Bold, color = DokanPurplePrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বাতিল") }
        }
    )
}
