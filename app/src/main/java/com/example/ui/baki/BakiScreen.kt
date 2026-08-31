package com.example.ui.baki

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BakiModel
import com.example.BakiTransaction
import com.example.PdfExporter
import com.example.ui.components.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BakiScreen(
    viewModel: BakiViewModel,
    onNavigate: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var showAddDueDialog by remember { mutableStateOf(false) }
    var showReceivePaymentDialog by remember { mutableStateOf(false) }
    var preselectedCustomerIdForTx by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DokanTopBar(
                title = "বাকির খাতা ও দেনা-পাওনা",
                subtitle = "মোট বকেয়া: ৳ ${PdfExporter.formatBengaliNumber(uiState.totalOutstandingDue)} (${uiState.totalDueCustomersCount} জন)",
                onNavigationClick = { onNavigate("open_drawer") },
                onCalculatorClick = { onNavigate("note_counter") },
                onSyncClick = { viewModel.loadBakiData() }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddCustomerDialog = true },
                containerColor = DokanPurplePrimary,
                contentColor = Color.White,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(4.dp)
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "নতুন কাস্টমার যোগ")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // 1. Outstanding Due Hero Card
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    BakiHeroCard(
                        totalDue = uiState.totalOutstandingDue,
                        customerCount = uiState.totalCustomerCount,
                        dueCustomerCount = uiState.totalDueCustomersCount,
                        onAddCustomer = { showAddCustomerDialog = true }
                    )
                }
            }

            // 2. Quick Action Buttons
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    DokanButton(
                        text = "+ বাকি লিখুন",
                        icon = Icons.Default.AddCircleOutline,
                        containerColor = ExpenseRed,
                        height = 44,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            preselectedCustomerIdForTx = null
                            showAddDueDialog = true
                        }
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    DokanButton(
                        text = "+ জমা নিন",
                        icon = Icons.Default.CheckCircleOutline,
                        containerColor = IncomeGreen,
                        height = 44,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            preselectedCustomerIdForTx = null
                            showReceivePaymentDialog = true
                        }
                    )
                }
            }

            // 3. Search Field
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    DokanTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.onSearchQueryChange(it) },
                        placeholder = "কাস্টমারের নাম, ফোন বা ঠিকানা দিয়ে খুঁজুন...",
                        leadingIcon = Icons.Default.Search
                    )
                }
            }

            // 4. Filter Chips
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BakiFilterChip(
                        title = "সকল (${uiState.customers.size})",
                        isSelected = uiState.filterType == "ALL",
                        onClick = { viewModel.setFilterType("ALL") }
                    )
                    BakiFilterChip(
                        title = "বকেয়া আছে (${uiState.totalDueCustomersCount})",
                        isSelected = uiState.filterType == "DUE",
                        onClick = { viewModel.setFilterType("DUE") }
                    )
                    BakiFilterChip(
                        title = "সর্বোচ্চ বকেয়া",
                        isSelected = uiState.filterType == "HIGHEST",
                        onClick = { viewModel.setFilterType("HIGHEST") }
                    )
                    BakiFilterChip(
                        title = "পরিশোধিত",
                        isSelected = uiState.filterType == "PAID",
                        onClick = { viewModel.setFilterType("PAID") }
                    )
                }
            }

            // 5. Customer Cards List
            if (uiState.filteredCustomers.isEmpty()) {
                item {
                    DokanEmptyState(
                        title = "কোনো কাস্টমার পাওয়া যায়নি",
                        message = "নতুন কাস্টমার যোগ করতে নিচের '+' বাটনে চাপুন।",
                        icon = Icons.Default.PeopleOutline
                    )
                }
            } else {
                items(uiState.filteredCustomers, key = { it.id }) { customer ->
                    BakiCustomerCard(
                        customer = customer,
                        onClick = { viewModel.selectCustomer(customer.id) },
                        onAddDue = {
                            preselectedCustomerIdForTx = customer.id
                            showAddDueDialog = true
                        },
                        onReceivePayment = {
                            preselectedCustomerIdForTx = customer.id
                            showReceivePaymentDialog = true
                        },
                        onWhatsApp = { viewModel.sendWhatsAppReminder(context, customer) },
                        onSms = { viewModel.sendSmsReminder(context, customer) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }

    // Modal Sheet for Customer Profile / Ledger Statement
    uiState.selectedCustomer?.let { customer ->
        ModalBottomSheet(
            onDismissRequest = { viewModel.clearSelectedCustomer() },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            CustomerDetailSheetContent(
                customer = customer,
                onAddDue = {
                    preselectedCustomerIdForTx = customer.id
                    showAddDueDialog = true
                },
                onReceivePayment = {
                    preselectedCustomerIdForTx = customer.id
                    showReceivePaymentDialog = true
                },
                onWhatsApp = { viewModel.sendWhatsAppReminder(context, customer) },
                onSms = { viewModel.sendSmsReminder(context, customer) },
                onCall = {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${customer.phone}"))
                    context.startActivity(intent)
                },
                onDelete = {
                    viewModel.deleteCustomer(customer.id)
                },
                onClose = { viewModel.clearSelectedCustomer() }
            )
        }
    }

    // Dialog: Add Customer
    if (showAddCustomerDialog) {
        AddCustomerDialog(
            onDismiss = { showAddCustomerDialog = false },
            onConfirm = { name, phone, address, initialDue ->
                viewModel.addCustomer(name, phone, address, initialDue)
                showAddCustomerDialog = false
            }
        )
    }

    // Dialog: Add Due
    if (showAddDueDialog) {
        BakiTransactionDialog(
            title = "বাকি যুক্ত করুন",
            isDue = true,
            customers = uiState.customers,
            preselectedCustomerId = preselectedCustomerIdForTx,
            onDismiss = { showAddDueDialog = false },
            onConfirm = { customerId, amount, note ->
                viewModel.addDueTransaction(customerId, amount, note)
                showAddDueDialog = false
            }
        )
    }

    // Dialog: Receive Payment
    if (showReceivePaymentDialog) {
        BakiTransactionDialog(
            title = "টাকা জমা / বাকি আদায়",
            isDue = false,
            customers = uiState.customers,
            preselectedCustomerId = preselectedCustomerIdForTx,
            onDismiss = { showReceivePaymentDialog = false },
            onConfirm = { customerId, amount, note ->
                viewModel.receivePaymentTransaction(customerId, amount, note)
                showReceivePaymentDialog = false
            }
        )
    }
}

@Composable
private fun BakiHeroCard(
    totalDue: Double,
    customerCount: Int,
    dueCustomerCount: Int,
    onAddCustomer: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF4338CA), // Deep Indigo
        shadowElevation = 3.dp
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
                        text = "মোট অনাদায়ী বাকি পাওনা",
                        color = Color(0xFFC7D2FE),
                        fontSize = 12.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "৳ ${PdfExporter.formatBengaliNumber(totalDue)}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp
                    )
                }

                Surface(
                    onClick = onAddCustomer,
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White.copy(alpha = 0.2f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "নতুন যোগ",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Divider(color = Color.White.copy(alpha = 0.15f), modifier = Modifier.padding(vertical = 12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "মোট কাস্টমার: ${PdfExporter.formatBengaliNumber(customerCount.toDouble()).replace(".00", "")} জন",
                    color = Color(0xFFE0E7FF),
                    fontSize = 12.5.sp
                )
                Text(
                    text = "বকেয়া রয়েছে: ${PdfExporter.formatBengaliNumber(dueCustomerCount.toDouble()).replace(".00", "")} জনের",
                    color = Color(0xFFFEF08A),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp
                )
            }
        }
    }
}

