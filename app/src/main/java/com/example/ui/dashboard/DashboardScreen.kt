package com.example.ui.dashboard

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ExpenseModel
import com.example.PdfExporter
import com.example.ui.components.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigate: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var expenseDialogType by remember { mutableStateOf(ExpenseModel.TYPE_SHOP) }
    var showEditOpeningCashDialog by remember { mutableStateOf(false) }
    var showActualCashDialog by remember { mutableStateOf(false) }
    var selectedFilterTab by remember { mutableIntStateOf(0) } // 0: All, 1: Sales, 2: Expenses/Purchases
    var searchQuery by remember { mutableStateOf("") }

    val filteredExpenses = remember(uiState.expensesList, selectedFilterTab, searchQuery) {
        val list = when (selectedFilterTab) {
            1 -> uiState.expensesList.filter { !it.isPurchase && !it.isHomeExpense }
            2 -> uiState.expensesList.filter { it.isPurchase || it.isHomeExpense }
            else -> uiState.expensesList
        }
        if (searchQuery.isBlank()) list else {
            val q = searchQuery.trim().lowercase()
            list.filter { (it.name ?: "").lowercase().contains(q) }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DokanTopBar(
                title = "মাওয়া ক্যাশ খাতা",
                subtitle = "${uiState.activeDateKey} (${uiState.activeDayOfWeek})",
                onNavigationClick = { onNavigate("open_drawer") },
                onCalculatorClick = { onNavigate("note_counter") },
                onSyncClick = { viewModel.loadDataForActiveCalendar() }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // 1. Bengali Date Selector Header
            item {
                DateSelectorHeader(
                    dateStr = uiState.activeDateKey,
                    dayStr = uiState.activeDayOfWeek,
                    onPrevClick = { viewModel.moveToPreviousDay() },
                    onNextClick = { viewModel.moveToNextDay() },
                    onCalendarClick = {
                        val cal = Calendar.getInstance()
                        DatePickerDialog(
                            context,
                            { _, y, m, d -> viewModel.selectDate(y, m, d) },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    onTodayClick = { viewModel.setDateToToday() }
                )
            }

            // 2. Primary Financial Balance Hero Card
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    HeroBalanceCard(
                        openingCash = uiState.openingCash,
                        expectedCash = uiState.expectedClosingCash,
                        actualCash = uiState.actualAvailableCash,
                        onEditOpening = { showEditOpeningCashDialog = true },
                        onEditActual = { showActualCashDialog = true }
                    )
                }
            }

            // 3. Grid of 4 Key Financial Metrics
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        DokanSummaryCard(
                            title = "মোট বিক্রি",
                            amount = uiState.totalSales,
                            subtitle = "নগদ: ৳${PdfExporter.formatBengaliNumber(uiState.cashSales)} | বাকি: ৳${PdfExporter.formatBengaliNumber(uiState.creditSales)}",
                            icon = Icons.Default.TrendingUp,
                            iconBgColor = IncomeGreenBg,
                            iconTintColor = IncomeGreen,
                            type = AmountType.INCOME,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        DokanSummaryCard(
                            title = "পণ্য ক্রয় (স্টক)",
                            amount = uiState.totalPurchases,
                            subtitle = "মাল কেনার ক্যাশ খরচ",
                            icon = Icons.Default.ShoppingCart,
                            iconBgColor = BalanceBlueBg,
                            iconTintColor = BalanceBlue,
                            type = AmountType.NEUTRAL,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        DokanSummaryCard(
                            title = "দোকান ও অন্যান্য খরচ",
                            amount = uiState.totalShopExpenses + uiState.totalHomeExpenses,
                            subtitle = "দোকান: ৳${PdfExporter.formatBengaliNumber(uiState.totalShopExpenses)} | বাড়ি: ৳${PdfExporter.formatBengaliNumber(uiState.totalHomeExpenses)}",
                            icon = Icons.Default.ReceiptLong,
                            iconBgColor = ExpenseRedBg,
                            iconTintColor = ExpenseRed,
                            type = AmountType.EXPENSE,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        DokanSummaryCard(
                            title = "আনুমানিক নিট লাভ",
                            amount = uiState.estimatedNetProfit,
                            subtitle = "মার্জিন: ${(uiState.estimatedGrossMarginRate * 100).toInt()}%",
                            icon = Icons.Default.Star,
                            iconBgColor = AmberGoldBg,
                            iconTintColor = AmberGold,
                            type = if (uiState.estimatedNetProfit >= 0) AmountType.INCOME else AmountType.EXPENSE,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 4. Quick Action Buttons
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    DokanButton(
                        text = "+ খরচ",
                        icon = Icons.Default.RemoveCircleOutline,
                        containerColor = ExpenseRed,
                        height = 44,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            expenseDialogType = ExpenseModel.TYPE_SHOP
                            showAddExpenseDialog = true
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    DokanButton(
                        text = "+ মাল কেনা",
                        icon = Icons.Default.AddShoppingCart,
                        containerColor = BalanceBlue,
                        height = 44,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            expenseDialogType = ExpenseModel.TYPE_PURCHASE
                            showAddExpenseDialog = true
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    DokanButton(
                        text = "+ বাড়ির খরচ",
                        icon = Icons.Default.Home,
                        containerColor = DokanPurplePrimary,
                        height = 44,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            expenseDialogType = ExpenseModel.TYPE_HOME
                            showAddExpenseDialog = true
                        }
                    )
                }
            }

            // 5. 7-Day Trend Chart
            if (uiState.trendPoints.isNotEmpty()) {
                item {
                    DokanCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "গত ৭ দিনের বিক্রয় ট্রেন্ড",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                )
                                Text(
                                    text = "দৈনিক মোট বিক্রি (৳)",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            DokanBezierTrendChart(
                                points = uiState.trendPoints,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                            )
                        }
                    }
                }
            }

            // 6. Purchase Slices Donut Chart
            if (uiState.purchaseSlices.isNotEmpty()) {
                item {
                    DokanCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "কোম্পানি/পণ্য ক্রয় ব্রেকডাউন",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            DokanDonutChart(
                                slices = uiState.purchaseSlices,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // 7. Transaction List Header & Filter Tabs
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(
                        text = "আজকের লেনদেন ও খরচের তালিকা",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(4.dp)
                    ) {
                        FilterPillTab(
                            title = "সকল (${uiState.expensesList.size})",
                            isSelected = selectedFilterTab == 0,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedFilterTab = 0 }
                        )
                        FilterPillTab(
                            title = "দোকান খরচ",
                            isSelected = selectedFilterTab == 1,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedFilterTab = 1 }
                        )
                        FilterPillTab(
                            title = "মাল ক্রয় / বাড়ি",
                            isSelected = selectedFilterTab == 2,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedFilterTab = 2 }
                        )
                    }
                }
            }

            // 8. Expense / Transaction Items
            if (filteredExpenses.isEmpty()) {
                item {
                    DokanEmptyState(
                        title = "কোনো লেনদেন এন্ট্রি নেই",
                        message = "আজকের দিনে নতুন খরচ বা মাল কেনার হিসাব যোগ করতে উপরের বাটন ব্যবহার করুন।",
                        icon = Icons.Default.ReceiptLong
                    )
                }
            } else {
                items(filteredExpenses, key = { it.id }) { expense ->
                    ExpenseRowItem(
                        expense = expense,
                        onDelete = { viewModel.deleteExpense(expense.id) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }

    // Dialog: Add Expense / Purchase
    if (showAddExpenseDialog) {
        AddExpenseDialog(
            type = expenseDialogType,
            onDismiss = { showAddExpenseDialog = false },
            onConfirm = { name, amount, type, note ->
                viewModel.addExpense(name, amount, type, note)
                showAddExpenseDialog = false
            }
        )
    }

    // Dialog: Edit Opening Cash
    if (showEditOpeningCashDialog) {
        EditCashAmountDialog(
            title = "সাবেক ক্যাশ (সকালের শুরু)",
            currentAmount = uiState.openingCash,
            onDismiss = { showEditOpeningCashDialog = false },
            onConfirm = { newAmount ->
                viewModel.updateOpeningCash(newAmount)
                showEditOpeningCashDialog = false
            }
        )
    }

    // Dialog: Edit Actual Available Cash
    if (showActualCashDialog) {
        EditCashAmountDialog(
            title = "হাতে থাকা প্রকৃত ক্যাশ (গোনা টাকা)",
            currentAmount = uiState.actualAvailableCash,
            onDismiss = { showActualCashDialog = false },
            onConfirm = { newAmount ->
                viewModel.updateAvailableCash(newAmount)
                showActualCashDialog = false
            }
        )
    }
}

@Composable
private fun DateSelectorHeader(
    dateStr: String,
    dayStr: String,
    onPrevClick: () -> Unit,
    onNextClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onTodayClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
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
            IconButton(onClick = onPrevClick) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "পূর্ববর্তী দিন", tint = DokanPurplePrimary)
            }

            Row(
                modifier = Modifier.clickable(onClick = onCalendarClick),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.CalendarToday, contentDescription = "ক্যালেন্ডার", tint = DokanPurplePrimary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = dateStr, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp))
                    Text(text = dayStr, style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.5.sp))
                }
            }

            IconButton(onClick = onNextClick) {
                Icon(Icons.Default.ChevronRight, contentDescription = "পরবর্তী দিন", tint = DokanPurplePrimary)
            }
        }
    }
}

