package com.example.ui.dashboard

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ExpenseModel
import com.example.PdfExporter
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

data class TransactionEntry(
    val id: String,
    val title: String,
    val amount: Double,
    val isIncome: Boolean,
    val category: String,
    val time: String,
    val date: String,
    val note: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    currentDateString: String,
    dayOfWeek: String,
    openingBalance: Double,
    dailyCashSales: Double,
    dailyBakiCollection: Double,
    dailyShopExpenses: Double,
    dailyHomeExpenses: Double,
    expenseList: List<ExpenseModel> = emptyList(),
    onPrevDayClick: () -> Unit = {},
    onNextDayClick: () -> Unit = {},
    onDateSelect: (String) -> Unit = {},
    onAddIncome: (amount: Double, type: String, note: String) -> Unit = { _, _, _ -> },
    onAddExpense: (amount: Double, category: String, isHome: Boolean, note: String) -> Unit = { _, _, _, _ -> },
    onUpdateOpeningBalance: (newBalance: Double) -> Unit = {},
    onDeleteExpense: (expenseId: String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val totalIncome = dailyCashSales + dailyBakiCollection
    val totalExpenses = dailyShopExpenses + dailyHomeExpenses
    val currentBalance = openingBalance + totalIncome - totalExpenses

    var showAddIncomeDialog by remember { mutableStateOf(false) }
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var showEditOpeningCashDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = All, 1 = Income, 2 = Expense
    var searchQuery by remember { mutableStateOf("") }

    val context = LocalContext.current

    // Convert existing expense list to transactions
    val transactions = remember(expenseList, dailyCashSales, dailyBakiCollection, currentDateString) {
        val list = mutableListOf<TransactionEntry>()
        if (dailyCashSales > 0) {
            list.add(
                TransactionEntry(
                    id = "income_cash_sale",
                    title = "নগদ বিক্রয় ক্যাশ",
                    amount = dailyCashSales,
                    isIncome = true,
                    category = "নগদ বিক্রয়",
                    time = "সারাদিনের হিসাব",
                    date = currentDateString,
                    note = "দোকানের নগদ ক্যাশ বিক্রি"
                )
            )
        }
        if (dailyBakiCollection > 0) {
            list.add(
                TransactionEntry(
                    id = "income_baki_col",
                    title = "বকেয়া বাকি আদায়",
                    amount = dailyBakiCollection,
                    isIncome = true,
                    category = "বাকি আদায়",
                    time = "সারাদিনের হিসাব",
                    date = currentDateString,
                    note = "কাস্টমারদের বকেয়া কালেকশন"
                )
            )
        }
        expenseList.forEach { exp ->
            val isHome = ExpenseModel.TYPE_HOME.equals(exp.getType(), ignoreCase = true)
            list.add(
                TransactionEntry(
                    id = exp.getId() ?: UUID.randomUUID().toString(),
                    title = exp.getName() ?: "সাধারণ খরচ",
                    amount = exp.getAmount(),
                    isIncome = false,
                    category = if (isHome) "সংসার খরচ" else "দোকান খরচ",
                    time = exp.getTime() ?: "",
                    date = exp.getDate() ?: currentDateString,
                    note = ""
                )
            )
        }
        list
    }

    val filteredTransactions = remember(transactions, selectedTab, searchQuery) {
        transactions.filter { tx ->
            val matchesTab = when (selectedTab) {
                1 -> tx.isIncome
                2 -> !tx.isIncome
                else -> true
            }
            val matchesSearch = if (searchQuery.isBlank()) true else {
                tx.title.contains(searchQuery, ignoreCase = true) ||
                tx.category.contains(searchQuery, ignoreCase = true) ||
                tx.amount.toString().contains(searchQuery)
            }
            matchesTab && matchesSearch
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("compose_dashboard_screen"),
        containerColor = NotebookBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Notebook Spiral Header & Date Navigator
            item {
                NotebookDateHeader(
                    dateString = currentDateString,
                    dayOfWeek = dayOfWeek,
                    onPrevClick = onPrevDayClick,
                    onNextClick = onNextDayClick,
                    onDateClick = {
                        val c = Calendar.getInstance()
                        try {
                            val parsed = SimpleDateFormat("dd-MM-yyyy", Locale.US).parse(currentDateString)
                            if (parsed != null) c.time = parsed
                        } catch (_: Exception) {}
                        DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                val newDateStr = String.format(Locale.US, "%02d-%02d-%04d", d, m + 1, y)
                                onDateSelect(newDateStr)
                            },
                            c.get(Calendar.YEAR),
                            c.get(Calendar.MONTH),
                            c.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    }
                )
            }

            // 2. Primary Card: Current Cash Balance in Hand
            item {
                CurrentBalanceCard(
                    currentBalance = currentBalance,
                    openingBalance = openingBalance,
                    totalIncome = totalIncome,
                    totalExpenses = totalExpenses,
                    onEditOpeningCash = { showEditOpeningCashDialog = true }
                )
            }

            // 3. Dual Daily Metric Cards: Total Daily Income & Total Daily Expenses
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Total Daily Income Card
                    DailyIncomeCard(
                        modifier = Modifier.weight(1f),
                        totalIncome = totalIncome,
                        cashSales = dailyCashSales,
                        bakiCollection = dailyBakiCollection,
                        onAddClick = { showAddIncomeDialog = true }
                    )

                    // Total Daily Expense Card
                    DailyExpenseCard(
                        modifier = Modifier.weight(1f),
                        totalExpenses = totalExpenses,
                        shopExpenses = dailyShopExpenses,
                        homeExpenses = dailyHomeExpenses,
                        onAddClick = { showAddExpenseDialog = true }
                    )
                }
            }

            // 4. Notebook Ruled Ledger Summary Card (খাতার দৈনন্দিন ব্যালেন্স শিট)
            item {
                NotebookRuledLedgerCard(
                    openingBalance = openingBalance,
                    cashSales = dailyCashSales,
                    bakiCollection = dailyBakiCollection,
                    shopExpenses = dailyShopExpenses,
                    homeExpenses = dailyHomeExpenses,
                    totalIncome = totalIncome,
                    totalExpenses = totalExpenses,
                    currentBalance = currentBalance
                )
            }

            // 5. Section Header & Filter Tabs for Transactions
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp, 20.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(BalanceBlue)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "আজকের লেনদেনের তালিকা",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        Text(
                            text = "${PdfExporter.toBengaliDigits(filteredTransactions.size.toString())} টি এন্ট্রি",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Filter Tab Pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NotebookFilterPill(
                            label = "সকল হিসাব (${transactions.size})",
                            isSelected = selectedTab == 0,
                            onClick = { selectedTab = 0 }
                        )
                        NotebookFilterPill(
                            label = "আয় (${transactions.count { it.isIncome }})",
                            isSelected = selectedTab == 1,
                            selectedColor = IncomeGreen,
                            selectedBg = IncomeGreenBg,
                            onClick = { selectedTab = 1 }
                        )
                        NotebookFilterPill(
                            label = "খরচ (${transactions.count { !it.isIncome }})",
                            isSelected = selectedTab == 2,
                            selectedColor = ExpenseRed,
                            selectedBg = ExpenseRedBg,
                            onClick = { selectedTab = 2 }
                        )
                    }
                }
            }

            // 6. Transaction Items / Empty State
            if (filteredTransactions.isEmpty()) {
                item {
                    NotebookEmptyStateCard(
                        isSearching = searchQuery.isNotBlank(),
                        selectedTab = selectedTab,
                        onAddIncomeClick = { showAddIncomeDialog = true },
                        onAddExpenseClick = { showAddExpenseDialog = true }
                    )
                }
            } else {
                items(filteredTransactions, key = { it.id }) { tx ->
                    NotebookTransactionRowCard(
                        transaction = tx,
                        onDeleteClick = {
                            if (!tx.isIncome) {
                                onDeleteExpense(tx.id)
                            }
                        }
                    )
                }
            }
        }
    }

    // Dialogs
    if (showAddIncomeDialog) {
        AddIncomeDialog(
            onDismiss = { showAddIncomeDialog = false },
            onConfirm = { amount, type, note ->
                onAddIncome(amount, type, note)
                showAddIncomeDialog = false
            }
        )
    }

    if (showAddExpenseDialog) {
        AddExpenseDialog(
            onDismiss = { showAddExpenseDialog = false },
            onConfirm = { amount, cat, isHome, note ->
                onAddExpense(amount, cat, isHome, note)
                showAddExpenseDialog = false
            }
        )
    }

    if (showEditOpeningCashDialog) {
        EditOpeningCashDialog(
            currentValue = openingBalance,
            onDismiss = { showEditOpeningCashDialog = false },
            onConfirm = { newBal ->
                onUpdateOpeningBalance(newBal)
                showEditOpeningCashDialog = false
            }
        )
    }
}

