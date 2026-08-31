package com.example.ui.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.baki.BakiScreen
import com.example.ui.baki.BakiViewModel
import com.example.ui.cloud.CloudBackupScreen
import com.example.ui.cloud.CloudBackupViewModel
import com.example.ui.dailycash.DailyCashScreen
import com.example.ui.dailycash.DailyCashViewModel
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.dashboard.DashboardViewModel
import com.example.ui.fordi.FordiScreen
import com.example.ui.fordi.FordiViewModel
import com.example.ui.notecounter.NoteCounterScreen
import com.example.ui.notecounter.NoteCounterViewModel
import com.example.ui.reports.ReportsScreen
import com.example.ui.reports.ReportsViewModel
import com.example.ui.settings.SettingsScreen
import com.example.ui.settings.SettingsViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DokanAppScaffold(
    modifier: Modifier = Modifier
) {
    var currentRoute by remember { mutableStateOf<String>(DokanNavRoute.Dashboard.route) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    // ViewModels
    val dashboardViewModel: DashboardViewModel = viewModel()
    val dailyCashViewModel: DailyCashViewModel = viewModel()
    val bakiViewModel: BakiViewModel = viewModel()
    val fordiViewModel: FordiViewModel = viewModel()
    val reportsViewModel: ReportsViewModel = viewModel()
    val noteCounterViewModel: NoteCounterViewModel = viewModel()
    val cloudBackupViewModel: CloudBackupViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()

    val onNavigate: (String) -> Unit = { route ->
        if (route == "open_drawer") {
            coroutineScope.launch { drawerState.open() }
        } else {
            currentRoute = route
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(310.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                DrawerHeaderContent()

                Spacer(modifier = Modifier.height(8.dp))

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("হোম ড্যাশবোর্ড", fontWeight = FontWeight.SemiBold) },
                    selected = currentRoute == DokanNavRoute.Dashboard.route,
                    onClick = {
                        currentRoute = DokanNavRoute.Dashboard.route
                        coroutineScope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.MenuBook, contentDescription = null) },
                    label = { Text("দৈনিক ক্যাশ খাতা", fontWeight = FontWeight.SemiBold) },
                    selected = currentRoute == DokanNavRoute.DailyCash.route,
                    onClick = {
                        currentRoute = DokanNavRoute.DailyCash.route
                        coroutineScope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.People, contentDescription = null) },
                    label = { Text("বাকির খাতা ও দেনা-পাওনা", fontWeight = FontWeight.SemiBold) },
                    selected = currentRoute == DokanNavRoute.Baki.route,
                    onClick = {
                        currentRoute = DokanNavRoute.Baki.route
                        coroutineScope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.ShoppingCart, contentDescription = null) },
                    label = { Text("বাজার ফর্দ ও কেনাকাটা", fontWeight = FontWeight.SemiBold) },
                    selected = currentRoute == DokanNavRoute.Fordi.route,
                    onClick = {
                        currentRoute = DokanNavRoute.Fordi.route
                        coroutineScope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Assessment, contentDescription = null) },
                    label = { Text("ব্যবসায়িক রিপোর্ট ও হিসাব", fontWeight = FontWeight.SemiBold) },
                    selected = currentRoute == DokanNavRoute.Reports.route,
                    onClick = {
                        currentRoute = DokanNavRoute.Reports.route
                        coroutineScope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )

                Divider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = NotebookCardBorder)

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Calculate, contentDescription = null, tint = AmberGold) },
                    label = { Text("নোট গণনা ও ক্যাশ কাউন্টার", fontWeight = FontWeight.SemiBold) },
                    selected = currentRoute == DokanNavRoute.NoteCounter.route,
                    onClick = {
                        currentRoute = DokanNavRoute.NoteCounter.route
                        coroutineScope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Cloud, contentDescription = null, tint = IncomeGreen) },
                    label = { Text("ক্লাউড ব্যাকআপ ও গুগল শিট", fontWeight = FontWeight.SemiBold) },
                    selected = currentRoute == DokanNavRoute.CloudBackup.route,
                    onClick = {
                        currentRoute = DokanNavRoute.CloudBackup.route
                        coroutineScope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = null, tint = DokanPurplePrimary) },
                    label = { Text("সেটিংস ও প্রোফাইল", fontWeight = FontWeight.SemiBold) },
                    selected = currentRoute == DokanNavRoute.Settings.route,
                    onClick = {
                        currentRoute = DokanNavRoute.Settings.route
                        coroutineScope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )
            }
        }
    ) {
        val showBottomBar = currentRoute in BottomNavItems.map { it.route }

        Scaffold(
            modifier = modifier.fillMaxSize(),
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        BottomNavItems.forEach { item ->
                            val isSelected = currentRoute == item.route
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { currentRoute = item.route },
                                icon = {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.title
                                    )
                                },
                                label = {
                                    Text(
                                        text = item.title,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = DokanPurplePrimary,
                                    selectedTextColor = DokanPurplePrimary,
                                    indicatorColor = DokanPurpleBg,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentRoute) {
                    DokanNavRoute.Dashboard.route -> {
                        DashboardScreen(
                            viewModel = dashboardViewModel,
                            onNavigate = onNavigate
                        )
                    }
                    DokanNavRoute.DailyCash.route -> {
                        DailyCashScreen(
                            viewModel = dailyCashViewModel,
                            onNavigate = onNavigate
                        )
                    }
                    DokanNavRoute.Baki.route -> {
                        BakiScreen(
                            viewModel = bakiViewModel,
                            onNavigate = onNavigate
                        )
                    }
                    DokanNavRoute.Fordi.route -> {
                        FordiScreen(
                            viewModel = fordiViewModel,
                            onNavigate = onNavigate
                        )
                    }
                    DokanNavRoute.Reports.route -> {
                        ReportsScreen(
                            viewModel = reportsViewModel,
                            onNavigate = onNavigate
                        )
                    }
                    DokanNavRoute.NoteCounter.route -> {
                        NoteCounterScreen(
                            viewModel = noteCounterViewModel,
                            onNavigateBack = { currentRoute = DokanNavRoute.Dashboard.route }
                        )
                    }
                    DokanNavRoute.CloudBackup.route -> {
                        CloudBackupScreen(
                            viewModel = cloudBackupViewModel,
                            onNavigateBack = { currentRoute = DokanNavRoute.Dashboard.route }
                        )
                    }
                    DokanNavRoute.Settings.route -> {
                        SettingsScreen(
                            viewModel = settingsViewModel,
                            onNavigateBack = { currentRoute = DokanNavRoute.Dashboard.route }
                        )
                    }
                    else -> {
                        DashboardScreen(
                            viewModel = dashboardViewModel,
                            onNavigate = onNavigate
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawerHeaderContent() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = DokanPurpleDark
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Storefront,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "মাওয়া স্টোর",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            )
            Text(
                text = "ডিজিটাল ক্যাশ ও বাকি খাতা",
                color = Color(0xFFDDD6FE),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp
                )
            )
        }
    }
}