@Composable
private fun BakiFilterChip(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) DokanPurplePrimary else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (isSelected) DokanPurplePrimary else NotebookCardBorder)
    ) {
        Text(
            text = title,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun BakiCustomerCard(
    customer: BakiModel,
    onClick: () -> Unit,
    onAddDue: () -> Unit,
    onReceivePayment: () -> Unit,
    onWhatsApp: () -> Unit,
    onSms: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasDue = customer.amount > 0
    val initials = (customer.customerName?.take(2) ?: "ক").uppercase()

    DokanCard(
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (hasDue) ExpenseRed.copy(alpha = 0.15f) else IncomeGreen.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        fontWeight = FontWeight.Bold,
                        color = if (hasDue) ExpenseRed else IncomeGreen,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = customer.customerName ?: "নাম নেই",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (!customer.phone.isNullOrBlank()) customer.phone else (customer.details ?: "মোবাইল নম্বর নেই"),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        ),
                        maxLines = 1
                    )
                }

                // Due Amount
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "৳ ${PdfExporter.formatBengaliNumber(customer.amount)}",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = if (hasDue) ExpenseRed else IncomeGreen
                        )
                    )
                    Text(
                        text = if (hasDue) "বাকি পাওনা" else "পরিশোধিত",
                        fontSize = 11.sp,
                        color = if (hasDue) ExpenseRed.copy(alpha = 0.8f) else IncomeGreen.copy(alpha = 0.8f)
                    )
                }
            }

            Divider(modifier = Modifier.padding(vertical = 10.dp), color = NotebookCardBorder.copy(alpha = 0.5f))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (!customer.phone.isNullOrBlank()) {
                        Surface(
                            onClick = onWhatsApp,
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF25D366).copy(alpha = 0.12f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Send, contentDescription = "WhatsApp", tint = Color(0xFF16A34A), modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("WhatsApp", color = Color(0xFF16A34A), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Surface(
                            onClick = onSms,
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Message, contentDescription = "SMS", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("মেসেজ", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        onClick = onAddDue,
                        shape = RoundedCornerShape(8.dp),
                        color = ExpenseRed.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "+ বাকি",
                            color = ExpenseRed,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }

                    Surface(
                        onClick = onReceivePayment,
                        shape = RoundedCornerShape(8.dp),
                        color = IncomeGreen.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "+ জমা",
                            color = IncomeGreen,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerDetailSheetContent(
    customer: BakiModel,
    onAddDue: () -> Unit,
    onReceivePayment: () -> Unit,
    onWhatsApp: () -> Unit,
    onSms: () -> Unit,
    onCall: () -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit
) {
    val hasDue = customer.amount > 0
    val transactions = customer.transactions ?: emptyList()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        // Customer Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = customer.customerName ?: "কাস্টমার বিবরণ",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                )
                if (!customer.phone.isNullOrBlank()) {
                    Text(
                        text = "ফোন: ${customer.phone}",
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
                if (!customer.details.isNullOrBlank()) {
                    Text(
                        text = "ঠিকানা: ${customer.details}",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }
            }

            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "বন্ধ করুন")
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Balance Banner
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = if (hasDue) ExpenseRedBg else IncomeGreenBg,
            border = BorderStroke(1.dp, if (hasDue) ExpenseRed.copy(alpha = 0.3f) else IncomeGreen.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (hasDue) "বর্তমান মোট বকেয়া:" else "বর্তমান স্থিতি:",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Text(
                    text = "৳ ${PdfExporter.formatBengaliNumber(customer.amount)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = if (hasDue) ExpenseRed else IncomeGreen
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Quick Actions Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!customer.phone.isNullOrBlank()) {
                OutlinedButton(
                    onClick = onCall,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("কল", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onWhatsApp,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF16A34A))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("WhatsApp", fontSize = 12.sp, color = Color(0xFF16A34A))
                }
            }

            Button(
                onClick = onAddDue,
                colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("+ বাকি", fontSize = 12.sp)
            }

            Button(
                onClick = onReceivePayment,
                colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("+ জমা", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Ledger History Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "খতিয়ান ও লেনদেন বিবরণী (${transactions.size} টি)",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )

            TextButton(
                onClick = onDelete,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("কাস্টমার ডিলিট", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Transactions List
        if (transactions.isEmpty()) {
            Text(
                text = "কোনো লেনদেন রেকর্ড নেই",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 250.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(transactions.reversed()) { tx ->
                    val isDueTx = "BAKI".equals(tx.type, ignoreCase = true)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (isDueTx) "বাকি যোগ" else "টাকা জমা",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = if (isDueTx) ExpenseRed else IncomeGreen
                                )
                                Text(
                                    text = "${tx.date} • ${tx.time} ${if (!tx.note.isNullOrBlank()) "(${tx.note})" else ""}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${if (isDueTx) "+" else "-"}৳ ${PdfExporter.formatBengaliNumber(tx.amount)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isDueTx) ExpenseRed else IncomeGreen
                                )
                                Text(
                                    text = "স্থিতি: ৳${PdfExporter.formatBengaliNumber(tx.balanceAfter)}",
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddCustomerDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, phone: String, address: String, initialDue: Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var initialDueStr by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "নতুন বাকি কাস্টমার যোগ", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DokanTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "কাস্টমারের নাম *",
                    placeholder = "যেমন: মোঃ রহিম মিয়া"
                )
                DokanTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = "মোবাইল নম্বর",
                    placeholder = "যেমন: 017xxxxxxxx"
                )
                DokanTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = "ঠিকানা / বিবরণ",
                    placeholder = "যেমন: পূর্ব পাড়া"
                )
                DokanAmountInput(
                    value = initialDueStr,
                    onValueChange = { initialDueStr = it },
                    label = "পূর্বের বকেয়া বাকি (যদি থাকে ৳)"
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val due = initialDueStr.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank()) {
                        onConfirm(name, phone, address, due)
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("সংরক্ষণ করুন", fontWeight = FontWeight.Bold, color = DokanPurplePrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বাতিল") }
        }
    )
}

@Composable
private fun BakiTransactionDialog(
    title: String,
    isDue: Boolean,
    customers: List<BakiModel>,
    preselectedCustomerId: String?,
    onDismiss: () -> Unit,
    onConfirm: (customerId: String, amount: Double, note: String) -> Unit
) {
    var selectedCustomerId by remember {
        mutableStateOf(preselectedCustomerId ?: customers.firstOrNull()?.id ?: "")
    }
    var amountStr by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (preselectedCustomerId == null && customers.isNotEmpty()) {
                    Text(text = "কাস্টমার নির্বাচন করুন:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    // Simple drop-down / selection pill list
                    LazyColumn(modifier = Modifier.heightIn(max = 120.dp)) {
                        items(customers) { c ->
                            Surface(
                                onClick = { selectedCustomerId = c.id },
                                shape = RoundedCornerShape(8.dp),
                                color = if (selectedCustomerId == c.id) DokanPurplePrimary.copy(alpha = 0.15f) else Color.Transparent,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = c.customerName ?: "",
                                        fontWeight = if (selectedCustomerId == c.id) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "৳${PdfExporter.formatBengaliNumber(c.amount)}",
                                        color = if (c.amount > 0) ExpenseRed else IncomeGreen,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }

                DokanAmountInput(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = if (isDue) "বাকির পরিমাণ (৳) *" else "জমা টাকার পরিমাণ (৳) *"
                )

                DokanTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = "মন্তব্য / পণ্যের বিবরণ",
                    placeholder = if (isDue) "যেমন: চাল ও ডাল নেওয়া" else "যেমন: নগদ পরিশোধ"
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amount = amountStr.toDoubleOrNull() ?: 0.0
                    if (selectedCustomerId.isNotBlank() && amount > 0.0) {
                        onConfirm(selectedCustomerId, amount, note)
                    }
                },
                enabled = selectedCustomerId.isNotBlank() && (amountStr.toDoubleOrNull() ?: 0.0) > 0.0
            ) {
                Text("নিশ্চিত", fontWeight = FontWeight.Bold, color = if (isDue) ExpenseRed else IncomeGreen)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বাতিল") }
        }
    )
}