// ==========================================
// 1. NOTEBOOK DATE HEADER COMPONENT
// ==========================================
@Composable
fun NotebookDateHeader(
    dateString: String,
    dayOfWeek: String,
    onPrevClick: () -> Unit,
    onNextClick: () -> Unit,
    onDateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("notebook_date_header"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NotebookPaper),
        border = BorderStroke(1.dp, NotebookCardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Spiral Binder Ring Motif Row
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .background(Color(0xFFF1F5F9))
                    .drawBehind {
                        val holeRadius = 3.dp.toPx()
                        val holeSpacing = 24.dp.toPx()
                        var currentX = 16.dp.toPx()
                        while (currentX < size.width - 16.dp.toPx()) {
                            drawCircle(
                                color = Color(0xFFCBD5E1),
                                radius = holeRadius,
                                center = Offset(currentX, size.height / 2)
                            )
                            currentX += holeSpacing
                        }
                    }
            )

            // Date Navigation Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onPrevClick,
                    modifier = Modifier.testTag("btn_prev_day")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "পূর্ববর্তী দিন",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(onClick = onDateClick)
                        .testTag("btn_select_date"),
                    color = BalanceBlueBg,
                    border = BorderStroke(1.dp, BalanceBlueBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "তারিখ",
                            tint = BalanceBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = PdfExporter.toBengaliDigits(dateString),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = BalanceNavy
                        )
                        if (dayOfWeek.isNotBlank()) {
                            Text(
                                text = "• $dayOfWeek",
                                style = MaterialTheme.typography.bodyMedium,
                                color = BalanceBlue
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onNextClick,
                    modifier = Modifier.testTag("btn_next_day")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "পরবর্তী দিন",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// ==========================================
// 2. HERO: CURRENT BALANCE NOTEBOOK CARD
// ==========================================
@Composable
fun CurrentBalanceCard(
    currentBalance: Double,
    openingBalance: Double,
    totalIncome: Double,
    totalExpenses: Double,
    onEditOpeningCash: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("card_current_balance"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = NotebookPaper),
        border = BorderStroke(1.5.dp, if (currentBalance >= 0) BalanceBlueBorder else ExpenseRedBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            if (currentBalance >= 0) Color(0xFFF8FAFC) else Color(0xFFFFF1F2),
                            Color.White
                        )
                    )
                )
                .padding(18.dp)
        ) {
            // Card Title & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (currentBalance >= 0) BalanceBlueBg else ExpenseRedBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = "ক্যাশ ব্যালেন্স",
                            tint = if (currentBalance >= 0) BalanceBlue else ExpenseRed,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "বর্তমান ক্যাশ ব্যালেন্স (হাতে স্থিতি)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (currentBalance >= 0) IncomeGreenBg else ExpenseRedBg,
                    border = BorderStroke(1.dp, if (currentBalance >= 0) IncomeGreenBorder else ExpenseRedBorder)
                ) {
                    Text(
                        text = if (currentBalance >= 0) "উদ্বৃত্ত / প্লাস" else "ঘাটতি",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (currentBalance >= 0) IncomeGreen else ExpenseRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Large Hero Balance
            Text(
                text = "৳ ${PdfExporter.formatBengaliNumber(currentBalance)}",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = if (currentBalance >= 0) BalanceNavy else ExpenseRed,
                modifier = Modifier.testTag("tv_current_balance_amount")
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Notebook Ledger Calculation Formula Row
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, NotebookCardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Opening / Sabek
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable(onClick = onEditOpeningCash)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "প্রারম্ভিক ক্যাশ",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextMuted
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = TextMuted,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                        Text(
                            text = "৳ ${PdfExporter.formatBengaliNumber(openingBalance)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Text(text = "+", fontWeight = FontWeight.Bold, color = TextMuted, fontSize = 16.sp)

                    // Income
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "মোট আয়",
                            style = MaterialTheme.typography.labelMedium,
                            color = IncomeGreen
                        )
                        Text(
                            text = "৳ ${PdfExporter.formatBengaliNumber(totalIncome)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = IncomeGreen
                        )
                    }

                    Text(text = "−", fontWeight = FontWeight.Bold, color = TextMuted, fontSize = 16.sp)

                    // Expense
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "মোট ব্যয়",
                            style = MaterialTheme.typography.labelMedium,
                            color = ExpenseRed
                        )
                        Text(
                            text = "৳ ${PdfExporter.formatBengaliNumber(totalExpenses)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = ExpenseRed
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. DAILY INCOME NOTEBOOK CARD
// ==========================================
@Composable
fun DailyIncomeCard(
    totalIncome: Double,
    cashSales: Double,
    bakiCollection: Double,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.testTag("card_daily_income"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = NotebookPaper),
        border = BorderStroke(1.dp, IncomeGreenBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(IncomeGreenBg.copy(alpha = 0.4f))
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(IncomeGreenBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = "Income",
                        tint = IncomeGreen,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = IncomeGreen,
                    modifier = Modifier.clickable(onClick = onAddClick)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "আয় যোগ",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.5.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "মোট দৈনিক আয়",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = "৳ ${PdfExporter.formatBengaliNumber(totalIncome)}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = IncomeGreen,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = IncomeGreenBorder.copy(alpha = 0.6f), thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(6.dp))

            // Sub Breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "নগদ বিক্রি:",
                    style = MaterialTheme.typography.labelMedium,
                    fontSize = 11.sp,
                    color = TextSecondary
                )
                Text(
                    text = "৳ ${PdfExporter.formatBengaliNumber(cashSales)}",
                    style = MaterialTheme.typography.labelMedium,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "বাকি আদায়:",
                    style = MaterialTheme.typography.labelMedium,
                    fontSize = 11.sp,
                    color = TextSecondary
                )
                Text(
                    text = "৳ ${PdfExporter.formatBengaliNumber(bakiCollection)}",
                    style = MaterialTheme.typography.labelMedium,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        }
    }
}

// ==========================================
// 4. DAILY EXPENSES NOTEBOOK CARD
// ==========================================
@Composable
fun DailyExpenseCard(
    totalExpenses: Double,
    shopExpenses: Double,
    homeExpenses: Double,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.testTag("card_daily_expense"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = NotebookPaper),
        border = BorderStroke(1.dp, ExpenseRedBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(ExpenseRedBg.copy(alpha = 0.4f))
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(ExpenseRedBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.TrendingDown,
                        contentDescription = "Expense",
                        tint = ExpenseRed,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ExpenseRed,
                    modifier = Modifier.clickable(onClick = onAddClick)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "খরচ যোগ",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.5.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "মোট দৈনিক ব্যয়",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = "৳ ${PdfExporter.formatBengaliNumber(totalExpenses)}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = ExpenseRed,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = ExpenseRedBorder.copy(alpha = 0.6f), thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(6.dp))

            // Sub Breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "দোকান খরচ:",
                    style = MaterialTheme.typography.labelMedium,
                    fontSize = 11.sp,
                    color = TextSecondary
                )
                Text(
                    text = "৳ ${PdfExporter.formatBengaliNumber(shopExpenses)}",
                    style = MaterialTheme.typography.labelMedium,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "সংসার/অন্যান্য:",
                    style = MaterialTheme.typography.labelMedium,
                    fontSize = 11.sp,
                    color = TextSecondary
                )
                Text(
                    text = "৳ ${PdfExporter.formatBengaliNumber(homeExpenses)}",
                    style = MaterialTheme.typography.labelMedium,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        }
    }
}

// ==========================================
// 5. NOTEBOOK RULED LEDGER SHEET CARD
// ==========================================
@Composable
fun NotebookRuledLedgerCard(
    openingBalance: Double,
    cashSales: Double,
    bakiCollection: Double,
    shopExpenses: Double,
    homeExpenses: Double,
    totalIncome: Double,
    totalExpenses: Double,
    currentBalance: Double,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("card_notebook_ruled_ledger"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = NotebookPaper),
        border = BorderStroke(1.dp, NotebookCardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    // Left notebook margin red rule line
                    val redLineX = 24.dp.toPx()
                    drawLine(
                        color = NotebookMarginRed.copy(alpha = 0.45f),
                        start = Offset(redLineX, 0f),
                        end = Offset(redLineX, size.height),
                        strokeWidth = 1.5.dp.toPx()
                    )
                }
                .padding(start = 32.dp, end = 16.dp, top = 16.dp, bottom = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = "Ledger",
                    tint = BalanceNavy,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "আজকের হিসাব খাতার বিবরণী",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = BalanceNavy
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Line 1: Opening
            LedgerLineItem(
                label = "১. প্রারম্ভিক ক্যাশ (সাবেক ক্যাশ)",
                amount = openingBalance,
                amountColor = TextPrimary
            )

            // Line 2: Income (+)
            LedgerLineItem(
                label = "২. (+) মোট দৈনিক ক্যাশ আয় (বিক্রি + আদায়)",
                amount = totalIncome,
                amountColor = IncomeGreen,
                isPlus = true
            )

            // Line 3: Expenses (-)
            LedgerLineItem(
                label = "৩. (−) মোট দৈনিক ব্যয় ও খরচ",
                amount = totalExpenses,
                amountColor = ExpenseRed,
                isMinus = true
            )

            Divider(color = NotebookCardBorder, thickness = 1.2.dp, modifier = Modifier.padding(vertical = 8.dp))

            // Line 4: Current Balance (=)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "হাতে থাকার কথা (সমাপনী ব্যালেন্স)",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = BalanceNavy
                )
                Text(
                    text = "৳ ${PdfExporter.formatBengaliNumber(currentBalance)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (currentBalance >= 0) BalanceBlue else ExpenseRed
                )
            }
        }
    }
}