@Composable
private fun HeroBalanceCard(
    openingCash: Double,
    expectedCash: Double,
    actualCash: Double,
    onEditOpening: () -> Unit,
    onEditActual: () -> Unit
) {
    val discrepancy = actualCash - expectedCash

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = DokanPurpleDark,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "প্রত্যাশিত ক্যাশ স্থিতি",
                        color = Color(0xFFDDD6FE),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "৳ ${PdfExporter.formatBengaliNumber(expectedCash)}",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 26.sp
                        )
                    )
                }

                Surface(
                    onClick = onEditOpening,
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "সাবেক: ৳${PdfExporter.formatBengaliNumber(openingCash)}",
                            color = Color.White,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Divider(
                color = Color.White.copy(alpha = 0.15f),
                modifier = Modifier.padding(vertical = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "গোনা ক্যাশ: ৳${PdfExporter.formatBengaliNumber(actualCash)}",
                        color = Color(0xFFE9D5FF),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "সম্পাদনা",
                        tint = Color(0xFFDDD6FE),
                        modifier = Modifier
                            .size(14.dp)
                            .clickable(onClick = onEditActual)
                    )
                }

                if (actualCash > 0) {
                    val isSurplus = discrepancy >= 0
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSurplus) IncomeGreen.copy(alpha = 0.25f) else ExpenseRed.copy(alpha = 0.25f)
                    ) {
                        Text(
                            text = if (isSurplus) "+৳${PdfExporter.formatBengaliNumber(discrepancy)} বাড়তি" else "-৳${PdfExporter.formatBengaliNumber(Math.abs(discrepancy))} কম",
                            color = if (isSurplus) IncomeGreenLight else ExpenseRedLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterPillTab(
    title: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(34.dp),
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) DokanPurplePrimary else Color.Transparent
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = title,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun ExpenseRowItem(
    expense: ExpenseModel,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPurchase = expense.isPurchase
    val isHome = expense.isHomeExpense

    val icon = when {
        isPurchase -> Icons.Default.ShoppingCart
        isHome -> Icons.Default.Home
        else -> Icons.Default.ReceiptLong
    }

    val iconColor = when {
        isPurchase -> BalanceBlue
        isHome -> DokanPurplePrimary
        else -> ExpenseRed
    }

    val iconBg = when {
        isPurchase -> BalanceBlueBg
        isHome -> DokanPurpleBg
        else -> ExpenseRedBg
    }

    DokanCard(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                )
                Text(
                    text = "${expense.time} • ${when {
                        isPurchase -> "পণ্য ক্রয় (স্টক)"
                        isHome -> "বাড়ির খরচ"
                        else -> "দোকান খরচ"
                    }}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                )
            }

            DokanAmountText(
                amount = expense.amount,
                type = AmountType.EXPENSE,
                fontSize = 15.sp
            )

            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = "মুছুন",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun AddExpenseDialog(
    type: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, amount: Double, type: String, note: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(type) }

    val title = when (selectedType) {
        ExpenseModel.TYPE_PURCHASE -> "নতুন মাল কেনা / পণ্য ক্রয়"
        ExpenseModel.TYPE_HOME -> "নতুন বাড়ির খরচ"
        else -> "নতুন দোকান খরচ"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                DokanTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = if (selectedType == ExpenseModel.TYPE_PURCHASE) "পণ্যের/কোম্পানির নাম" else "খরচের বিবরণ",
                    placeholder = if (selectedType == ExpenseModel.TYPE_PURCHASE) "যেমন: ইউনিলিভার মাল কেনা" else "যেমন: দোকান ভাড়া, চা-নাস্তা"
                )
                Spacer(modifier = Modifier.height(12.dp))
                DokanAmountInput(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = "টাকার পরিমাণ (৳)"
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amount = amountStr.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank() && amount > 0.0) {
                        onConfirm(name, amount, selectedType, "")
                    }
                },
                enabled = name.isNotBlank() && (amountStr.toDoubleOrNull() ?: 0.0) > 0.0
            ) {
                Text("সংরক্ষণ করুন", fontWeight = FontWeight.Bold, color = DokanPurplePrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        }
    )
}

@Composable
private fun EditCashAmountDialog(
    title: String,
    currentAmount: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var amountStr by remember { mutableStateOf(if (currentAmount > 0) currentAmount.toString() else "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            DokanAmountInput(
                value = amountStr,
                onValueChange = { amountStr = it },
                label = "টাকার পরিমাণ (৳)"
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amount = amountStr.toDoubleOrNull() ?: 0.0
                    onConfirm(amount)
                }
            ) {
                Text("আপডেট করুন", fontWeight = FontWeight.Bold, color = DokanPurplePrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        }
    )
}
