package com.example.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AiBusinessCoachCard
import com.example.ui.components.EmptyStateView
import com.example.ui.components.HeroBalanceCard
import com.example.ui.components.OverviewGrid2x2
import com.example.ui.components.SyncIndicator
import com.example.ui.components.TransactionCard
import com.example.ui.theme.extendedColors
import com.example.util.DateUtils
import com.example.util.MoneyFormatter
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToTransactions: () -> Unit,
    onNavigateToTransactionDetail: (String) -> Unit,
    onNavigateToEditTransaction: (String) -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToInventory: () -> Unit,
    onNavigateToShopping: () -> Unit,
    onNavigateToProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val isDark = isSystemInDarkTheme()

    var showNotificationDialog by remember { mutableStateOf(false) }
    var showAiCoachBottomSheet by remember { mutableStateOf(false) }

    val greetingText = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 4..10 -> "Selamat Pagi"
            in 11..14 -> "Selamat Siang"
            in 15..18 -> "Selamat Sore"
            else -> "Selamat Malam"
        }
    }

    val periodDisplayLabel = when (uiState.selectedPeriod) {
        "today" -> "Hari Ini"
        "7days" -> "7 Hari Terakhir"
        else -> "Bulan Ini"
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("home_screen_content"),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // 1. Top Greeting Bar (Avatar left, Greeting text, Bell + Sync indicator right)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .clickable { onNavigateToProfile() },
                            color = MaterialTheme.extendedColors.accentContainer,
                            shape = CircleShape
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                val ownerName = uiState.userProfile?.fullName ?: "Bu Sari"
                                val initial = ownerName.take(1).uppercase()
                                Text(
                                    text = initial,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.extendedColors.accent
                                )
                            }
                        }

                        Column {
                            val ownerName = uiState.userProfile?.fullName ?: "Bu Sari"
                            Text(
                                text = "$greetingText 👋",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = ownerName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SyncIndicator(
                            syncState = uiState.syncState,
                            pendingCount = uiState.pendingSyncCount,
                            onSyncClick = { viewModel.triggerSync() }
                        )

                        // Notification Bell
                        IconButton(
                            onClick = { showNotificationDialog = true },
                            modifier = Modifier.size(40.dp)
                        ) {
                            if (uiState.lowStockProducts.isNotEmpty()) {
                                BadgedBox(
                                    badge = {
                                        Badge(
                                            containerColor = MaterialTheme.extendedColors.warning
                                        ) {
                                            Text("${uiState.lowStockProducts.size}")
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = "Pemberitahuan",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.Default.NotificationsNone,
                                    contentDescription = "Pemberitahuan",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // 2. Hero Balance Card (Super-sized number, count-up animation, gradient underline)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    HeroBalanceCard(
                        title = "Total Saldo / Keuntungan",
                        amount = uiState.netProfit,
                        periodLabel = periodDisplayLabel,
                        growthPercentage = if (uiState.totalIncome > 0) {
                            val margin = ((uiState.netProfit.toDouble() / uiState.totalIncome) * 100).toInt()
                            if (margin >= 0) "+$margin%" else "$margin%"
                        } else "+0%"
                    )
                }
            }

            // 3. Period Pill Navigation Filter
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("today" to "Hari Ini", "7days" to "7 Hari", "month" to "Bulan Ini").forEach { (key, label) ->
                        val isSelected = uiState.selectedPeriod == key
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { viewModel.selectPeriod(key) },
                            color = if (isSelected) {
                                if (isDark) MaterialTheme.colorScheme.primary else Color(0xFF0F172A)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            },
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isSelected) {
                                    if (isDark) Color(0xFF0F172A) else Color.White
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            // 4. Overview 2x2 Grid (Pemasukan, Pengeluaran, Nilai Stok, Kas Tunai)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    OverviewGrid2x2(
                        incomeAmount = uiState.totalIncome,
                        expenseAmount = uiState.totalExpense,
                        stockValue = uiState.totalStockValue,
                        cashAmount = uiState.totalCumulativeBalance,
                        onIncomeClick = onNavigateToTransactions,
                        onExpenseClick = onNavigateToTransactions,
                        onStockClick = onNavigateToInventory,
                        onCashClick = onNavigateToReports
                    )
                }
            }

            // 5. Asisten Usaha AI Coach Card
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    AiBusinessCoachCard(
                        onClick = { showAiCoachBottomSheet = true }
                    )
                }
            }

            // 6. Low Stock Alert Banner (If Any)
            if (uiState.lowStockProducts.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.extendedColors.warningContainer.copy(alpha = 0.6f)
                        ),
                        border = if (isDark) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.extendedColors.warning.copy(alpha = 0.3f)) else null
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Peringatan Stok",
                                tint = MaterialTheme.extendedColors.warning,
                                modifier = Modifier.size(26.dp)
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${uiState.lowStockProducts.size} Barang Menipis / Habis",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.extendedColors.warning
                                )
                                Text(
                                    text = uiState.lowStockProducts.take(3).joinToString(", ") { it.name },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }

                            TextButton(
                                onClick = {
                                    viewModel.addLowStockToShoppingList()
                                    onNavigateToShopping()
                                }
                            ) {
                                Text(
                                    text = "+ Belanja",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.extendedColors.warning
                                )
                            }
                        }
                    }
                }
            }

            // 7. Riwayat Transaksi Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Riwayat Transaksi",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    TextButton(onClick = onNavigateToTransactions) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Lihat Semua",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.extendedColors.accent
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                contentDescription = null,
                                tint = MaterialTheme.extendedColors.accent,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }

            // 8. Recent Transaction Items List
            if (uiState.recentTransactions.isEmpty()) {
                item {
                    EmptyStateView(
                        title = "Belum Ada Catatan",
                        description = "Belum ada transaksi di periode ini.\nYuk, catat transaksi penjualan atau belanja pertamamu!",
                        buttonText = "+ Catat Transaksi",
                        onButtonClick = onNavigateToTransactions,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                items(
                    items = uiState.recentTransactions,
                    key = { it.transaction.id }
                ) { txWithItems ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        TransactionCard(
                            txWithItems = txWithItems,
                            categoryName = txWithItems.transaction.categoryId ?: if (txWithItems.transaction.type == "income") "Penjualan" else "Pengeluaran",
                            categoryIcon = null,
                            categoryColor = null,
                            onClick = { onNavigateToTransactionDetail(txWithItems.transaction.id) },
                            onEdit = { onNavigateToEditTransaction(txWithItems.transaction.id) },
                            onDelete = {
                                val txId = txWithItems.transaction.id
                                viewModel.deleteTransaction(txId)
                                scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = "Catatan berhasil dihapus",
                                        actionLabel = "Urungkan",
                                        duration = SnackbarDuration.Short
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        viewModel.restoreLastDeleted()
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Notification Dialog
    if (showNotificationDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationDialog = false },
            title = { Text("Pemberitahuan Usaha", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (uiState.lowStockProducts.isNotEmpty()) {
                        Text("⚠ ${uiState.lowStockProducts.size} produk stok menipis/habis.")
                    }
                    if (uiState.pendingSyncCount > 0) {
                        Text("☁ ${uiState.pendingSyncCount} mutasi menunggu sinkronisasi.")
                    } else {
                        Text("✓ Semua data telah tersinkron aman di perangkat Anda.")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNotificationDialog = false }) {
                    Text("Tutup", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // AI Coach Bottom Sheet
    if (showAiCoachBottomSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showAiCoachBottomSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(MaterialTheme.extendedColors.accentContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.extendedColors.accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Asisten Usaha CatatUsaha",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Wawasan cerdas untuk mengembangkan usaha Anda",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("💰 Evaluasi Arus Kas", fontWeight = FontWeight.Bold)
                        val profitStatus = if (uiState.netProfit >= 0) "Surplus Rp${MoneyFormatter.format(uiState.netProfit)}" else "Defisit"
                        Text(
                            "Arus kas Anda saat ini $profitStatus. Pengeluaran terbesar dapat ditinjau di tab Analitik.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("📦 Manajemen Stok", fontWeight = FontWeight.Bold)
                        if (uiState.lowStockProducts.isNotEmpty()) {
                            Text(
                                "Ada ${uiState.lowStockProducts.size} barang yang perlu segera dibeli ulang agar tidak kehabisan penjualan.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        } else {
                            Text(
                                "Semua stok produk berada di batas aman. Pertahankan perputaran persediaan yang sehat.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}