@Composable
fun LedgerLineItem(
    label: String,
    amount: Double,
    amountColor: Color,
    isPlus: Boolean = false,
    isMinus: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Text(
                text = (if (isPlus) "+ ৳ " else if (isMinus) "− ৳ " else "৳ ") + PdfExporter.formatBengaliNumber(amount),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = amountColor
            )
        }
        Divider(color = NotebookRuledLine, thickness = 0.8.dp)
    }
}

// ==========================================
// 6. NOTEBOOK TRANSACTION ROW CARD
// ==========================================
@Composable
fun NotebookTransactionRowCard(
    transaction: TransactionEntry,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("tx_item_${transaction.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = NotebookPaper),
        border = BorderStroke(1.dp, if (transaction.isIncome) IncomeGreenBorder.copy(alpha = 0.5f) else NotebookCardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Icon Pill
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (transaction.isIncome) IncomeGreenBg else ExpenseRedBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (transaction.isIncome) Icons.Default.ArrowDownward else Icons.Default.Receipt,
                    contentDescription = transaction.category,
                    tint = if (transaction.isIncome) IncomeGreen else ExpenseRed,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Info Column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFF1F5F9)
                    ) {
                        Text(
                            text = transaction.category,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                    if (transaction.time.isNotBlank()) {
                        Text(
                            text = transaction.time,
                            style = MaterialTheme.typography.labelMedium,
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Amount Column
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = (if (transaction.isIncome) "+ ৳ " else "− ৳ ") + PdfExporter.formatBengaliNumber(transaction.amount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (transaction.isIncome) IncomeGreen else ExpenseRed
                )
            }

            // Delete button for custom expenses
            if (!transaction.isIncome) {
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = Color(0xFFCBD5E1),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ==========================================
// 7. FILTER PILL COMPONENT
// ==========================================
@Composable
fun NotebookFilterPill(
    label: String,
    isSelected: Boolean,
    selectedColor: Color = BalanceBlue,
    selectedBg: Color = BalanceBlueBg,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) selectedBg else Color.Transparent,
        border = BorderStroke(1.dp, if (isSelected) selectedColor else NotebookCardBorder)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) selectedColor else TextSecondary
        )
    }
}

// ==========================================
// 8. EMPTY STATE COMPONENT
// ==========================================
@Composable
fun NotebookEmptyStateCard(
    isSearching: Boolean,
    selectedTab: Int,
    onAddIncomeClick: () -> Unit,
    onAddExpenseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NotebookPaper),
        border = BorderStroke(1.dp, NotebookCardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF1F5F9)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ReceiptLong,
                    contentDescription = "Empty",
                    tint = TextMuted,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (isSearching) "কোনো ফলাফল পাওয়া যায়নি" else "এই তারিখে কোনো লেনদেন পাওয়া যায়নি",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "আজকের বিক্রি বা খরচের হিসাব সহজে লিখে রাখুন",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onAddIncomeClick,
                    colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "আয় যোগ করুন", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onAddExpenseClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpenseRed),
                    border = BorderStroke(1.dp, ExpenseRedBorder),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "খরচ যোগ করুন", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ==========================================
