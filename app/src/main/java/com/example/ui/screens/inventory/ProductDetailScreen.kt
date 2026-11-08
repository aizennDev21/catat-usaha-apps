package com.example.ui.screens.inventory

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.StockMovementEntity
import com.example.ui.components.ProductFormDialog
import com.example.ui.components.StockBadge
import com.example.ui.components.StockOpnameDialog
import com.example.ui.theme.extendedColors
import com.example.util.DateUtils
import com.example.util.MoneyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    productId: String,
    viewModel: InventoryViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val productWithMovements by viewModel.getProductWithMovements(productId).collectAsState(initial = null)
    var showEditDialog by remember { mutableStateOf(false) }
    var showOpnameDialog by remember { mutableStateOf(false) }

    if (showEditDialog && productWithMovements?.product != null) {
        ProductFormDialog(
            initialProduct = productWithMovements!!.product,
            onDismiss = { showEditDialog = false },
            onSave = { id, name, sku, category, unit, stockQty, minStock, buyPrice, sellPrice, note ->
                viewModel.saveProduct(id, name, sku, category, unit, stockQty, minStock, buyPrice, sellPrice, note)
                showEditDialog = false
            }
        )
    }

    if (showOpnameDialog && productWithMovements?.product != null) {
        StockOpnameDialog(
            product = productWithMovements!!.product,
            onDismiss = { showOpnameDialog = false },
            onConfirm = { count, note ->
                viewModel.performStockOpname(productId, count, note)
                showOpnameDialog = false
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Detail Produk", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Produk")
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (productWithMovements == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val product = productWithMovements!!.product
            val movements = productWithMovements!!.movements

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .testTag("product_detail_content"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Main Product Stats Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder()
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
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = product.name,
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (!product.sku.isNullOrBlank()) {
                                        Text(
                                            text = "SKU: ${product.sku} • Kategori: ${product.category ?: "-"}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                StockBadge(stockQty = product.stockQty, minStock = product.minStock)
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Big Current Stock Box
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                        RoundedCornerShape(14.dp)
                                    )
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Stok Saat Ini",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "${product.stockQty} ${product.unit}",
                                        style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Batas stok menipis: ${product.minStock} ${product.unit}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Button(
                                    onClick = { showOpnameDialog = true },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Stok Opname")
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Pricing Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Harga Jual", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(MoneyFormatter.format(product.sellPrice), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                }
                                Column {
                                    Text("Harga Beli (Modal)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(MoneyFormatter.format(product.buyPrice), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                }
                                Column {
                                    Text("Margin Untung", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    val margin = product.sellPrice - product.buyPrice
                                    Text(
                                        MoneyFormatter.format(margin),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.extendedColors.income
                                    )
                                }
                            }
                        }
                    }
                }

                // Movement History Header
                item {
                    Text(
                        text = "Riwayat Keluar Masuk Stok (${movements.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (movements.isEmpty()) {
                    item {
                        Text(
                            text = "Belum ada riwayat pergerakan stok untuk produk ini.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                } else {
                    items(movements) { movement ->
                        StockMovementItemCard(movement = movement, unit = product.unit)
                    }
                }
            }
        }
    }
}

@Composable
private fun StockMovementItemCard(movement: StockMovementEntity, unit: String) {
    val isIn = movement.type == "in"
    val isAdjust = movement.type == "adjust"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(
                        when {
                            isAdjust -> MaterialTheme.extendedColors.warningContainer
                            isIn -> MaterialTheme.extendedColors.incomeContainer
                            else -> MaterialTheme.extendedColors.expenseContainer
                        },
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        isAdjust -> Icons.Default.SyncAlt
                        isIn -> Icons.Default.ArrowUpward
                        else -> Icons.Default.ArrowDownward
                    },
                    contentDescription = null,
                    tint = when {
                        isAdjust -> MaterialTheme.extendedColors.warning
                        isIn -> MaterialTheme.extendedColors.income
                        else -> MaterialTheme.extendedColors.expense
                    },
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = movement.note ?: when (movement.type) {
                        "in" -> "Barang Masuk"
                        "out" -> "Barang Terjual / Keluar"
                        else -> "Penyesuaian Opname"
                    },
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${DateUtils.formatFullDate(movement.occurredAt)} • ${DateUtils.formatTime(movement.occurredAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "${if (isIn) "+" else if (movement.qty > 0 && !isAdjust) "-" else ""}${movement.qty} $unit",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = when {
                    isAdjust -> MaterialTheme.extendedColors.warning
                    isIn -> MaterialTheme.extendedColors.income
                    else -> MaterialTheme.extendedColors.expense
                }
            )
        }
    }
}
