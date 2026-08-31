package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.DokanPurpleDark
import com.example.ui.theme.DokanPurplePrimary

@Composable
fun DokanDrawer(
    onNavigate: (String) -> Unit,
    onCloseDrawer: () -> Unit,
    userEmail: String? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .width(310.dp)
            .fillMaxHeight(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
        ) {
            // Drawer Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DokanPurplePrimary)
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.img_mawa_brand_logo),
                        contentDescription = "লোগো",
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "মাওয়া ক্যাশ খাতা",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                        )
                        Text(
                            text = if (!userEmail.isNullOrBlank()) userEmail else "ডিজিটাল ক্যাশ ও বাকি হিসাব",
                            color = Color(0xFFE9D5FF),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Navigation Section
            DrawerSectionTitle(title = "মূল মেনু")
            DrawerMenuItem(
                icon = Icons.Default.MenuBook,
                title = "দৈনিক ক্যাশ হিসাব",
                onClick = { onNavigate("dashboard"); onCloseDrawer() }
            )
            DrawerMenuItem(
                icon = Icons.Default.People,
                title = "বাকি খাতা (দেনা-পাওনা)",
                onClick = { onNavigate("baki"); onCloseDrawer() }
            )
            DrawerMenuItem(
                icon = Icons.Default.ShoppingCart,
                title = "বাজার ফর্দ ও তালিকা",
                onClick = { onNavigate("fordi"); onCloseDrawer() }
            )
            DrawerMenuItem(
                icon = Icons.Default.Assessment,
                title = "রিপোর্ট ও লাভ-ক্ষতি হিসাব",
                onClick = { onNavigate("reports"); onCloseDrawer() }
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp))

            // Cloud, Sync & Backup Section (Separated into Drawer as requested)
            DrawerSectionTitle(title = "ক্লাউড ও ব্যাকআপ")
            DrawerMenuItem(
                icon = Icons.Default.Cloud,
                title = "সুপাবেজ ক্লাউড সিঙ্ক",
                subtitle = "অনলাইন অটো ব্যাকআপ",
                onClick = { onNavigate("cloud_backup"); onCloseDrawer() }
            )
            DrawerMenuItem(
                icon = Icons.Default.Backup,
                title = "ডাটা ব্যাকআপ ও রিস্টোর",
                subtitle = "JSON / ফাইল সংরক্ষণ",
                onClick = { onNavigate("backup_restore"); onCloseDrawer() }
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp))

            // Smart Tools Section
            DrawerSectionTitle(title = "স্মার্ট টুলস")
            DrawerMenuItem(
                icon = Icons.Default.Calculate,
                title = "নোট কাউন্টার (টাকা গণনা)",
                onClick = { onNavigate("note_counter"); onCloseDrawer() }
            )
            DrawerMenuItem(
                icon = Icons.Default.Settings,
                title = "অ্যাপ সেটিংস ও পিন লক",
                onClick = { onNavigate("settings"); onCloseDrawer() }
            )
            DrawerMenuItem(
                icon = Icons.Default.Info,
                title = "ডেভেলপার পরিচিতি ও সহায়তা",
                onClick = { onNavigate("developer_info"); onCloseDrawer() }
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun DrawerSectionTitle(title: String) {
    Text(
        text = title,
        modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 4.dp),
        style = MaterialTheme.typography.labelSmall.copy(
            color = DokanPurplePrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
        )
    )
}

@Composable
private fun DrawerMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = DokanPurplePrimary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.5.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}
