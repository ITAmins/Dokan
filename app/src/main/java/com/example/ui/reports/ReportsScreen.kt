package com.example.ui.reports

import android.content.Intent
import android.widget.Toast
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.CsvExporter
import com.example.MainViewModel
import com.example.PdfExporter
import com.example.ui.components.*
import com.example.ui.theme.*
import java.io.File

@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel,
    onNavigate: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var isExporting by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            DokanTopBar(
                title = "ব্যবসায়িক রিপোর্ট ও হিসাব",
                subtitle = "বিক্রি, লাভ ও সার্বিক আর্থিক চিত্র",
                onNavigationClick = { onNavigate("open_drawer") },
                onCalculatorClick = { onNavigate("note_counter") },
                onSyncClick = { viewModel.loadReports() }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // 1. Period Selector Filter Tabs
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(4.dp)
                ) {
                    ReportFilterPill(
                        title = "সাপ্তাহিক",
                        isSelected = uiState.selectedFilterType == "WEEKLY",
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.setFilterType("WEEKLY") }
                    )
                    ReportFilterPill(
                        title = "মাসিক",
                        isSelected = uiState.selectedFilterType == "MONTHLY",
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.setFilterType("MONTHLY") }
                    )
                    ReportFilterPill(
                        title = "সকল রেকর্ড",
                        isSelected = uiState.selectedFilterType == "ALL",
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.setFilterType("ALL") }
                    )
                }
            }

            // 2. Grand Period Financial Overview Hero Card
            item {
                PeriodHeroReportCard(
                    totalSales = uiState.totalPeriodSales,
                    totalPurchases = uiState.totalPeriodPurchases,
                    totalExpenses = uiState.totalPeriodExpenses,
                    netProfit = uiState.totalPeriodNetProfit,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            // 3. Export PDF & CSV Action Buttons
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    DokanButton(
                        text = "PDF রিপোর্ট ডাউনলোড",
                        icon = Icons.Default.PictureAsPdf,
                        containerColor = DokanPurplePrimary,
                        height = 46,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val daySummaries = uiState.reportsList.map {
                                MainViewModel.DaySummary(
                                    it.dateKey,
                                    it.closingCash,
                                    it.totalExpenses,
                                    0.0,
                                    it.totalSales,
                                    it.netProfit
                                )
                            }
                            val pdfFile = PdfExporter.exportPeriodReportToPdf(
                                context,
                                when (uiState.selectedFilterType) {
                                    "WEEKLY" -> "সাপ্তাহিক হিসাব বিবরণী"
                                    "MONTHLY" -> "মাসিক হিসাব বিবরণী"
                                    else -> "সার্বিক ব্যবসায়িক প্রতিবেদন"
                                },
                                uiState.totalPeriodSales,
                                uiState.totalPeriodExpenses,
                                uiState.totalPeriodNetProfit,
                                daySummaries
                            )
                            if (pdfFile != null && pdfFile.exists()) {
                                openExportedFile(context, pdfFile, "application/pdf")
                            } else {
                                Toast.makeText(context, "PDF তৈরি ব্যর্থ হয়েছে", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    DokanButton(
                        text = "Excel/CSV",
                        icon = Icons.Default.TableChart,
                        containerColor = IncomeGreen,
                        height = 46,
                        modifier = Modifier.weight(0.7f),
                        onClick = {
                            val daySummaries = uiState.reportsList.map {
                                MainViewModel.DaySummary(
                                    it.dateKey,
                                    it.closingCash,
                                    it.totalExpenses,
                                    0.0,
                                    it.totalSales,
                                    it.netProfit
                                )
                            }
                            val csvFile = CsvExporter.exportPeriodCashBookToCsv(
                                context,
                                "ব্যবসায়িক_প্রতিবেদন_${uiState.selectedFilterType}",
                                daySummaries,
                                uiState.totalPeriodSales,
                                uiState.totalPeriodExpenses,
                                uiState.totalPeriodNetProfit
                            )
                            if (csvFile != null && csvFile.exists()) {
                                openExportedFile(context, csvFile, "text/csv")
                            } else {
                                Toast.makeText(context, "CSV তৈরি ব্যর্থ হয়েছে", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }

            // 4. Sales & Profit Visual Bar/Trend Chart
            if (uiState.reportsList.isNotEmpty()) {
                val chartPoints = uiState.reportsList.takeLast(7).map {
                    ChartPoint(label = it.dateKey.take(5), value = it.totalSales)
                }
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
                                    text = "সাম্প্রতিক বিক্রয় বিশ্লেষণ",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.5.sp
                                    )
                                )
                                Text(
                                    text = "দৈনিক নিট বিক্রি (৳)",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            DokanBezierTrendChart(
                                points = chartPoints,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                            )
                        }
                    }
                }
            }

            // 5. Daily Breakdown History List
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "দৈনিক হিসাবের বিস্তারিত খতিয়ান",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    )
                    Text(
                        text = "${PdfExporter.toBengaliDigits(uiState.reportsList.size.toString())} দিনের তথ্য",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    )
                }
            }

            if (uiState.reportsList.isEmpty()) {
                item {
                    DokanEmptyState(
                        title = "কোনো রিপোর্ট পাওয়া যায়নি",
                        message = "হিসাব নথিবদ্ধ থাকলে এখানে দৈনিক খতিয়ান প্রদর্শিত হবে।",
                        icon = Icons.Default.Assessment
                    )
                }
            } else {
                items(uiState.reportsList, key = { it.dateKey }) { report ->
                    ReportSummaryRowItem(
                        item = report,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PeriodHeroReportCard(
    totalSales: Double,
    totalPurchases: Double,
    totalExpenses: Double,
    netProfit: Double,
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
            Text(
                text = "পিরিয়ডের সর্বমোট বিক্রি",
                color = Color(0xFFDDD6FE),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "৳ ${PdfExporter.formatBengaliNumber(totalSales)}",
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp
                )
            )

            Divider(
                color = Color.White.copy(alpha = 0.15f),
                modifier = Modifier.padding(vertical = 12.dp)
            )

            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "পণ্য ক্রয় (স্টক)",
                        color = Color(0xFFE9D5FF),
                        fontSize = 11.sp
                    )
                    Text(
                        text = "৳ ${PdfExporter.formatBengaliNumber(totalPurchases)}",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "দোকান ও অন্যান্য খরচ",
                        color = Color(0xFFE9D5FF),
                        fontSize = 11.sp
                    )
                    Text(
                        text = "৳ ${PdfExporter.formatBengaliNumber(totalExpenses)}",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(modifier = Modifier.weight(1.1f), horizontalAlignment = Alignment.End) {
                    Text(
                        text = "আনুমানিক নিট লাভ",
                        color = IncomeGreenLight,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "৳ ${PdfExporter.formatBengaliNumber(netProfit)}",
                        color = IncomeGreenLight,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ReportSummaryRowItem(
    item: ReportSummaryItem,
    modifier: Modifier = Modifier
) {
    DokanCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = DokanPurplePrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.dateKey,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp
                        )
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = IncomeGreen.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "লাভ: ৳${PdfExporter.formatBengaliNumber(item.netProfit)}",
                        color = IncomeGreen,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = NotebookCardBorder, thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                ReportMetricColumn(
                    title = "মোট বিক্রি",
                    value = item.totalSales,
                    color = BalanceBlue,
                    modifier = Modifier.weight(1f)
                )
                ReportMetricColumn(
                    title = "পণ্য ক্রয়",
                    value = item.totalPurchases,
                    color = DokanPurplePrimary,
                    modifier = Modifier.weight(1f)
                )
                ReportMetricColumn(
                    title = "মোট খরচ",
                    value = item.totalExpenses,
                    color = ExpenseRed,
                    modifier = Modifier.weight(1f)
                )
                ReportMetricColumn(
                    title = "ক্যাশ স্থিতি",
                    value = item.closingCash,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ReportMetricColumn(
    title: String,
    value: Double,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.5.sp
            )
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "৳${PdfExporter.formatBengaliNumber(value)}",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                color = color,
                fontSize = 12.5.sp
            )
        )
    }
}

@Composable
private fun ReportFilterPill(
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

private fun openExportedFile(context: android.content.Context, file: File, mimeType: String) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "রিপোর্ট খুলুন বা শেয়ার করুন"))
    } catch (e: Exception) {
        Toast.makeText(context, "ফাইলটি খোলা সম্ভব হয়নি: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    }
}
