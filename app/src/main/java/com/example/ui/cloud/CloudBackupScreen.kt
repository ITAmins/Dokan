package com.example.ui.cloud

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.GoogleSheetsSyncManager
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun CloudBackupScreen(
    viewModel: CloudBackupViewModel,
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var showConfigDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.statusMessage) {
        uiState.statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            DokanTopBar(
                title = "ক্লাউড ব্যাকআপ ও গুগল শিট",
                subtitle = "ডাটা সুরক্ষা ও নিরাপদ অনলাইন ব্যাকআপ",
                onNavigationClick = onNavigateBack,
                navigationIcon = Icons.Default.ArrowBack
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // 1. Cloud Status Hero Card
            item {
                CloudHeroCard(
                    lastSyncTime = uiState.lastSyncTime,
                    isConfigured = uiState.isGoogleSheetsConfigured,
                    isSyncing = uiState.isSyncing,
                    onSyncNow = { viewModel.syncToGoogleSheets(context) },
                    onOpenSettings = { showConfigDialog = true },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }

            // 2. Google Sheets Section Card
            item {
                DokanCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(IncomeGreenBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.TableChart, contentDescription = null, tint = IncomeGreen)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "গুগল শিট অটো-সিঙ্ক",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                )
                                Text(
                                    text = "আপনার গুগল স্প্রেডশিটে খাতার ডাটা ব্যাকআপ রাখুন",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.5.sp
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Divider(color = NotebookCardBorder, thickness = 0.8.dp)
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            DokanButton(
                                text = if (uiState.isSyncing) "সিঙ্ক হচ্ছে..." else "এখনই সিঙ্ক করুন",
                                icon = Icons.Default.CloudUpload,
                                containerColor = IncomeGreen,
                                height = 44,
                                enabled = !uiState.isSyncing && !uiState.isRestoring,
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.syncToGoogleSheets(context) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            DokanButton(
                                text = "শিট খুলুন",
                                icon = Icons.Default.OpenInNew,
                                containerColor = DokanPurplePrimary,
                                height = 44,
                                modifier = Modifier.weight(0.8f),
                                onClick = {
                                    GoogleSheetsSyncManager.getInstance(context).openSheetInBrowser(context)
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = { viewModel.restoreFromGoogleSheets(context) },
                            modifier = Modifier.fillMaxWidth().height(42.dp),
                            shape = RoundedCornerShape(10.dp),
                            enabled = !uiState.isSyncing && !uiState.isRestoring
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("গুগল শিট থেকে ডাটা ফিরিয়ে আনুন (Restore)", fontSize = 12.5.sp)
                        }
                    }
                }
            }

            // 3. Local JSON File Backup Section
            item {
                DokanCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(BalanceBlueBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.SaveAlt, contentDescription = null, tint = BalanceBlue)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "অফলাইন লোকাল ফাইল ব্যাকআপ",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                )
                                Text(
                                    text = "ফোনের মেমরিতে ব্যাকআপ ফাইল সেভ বা শেয়ার করুন",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.5.sp
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        DokanButton(
                            text = "ব্যাকআপ ফাইল তৈরি ও শেয়ার করুন",
                            icon = Icons.Default.Share,
                            containerColor = BalanceBlue,
                            height = 44,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                val file = viewModel.createLocalBackupFile(context)
                                if (file != null && file.exists()) {
                                    try {
                                        val uri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            file
                                        )
                                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "application/json"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, "ব্যাকআপ ফাইল পাঠান"))
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "শেয়ার ব্যর্থ: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Sheet Config Dialog
    if (showConfigDialog) {
        GoogleSheetConfigDialog(
            currentSheetId = uiState.spreadsheetId,
            currentGid = uiState.sheetGid,
            currentWebAppUrl = uiState.webAppUrl,
            onDismiss = { showConfigDialog = false },
            onSave = { sheetId, gid, webAppUrl ->
                viewModel.saveGoogleSheetConfig(sheetId, gid, webAppUrl)
                showConfigDialog = false
            }
        )
    }
}

@Composable
private fun CloudHeroCard(
    lastSyncTime: String,
    isConfigured: Boolean,
    isSyncing: Boolean,
    onSyncNow: () -> Unit,
    onOpenSettings: () -> Unit,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CloudDone,
                        contentDescription = null,
                        tint = if (isConfigured) IncomeGreenLight else AmberGold,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isConfigured) "ক্লাউড সিঙ্ক সক্রিয়" else "সেটআপ সম্পন্ন করুন",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    )
                }

                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "সেটিংস", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "সর্বশেষ সিঙ্ক: $lastSyncTime",
                color = Color(0xFFE9D5FF),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onSyncNow,
                enabled = !isSyncing,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = DokanPurpleDark
                ),
                modifier = Modifier.fillMaxWidth().height(42.dp)
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = DokanPurpleDark)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("সিঙ্ক করা হচ্ছে...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("এখনই ক্লাউডে আপলোড করুন", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun GoogleSheetConfigDialog(
    currentSheetId: String,
    currentGid: String,
    currentWebAppUrl: String,
    onDismiss: () -> Unit,
    onSave: (sheetId: String, gid: String, webAppUrl: String) -> Unit
) {
    var sheetId by remember { mutableStateOf(currentSheetId) }
    var gid by remember { mutableStateOf(currentGid) }
    var webAppUrl by remember { mutableStateOf(currentWebAppUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("গুগল শিট সিঙ্ক সেটিংস", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                DokanTextField(
                    value = sheetId,
                    onValueChange = { sheetId = it },
                    label = "Google Spreadsheet ID / লিংক",
                    placeholder = "স্প্রেডশিটের লিংক বা আইডি পেস্ট করুন"
                )
                Spacer(modifier = Modifier.height(10.dp))
                DokanTextField(
                    value = gid,
                    onValueChange = { gid = it },
                    label = "Sheet GID (ডিফল্ট 0)",
                    placeholder = "0"
                )
                Spacer(modifier = Modifier.height(10.dp))
                DokanTextField(
                    value = webAppUrl,
                    onValueChange = { webAppUrl = it },
                    label = "Apps Script Web App URL (ঐচ্ছিক)",
                    placeholder = "https://script.google.com/..."
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(sheetId.trim(), gid.trim(), webAppUrl.trim()) }
            ) {
                Text("সংরক্ষণ করুন", fontWeight = FontWeight.Bold, color = DokanPurplePrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        }
    )
}
