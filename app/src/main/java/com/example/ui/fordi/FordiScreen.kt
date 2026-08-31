package com.example.ui.fordi

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.FordiItemModel
import com.example.FordiModel
import com.example.PdfExporter

// Theme colors matching exact screenshots
private val ScreenshotDarkGreen = Color(0xFF047857)
private val ScreenshotDeepGreen = Color(0xFF065F46)
private val ScreenshotMintBg = Color(0xFFD1FAE5)
private val ScreenshotMintBorder = Color(0xFFA7F3D0)
private val ScreenshotSlateBg = Color(0xFFF1F5F9)
private val ScreenshotSlateBorder = Color(0xFFE2E8F0)
private val ScreenshotTextDark = Color(0xFF1E293B)
private val ScreenshotTextMuted = Color(0xFF64748B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FordiScreen(
    viewModel: FordiViewModel,
    onNavigate: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showMenu by remember { mutableStateOf(false) }
    var showCatalogSheet by remember { mutableStateOf(false) }
    var showFordiListSheet by remember { mutableStateOf(false) }
    var showBudgetDialog by remember { mutableStateOf(false) }
    var showNewFordiDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<FordiItemModel?>(null) }
    var showCompleteConfirmDialog by remember { mutableStateOf(false) }

    val currentFordi = uiState.currentFordi
    val items = currentFordi?.items ?: emptyList()
    val isOfficeTemplate = currentFordi?.title?.contains("অফিস", ignoreCase = true) == true

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = currentFordi?.title ?: "দোকানের মাল তোলার ফর্দ",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = ScreenshotTextDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { showFordiListSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "মেনু বা ব্যাক",
                            tint = ScreenshotTextDark
                        )
                    }
                },
                actions = {
                    // Language Switcher Pill (Screenshot 1, 3)
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .clickable { viewModel.toggleLanguage() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Language,
                                contentDescription = "ভাষা",
                                tint = ScreenshotDarkGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (uiState.isEnglish) "EN | বাং" else "বাং | EN",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ScreenshotDarkGreen
                            )
                        }
                    }

                    // Cart Icon to switch lists (Screenshot 1)
                    IconButton(onClick = { showFordiListSheet = true }) {
                        BadgedBox(
                            badge = {
                                if (uiState.fordiList.size > 1) {
                                    Badge(containerColor = ScreenshotDarkGreen) {
                                        Text(text = uiState.fordiList.size.toString())
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ShoppingCart,
                                contentDescription = "ফর্দ তালিকা",
                                tint = ScreenshotDarkGreen
                            )
                        }
                    }

                    // Share Icon (Screenshot 1)
                    IconButton(onClick = {
                        val shareText = viewModel.buildShareText()
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, "বাজার ফর্দ শেয়ার করুন")
                        context.startActivity(shareIntent)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "শেয়ার",
                            tint = ScreenshotDarkGreen
                        )
                    }

                    // Overflow Menu (Screenshot 1)
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "আরও অপশন",
                                tint = ScreenshotTextDark
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("নতুন ফর্দ তৈরি করুন") },
                                onClick = {
                                    showMenu = false
                                    showNewFordiDialog = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = ScreenshotDarkGreen)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("সব টিক করুন") },
                                onClick = {
                                    showMenu = false
                                    viewModel.toggleAllItems(true)
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ScreenshotDarkGreen)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("সব আনচেক করুন") },
                                onClick = {
                                    showMenu = false
                                    viewModel.toggleAllItems(false)
                                },
                                leadingIcon = {
                                    Icon(Icons.Outlined.CheckBoxOutlineBlank, contentDescription = null, tint = ScreenshotTextMuted)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("বাজেট সেট করুন") },
                                onClick = {
                                    showMenu = false
                                    showBudgetDialog = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Outlined.AttachMoney, contentDescription = null, tint = ScreenshotDarkGreen)
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("বর্তমান ফর্দ ডিলিট", color = Color(0xFFDC2626)) },
                                onClick = {
                                    showMenu = false
                                    currentFordi?.id?.let { viewModel.deleteFordi(it) }
                                    Toast.makeText(context, "ফর্দ মুছে ফেলা হয়েছে", Toast.LENGTH_SHORT).show()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFDC2626))
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        floatingActionButton = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Mini tool button (Cash Denomination counter / lock)
                FloatingActionButton(
                    onClick = { onNavigate("note_counter") },
                    containerColor = ScreenshotMintBg,
                    contentColor = ScreenshotDeepGreen,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Calculate,
                        contentDescription = "নোট কাউন্টার",
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Main Capsule Floating Button (Screenshots 1 & 3)
                Button(
                    onClick = {
                        if (items.isEmpty()) {
                            showNewFordiDialog = true
                        } else {
                            showCompleteConfirmDialog = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ScreenshotDeepGreen
                    ),
                    shape = RoundedCornerShape(24.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    Icon(
                        imageVector = if (items.isEmpty()) Icons.Default.Add else Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (items.isEmpty()) {
                            "+ নতুন ফর্দ"
                        } else if (isOfficeTemplate) {
                            "+ ফর্দ তৈরি করুন (${PdfExporter.formatBengaliNumber(uiState.totalItemsCount)} টি পণ্য)"
                        } else {
                            "বাজার সম্পন্ন করুন"
                        },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFAFAFA))
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header for Office template (Screenshot 3)
            if (!currentFordi?.notes.isNullOrBlank() && isOfficeTemplate) {
                item {
                    Column(modifier = Modifier.padding(bottom = 4.dp)) {
                        Text(
                            text = currentFordi?.title ?: "অফিস বাজার",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = ScreenshotTextDark
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = currentFordi?.notes ?: "",
                            fontSize = 13.sp,
                            color = ScreenshotTextMuted
                        )
                    }
                }
            }

            // Top Status & Budget Summary Card (Screenshot 1)
            if (items.isNotEmpty() && !isOfficeTemplate) {
                item {
                    StatusAndBudgetSummaryCard(
                        checkedCount = uiState.checkedItemsCount,
                        totalCount = uiState.totalItemsCount,
                        checkedAmount = uiState.totalCheckedAmount,
                        remainingAmount = uiState.totalRemainingAmount,
                        grandTotal = uiState.totalFordiAmount,
                        budgetAmount = uiState.budgetAmount,
                        onBudgetClick = { showBudgetDialog = true }
                    )
                }
            }

            // Input Bar with Grid Icon, Search, Mic, Plus Button (Screenshots 1 & 2)
            item {
                SearchBarInputSection(
                    searchQuery = uiState.searchQuery,
                    onQueryChange = { viewModel.onSearchQueryChanged(it) },
                    onDirectAdd = { name ->
                        viewModel.addItem(name)
                    },
                    onCatalogClick = { showCatalogSheet = true }
                )
            }

            // Search Matching Suggestions (Screenshot 2)
            if (uiState.searchQuery.isNotBlank()) {
                item {
                    SearchSuggestionsSection(
                        query = uiState.searchQuery,
                        matchingList = uiState.matchingSuggestions,
                        onSelectDirect = {
                            viewModel.addItem(uiState.searchQuery)
                        },
                        onSelectCatalog = { catalogItem ->
                            viewModel.quickAddSuggestion(
                                name = catalogItem.name,
                                defaultPrice = catalogItem.defaultPrice,
                                defaultUnit = catalogItem.unit,
                                quantity = catalogItem.defaultQty
                            )
                        }
                    )
                }
            }

            // Empty state notice banner (Screenshot 2)
            if (items.isEmpty() && uiState.searchQuery.isBlank()) {
                item {
                    EmptyNoticeCard(
                        onOpenNewFordi = { showNewFordiDialog = true }
                    )
                }
            }

            // Smart Reminder Card (Screenshot 2)
            if (uiState.showSmartReminder && items.isEmpty()) {
                item {
                    SmartReminderCard(
                        onAdd = {
                            viewModel.addItem("অ্যালুমিনিয়াম ফয়েল পেপার রোল", 1.0, "রোল", 180.0)
                            viewModel.dismissSmartReminder()
                        },
                        onDismiss = {
                            viewModel.dismissSmartReminder()
                        }
                    )
                }
            }

            // Quick Add Suggestion Chips (Screenshots 1 & 2)
            item {
                QuickAddChipsRow(
                    onItemClick = { item ->
                        viewModel.quickAddSuggestion(
                            name = item.name,
                            defaultPrice = item.defaultPrice,
                            defaultUnit = item.unit,
                            quantity = item.defaultQty
                        )
                    },
                    onOpenCatalog = { showCatalogSheet = true }
                )
            }

            // Section Header (Screenshots 1 & 3)
            if (items.isNotEmpty()) {
                item {
                    SectionHeaderRow(
                        isOfficeTemplate = isOfficeTemplate,
                        remainingCount = uiState.totalItemsCount - uiState.checkedItemsCount,
                        remainingAmount = uiState.totalRemainingAmount,
                        totalCount = uiState.totalItemsCount,
                        allChecked = uiState.checkedItemsCount == uiState.totalItemsCount && uiState.totalItemsCount > 0,
                        onToggleAll = { checkAll ->
                            viewModel.toggleAllItems(checkAll)
                        }
                    )
                }

                // List Items (Screenshots 1 & 3)
                items(items, key = { it.id }) { item ->
                    FordiItemCard(
                        item = item,
                        onToggleCheck = { isChecked ->
                            viewModel.toggleItemChecked(item.id, isChecked)
                        },
                        onEdit = {
                            editingItem = item
                        },
                        onDelete = {
                            viewModel.deleteItem(item.id)
                        }
                    )
                }
            } else if (uiState.searchQuery.isBlank()) {
                // Big Empty Slate (Screenshot 2)
                item {
                    BigEmptyStateCard(
                        onCreateFordi = { showNewFordiDialog = true }
                    )
                }
            }

            // Bottom Spacing for Floating Action Button
            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }

    // Dialogs & Sheets
    if (showBudgetDialog) {
        BudgetDialog(
            currentBudget = uiState.budgetAmount,
            onDismiss = { showBudgetDialog = false },
            onSave = { amount ->
                viewModel.setBudget(amount)
                showBudgetDialog = false
            }
        )
    }

    if (showNewFordiDialog) {
        NewFordiDialog(
            onDismiss = { showNewFordiDialog = false },
            onCreate = { title, subtitle ->
                viewModel.createNewFordi(title, subtitle)
                showNewFordiDialog = false
            }
        )
    }

    editingItem?.let { item ->
        EditItemDialog(
            item = item,
            onDismiss = { editingItem = null },
            onSave = { name, qty, unit, price ->
                viewModel.updateItem(item.id, name, qty, unit, price)
                editingItem = null
            }
        )
    }

    if (showCompleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showCompleteConfirmDialog = false },
            icon = { Icon(Icons.Default.ShoppingCartCheckout, contentDescription = null, tint = ScreenshotDarkGreen) },
            title = { Text("বাজার সম্পন্ন ও ক্যাশে পোস্টিং") },
            text = {
                val amount = if (uiState.totalCheckedAmount > 0) uiState.totalCheckedAmount else uiState.totalFordiAmount
                Text("মোট ৳${PdfExporter.formatBengaliNumber(amount)} টাকার মাল কেনা হয়েছে। এটি কি আজকের দোকান ক্যাশ খাতায় 'মাল ক্রয়' খরচ হিসেবে যুক্ত করবেন?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.completeShoppingAndPostToAccounting { postedAmount ->
                            Toast.makeText(context, "৳${PdfExporter.formatBengaliNumber(postedAmount)} ক্যাশ খাতায় পোস্টিং সম্পন্ন হয়েছে", Toast.LENGTH_LONG).show()
                        }
                        showCompleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ScreenshotDeepGreen)
                ) {
                    Text("হ্যাঁ, পোস্টিং করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCompleteConfirmDialog = false }) {
                    Text("শুধু ফর্দ রাখুন")
                }
            }
        )
    }

    if (showCatalogSheet) {
        CatalogBottomSheet(
            catalogList = viewModel.globalCatalog,
            onDismiss = { showCatalogSheet = false },
            onSelectItem = { catItem ->
                viewModel.quickAddSuggestion(
                    name = catItem.name,
                    defaultPrice = catItem.defaultPrice,
                    defaultUnit = catItem.unit,
                    quantity = catItem.defaultQty
                )
                showCatalogSheet = false
            }
        )
    }

    if (showFordiListSheet) {
        FordiSwitcherBottomSheet(
            fordiList = uiState.fordiList,
            selectedFordiId = currentFordi?.id,
            onDismiss = { showFordiListSheet = false },
            onSelectFordi = { fordiId ->
                viewModel.selectFordi(fordiId)
                showFordiListSheet = false
            },
            onCreateNew = {
                showFordiListSheet = false
                showNewFordiDialog = true
            }
        )
    }
}

