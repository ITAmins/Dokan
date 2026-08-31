package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector

sealed class DokanNavRoute(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Dashboard : DokanNavRoute("dashboard", "হোম", Icons.Default.Home)
    object DailyCash : DokanNavRoute("daily_cash", "দৈনিক ক্যাশ", Icons.Default.MenuBook)
    object Baki : DokanNavRoute("baki", "বাকি খাতা", Icons.Default.People)
    object Fordi : DokanNavRoute("fordi", "বাজার ফর্দ", Icons.Default.ShoppingCart)
    object Reports : DokanNavRoute("reports", "রিপোর্ট", Icons.Default.Assessment)
    object CustomerDetails : DokanNavRoute("customer_details/{customerId}", "কাস্টমার খাতা", Icons.Default.People) {
        fun createRoute(customerId: String) = "customer_details/$customerId"
    }
    object NoteCounter : DokanNavRoute("note_counter", "নোট গণনা", Icons.Default.Calculate)
    object CloudBackup : DokanNavRoute("cloud_backup", "ক্লাউড সিঙ্ক", Icons.Default.Cloud)
    object Settings : DokanNavRoute("settings", "সেটিংস", Icons.Default.Settings)
}

val BottomNavItems = listOf(
    DokanNavRoute.Dashboard,
    DokanNavRoute.DailyCash,
    DokanNavRoute.Baki,
    DokanNavRoute.Fordi,
    DokanNavRoute.Reports
)
