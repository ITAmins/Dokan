package com.example.ui.reports

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.AccountingService
import com.example.CsvExporter
import com.example.PdfExporter
import com.example.data.repository.DokanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class ReportSummaryItem(
    val dateKey: String,
    val totalSales: Double,
    val totalPurchases: Double,
    val totalExpenses: Double,
    val grossProfit: Double,
    val netProfit: Double,
    val closingCash: Double
)

data class ReportsUiState(
    val selectedFilterType: String = "WEEKLY", // DAILY, WEEKLY, MONTHLY, ALL
    val reportsList: List<ReportSummaryItem> = emptyList(),
    val totalPeriodSales: Double = 0.0,
    val totalPeriodPurchases: Double = 0.0,
    val totalPeriodExpenses: Double = 0.0,
    val totalPeriodGrossProfit: Double = 0.0,
    val totalPeriodNetProfit: Double = 0.0,
    val isLoading: Boolean = false,
    val exportSuccessMessage: String? = null
)

class ReportsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DokanRepository.getInstance(application)

    private val _uiState = MutableStateFlow(ReportsUiState())
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    init {
        loadReports()
    }

    fun setFilterType(filter: String) {
        _uiState.update { it.copy(selectedFilterType = filter) }
        loadReports()
    }

    fun loadReports() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val activeDates = repository.getActiveDates()
            val list = mutableListOf<ReportSummaryItem>()

            for (dateKey in activeDates) {
                val summary = repository.calculateDailySummary(dateKey)
                list.add(
                    ReportSummaryItem(
                        dateKey = dateKey,
                        totalSales = summary.totalSales,
                        totalPurchases = summary.totalPurchases,
                        totalExpenses = summary.totalCashOutflow,
                        grossProfit = summary.estimatedGrossProfit,
                        netProfit = summary.estimatedNetProfit,
                        closingCash = summary.expectedClosingCash
                    )
                )
            }

            val sales = list.sumOf { it.totalSales }
            val purchases = list.sumOf { it.totalPurchases }
            val expenses = list.sumOf { it.totalExpenses }
            val gross = list.sumOf { it.grossProfit }
            val net = list.sumOf { it.netProfit }

            _uiState.update {
                it.copy(
                    reportsList = list,
                    totalPeriodSales = sales,
                    totalPeriodPurchases = purchases,
                    totalPeriodExpenses = expenses,
                    totalPeriodGrossProfit = gross,
                    totalPeriodNetProfit = net,
                    isLoading = false
                )
            }
        }
    }
}