// -------------------------------------------------------------------------
// COMPONENT 1: Status & Budget Summary Card (Screenshot 1)
// -------------------------------------------------------------------------
@Composable
private fun StatusAndBudgetSummaryCard(
    checkedCount: Int,
    totalCount: Int,
    checkedAmount: Double,
    remainingAmount: Double,
    grandTotal: Double,
    budgetAmount: Double,
    onBudgetClick: () -> Unit
) {
    val percentage = if (totalCount > 0) (checkedCount * 100) / totalCount else 0
    val progress = if (totalCount > 0) checkedCount.toFloat() / totalCount.toFloat() else 0f

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, ScreenshotSlateBorder),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Status Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = ScreenshotDarkGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "${PdfExporter.formatBengaliNumber(checkedCount)}/${PdfExporter.formatBengaliNumber(totalCount)} টি টিক করা (${PdfExporter.formatBengaliNumber(percentage)}%)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = ScreenshotDarkGreen
                    )
                }

                // Budget Pill Button (Screenshot 1)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFECFDF5),
                    border = BorderStroke(1.dp, ScreenshotMintBorder),
                    modifier = Modifier.clickable { onBudgetClick() }
                ) {
                    Text(
                        text = if (budgetAmount > 0) "বাজেট: ৳${PdfExporter.formatBengaliNumber(budgetAmount)}" else "+ বাজেট দিন",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ScreenshotDeepGreen,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            // Linear Progress Bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = ScreenshotDarkGreen,
                trackColor = Color(0xFFE2E8F0)
            )

            // Two Comparison Metric Boxes (Screenshot 1)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Left Box: Checked Total (Mint Green)
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = ScreenshotMintBg
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "✓ টিক মার্ক করা মোট",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ScreenshotDeepGreen
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "৳${PdfExporter.formatBengaliNumber(checkedAmount)}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = ScreenshotDeepGreen
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${PdfExporter.formatBengaliNumber(checkedCount)} টি পণ্য",
                            fontSize = 12.sp,
                            color = ScreenshotDarkGreen
                        )
                    }
                }

                // Right Box: Remaining Total (Slate Gray)
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = ScreenshotSlateBg
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "বাকি কেনার মোট",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF475569)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "৳${PdfExporter.formatBengaliNumber(remainingAmount)}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = ScreenshotTextDark
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${PdfExporter.formatBengaliNumber(totalCount - checkedCount)} টি পণ্য",
                            fontSize = 12.sp,
                            color = ScreenshotTextMuted
                        )
                    }
                }
            }

            // Bottom Total Summary Line (Screenshot 1)
            Text(
                text = "ফর্দের মোট: ৳${PdfExporter.formatBengaliNumber(grandTotal)}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = ScreenshotTextDark
            )
        }
    }
}

