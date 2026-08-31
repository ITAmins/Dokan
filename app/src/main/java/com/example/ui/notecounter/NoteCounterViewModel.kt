package com.example.ui.notecounter

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.DokanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DenominationItem(
    val noteValue: Int,
    val count: Int = 0
) {
    val totalAmount: Long
        get() = noteValue.toLong() * count
}

data class NoteCounterUiState(
    val denominations: List<DenominationItem> = listOf(
        DenominationItem(1000),
        DenominationItem(500),
        DenominationItem(200),
        DenominationItem(100),
        DenominationItem(50),
        DenominationItem(20),
        DenominationItem(10),
        DenominationItem(5),
        DenominationItem(2),
        DenominationItem(1)
    ),
    val totalNotesCount: Int = 0,
    val grandTotalCash: Double = 0.0,
    val updateCashSuccessMessage: String? = null
)

class NoteCounterViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DokanRepository.getInstance(application)

    private val _uiState = MutableStateFlow(NoteCounterUiState())
    val uiState: StateFlow<NoteCounterUiState> = _uiState.asStateFlow()

    fun updateCount(noteValue: Int, count: Int) {
        _uiState.update { current ->
            val updated = current.denominations.map {
                if (it.noteValue == noteValue) it.copy(count = count.coerceAtLeast(0)) else it
            }
            val totalNotes = updated.sumOf { it.count }
            val grandTotal = updated.sumOf { it.totalAmount }.toDouble()
            current.copy(
                denominations = updated,
                totalNotesCount = totalNotes,
                grandTotalCash = grandTotal
            )
        }
    }

    fun incrementCount(noteValue: Int) {
        val currentCount = _uiState.value.denominations.find { it.noteValue == noteValue }?.count ?: 0
        updateCount(noteValue, currentCount + 1)
    }

    fun decrementCount(noteValue: Int) {
        val currentCount = _uiState.value.denominations.find { it.noteValue == noteValue }?.count ?: 0
        if (currentCount > 0) {
            updateCount(noteValue, currentCount - 1)
        }
    }

    fun clearAll() {
        _uiState.update { current ->
            val cleared = current.denominations.map { it.copy(count = 0) }
            current.copy(
                denominations = cleared,
                totalNotesCount = 0,
                grandTotalCash = 0.0,
                updateCashSuccessMessage = null
            )
        }
    }

    fun applyToAvailableCash(onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val totalCash = _uiState.value.grandTotalCash
            val todayKey = repository.getTodayDateKey()
            repository.updateAvailableCash(todayKey, totalCash)
            _uiState.update { it.copy(updateCashSuccessMessage = "আজকের ক্যাশ হিসেবে ৳$totalCash সেট করা হয়েছে!") }
            onSuccess()
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(updateCashSuccessMessage = null) }
    }
}
