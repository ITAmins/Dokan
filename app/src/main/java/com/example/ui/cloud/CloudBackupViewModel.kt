package com.example.ui.cloud

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.GoogleSheetsSyncManager
import com.example.StorageManager
import com.example.SupabaseClientConfig
import com.example.data.repository.DokanRepository
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CloudBackupUiState(
    val spreadsheetId: String = "",
    val sheetGid: String = "0",
    val webAppUrl: String = "",
    val lastSyncTime: String = "এখনো সিঙ্ক করা হয়নি",
    val isGoogleSheetsConfigured: Boolean = false,
    val isSupabaseConfigured: Boolean = false,
    val isSyncing: Boolean = false,
    val isRestoring: Boolean = false,
    val statusMessage: String? = null,
    val isErrorMessage: Boolean = false
)

class CloudBackupViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DokanRepository.getInstance(application)
    private val sheetsManager = GoogleSheetsSyncManager.getInstance(application)
    private val storage = StorageManager.getInstance(application)
    private val supabaseManager = SupabaseClientConfig.getInstance(application)

    private val _uiState = MutableStateFlow(CloudBackupUiState())
    val uiState: StateFlow<CloudBackupUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    fun loadSettings() {
        val sheetId = sheetsManager.spreadsheetId
        val gid = sheetsManager.sheetGid
        val url = sheetsManager.webAppUrl
        val lastSync = sheetsManager.lastSyncTime
        val isConfigured = sheetsManager.isConfigured
        val isSupa = supabaseManager.isConfigured

        _uiState.update {
            it.copy(
                spreadsheetId = sheetId,
                sheetGid = gid,
                webAppUrl = url,
                lastSyncTime = lastSync,
                isGoogleSheetsConfigured = isConfigured,
                isSupabaseConfigured = isSupa
            )
        }
    }

    fun saveGoogleSheetConfig(sheetId: String, gid: String, webAppUrl: String) {
        sheetsManager.saveSettings(sheetId, gid, webAppUrl)
        loadSettings()
        _uiState.update { it.copy(statusMessage = "গুগল শিট কনফিগারেশন সংরক্ষিত হয়েছে!", isErrorMessage = false) }
    }

    fun syncToGoogleSheets(context: Context) {
        _uiState.update { it.copy(isSyncing = true, statusMessage = null) }
        sheetsManager.syncData(context, object : GoogleSheetsSyncManager.SyncCallback {
            override fun onSuccess(message: String) {
                val now = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(Date())
                sheetsManager.setLastSyncTime(now)
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        lastSyncTime = now,
                        statusMessage = message,
                        isErrorMessage = false
                    )
                }
            }

            override fun onFailure(error: String) {
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        statusMessage = error,
                        isErrorMessage = true
                    )
                }
            }
        })
    }

    fun restoreFromGoogleSheets(context: Context) {
        _uiState.update { it.copy(isRestoring = true, statusMessage = null) }
        sheetsManager.restoreFromGoogleSheet(context, object : GoogleSheetsSyncManager.DataCallback {
            override fun onSuccess(data: Map<String, Any>) {
                storage.importAllData(data)
                _uiState.update {
                    it.copy(
                        isRestoring = false,
                        statusMessage = "গুগল শিট থেকে সকল হিসাব সফলভাবে রিস্টোর হয়েছে!",
                        isErrorMessage = false
                    )
                }
            }

            override fun onFailure(error: String) {
                _uiState.update {
                    it.copy(
                        isRestoring = false,
                        statusMessage = error,
                        isErrorMessage = true
                    )
                }
            }
        })
    }

    fun createLocalBackupFile(context: Context): File? {
        return try {
            val allData = storage.exportAllData()
            val json = Gson().toJson(allData)
            val dir = context.getExternalFilesDir(null) ?: context.filesDir
            val file = File(dir, "MawaStore_Backup_${System.currentTimeMillis()}.json")
            file.writeText(json)
            _uiState.update { it.copy(statusMessage = "লোকাল ব্যাকআপ তৈরি হয়েছে: ${file.name}", isErrorMessage = false) }
            file
        } catch (e: Exception) {
            _uiState.update { it.copy(statusMessage = "ব্যাকআপ তৈরিতে ত্রুটি: ${e.message}", isErrorMessage = true) }
            null
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }
}