// -------------------------------------------------------------------------
// COMPONENT 2: Search Bar Input Section (Screenshots 1 & 2)
// -------------------------------------------------------------------------
@Composable
private fun SearchBarInputSection(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    onDirectAdd: (String) -> Unit,
    onCatalogClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category / Grid icon on left
            IconButton(
                onClick = onCatalogClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.GridView,
                    contentDescription = "ক্যাটালগ",
                    tint = ScreenshotDarkGreen
                )
            }

            // Input TextField
            TextField(
                value = searchQuery,
                onValueChange = onQueryChange,
                placeholder = {
                    Text(
                        text = "যেমন: ৫ কেজি চাল, ১২টি ডিম...",
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8)
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (searchQuery.isNotBlank()) {
                        onDirectAdd(searchQuery)
                    }
                }),
                modifier = Modifier.weight(1f)
            )

            // Clear Button when typing (Screenshot 2)
            if (searchQuery.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "মুছুন",
                        tint = ScreenshotTextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                // Mic Icon (Screenshot 1)
                IconButton(
                    onClick = { onCatalogClick() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "ভয়েস ইনপুট",
                        tint = ScreenshotDarkGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Plus Action Button (Screenshots 1 & 2)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = ScreenshotDeepGreen,
                modifier = Modifier
                    .size(42.dp)
                    .clickable {
                        if (searchQuery.isNotBlank()) {
                            onDirectAdd(searchQuery)
                        } else {
                            onCatalogClick()
                        }
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "যোগ করুন",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// COMPONENT 3: Search Suggestions Section (Screenshot 2)
// -------------------------------------------------------------------------
@Composable
private fun SearchSuggestionsSection(
    query: String,
    matchingList: List<CatalogItem>,
    onSelectDirect: () -> Unit,
    onSelectCatalog: (CatalogItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = ScreenshotDarkGreen,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "পণ্য তালিকা বা সরাসরি নির্বাচন করুন:",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = ScreenshotTextDark
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Direct text add chip (Screenshot 2)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = ScreenshotMintBg,
                border = BorderStroke(1.dp, ScreenshotMintBorder),
                modifier = Modifier.clickable { onSelectDirect() }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = ScreenshotDeepGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "সরাসরি: '$query'",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ScreenshotDeepGreen
                    )
                }
            }

            // Matching catalog chips (Screenshot 2)
            matchingList.take(6).forEach { catItem ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, ScreenshotSlateBorder),
                    modifier = Modifier.clickable { onSelectCatalog(catItem) }
                ) {
                    Text(
                        text = "${catItem.name} • ৳${PdfExporter.formatBengaliNumber(catItem.defaultPrice)}/${catItem.unit}",
                        fontSize = 12.sp,
                        color = ScreenshotTextDark,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// COMPONENT 4: Empty State Notice Card (Screenshot 2)
// -------------------------------------------------------------------------
@Composable
private fun EmptyNoticeCard(
    onOpenNewFordi: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, ScreenshotSlateBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "আজকের ফর্দ নেই",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = ScreenshotTextDark
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "কেনাকাটা শুরু করতে ফর্দ খুলুন",
                    fontSize = 12.sp,
                    color = ScreenshotTextMuted
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = ScreenshotMintBg,
                border = BorderStroke(1.dp, ScreenshotMintBorder),
                modifier = Modifier.clickable { onOpenNewFordi() }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = ScreenshotDeepGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "নতুন ফর্দ",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = ScreenshotDeepGreen
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// COMPONENT 5: Smart Reminder Card (Screenshot 2)
// -------------------------------------------------------------------------
@Composable
private fun SmartReminderCard(
    onAdd: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFEFF6FF),
        border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lightbulb,
                contentDescription = null,
                tint = Color(0xFF2563EB),
                modifier = Modifier.size(20.dp)
            )

            Text(
                text = "আপনি সাধারণত প্রতি ৭ দিনে অ্যালুমিনিয়া...",
                fontSize = 12.sp,
                color = Color(0xFF1E3A8A),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            // Add button
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF1E40AF),
                modifier = Modifier.clickable { onAdd() }
            ) {
                Text(
                    text = "যোগ করুন",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            // Close button
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "বন্ধ করুন",
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

// -------------------------------------------------------------------------
// COMPONENT 6: Quick Add Chips Row (Screenshots 1 & 2)
// -------------------------------------------------------------------------
@Composable
private fun QuickAddChipsRow(
    onItemClick: (CatalogItem) -> Unit,
    onOpenCatalog: () -> Unit
) {
    val quickItems = remember {
        listOf(
            CatalogItem("অ্যালুমিনিয়াম ফয়েল পেপার রোল", "রোল", 180.0, 1.0, "স্টেশনারি"),
            CatalogItem("আটা / ময়দা ৫০ কেজি বস্তা", "বস্তা", 2250.0, 1.0, "মুদি"),
            CatalogItem("মিনিকেট চাল (৫০ কেজি)", "বস্তা", 3450.0, 5.0, "মুদি"),
            CatalogItem("সয়াবিন তেল ৫ লিটার", "বোতল", 820.0, 1.0, "তেল"),
            CatalogItem("পেঁয়াজের বস্তা (৪০ কেজি)", "বস্তা", 2800.0, 2.0, "কাঁচাবাজার")
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "দ্রুত যোগ করুন",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = ScreenshotTextDark
            )

            Text(
                text = "সব ক্যাটালগ →",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = ScreenshotDarkGreen,
                modifier = Modifier.clickable { onOpenCatalog() }
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            quickItems.forEach { item ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, ScreenshotSlateBorder),
                    modifier = Modifier.clickable { onItemClick(item) }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = ScreenshotDarkGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = item.name,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = ScreenshotTextDark
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// COMPONENT 7: Section Header Row (Screenshots 1 & 3)
// -------------------------------------------------------------------------
@Composable
private fun SectionHeaderRow(
    isOfficeTemplate: Boolean,
    remainingCount: Int,
    remainingAmount: Double,
    totalCount: Int,
    allChecked: Boolean,
    onToggleAll: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isOfficeTemplate) {
            Text(
                text = "পণ্য তালিকা (${PdfExporter.formatBengaliNumber(totalCount)} টি পণ্য)",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = ScreenshotTextDark
            )

            Text(
                text = if (allChecked) "সব আনচেক করুন" else "সব চেক করুন",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = ScreenshotDarkGreen,
                modifier = Modifier.clickable { onToggleAll(!allChecked) }
            )
        } else {
            Text(
                text = "কেনা বাকি (${PdfExporter.formatBengaliNumber(remainingCount)} টি পণ্য)",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = ScreenshotDarkGreen
            )

            Text(
                text = "৳${PdfExporter.formatBengaliNumber(remainingAmount)}",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = ScreenshotTextDark
            )
        }
    }
}

// -------------------------------------------------------------------------
// COMPONENT 8: Product List Card (Screenshots 1 & 3)
// -------------------------------------------------------------------------
@Composable
private fun FordiItemCard(
    item: FordiItemModel,
    onToggleCheck: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, ScreenshotSlateBorder),
        shadowElevation = 0.5.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Square Checkbox (Screenshots 1 & 3)
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (item.isChecked) ScreenshotDarkGreen else Color.Transparent)
                    .border(
                        1.5.dp,
                        if (item.isChecked) ScreenshotDarkGreen else Color(0xFF64748B),
                        RoundedCornerShape(4.dp)
                    )
                    .clickable { onToggleCheck(!item.isChecked) },
                contentAlignment = Alignment.Center
            ) {
                if (item.isChecked) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "টিক করা",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Name, Quantity Badge, Price
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = item.productName ?: item.name ?: "",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = ScreenshotTextDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Gray badge for quantity (Screenshot 1)
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFF1F5F9)
                    ) {
                        Text(
                            text = "${PdfExporter.formatBengaliNumber(item.plannedQuantity)} ${item.unit}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ScreenshotTextDark,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = "• ৳${PdfExporter.formatBengaliNumber(item.plannedTotal)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ScreenshotTextMuted
                    )
                }
            }

            // Edit & Delete icons (Screenshot 1)
            IconButton(
                onClick = onEdit,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = "সম্পাদনা",
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "মুছুন",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// -------------------------------------------------------------------------
// COMPONENT 9: Big Empty State Card (Screenshot 2)
// -------------------------------------------------------------------------
@Composable
private fun BigEmptyStateCard(
    onCreateFordi: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "আপনার চলতি ফর্দসমূহ",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = ScreenshotTextDark
            )
            Text(
                text = "০ টি ফর্দ",
                fontSize = 13.sp,
                color = ScreenshotTextMuted
            )
        }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            border = BorderStroke(1.dp, ScreenshotSlateBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.ShoppingCart,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(48.dp)
                )

                Text(
                    text = "কোনো ফর্দ নেই",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = ScreenshotTextDark
                )

                Text(
                    text = "নতুন ফর্দ তৈরি করে কেনাকাটার হিসাব রাখুন।",
                    fontSize = 13.sp,
                    color = ScreenshotTextMuted
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedButton(
                    onClick = onCreateFordi,
                    border = BorderStroke(1.dp, ScreenshotDarkGreen),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = ScreenshotDarkGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "তৈরি করুন",
                        fontWeight = FontWeight.Bold,
                        color = ScreenshotDarkGreen
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// DIALOG 1: Budget Dialog
// -------------------------------------------------------------------------
@Composable
private fun BudgetDialog(
    currentBudget: Double,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var budgetText by remember { mutableStateOf(if (currentBudget > 0) currentBudget.toInt().toString() else "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("বাজারের বাজেট নির্ধারণ করুন") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "কত টাকার মধ্যে মালামাল তুলতে চান তার সর্বোচ্চ সীমা লিখুন:",
                    fontSize = 13.sp,
                    color = ScreenshotTextMuted
                )
                OutlinedTextField(
                    value = budgetText,
                    onValueChange = { budgetText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("বাজেট পরিমাণ (টাকা)") },
                    placeholder = { Text("যেমন: ৫০০০০") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = budgetText.toDoubleOrNull() ?: 0.0
                    onSave(amount)
                },
                colors = ButtonDefaults.buttonColors(containerColor = ScreenshotDeepGreen)
            ) {
                Text("সংরক্ষণ করুন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        }
    )
}

// -------------------------------------------------------------------------
// DIALOG 2: New Fordi Dialog
// -------------------------------------------------------------------------
@Composable
private fun NewFordiDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var subtitle by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("নতুন বাজার ফর্দ") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("ফর্দের নাম") },
                    placeholder = { Text("যেমন: দোকানের মাল তোলার ফর্দ") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = subtitle,
                    onValueChange = { subtitle = it },
                    label = { Text("বিবরণ / নোট (ঐচ্ছিক)") },
                    placeholder = { Text("যেমন: পাইকারি বাজার থেকে মালামাল") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onCreate(title, subtitle)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ScreenshotDeepGreen)
            ) {
                Text("তৈরি করুন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        }
    )
}

// -------------------------------------------------------------------------
// DIALOG 3: Edit Item Dialog
// -------------------------------------------------------------------------
@Composable
private fun EditItemDialog(
    item: FordiItemModel,
    onDismiss: () -> Unit,
    onSave: (String, Double, String, Double) -> Unit
) {
    var name by remember { mutableStateOf(item.productName ?: item.name ?: "") }
    var qtyText by remember { mutableStateOf(item.plannedQuantity.toString().removeSuffix(".0")) }
    var unit by remember { mutableStateOf(item.unit ?: "কেজি") }
    var priceText by remember { mutableStateOf(if (item.purchaseRate > 0) item.purchaseRate.toInt().toString() else "") }

    val units = listOf("কেজি", "গ্রাম", "লিটার", "বস্তা", "কার্টন", "প্যাকেট", "ডজন", "টি", "পিস", "জার", "বক্স", "বোতল", "রোল", "রিম")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("পণ্য সম্পাদনা") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("পণ্যের নাম") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = qtyText,
                        onValueChange = { qtyText = it },
                        label = { Text("পরিমাণ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("একক") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text("আনুমানিক দর / মূল্য (টাকা)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = qtyText.toDoubleOrNull() ?: 1.0
                    val price = priceText.toDoubleOrNull() ?: 0.0
                    onSave(name, qty, unit, price)
                },
                colors = ButtonDefaults.buttonColors(containerColor = ScreenshotDeepGreen)
            ) {
                Text("আপডেট")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        }
    )
}

// -------------------------------------------------------------------------
// BOTTOM SHEET 1: Catalog Browser Sheet
// -------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CatalogBottomSheet(
    catalogList: List<CatalogItem>,
    onDismiss: () -> Unit,
    onSelectItem: (CatalogItem) -> Unit
) {
    val categories = remember {
        listOf("সকল", "মুদি পাইকারি", "কাঁচাবাজার", "টয়লেট্রিজ", "চা ও কফি", "ক্লিনিং", "স্টেশনারি", "মসলা")
    }
    var selectedCategory by remember { mutableStateOf("সকল") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "পাইকারি ও সাধারণ পণ্য ক্যাটালগ",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = ScreenshotTextDark
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "বন্ধ করুন")
                }
            }

            // Category Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    val isSelected = selectedCategory == cat
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) ScreenshotDeepGreen else Color(0xFFF1F5F9),
                        modifier = Modifier.clickable { selectedCategory = cat }
                    ) {
                        Text(
                            text = cat,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else ScreenshotTextDark,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            val filteredList = if (selectedCategory == "সকল") {
                catalogList
            } else {
                catalogList.filter { it.category == selectedCategory }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(filteredList) { item ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, ScreenshotSlateBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectItem(item) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = item.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = ScreenshotTextDark
                                )
                                Text(
                                    text = "${PdfExporter.formatBengaliNumber(item.defaultQty)} ${item.unit} • আনুমানিক ৳${PdfExporter.formatBengaliNumber(item.defaultPrice)}",
                                    fontSize = 12.sp,
                                    color = ScreenshotTextMuted
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.AddCircle,
                                contentDescription = "যোগ",
                                tint = ScreenshotDarkGreen,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// BOTTOM SHEET 2: Fordi Switcher Sheet
// -------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FordiSwitcherBottomSheet(
    fordiList: List<FordiModel>,
    selectedFordiId: String?,
    onDismiss: () -> Unit,
    onSelectFordi: (String) -> Unit,
    onCreateNew: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "আপনার সকল বাজার ফর্দ",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = ScreenshotTextDark
                )

                Button(
                    onClick = onCreateNew,
                    colors = ButtonDefaults.buttonColors(containerColor = ScreenshotDeepGreen),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("নতুন ফর্দ", fontSize = 12.sp)
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 350.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(fordiList) { fordi ->
                    val isSelected = fordi.id == selectedFordiId
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) ScreenshotMintBg else Color.White,
                        border = BorderStroke(1.dp, if (isSelected) ScreenshotDarkGreen else ScreenshotSlateBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectFordi(fordi.id) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = fordi.title ?: "বাজার ফর্দ",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isSelected) ScreenshotDeepGreen else ScreenshotTextDark
                                )
                                if (!fordi.notes.isNullOrBlank()) {
                                    Text(
                                        text = fordi.notes ?: "",
                                        fontSize = 12.sp,
                                        color = ScreenshotTextMuted,
                                        maxLines = 1
                                    )
                                }
                                Text(
                                    text = "মোট পণ্য: ${PdfExporter.formatBengaliNumber(fordi.totalItemCount)} টি • ৳${PdfExporter.formatBengaliNumber(fordi.plannedTotal)}",
                                    fontSize = 11.sp,
                                    color = if (isSelected) ScreenshotDarkGreen else ScreenshotTextMuted
                                )
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "নির্বাচিত",
                                    tint = ScreenshotDarkGreen
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
