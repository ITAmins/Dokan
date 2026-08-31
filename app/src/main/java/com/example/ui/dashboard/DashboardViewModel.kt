package com.example.ui.dashboard

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.AccountingService
import com.example.ExpenseModel
import com.example.data.repository.DokanRepository
import com.example.ui.components.ChartPoint
import com.example.ui.components.SliceData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DashboardUiState(
    val activeDateKey: String = "",
    val activeDayOfWeek: String = "",
    val openingCash: Double = 0.0,
    val cashSales: Double = 0.0,
    val creditSales: Double = 0.0,
    val totalSales: Double = 0.0,
    val bakiCollection: Double = 0.0,
    val totalPurchases: Double = 0.0,
    val totalShopExpenses: Double = 0.0,
    val totalHomeExpenses: Double = 0.0,
    val totalCashOutflow: Double = 0.0,
    val expectedClosingCash: Double = 0.0,
    val actualAvailableCash: Double = 0.0,
    val estimatedGrossProfit: Double = 0.0,
    val estimatedNetProfit: Double = 0.0,
    val estimatedGrossMarginRate: Double = 0.20,
    val expensesList: List<ExpenseModel> = emptyList(),
    val trendPoints: List<ChartPoint> = emptyList(),
    val purchaseSlices: List<SliceData> = emptyList(),
    val isLoading: Boolean = false,
    val userEmail: String? = null
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DokanRepository.getInstance(application)
    private val activeCalendar = Calendar.getInstance()

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDataForActiveCalendar()
    }

    private fun updateDateStrings() {
        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.US)
        val dateKey = dateFormat.format(activeCalendar.time)
        val dayOfWeek = repository.getBengaliDayOfWeek(activeCalendar)
        _uiState.update { it.copy(activeDateKey = dateKey, activeDayOfWeek = dayOfWeek) }
    }

    fun loadDataForActiveCalendar() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            updateDateStrings()

            val dateKey = _uiState.value.activeDateKey
            val summary = repository.calculateDailySummary(dateKey)
            val expenses = repository.getExpenses(dateKey)
            val authUser = repository.getAuthManager().userEmail

            // Build purchase slices for pie chart
            val purchases = expenses.filter { it.isPurchase }
            val sliceColors = listOf(
                Color(0xFF6D28D9),
                Color(0xFF059669),
                Color(0xFF2563EB),
                Color(0xFFD97706),
                Color(0xFFDC2626)
            )
            val slices = purchases.take(5).mapIndexed { index, exp ->
                SliceData(
                    name = exp.name,
                    value = exp.amount,
                    color = sliceColors[index % sliceColors.size]
                )
            }

            // Build 7-day trend
            val trend = calculateHistoricalTrend()

            _uiState.update {
                it.copy(
                    openingCash = summary.openingCash,
                    cashSales = summary.cashSales,
                    creditSales = summary.creditSales,
                    totalSales = summary.totalSales,
                    bakiCollection = summary.bakiCollection,
                    totalPurchases = summary.totalPurchases,
                    totalShopExpenses = summary.totalShopExpenses,
                    totalHomeExpenses = summary.totalHomeExpenses,
                    totalCashOutflow = summary.totalCashOutflow,
                    expectedClosingCash = summary.expectedClosingCash,
                    actualAvailableCash = summary.actualAvailableCash,
                    estimatedGrossProfit = summary.estimatedGrossProfit,
                    estimatedNetProfit = summary.estimatedNetProfit,
                    estimatedGrossMarginRate = summary.estimatedGrossMarginRate,
                    expensesList = expenses,
                    purchaseSlices = slices,
                    trendPoints = trend,
                    isLoading = false,
                    userEmail = authUser
                )
            }
        }
    }

    private suspend fun calculateHistoricalTrend(): List<ChartPoint> {
        val list = mutableListOf<ChartPoint>()
        val cal = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.US)
        val shortFormat = SimpleDateFormat("dd MMM", Locale("bn", "BD"))

        cal.time = activeCalendar.time
        cal.add(Calendar.DAY_OF_YEAR, -6)

        for (i in 0..6) {
            val key = dateFormat.format(cal.time)
            val label = shortFormat.format(cal.time)
            val daySum = repository.calculateDailySummary(key)
            list.add(ChartPoint(label = label, value = daySum.totalSales))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return list
    }

    fun moveToPreviousDay() {
        activeCalendar.add(Calendar.DAY_OF_YEAR, -1)
        loadDataForActiveCalendar()
    }

    fun moveToNextDay() {
        activeCalendar.add(Calendar.DAY_OF_YEAR, 1)
        loadDataForActiveCalendar()
    }

    fun setDateToToday() {
        activeCalendar.time = Date()
        loadDataForActiveCalendar()
    }

    fun selectDate(year: Int, month: Int, dayOfMonth: Int) {
        activeCalendar.set(Calendar.YEAR, year)
        activeCalendar.set(Calendar.MONTH, month)
        activeCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
        loadDataForActiveCalendar()
    }

    fun selectDateString(dateStr: String) {
        try {
            val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.US)
            val parsed = dateFormat.parse(dateStr)
            if (parsed != null) {
                activeCalendar.time = parsed
                loadDataForActiveCalendar()
            }
        } catch (_: Exception) {}
    }

    fun addExpense(name: String, amount: Double, type: String = ExpenseModel.TYPE_SHOP, note: String = "") {
        viewModelScope.launch {
            val dateKey = _uiState.value.activeDateKey
            val success = repository.addExpense(dateKey, name, amount, type, note)
            if (success) {
                loadDataForActiveCalendar()
            }
        }
    }

    fun updateOpeningCash(newBalance: Double) {
        viewModelScope.launch {
            val dateKey = _uiState.value.activeDateKey
            repository.setSabekCash(dateKey, newBalance)
            loadDataForActiveCalendar()
        }
    }

    fun updateAvailableCash(actualCash: Double) {
        viewModelScope.launch {
            val dateKey = _uiState.value.activeDateKey
            repository.setAvailableCash(dateKey, actualCash)
            loadDataForActiveCalendar()
        }
    }

    fun deleteExpense(expenseId: String) {
        viewModelScope.launch {
            val dateKey = _uiState.value.activeDateKey
            val success = repository.deleteExpense(dateKey, expenseId)
            if (success) {
                loadDataForActiveCalendar()
            }
        }
    }
}
