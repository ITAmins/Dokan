package com.example.ui.settings

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.PdfExporter
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var showProfileDialog by remember { mutableStateOf(false) }
    var showMarginDialog by remember { mutableStateOf(false) }

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
                title = "সেটিংস ও প্রোফাইল",
                subtitle = "দোকানের প্রোফাইল, মার্জিন ও অ্যাপ সেটিংস",
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
            // 1. Store Profile Hero Header
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = DokanPurpleDark,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Storefront,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = uiState.storeName,
                                color = Color.White,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = uiState.storeOwner,
                                color = Color(0xFFDDD6FE),
                                fontSize = 12.5.sp
                            )
                            if (uiState.storePhone.isNotBlank()) {
                                Text(
                                    text = "মোবাইল: ${uiState.storePhone}",
                                    color = Color(0xFFE9D5FF),
                                    fontSize = 11.5.sp
                                )
                            }
                        }

                        IconButton(
                            onClick = { showProfileDialog = true },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.15f))
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "প্রোফাইল সম্পাদনা", tint = Color.White)
                        }
                    }
                }
            }

            // 2. Business Preferences Card
            item {
                SettingsSectionHeader(title = "ব্যবসায়িক হিসাব সেটিংস")
                DokanCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SettingsClickableRow(
                            icon = Icons.Default.TrendingUp,
                            iconColor = IncomeGreen,
                            title = "আনুমানিক শতকরা লাভ (মার্জিন)",
                            subtitle = "বর্তমান হার: ${PdfExporter.toBengaliDigits(uiState.profitMarginPercent.toString())}%",
                            onClick = { showMarginDialog = true }
                        )

                        Divider(color = NotebookCardBorder, modifier = Modifier.padding(vertical = 12.dp))

                        SettingsSwitchRow(
                            icon = Icons.Default.ShoppingCart,
                            iconColor = BalanceBlue,
                            title = "বাজার ফর্দ কমপ্যাক্ট মোড",
                            subtitle = "ফর্দ তালিকায় ক্লিন ও দ্রুত এন্ট্রি মোড চালু রাখুন",
                            checked = uiState.isFordiCardlessMode,
                            onCheckedChange = { viewModel.toggleFordiCardless(it) }
                        )
                    }
                }
            }

            // 3. App System & Security Card
            item {
                SettingsSectionHeader(title = "অ্যাপ সেটিংস ও তথ্য")
                DokanCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SettingsClickableRow(
                            icon = Icons.Default.CleaningServices,
                            iconColor = AmberGold,
                            title = "ক্যাশ পরিষ্কার ও অপ্টিমাইজ",
                            subtitle = "অ্যাপের গতি বৃদ্ধি ও মেমোরি ফ্রি করুন",
                            onClick = { viewModel.clearAllCache() }
                        )

                        Divider(color = NotebookCardBorder, modifier = Modifier.padding(vertical = 12.dp))

                        SettingsClickableRow(
                            icon = Icons.Default.Info,
                            iconColor = DokanPurplePrimary,
                            title = "অ্যাপ সংস্করণ (Version)",
                            subtitle = uiState.appVersion,
                            onClick = {
                                Toast.makeText(context, "Dokan খাতা - Compose Native Edition", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }

    // Profile Edit Dialog
    if (showProfileDialog) {
        StoreProfileDialog(
            name = uiState.storeName,
            owner = uiState.storeOwner,
            phone = uiState.storePhone,
            address = uiState.storeAddress,
            onDismiss = { showProfileDialog = false },
            onSave = { n, o, p, a ->
                viewModel.saveStoreProfile(n, o, p, a)
                showProfileDialog = false
            }
        )
    }

    // Margin Dialog
    if (showMarginDialog) {
        ProfitMarginDialog(
            currentMargin = uiState.profitMarginPercent,
            onDismiss = { showMarginDialog = false },
            onSave = { m ->
                viewModel.saveProfitMargin(m)
                showMarginDialog = false
            }
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
    )
}

@Composable
private fun SettingsClickableRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.5.sp
                )
            )
        }

        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.5.sp
                )
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = DokanPurplePrimary
            )
        )
    }
}

@Composable
private fun StoreProfileDialog(
    name: String,
    owner: String,
    phone: String,
    address: String,
    onDismiss: () -> Unit,
    onSave: (name: String, owner: String, phone: String, address: String) -> Unit
) {
    var storeName by remember { mutableStateOf(name) }
    var storeOwner by remember { mutableStateOf(owner) }
    var storePhone by remember { mutableStateOf(phone) }
    var storeAddress by remember { mutableStateOf(address) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("দোকানের প্রোফাইল সম্পাদনা", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                DokanTextField(
                    value = storeName,
                    onValueChange = { storeName = it },
                    label = "দোকানের নাম",
                    placeholder = "যেমন: মাওয়া স্টোর"
                )
                Spacer(modifier = Modifier.height(10.dp))
                DokanTextField(
                    value = storeOwner,
                    onValueChange = { storeOwner = it },
                    label = "প্রোপাইটর / বিবরণ",
                    placeholder = "যেমন: মুদি ও জেনারেল মার্চেন্ট"
                )
                Spacer(modifier = Modifier.height(10.dp))
                DokanTextField(
                    value = storePhone,
                    onValueChange = { storePhone = it },
                    label = "মোবাইল নম্বর",
                    placeholder = "০১৭xxxxxxxx"
                )
                Spacer(modifier = Modifier.height(10.dp))
                DokanTextField(
                    value = storeAddress,
                    onValueChange = { storeAddress = it },
                    label = "দোকানের ঠিকানা",
                    placeholder = "বাজারের নাম, এলাকা"
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(storeName, storeOwner, storePhone, storeAddress) },
                enabled = storeName.isNotBlank()
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

@Composable
private fun ProfitMarginDialog(
    currentMargin: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    var marginStr by remember { mutableStateOf(currentMargin.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("আনুমানিক শতকরা লাভের হার (%)", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    text = "মুদি বা খুচরা দোকানের মোট বিক্রির ওপর আনুমানিক নিট লাভ হিসাব করতে এই শতকরা হার ব্যবহৃত হয়।",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                DokanAmountInput(
                    value = marginStr,
                    onValueChange = { marginStr = it },
                    label = "শতকরা হার (%)"
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val m = marginStr.toIntOrNull() ?: 10
                    onSave(m.coerceIn(1, 100))
                }
            ) {
                Text("আপডেট করুন", fontWeight = FontWeight.Bold, color = DokanPurplePrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        }
    )
}
