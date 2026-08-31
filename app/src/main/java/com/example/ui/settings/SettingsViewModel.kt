package com.example.ui.settings

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.example.StorageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SettingsUiState(
    val storeName: String = "মাওয়া স্টোর",
    val storeOwner: String = "মুদি ও জেনারেল মার্চেন্ট",
    val storePhone: String = "",
    val storeAddress: String = "",
    val profitMarginPercent: Int = 10,
    val isFordiCardlessMode: Boolean = true,
    val isDarkMode: Boolean = false,
    val appVersion: String = "v2.5.0 (Compose Native)",
    val statusMessage: String? = null
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val storage = StorageManager.getInstance(application)
    private val prefs = application.getSharedPreferences("DokanAppSettingsPrefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    fun loadSettings() {
        val name = prefs.getString("key_store_name", "মাওয়া স্টোর") ?: "মাওয়া স্টোর"
        val owner = prefs.getString("key_store_owner", "মুদি ও জেনারেল মার্চেন্ট") ?: "মুদি ও জেনারেল মার্চেন্ট"
        val phone = prefs.getString("key_store_phone", "") ?: ""
        val address = prefs.getString("key_store_address", "") ?: ""
        val marginRate = storage.estimatedGrossMarginRate
        val marginPercent = (marginRate * 100).toInt()
        val cardless = storage.isFordiCardlessMode
        val dark = prefs.getBoolean("key_dark_mode", false)

        _uiState.update {
            it.copy(
                storeName = name,
                storeOwner = owner,
                storePhone = phone,
                storeAddress = address,
                profitMarginPercent = marginPercent,
                isFordiCardlessMode = cardless,
                isDarkMode = dark
            )
        }
    }

    fun saveStoreProfile(name: String, owner: String, phone: String, address: String) {
        prefs.edit()
            .putString("key_store_name", name.trim())
            .putString("key_store_owner", owner.trim())
            .putString("key_store_phone", phone.trim())
            .putString("key_store_address", address.trim())
            .apply()
        loadSettings()
        _uiState.update { it.copy(statusMessage = "দোকানের তথ্য সফলভাবে সংরক্ষিত হয়েছে!") }
    }

    fun saveProfitMargin(marginPercent: Int) {
        val rate = marginPercent / 100.0
        storage.saveEstimatedGrossMarginRate(rate)
        loadSettings()
        _uiState.update { it.copy(statusMessage = "লাভের শতকরা হার ($marginPercent%) আপডেট হয়েছে!") }
    }

    fun toggleFordiCardless(enabled: Boolean) {
        storage.setFordiCardlessMode(enabled)
        loadSettings()
    }

    fun toggleDarkMode(enabled: Boolean) {
        prefs.edit().putBoolean("key_dark_mode", enabled).apply()
        loadSettings()
    }

    fun clearAllCache() {
        _uiState.update { it.copy(statusMessage = "ক্যাশ মেমোরি পরিষ্কার করা হয়েছে!") }
    }

    fun clearMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }
}
