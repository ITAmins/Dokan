package com.example.ui.notecounter

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.PdfExporter
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun NoteCounterScreen(
    viewModel: NoteCounterViewModel,
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.updateCashSuccessMessage) {
        uiState.updateCashSuccessMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            DokanTopBar(
                title = "নোট গণনা ও ক্যাশ কাউন্টার",
                subtitle = "টাকার নোট গুনে মোট ক্যাশ হিসাব",
                onNavigationClick = onNavigateBack,
                navigationIcon = Icons.Default.ArrowBack,
                onCalculatorClick = { viewModel.clearAll() }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // 1. Total Count Hero Card
            item {
                NoteCounterHeroCard(
                    totalCash = uiState.grandTotalCash,
                    totalNotes = uiState.totalNotesCount,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }

            // 2. Action Controls (Apply to Daily Cash, Share, Clear)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    DokanButton(
                        text = "ক্যাশে সেট করুন",
                        icon = Icons.Default.CheckCircle,
                        containerColor = IncomeGreen,
                        height = 46,
                        modifier = Modifier.weight(1.2f),
                        onClick = {
                            viewModel.applyToAvailableCash {
                                Toast.makeText(context, "আজকের ক্যাশে টাকা সেট হয়েছে", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    DokanButton(
                        text = "শেয়ার",
                        icon = Icons.Default.Share,
                        containerColor = DokanPurplePrimary,
                        height = 46,
                        modifier = Modifier.weight(0.9f),
                        onClick = {
                            val shareText = buildShareableNoteSummary(uiState)
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, shareText)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "নোট গণনার হিসাব পাঠান"))
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { viewModel.clearAll() },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ExpenseRedBg)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "রিসেট",
                            tint = ExpenseRed
                        )
                    }
                }
            }

            // 3. Denominations List Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "নোটের মান ও সংখ্যা",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    )
                    Text(
                        text = "মোট নোট: ${PdfExporter.toBengaliDigits(uiState.totalNotesCount.toString())} খানা",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            // 4. Denominations Rows
            items(uiState.denominations, key = { it.noteValue }) { denom ->
                DenominationRowItem(
                    item = denom,
                    onCountChanged = { newCount -> viewModel.updateCount(denom.noteValue, newCount) },
                    onIncrement = { viewModel.incrementCount(denom.noteValue) },
                    onDecrement = { viewModel.decrementCount(denom.noteValue) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun NoteCounterHeroCard(
    totalCash: Double,
    totalNotes: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
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
                Text(
                    text = "সর্বমোট ক্যাশ ব্যালেন্স",
                    color = Color(0xFFDDD6FE),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp)
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "${PdfExporter.toBengaliDigits(totalNotes.toString())} টি নোট",
                        color = Color.White,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "৳ ${PdfExporter.formatBengaliNumber(totalCash)}",
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 30.sp
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "ক্যাশ কাউন্টার দিয়ে ড্রয়ারের টাকা গুনে সরাসরি দৈনিক হিসাবে যোগ করতে পারবেন।",
                color = Color(0xFFE9D5FF),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp)
            )
        }
    }
}

@Composable
private fun DenominationRowItem(
    item: DenominationItem,
    onCountChanged: (Int) -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier
) {
    val noteColor = when (item.noteValue) {
        1000 -> Color(0xFF8B5CF6)
        500 -> Color(0xFF10B981)
        200 -> Color(0xFF06B6D4)
        100 -> Color(0xFF3B82F6)
        50 -> Color(0xFFF59E0B)
        20 -> Color(0xFFEC4899)
        10 -> Color(0xFF64748B)
        else -> DokanPurplePrimary
    }

    DokanCard(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Note Badge
            Surface(
                modifier = Modifier.width(68.dp),
                shape = RoundedCornerShape(8.dp),
                color = noteColor.copy(alpha = 0.15f)
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "৳${item.noteValue}",
                        fontWeight = FontWeight.Bold,
                        color = noteColor,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Decrement Button
            IconButton(
                onClick = onDecrement,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(
                    Icons.Default.Remove,
                    contentDescription = "কমান",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Count Input Field
            OutlinedTextField(
                value = if (item.count > 0) item.count.toString() else "",
                onValueChange = { str ->
                    val c = str.filter { it.isDigit() }.toIntOrNull() ?: 0
                    onCountChanged(c)
                },
                placeholder = { Text("০", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .width(60.dp)
                    .height(48.dp),
                textStyle = LocalTextStyle.current.copy(
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                ),
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.width(6.dp))

            // Increment Button
            IconButton(
                onClick = onIncrement,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(DokanPurpleBg)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "বাড়ান",
                    tint = DokanPurplePrimary,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Calculated Amount Subtotal
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "= ৳${PdfExporter.formatBengaliNumber(item.totalAmount.toDouble())}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (item.totalAmount > 0) IncomeGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                )
            }
        }
    }
}

private fun buildShareableNoteSummary(state: NoteCounterUiState): String {
    val sb = StringBuilder()
    sb.append("📋 মাওয়া স্টোর - নগদ নোট গণনার হিসাব:\n\n")
    for (d in state.denominations) {
        if (d.count > 0) {
            sb.append("৳${d.noteValue} × ${d.count} = ৳${d.totalAmount}\n")
        }
    }
    sb.append("\n-----------------------\n")
    sb.append("মোট নোট সংখ্যা: ${state.totalNotesCount} টি\n")
    sb.append("সর্বমোট নগদ ক্যাশ: ৳${PdfExporter.formatBengaliNumber(state.grandTotalCash)}\n")
    return sb.toString()
}
