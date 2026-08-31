package com.example.ui.fordi

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ExpenseModel
import com.example.FordiItemModel
import com.example.FordiModel
import com.example.PdfExporter
import com.example.data.repository.DokanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class CatalogItem(
    val name: String,
    val unit: String,
    val defaultPrice: Double,
    val defaultQty: Double = 1.0,
    val category: String = "সাধারণ"
)

data class FordiUiState(
    val fordiList: List<FordiModel> = emptyList(),
    val currentFordi: FordiModel? = null,
    val totalCheckedAmount: Double = 0.0,
    val totalRemainingAmount: Double = 0.0,
    val totalFordiAmount: Double = 0.0,
    val checkedItemsCount: Int = 0,
    val totalItemsCount: Int = 0,
    val budgetAmount: Double = 0.0,
    val isPostedToAccounting: Boolean = false,
    val searchQuery: String = "",
    val matchingSuggestions: List<CatalogItem> = emptyList(),
    val showSmartReminder: Boolean = true,
    val isEnglish: Boolean = false,
    val isLoading: Boolean = false
)

class FordiViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DokanRepository.getInstance(application)

    private val _uiState = MutableStateFlow(FordiUiState())
    val uiState: StateFlow<FordiUiState> = _uiState.asStateFlow()

    val globalCatalog = listOf(
        CatalogItem("লাক্স / ডেটোল সাবান কার্টন", "কার্টন", 4400.0, 2.0, "টয়লেট্রিজ"),
        CatalogItem("হুইল ওয়াশিং পাউডার বস্তা", "বস্তা", 2400.0, 2.0, "ক্লিনিং"),
        CatalogItem("পেঁয়াজের বস্তা (৪০ কেজি)", "বস্তা", 2800.0, 2.0, "কাঁচাবাজার"),
        CatalogItem("আলুর বস্তা (৫০ কেজি)", "বস্তা", 2100.0, 3.0, "কাঁচাবাজার"),
        CatalogItem("মিনিকেট চাল (৫০ কেজি)", "বস্তা", 3450.0, 5.0, "মুদি পাইকারি"),
        CatalogItem("অ্যালুমিনিয়াম ফয়েল পেপার রোল", "রোল", 180.0, 1.0, "স্টেশনারি"),
        CatalogItem("আটা / ময়দা ৫০ কেজি বস্তা", "বস্তা", 2250.0, 1.0, "মুদি পাইকারি"),
        CatalogItem("আস্ত ছোলা বুট", "কেজি", 110.0, 1.0, "ডাল ও শস্য"),
        CatalogItem("আয়োডিনযুক্ত লবণ", "কেজি", 40.0, 1.0, "মসলা ও লবণ"),
        CatalogItem("ইস্পাহানি মির্জাপুর চা পাতা", "প্যাকেট", 110.0, 2.0, "চা ও কফি"),
        CatalogItem("নেসক্যাফে ক্লাসিক কফি জার", "জার", 380.0, 1.0, "চা ও কফি"),
        CatalogItem("সাদা চিনি", "কেজি", 130.0, 3.0, "মুদি"),
        CatalogItem("ডানো ফুল ক্রিম গুঁড়া দুধ", "প্যাকেট", 225.0, 2.0, "দুধ ও দুগ্ধজাত"),
        CatalogItem("লেক্সাস ভেজিটেবল বিস্কুট", "প্যাকেট", 10.0, 5.0, "বিস্কুট ও স্ন্যাকস"),
        CatalogItem("ফেসিয়াল টিস্যু বক্স", "বক্স", 75.0, 4.0, "টিস্যু ও পরিচ্ছন্নতা"),
        CatalogItem("হারপিক টয়লেট ক্লিনার", "বোতল", 160.0, 1.0, "ক্লিনিং"),
        CatalogItem("ভিম ডিশওয়াশ লিকুইড", "বোতল", 130.0, 1.0, "ক্লিনিং"),
        CatalogItem("এ৪ সাইজ প্রিন্টার পেপার রিম", "রিম", 180.0, 2.0, "স্টেশনারি"),
        CatalogItem("সয়াবিন তেল ৫ লিটার", "বোতল", 820.0, 1.0, "তেল"),
        CatalogItem("রসুন (দেশি/আমদানি)", "বস্তা", 4200.0, 1.0, "কাঁচাবাজার"),
        CatalogItem("শুকনা মরিচ বস্তা", "বস্তা", 3650.0, 1.0, "মসলা"),
        CatalogItem("হলুদ গুঁড়া প্যাকেট", "কার্টন", 2500.0, 1.0, "মসলা")
    )

    init {
        loadFordiList()
    }

    fun loadFordiList() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            var list = repository.getFordiList()

            // If empty, initialize rich realistic default lists based on screenshots
            if (list.isEmpty()) {
                list = createInitialPresetFordis()
                repository.saveFordiList(list)
            }

            val activeFordi = list.firstOrNull() ?: createDefaultFordi()
            recalculateUiState(list, activeFordi)
        }
    }

    private fun createInitialPresetFordis(): List<FordiModel> {
        val dateKey = repository.getTodayDateKey()

        // 1. Dokaner Mal Tolar Fordi (Screenshot 1)
        val dokanItems = arrayListOf(
            FordiItemModel(UUID.randomUUID().toString(), "লাক্স / ডেটোল সাবান কার্টন", "কার্টন", 2.0, 4400.0, 0.0).apply { isChecked = false; recalculate() },
            FordiItemModel(UUID.randomUUID().toString(), "হুইল ওয়াশিং পাউডার বস্তা", "বস্তা", 2.0, 2400.0, 0.0).apply { isChecked = false; recalculate() },
            FordiItemModel(UUID.randomUUID().toString(), "পেঁয়াজের বস্তা (৪০ কেজি)", "বস্তা", 2.0, 2800.0, 0.0).apply { isChecked = false; recalculate() },
            FordiItemModel(UUID.randomUUID().toString(), "আলুর বস্তা (৫০ কেজি)", "বস্তা", 3.0, 2100.0, 0.0).apply { isChecked = false; recalculate() },
            FordiItemModel(UUID.randomUUID().toString(), "মিনিকেট চাল (৫০ কেজি)", "বস্তা", 5.0, 3450.0, 0.0).apply { isChecked = false; recalculate() },
            FordiItemModel(UUID.randomUUID().toString(), "সয়াবিন তেল ড্রাম (২০০ লিটার)", "ড্রাম", 1.0, 32000.0, 0.0).apply { isChecked = false; recalculate() },
            FordiItemModel(UUID.randomUUID().toString(), "চিনিগুঁড়া পোলাও চাল", "বস্তা", 1.0, 3800.0, 0.0).apply { isChecked = false; recalculate() },
            FordiItemModel(UUID.randomUUID().toString(), "রসুন (দেশি/আমদানি)", "বস্তা", 1.0, 4200.0, 0.0).apply { isChecked = false; recalculate() },
            FordiItemModel(UUID.randomUUID().toString(), "শুকনা মরিচ বস্তা", "বস্তা", 1.0, 3650.0, 0.0).apply { isChecked = false; recalculate() },
            FordiItemModel(UUID.randomUUID().toString(), "হলুদ গুঁড়া প্যাকেট", "কার্টন", 1.0, 2500.0, 0.0).apply { isChecked = false; recalculate() },
            FordiItemModel(UUID.randomUUID().toString(), "অ্যালুমিনিয়াম ফয়েল ও পলিব্যাগ বান্ডিল", "বান্ডিল", 1.0, 8200.0, 0.0).apply { isChecked = true; actualQuantity = 1.0; recalculate() }
        )
        val dokanFordi = FordiModel(
            UUID.randomUUID().toString(),
            "দোকানের মাল তোলার ফর্দ",
            dateKey,
            dokanItems,
            "#059669"
        ).apply {
            notes = "পাইকারি বাজার থেকে মালামাল তোলার নিয়মিত ফর্দ"
        }

        // 2. Office Bazar (Screenshot 3)
        val officeItems = arrayListOf(
            FordiItemModel(UUID.randomUUID().toString(), "ইস্পাহানি মির্জাপুর চা পাতা", "প্যাকেট", 2.0, 110.0, 0.0).apply { isChecked = true; actualQuantity = 2.0; recalculate() },
            FordiItemModel(UUID.randomUUID().toString(), "নেসক্যাফে ক্লাসিক কফি জার", "জার", 1.0, 380.0, 0.0).apply { isChecked = true; actualQuantity = 1.0; recalculate() },
            FordiItemModel(UUID.randomUUID().toString(), "সাদা চিনি", "কেজি", 3.0, 130.0, 0.0).apply { isChecked = true; actualQuantity = 3.0; recalculate() },
            FordiItemModel(UUID.randomUUID().toString(), "ডানো ফুল ক্রিম গুঁড়া দুধ", "প্যাকেট", 2.0, 225.0, 0.0).apply { isChecked = true; actualQuantity = 2.0; recalculate() },
            FordiItemModel(UUID.randomUUID().toString(), "লেক্সাস ভেজিটেবল বিস্কুট", "প্যাকেট", 5.0, 10.0, 0.0).apply { isChecked = true; actualQuantity = 5.0; recalculate() },
            FordiItemModel(UUID.randomUUID().toString(), "ফেসিয়াল টিস্যু বক্স", "বক্স", 4.0, 75.0, 0.0).apply { isChecked = true; actualQuantity = 4.0; recalculate() },
            FordiItemModel(UUID.randomUUID().toString(), "হারপিক টয়লেট ক্লিনার", "বোতল", 1.0, 160.0, 0.0).apply { isChecked = true; actualQuantity = 1.0; recalculate() },
            FordiItemModel(UUID.randomUUID().toString(), "ভিম ডিশওয়াশ লিকুইড", "বোতল", 1.0, 130.0, 0.0).apply { isChecked = true; actualQuantity = 1.0; recalculate() },
            FordiItemModel(UUID.randomUUID().toString(), "এ৪ সাইজ প্রিন্টার পেপার রিম", "রিম", 2.0, 180.0, 0.0).apply { isChecked = true; actualQuantity = 2.0; recalculate() }
        )
        val officeFordi = FordiModel(
            UUID.randomUUID().toString(),
            "অফিস বাজার",
            dateKey,
            officeItems,
            "#2563EB"
        ).apply {
            notes = "অফিস কিচেনের চা, কফি, চিনি, বিস্কুট ও পরিচ্ছন্নতা ফর্দ।"
        }

        return listOf(dokanFordi, officeFordi)
    }

    private fun createDefaultFordi(): FordiModel {
        val dateKey = repository.getTodayDateKey()
        return FordiModel(
            UUID.randomUUID().toString(),
            "দোকানের মাল তোলার ফর্দ",
            dateKey,
            ArrayList(),
            "#059669"
        )
    }

    private fun recalculateUiState(list: List<FordiModel>, activeFordi: FordiModel) {
        val items = activeFordi.items ?: emptyList()
        val checkedItems = items.filter { it.isChecked }
        val remainingItems = items.filter { !it.isChecked }

        val checkedTotal = checkedItems.sumOf { it.plannedTotal }
        val remainingTotal = remainingItems.sumOf { it.plannedTotal }
        val grandTotal = items.sumOf { it.plannedTotal }

        _uiState.update {
            it.copy(
                fordiList = list,
                currentFordi = activeFordi,
                totalCheckedAmount = checkedTotal,
                totalRemainingAmount = remainingTotal,
                totalFordiAmount = grandTotal,
                checkedItemsCount = checkedItems.size,
                totalItemsCount = items.size,
                isPostedToAccounting = activeFordi.isPostedToAccounting,
                isLoading = false
            )
        }
    }

    fun toggleLanguage() {
        _uiState.update { it.copy(isEnglish = !it.isEnglish) }
    }

    fun setBudget(amount: Double) {
        _uiState.update { it.copy(budgetAmount = amount) }
    }

    fun onSearchQueryChanged(query: String) {
        val trimmed = query.trim()
        val suggestions = if (trimmed.isNotEmpty()) {
            globalCatalog.filter { it.name.contains(trimmed, ignoreCase = true) || it.category.contains(trimmed, ignoreCase = true) }
        } else {
            emptyList()
        }
        _uiState.update {
            it.copy(
                searchQuery = query,
                matchingSuggestions = suggestions
            )
        }
    }

    fun dismissSmartReminder() {
        _uiState.update { it.copy(showSmartReminder = false) }
    }

    fun addItem(name: String, quantity: Double = 1.0, unit: String = "কেজি", price: Double = 0.0) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val current = _uiState.value.currentFordi ?: createDefaultFordi()
            val items = ArrayList(current.items ?: emptyList())

            val newItem = FordiItemModel(
                UUID.randomUUID().toString(),
                name.trim(),
                unit.trim().ifEmpty { "টি" },
                if (quantity > 0) quantity else 1.0,
                price,
                0.0
            )
            newItem.isChecked = false
            newItem.recalculate()
            items.add(0, newItem)
            current.items = items
            current.updatedAt = System.currentTimeMillis()

            saveCurrentFordi(current)
            onSearchQueryChanged("")
        }
    }

    fun quickAddSuggestion(name: String, defaultPrice: Double = 0.0, defaultUnit: String = "টি", quantity: Double = 1.0) {
        addItem(name, quantity, defaultUnit, defaultPrice)
    }

    fun toggleItemChecked(itemId: String, checked: Boolean) {
        viewModelScope.launch {
            val current = _uiState.value.currentFordi ?: return@launch
            val items = current.items ?: return@launch
            val item = items.firstOrNull { it.id == itemId } ?: return@launch

            item.isChecked = checked
            item.actualQuantity = if (checked) item.plannedQuantity else 0.0
            item.recalculate()

            current.updatedAt = System.currentTimeMillis()
            saveCurrentFordi(current)
        }
    }

    fun toggleAllItems(checkAll: Boolean) {
        viewModelScope.launch {
            val current = _uiState.value.currentFordi ?: return@launch
            val items = current.items ?: return@launch

            items.forEach { item ->
                item.isChecked = checkAll
                item.actualQuantity = if (checkAll) item.plannedQuantity else 0.0
                item.recalculate()
            }

            current.updatedAt = System.currentTimeMillis()
            saveCurrentFordi(current)
        }
    }

    fun updateItem(itemId: String, name: String, quantity: Double, unit: String, price: Double) {
        viewModelScope.launch {
            val current = _uiState.value.currentFordi ?: return@launch
            val items = current.items ?: return@launch
            val item = items.firstOrNull { it.id == itemId } ?: return@launch

            item.productName = name.trim()
            item.plannedQuantity = if (quantity > 0) quantity else 1.0
            item.unit = unit.trim().ifEmpty { "টি" }
            item.purchaseRate = price
            item.recalculate()

            current.updatedAt = System.currentTimeMillis()
            saveCurrentFordi(current)
        }
    }

    fun deleteItem(itemId: String) {
        viewModelScope.launch {
            val current = _uiState.value.currentFordi ?: return@launch
            val items = ArrayList(current.items ?: emptyList())
            items.removeAll { it.id == itemId }
            current.items = items
            current.updatedAt = System.currentTimeMillis()

            saveCurrentFordi(current)
        }
    }

    fun createNewFordi(title: String, subtitle: String = "") {
        viewModelScope.launch {
            val dateKey = repository.getTodayDateKey()
            val newFordi = FordiModel(
                UUID.randomUUID().toString(),
                if (title.isNotBlank()) title.trim() else "নতুন বাজার ফর্দ ($dateKey)",
                dateKey,
                ArrayList(),
                "#059669"
            ).apply {
                if (subtitle.isNotBlank()) {
                    notes = subtitle.trim()
                }
            }

            val allLists = ArrayList(_uiState.value.fordiList)
            allLists.add(0, newFordi)
            repository.saveFordiList(allLists)
            recalculateUiState(allLists, newFordi)
        }
    }

    fun selectFordi(fordiId: String) {
        val selected = _uiState.value.fordiList.firstOrNull { it.id == fordiId } ?: return
        recalculateUiState(_uiState.value.fordiList, selected)
    }

    fun deleteFordi(fordiId: String) {
        viewModelScope.launch {
            val allLists = ArrayList(_uiState.value.fordiList)
            allLists.removeAll { it.id == fordiId }
            repository.saveFordiList(allLists)
            val nextFordi = allLists.firstOrNull() ?: createDefaultFordi()
            recalculateUiState(allLists, nextFordi)
        }
    }

    fun completeShoppingAndPostToAccounting(onSuccess: (Double) -> Unit) {
        viewModelScope.launch {
            val current = _uiState.value.currentFordi ?: return@launch
            val items = current.items ?: return@launch

            val boughtTotal = items.filter { it.isChecked }.sumOf { it.plannedTotal }
            val amountToPost = if (boughtTotal > 0) boughtTotal else items.sumOf { it.plannedTotal }

            if (amountToPost > 0) {
                val dateKey = repository.getTodayDateKey()
                repository.addExpense(
                    dateKey = dateKey,
                    name = "দোকানের মাল ক্রয় (${current.title ?: "ফর্দ"})",
                    amount = amountToPost,
                    expenseType = ExpenseModel.TYPE_PURCHASE,
                    note = "ফর্দ থেকে যোগ করা হয়েছে"
                )

                current.isPostedToAccounting = true
                current.postedAmount = amountToPost
                current.postedDate = dateKey
                saveCurrentFordi(current)

                onSuccess(amountToPost)
            }
        }
    }

    fun buildShareText(): String {
        val state = _uiState.value
        val current = state.currentFordi ?: return "ফর্দ খালি"
        val items = current.items ?: emptyList()
        val sb = java.lang.StringBuilder()
        sb.append("📋 ${current.title ?: "বাজার ফর্দ"}\n")
        if (!current.notes.isNullOrBlank()) {
            sb.append("${current.notes}\n")
        }
        sb.append("তারিখ: ${current.date ?: ""}\n")
        sb.append("----------------------------\n")
        items.forEachIndexed { index, item ->
            val checkMark = if (item.isChecked) "☑" else "☐"
            val qty = PdfExporter.formatBengaliNumber(item.plannedQuantity)
            val total = PdfExporter.formatBengaliNumber(item.plannedTotal)
            sb.append("$checkMark ${index + 1}. ${item.productName} ($qty ${item.unit}) - ৳$total\n")
        }
        sb.append("----------------------------\n")
        sb.append("মোট কেনা বাকি: ৳${PdfExporter.formatBengaliNumber(state.totalRemainingAmount)}\n")
        sb.append("সর্বমোট ফর্দ: ৳${PdfExporter.formatBengaliNumber(state.totalFordiAmount)}\n")
        return sb.toString()
    }

    private suspend fun saveCurrentFordi(current: FordiModel) {
        val allLists = ArrayList(_uiState.value.fordiList)
        val idx = allLists.indexOfFirst { it.id == current.id }
        if (idx >= 0) {
            allLists[idx] = current
        } else {
            allLists.add(0, current)
        }

        repository.saveFordiList(allLists)
        recalculateUiState(allLists, current)
    }
}