// 9. DIALOGS
// ==========================================

@Composable
fun AddIncomeDialog(
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, type: String, note: String) -> Unit
) {
    var amountStr by remember { mutableStateOf("") }
    var incomeType by remember { mutableStateOf("নগদ বিক্রয়") }
    var note by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "দৈনিক আয় / নগদ ক্যাশ যোগ",
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = {
                        amountStr = it
                        isError = false
                    },
                    label = { Text("টাকার পরিমাণ (৳)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = isError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "আয়ের উৎস বেছে নিন:",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NotebookFilterPill(
                        label = "নগদ বিক্রয়",
                        isSelected = incomeType == "নগদ বিক্রয়",
                        selectedColor = IncomeGreen,
                        selectedBg = IncomeGreenBg,
                        onClick = { incomeType = "নগদ বিক্রয়" }
                    )
                    NotebookFilterPill(
                        label = "বাকি আদায়",
                        isSelected = incomeType == "বাকি আদায়",
                        selectedColor = IncomeGreen,
                        selectedBg = IncomeGreenBg,
                        onClick = { incomeType = "বাকি আদায়" }
                    )
                    NotebookFilterPill(
                        label = "অন্যান্য",
                        isSelected = incomeType == "অন্যান্য",
                        selectedColor = IncomeGreen,
                        selectedBg = IncomeGreenBg,
                        onClick = { incomeType = "অন্যান্য" }
                    )
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("বিবরণ / নোট (ঐচ্ছিক)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull()
                    if (amt != null && amt > 0) {
                        onConfirm(amt, incomeType, note)
                    } else {
                        isError = true
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen)
            ) {
                Text("সংরক্ষণ করুন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল", color = TextSecondary)
            }
        }
    )
}

