package com.example.ui.baki

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BakiModel
import com.example.BakiTransaction
import com.example.PdfExporter
import com.example.data.repository.DokanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BakiUiState(
    val customers: List<BakiModel> = emptyList(),
    val filteredCustomers: List<BakiModel> = emptyList(),
    val searchQuery: String = "",
    val filterType: String = "ALL", // ALL, DUE, HIGHEST, PAID
    val totalOutstandingDue: Double = 0.0,
    val totalCustomerCount: Int = 0,
    val totalDueCustomersCount: Int = 0,
    val selectedCustomer: BakiModel? = null,
    val isLoading: Boolean = false
)

class BakiViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DokanRepository.getInstance(application)

    private val _uiState = MutableStateFlow(BakiUiState())
    val uiState: StateFlow<BakiUiState> = _uiState.asStateFlow()

    init {
        loadBakiData()
    }

    fun loadBakiData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val list = repository.getAllBakiCustomers()
            val totalDue = list.sumOf { if (it.amount > 0) it.amount else 0.0 }
            val dueCustomers = list.count { it.amount > 0 }

            _uiState.update {
                it.copy(
                    customers = list,
                    filteredCustomers = applyFilterAndSearch(list, it.searchQuery, it.filterType),
                    totalOutstandingDue = totalDue,
                    totalCustomerCount = list.size,
                    totalDueCustomersCount = dueCustomers,
                    isLoading = false
                )
            }

            // If a customer was currently selected, refresh their details
            val currentSelectedId = _uiState.value.selectedCustomer?.id
            if (currentSelectedId != null) {
                selectCustomer(currentSelectedId)
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update {
            it.copy(
                searchQuery = query,
                filteredCustomers = applyFilterAndSearch(it.customers, query, it.filterType)
            )
        }
    }

    fun setFilterType(filter: String) {
        _uiState.update {
            it.copy(
                filterType = filter,
                filteredCustomers = applyFilterAndSearch(it.customers, it.searchQuery, filter)
            )
        }
    }

    private fun applyFilterAndSearch(list: List<BakiModel>, query: String, filter: String): List<BakiModel> {
        var result = list

        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            result = result.filter {
                (it.customerName ?: "").lowercase().contains(q) ||
                (it.phone ?: "").contains(q) ||
                (it.details ?: "").lowercase().contains(q)
            }
        }

        result = when (filter) {
            "DUE" -> result.filter { it.amount > 0 }
            "PAID" -> result.filter { it.amount <= 0 }
            "HIGHEST" -> result.sortedByDescending { it.amount }
            else -> result
        }

        return result
    }

    fun addCustomer(name: String, phone: String, address: String, initialDue: Double, dueDate: String = "") {
        viewModelScope.launch {
            val newCustomer = repository.addBakiCustomer(name, phone, address, initialDue, dueDate)
            if (newCustomer != null) {
                loadBakiData()
            }
        }
    }

    fun addDueTransaction(customerId: String, amount: Double, note: String = "") {
        viewModelScope.launch {
            val dateKey = repository.getTodayDateKey()
            val success = repository.addBakiTransaction(customerId, amount, "BAKI", dateKey, note)
            if (success) {
                loadBakiData()
            }
        }
    }

    fun receivePaymentTransaction(customerId: String, amount: Double, note: String = "") {
        viewModelScope.launch {
            val dateKey = repository.getTodayDateKey()
            val success = repository.addBakiTransaction(customerId, amount, "JOMA", dateKey, note)
            if (success) {
                loadBakiData()
            }
        }
    }

    fun selectCustomer(customerId: String) {
        viewModelScope.launch {
            val customer = repository.getCustomerById(customerId)
            _uiState.update { it.copy(selectedCustomer = customer) }
        }
    }

    fun clearSelectedCustomer() {
        _uiState.update { it.copy(selectedCustomer = null) }
    }

    fun deleteCustomer(customerId: String) {
        viewModelScope.launch {
            val success = repository.deleteCustomer(customerId)
            if (success) {
                loadBakiData()
                clearSelectedCustomer()
            }
        }
    }

    fun getWhatsAppReminderMessage(customer: BakiModel): String {
        val amountStr = PdfExporter.formatBengaliNumber(customer.amount)
        return "আসসালামু আলাইকুম ${customer.customerName} ভাই, আপনার নিকট আমাদের দোকানের বাকি বকেয়া ৳${amountStr} টাকা। অনুগ্রহপূর্বক দ্রুত পরিশোধ করার অনুরোধ রইল। ধন্যবাদ।"
    }

    fun sendWhatsAppReminder(context: Context, customer: BakiModel) {
        val rawPhone = customer.phone ?: ""
        val cleanPhone = rawPhone.replace("+", "").replace("-", "").replace(" ", "").trim()
        val formattedPhone = if (cleanPhone.startsWith("01")) "88$cleanPhone" else cleanPhone
        val message = getWhatsAppReminderMessage(customer)

        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://api.whatsapp.com/send?phone=$formattedPhone&text=${Uri.encode(message)}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "হোয়াটসঅ্যাপ অ্যাপটি পাওয়া যায়নি", Toast.LENGTH_SHORT).show()
        }
    }

    fun sendSmsReminder(context: Context, customer: BakiModel) {
        val message = getWhatsAppReminderMessage(customer)
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:${customer.phone ?: ""}")
                putExtra("sms_body", message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "মেসেজ পাঠানো সম্ভব হয়নি", Toast.LENGTH_SHORT).show()
        }
    }
}
