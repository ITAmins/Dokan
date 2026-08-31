package com.example.ui.dailycash

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ExpenseModel
import com.example.data.repository.DokanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DailyCashUiState(
    val activeDateKey: String = "",
    val activeDayOfWeek: String = "",
    val openingCash: Double = 0.0,
    val cashSales: Double = 0.0,
    val bakiCollection: Double = 0.0,
    val totalPurchases: Double = 0.0,
    val totalOperatingExpenses: Double = 0.0,
    val totalHomeExpenses: Double = 0.0,
    val totalCashOutflow: Double = 0.0,
    val expectedClosingCash: Double = 0.0,
    val actualAvailableCash: Double = 0.0,
    val cashDiscrepancy: Double = 0.0,
    val expenses: List<ExpenseModel> = emptyList(),
    val isLoading: Boolean = false
)

class DailyCashViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DokanRepository.getInstance(application)
    private val activeCalendar = Calendar.getInstance()

    private val _uiState = MutableStateFlow(DailyCashUiState())
    val uiState: StateFlow<DailyCashUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.US)
            val dateKey = dateFormat.format(activeCalendar.time)
            val dayOfWeek = repository.getBengaliDayOfWeek(activeCalendar)

            val summary = repository.calculateDailySummary(dateKey)
            val expList = repository.getExpenses(dateKey)

            _uiState.update {
                it.copy(
                    activeDateKey = dateKey,
                    activeDayOfWeek = dayOfWeek,
                    openingCash = summary.openingCash,
                    cashSales = summary.cashSales,
                    bakiCollection = summary.bakiCollection,
                    totalPurchases = summary.totalPurchases,
                    totalOperatingExpenses = summary.totalOperatingExpenses + summary.totalShopExpenses,
                    totalHomeExpenses = summary.totalHomeExpenses,
                    totalCashOutflow = summary.totalCashOutflow,
                    expectedClosingCash = summary.expectedClosingCash,
                    actualAvailableCash = summary.actualAvailableCash,
                    cashDiscrepancy = summary.cashDiscrepancy,
                    expenses = expList,
                    isLoading = false
                )
            }
        }
    }

    fun moveToPreviousDay() {
        activeCalendar.add(Calendar.DAY_OF_YEAR, -1)
        loadData()
    }

    fun moveToNextDay() {
        activeCalendar.add(Calendar.DAY_OF_YEAR, 1)
        loadData()
    }

    fun setDate(year: Int, month: Int, dayOfMonth: Int) {
        activeCalendar.set(Calendar.YEAR, year)
        activeCalendar.set(Calendar.MONTH, month)
        activeCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
        loadData()
    }

    fun setSabekCash(amount: Double) {
        viewModelScope.launch {
            val dateKey = _uiState.value.activeDateKey
            repository.setSabekCash(dateKey, amount)
            loadData()
        }
    }

    fun setAvailableCash(amount: Double) {
        viewModelScope.launch {
            val dateKey = _uiState.value.activeDateKey
            repository.setAvailableCash(dateKey, amount)
            loadData()
        }
    }

    fun addExpense(name: String, amount: Double, type: String = ExpenseModel.TYPE_SHOP, note: String = "") {
        viewModelScope.launch {
            val dateKey = _uiState.value.activeDateKey
            repository.addExpense(dateKey, name, amount, type, note)
            loadData()
        }
    }

    fun deleteExpense(id: String) {
        viewModelScope.launch {
            val dateKey = _uiState.value.activeDateKey
            repository.deleteExpense(dateKey, id)
            loadData()
        }
    }
}