@Composable
fun AddExpenseDialog(
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, category: String, isHome: Boolean, note: String) -> Unit
) {
    var amountStr by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("দোকান খরচ") }
    var isHome by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    val categories = listOf(
        "দোকান খরচ" to false,
        "পণ্য ক্রয়" to false,
        "বিদ্যুৎ ও বিল" to false,
        "কর্মচারী বেতন" to false,
        "বাজার ও খাবার" to true,
        "বাসা ভাড়া" to true,
        "ব্যক্তিগত হাতখরচ" to true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "নতুন খরচ যোগ করুন",
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = {
                        amountStr = it
                        isError = false
                    },
                    label = { Text("টাকার পরিমাণ (৳)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = isError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "ক্যাটাগরি বেছে নিন:",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.take(4).forEach { (cat, homeFlag) ->
                            NotebookFilterPill(
                                label = cat,
                                isSelected = selectedCategory == cat,
                                selectedColor = ExpenseRed,
                                selectedBg = ExpenseRedBg,
                                onClick = {
                                    selectedCategory = cat
                                    isHome = homeFlag
                                }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.drop(4).forEach { (cat, homeFlag) ->
                            NotebookFilterPill(
                                label = cat,
                                isSelected = selectedCategory == cat,
                                selectedColor = ExpenseRed,
                                selectedBg = ExpenseRedBg,
                                onClick = {
                                    selectedCategory = cat
                                    isHome = homeFlag
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("খরচের নাম বা বিবরণ") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull()
                    if (amt != null && amt > 0) {
                        onConfirm(amt, selectedCategory, isHome, note)
                    } else {
                        isError = true
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed)
            ) {
                Text("সংরক্ষণ করুন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল", color = TextSecondary)
            }
        }
    )
}

@Composable
fun EditOpeningCashDialog(
    currentValue: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var amountStr by remember { mutableStateOf(if (currentValue > 0) currentValue.toInt().toString() else "") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "প্রারম্ভিক ক্যাশ (সাবেক ক্যাশ) সংশোধন",
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "আজকের দিন শুরু করার সময় ক্যাশ বাক্সে যে টাকা ছিল তা লিখুন:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = {
                        amountStr = it
                        isError = false
                    },
                    label = { Text("প্রারম্ভিক ক্যাশ (৳)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = isError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    onConfirm(amt)
                },
                colors = ButtonDefaults.buttonColors(containerColor = BalanceBlue)
            ) {
                Text("হালনাগাদ করুন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল", color = TextSecondary)
            }
        }
    )
}
