package com.example.ui.screens.transactions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.TransactionWithItems
import com.example.ui.components.EmptyStateView
import com.example.ui.components.TransactionCard
import com.example.ui.theme.extendedColors
import com.example.util.DateUtils
import com.example.util.MoneyFormatter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    viewModel: TransactionViewModel,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToEdit: (String) -> Unit,
    onNavigateToExport: () -> Unit,
    onAddNewTransaction: (String) -> Unit, // "income" or "expense"
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.listUiState.collectAsState()
    var isSearchOpen by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "Catatan Keuangan",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    },
                    actions = {
                        IconButton(onClick = { isSearchOpen = !isSearchOpen }) {
                            Icon(
                                imageVector = if (isSearchOpen) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = "Cari"
                            )
                        }
                        IconButton(onClick = onNavigateToExport) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = "Ekspor Laporan",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                // Search Box if active
                AnimatedVisibility(visible = isSearchOpen) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text("Cari barang, pelanggan, toko...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("transaction_search_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            trailingIcon = {
                                if (uiState.searchQuery.isNotBlank()) {
                                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                        Icon(imageVector = Icons.Default.Close, contentDescription = "Hapus")
                                    }
                                }
                            }
                        )
                    }
                }

                // Filter Row: Type & Period
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    // Type Filter Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("all" to "Semua", "income" to "▲ Pemasukan", "expense" to "▼ Pengeluaran").forEach { (typeKey, label) ->
                            val isSelected = uiState.filterType == typeKey
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setFilterType(typeKey) },
                                label = { Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                shape = RoundedCornerShape(50),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = when (typeKey) {
                                        "income" -> MaterialTheme.extendedColors.incomeContainer
                                        "expense" -> MaterialTheme.extendedColors.expenseContainer
                                        else -> MaterialTheme.colorScheme.primaryContainer
                                    },
                                    selectedLabelColor = when (typeKey) {
                                        "income" -> MaterialTheme.extendedColors.income
                                        "expense" -> MaterialTheme.extendedColors.expense
                                        else -> MaterialTheme.colorScheme.onPrimaryContainer
                                    }
                                )
                            )
                        }
                    }

                    // Period Filter Chips
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val periods = listOf("all" to "Semua Periode", "today" to "Hari Ini", "7days" to "7 Hari", "month" to "Bulan Ini")
                        items(periods) { (periodKey, label) ->
                            val isSelected = uiState.filterPeriod == periodKey
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setFilterPeriod(periodKey) },
                                label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                                shape = RoundedCornerShape(50)
                            )
                        }
                    }
                }

                // Summary Strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = "Masuk: ${MoneyFormatter.format(uiState.totalIncome)}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.extendedColors.income
                        )
                        Text(
                            text = "Keluar: ${MoneyFormatter.format(uiState.totalExpense)}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.extendedColors.expense
                        )
                    }
                    Text(
                        text = "${uiState.transactions.size} transaksi",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (uiState.transactions.isEmpty()) {
            EmptyStateView(
                title = "Tidak Ada Catatan",
                description = if (uiState.searchQuery.isNotBlank()) "Tidak ditemukan catatan sesuai kata kunci" else "Belum ada catatan pada filter ini.",
                buttonText = "+ Catat Pemasukan",
                onButtonClick = { onAddNewTransaction("income") },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            // Group transactions by date
            val grouped = uiState.transactions.groupBy {
                DateUtils.formatFullDate(it.transaction.occurredAt)
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .testTag("transactions_list"),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                grouped.forEach { (dateHeader, transactionsForDay) ->
                    val dayIncome = transactionsForDay.filter { it.transaction.type == "income" }.sumOf { it.transaction.totalAmount }
                    val dayExpense = transactionsForDay.filter { it.transaction.type == "expense" }.sumOf { it.transaction.totalAmount }
                    val dayNet = dayIncome - dayExpense

                    // Date Header with daily total
                    item(key = "header_$dateHeader") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = dateHeader,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Text(
                                text = MoneyFormatter.formatSigned(dayNet, dayNet >= 0),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (dayNet >= 0) MaterialTheme.extendedColors.income else MaterialTheme.extendedColors.expense
                            )
                        }
                    }

                    items(
                        items = transactionsForDay,
                        key = { it.transaction.id }
                    ) { txWithItems ->
                        val category = uiState.categories.find { it.id == txWithItems.transaction.categoryId }

                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                            TransactionCard(
                                txWithItems = txWithItems,
                                categoryName = category?.name,
                                categoryIcon = category?.icon ?: (if (txWithItems.transaction.type == "income") "storefront" else "shopping_basket"),
                                categoryColor = category?.color ?: (if (txWithItems.transaction.type == "income") "#15803D" else "#BE123C"),
                                onClick = { onNavigateToDetail(txWithItems.transaction.id) },
                                onEdit = { onNavigateToEdit(txWithItems.transaction.id) },
                                onDelete = {
                                    viewModel.deleteTransaction(txWithItems.transaction.id)
                                    scope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = "Catatan dihapus",
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
    }
}
