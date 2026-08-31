package com.example.data.repository

import android.app.Application
import android.content.Context
import com.example.AccountingService
import com.example.BakiKhataManager
import com.example.BakiModel
import com.example.BakiTransaction
import com.example.ExpenseModel
import com.example.FordiItemModel
import com.example.FordiModel
import com.example.MawaSyncManager
import com.example.StorageManager
import com.example.SupabaseAuthManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Clean Kotlin Repository acting as a unified bridge over existing Java managers & storage.
 * Preserves 100% of offline-first persistence, business logic, sync, and models.
 */
class DokanRepository private constructor(private val application: Application) {

    private val storageManager: StorageManager = StorageManager.getInstance(application)
    private val accountingService: AccountingService = AccountingService.getInstance(application)
    private val syncManager: MawaSyncManager = MawaSyncManager.getInstance(application)
    private val authManager: SupabaseAuthManager = SupabaseAuthManager.getInstance(application)

    companion object {
        @Volatile
        private var instance: DokanRepository? = null

        fun getInstance(application: Application): DokanRepository {
            return instance ?: synchronized(this) {
                instance ?: DokanRepository(application).also { instance = it }
            }
        }
    }

    // ==========================================
    // DATE & CALENDAR MANAGEMENT
    // ==========================================

    fun getTodayDateKey(): String {
        val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.US)
        return dateFormat.format(Date())
    }

    fun getBengaliDayOfWeek(calendar: Calendar): String {
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SUNDAY -> "রবিবার"
            Calendar.MONDAY -> "সোমবার"
            Calendar.TUESDAY -> "মঙ্গলবার"
            Calendar.WEDNESDAY -> "বুধবার"
            Calendar.THURSDAY -> "বৃহস্পতিবার"
            Calendar.FRIDAY -> "শুক্রবার"
            Calendar.SATURDAY -> "শনিবার"
            else -> "দৈনিক দিন"
        }
    }

    // ==========================================
    // DAILY CASH & EXPENSES
    // ==========================================

    suspend fun getExpenses(dateKey: String): List<ExpenseModel> = withContext(Dispatchers.IO) {
        storageManager.loadExpenses(dateKey) ?: emptyList()
    }

    suspend fun saveExpenses(dateKey: String, expenses: List<ExpenseModel>) = withContext(Dispatchers.IO) {
        storageManager.saveExpenses(dateKey, expenses)
        storageManager.saveActiveDate(dateKey)
    }

    suspend fun addExpense(
        dateKey: String,
        name: String,
        amount: Double,
        expenseType: String = ExpenseModel.TYPE_SHOP,
        note: String = ""
    ): Boolean = withContext(Dispatchers.IO) {
        if (name.isBlank() || amount <= 0.0) return@withContext false

        val timeFormat = SimpleDateFormat("hh:mm a", Locale.US)
        val timeStr = timeFormat.format(Date())
        val expType = if (expenseType.isNotBlank()) expenseType else ExpenseModel.TYPE_SHOP
        val legacyType = if (ExpenseModel.TYPE_HOME.equals(expType, ignoreCase = true)) {
            ExpenseModel.TYPE_HOME
        } else {
            ExpenseModel.autoClassifyType(name)
        }

        val newExpense = ExpenseModel(
            UUID.randomUUID().toString(),
            name.trim(),
            amount,
            dateKey,
            timeStr,
            legacyType,
            expType
        )

        val currentList = ArrayList(storageManager.loadExpenses(dateKey) ?: emptyList())
        currentList.add(0, newExpense)
        storageManager.saveExpenses(dateKey, currentList)
        storageManager.saveProductSuggestion(name.trim())
        storageManager.saveActiveDate(dateKey)
        true
    }

    suspend fun deleteExpense(dateKey: String, expenseId: String): Boolean = withContext(Dispatchers.IO) {
        val currentList = ArrayList(storageManager.loadExpenses(dateKey) ?: emptyList())
        val removed = currentList.removeAll { it.id == expenseId }
        if (removed) {
            storageManager.saveExpenses(dateKey, currentList)
        }
        removed
    }

    suspend fun getSabekCash(dateKey: String): Double = withContext(Dispatchers.IO) {
        storageManager.loadSabekCash(dateKey)
    }

    suspend fun setSabekCash(dateKey: String, cash: Double) = withContext(Dispatchers.IO) {
        storageManager.saveSabekCash(dateKey, cash)
        storageManager.saveActiveDate(dateKey)
    }

    suspend fun getAvailableCash(dateKey: String): Double = withContext(Dispatchers.IO) {
        storageManager.loadAvailableCash(dateKey)
    }

    suspend fun setAvailableCash(dateKey: String, cash: Double) = withContext(Dispatchers.IO) {
        storageManager.saveAvailableCash(dateKey, cash)
        storageManager.saveActiveDate(dateKey)
    }

    suspend fun updateAvailableCash(dateKey: String, cash: Double) = setAvailableCash(dateKey, cash)

    suspend fun getSuggestedSabekCash(dateKey: String): Double = withContext(Dispatchers.IO) {
        storageManager.getPreviousDayClosingCash(dateKey)
    }

    // ==========================================
    // ACCOUNTING SERVICE WRAPPER
    // ==========================================

    suspend fun calculateDailySummary(dateKey: String): AccountingService.DailyAccountingSummary =
        withContext(Dispatchers.IO) {
            accountingService.calculateDailySummary(dateKey)
        }

    fun getEstimatedGrossMarginRate(): Double {
        return accountingService.estimatedGrossMarginRate
    }

    suspend fun setEstimatedGrossMarginRate(rate: Double) = withContext(Dispatchers.IO) {
        accountingService.estimatedGrossMarginRate = rate
    }

    // ==========================================
    // BAKI KHATA (দেনা-পাওনা)
    // ==========================================

    suspend fun getAllBakiCustomers(): List<BakiModel> = withContext(Dispatchers.IO) {
        storageManager.loadBakiRecords() ?: emptyList()
    }

    suspend fun saveAllBakiCustomers(list: List<BakiModel>) = withContext(Dispatchers.IO) {
        storageManager.saveBakiRecords(list)
    }

    suspend fun addBakiCustomer(
        name: String,
        phone: String,
        address: String,
        initialDue: Double,
        dueDate: String = ""
    ): BakiModel? = withContext(Dispatchers.IO) {
        if (name.isBlank()) return@withContext null

        val currentList = ArrayList(storageManager.loadBakiRecords() ?: emptyList())
        val dateKey = getTodayDateKey()
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.US)
        val timeStr = timeFormat.format(Date())

        val customerId = UUID.randomUUID().toString()
        val customer = BakiModel(
            customerId,
            name.trim(),
            phone.trim(),
            initialDue,
            dateKey,
            dueDate,
            address.trim()
        )

        if (initialDue > 0) {
            val initialTx = BakiTransaction(
                UUID.randomUUID().toString(),
                dateKey,
                timeStr,
                "BAKI",
                initialDue,
                "প্রারম্ভিক বকেয়া",
                initialDue
            )
            customer.addTransaction(initialTx)
        }

        currentList.add(0, customer)
        storageManager.saveBakiRecords(currentList)
        customer
    }

    suspend fun addBakiTransaction(
        customerId: String,
        amount: Double,
        type: String, // "BAKI" or "JOMA"
        dateKey: String,
        note: String
    ): Boolean = withContext(Dispatchers.IO) {
        if (amount <= 0.0) return@withContext false

        val currentList = ArrayList(storageManager.loadBakiRecords() ?: emptyList())
        val customer = currentList.firstOrNull { it.id == customerId } ?: return@withContext false

        val timeFormat = SimpleDateFormat("hh:mm a", Locale.US)
        val timeStr = timeFormat.format(Date())

        val currentDue = customer.amount
        val newDue = if ("BAKI".equals(type, ignoreCase = true)) {
            currentDue + amount
        } else {
            currentDue - amount
        }

        customer.amount = newDue
        customer.updatedAt = System.currentTimeMillis()

        val tx = BakiTransaction(
            UUID.randomUUID().toString(),
            dateKey,
            timeStr,
            if ("BAKI".equals(type, ignoreCase = true)) "BAKI" else "JOMA",
            amount,
            note.trim(),
            newDue
        )
        customer.addTransaction(tx)

        storageManager.saveBakiRecords(currentList)
        true
    }

    suspend fun getCustomerById(customerId: String): BakiModel? = withContext(Dispatchers.IO) {
        val currentList = storageManager.loadBakiRecords() ?: emptyList()
        currentList.firstOrNull { it.id == customerId }
    }

    suspend fun getTotalBakiDue(): Double = withContext(Dispatchers.IO) {
        val currentList = storageManager.loadBakiRecords() ?: emptyList()
        currentList.sumOf { if (it.amount > 0) it.amount else 0.0 }
    }

    suspend fun deleteCustomer(customerId: String): Boolean = withContext(Dispatchers.IO) {
        val currentList = ArrayList(storageManager.loadBakiRecords() ?: emptyList())
        val removed = currentList.removeAll { it.id == customerId }
        if (removed) {
            storageManager.saveBakiRecords(currentList)
        }
        removed
    }

    // ==========================================
    // FORDI (বাজার ফর্দ)
    // ==========================================

    suspend fun getFordiList(): List<FordiModel> = withContext(Dispatchers.IO) {
        storageManager.loadFordiRecords() ?: emptyList()
    }

    suspend fun saveFordiList(list: List<FordiModel>) = withContext(Dispatchers.IO) {
        storageManager.saveFordiRecords(list)
    }

    // ==========================================
    // SYNC & AUTH
    // ==========================================

    fun getSyncManager(): MawaSyncManager = syncManager
    fun getAuthManager(): SupabaseAuthManager = authManager
    fun getStorageManager(): StorageManager = storageManager

    suspend fun getActiveDates(): List<String> = withContext(Dispatchers.IO) {
        storageManager.activeDates ?: emptyList()
    }
}
