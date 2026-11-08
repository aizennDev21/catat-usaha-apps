package com.example.ui.screens.transactions

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.TransactionItemEntity
import com.example.ui.components.AppPrimaryButton
import com.example.ui.components.CategoryIconHelper
import com.example.ui.components.MoneyField
import com.example.ui.components.QtyStepper
import com.example.ui.theme.extendedColors
import com.example.util.DateUtils
import com.example.util.MoneyFormatter
import kotlinx.coroutines.launch
import java.util.UUID

data class FormItemState(
    val id: String = UUID.randomUUID().toString(),
    val productId: String? = null,
    val name: String = "",
    val qty: Double = 1.0,
    val unit: String = "pcs",
    val unitPrice: Long = 0L
) {
    val subtotal: Long get() = (qty * unitPrice).toLong()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionFormScreen(
    initialType: String = "income", // "income" or "expense"
    editTransactionId: String? = null,
    viewModel: TransactionViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var type by remember { mutableStateOf(initialType) }
    val isIncome = type == "income"

    var occurredAt by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var counterparty by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("cash") } // cash, transfer, qris, other
    var note by remember { mutableStateOf("") }
    var autoUpdateStock by remember { mutableStateOf(true) }

    val items = remember {
        mutableStateListOf(
            FormItemState(
                name = "",
                qty = 1.0,
                unit = "pcs",
                unitPrice = 0L
            )
        )
    }

    val availableProducts by viewModel.getAllProducts().collectAsState(initial = emptyList())
    val categoriesByType by viewModel.getCategoriesByType(type).collectAsState(initial = emptyList())
    var showDiscardDialog by remember { mutableStateOf(false) }

    // Load existing transaction if edit mode
    LaunchedEffect(editTransactionId) {
        if (editTransactionId != null) {
            val existing = viewModel.getTransactionById(editTransactionId)
            if (existing != null) {
                type = existing.transaction.type
                occurredAt = existing.transaction.occurredAt
                selectedCategoryId = existing.transaction.categoryId
                counterparty = existing.transaction.counterparty ?: ""
                paymentMethod = existing.transaction.paymentMethod
                note = existing.transaction.note ?: ""

                items.clear()
                if (existing.items.isNotEmpty()) {
                    existing.items.forEach { item ->
                        items.add(
                            FormItemState(
                                id = item.id,
                                productId = item.productId,
                                name = item.name,
                                qty = item.qty,
                                unit = item.unit,
                                unitPrice = item.unitPrice
                            )
                        )
                    }
                } else {
                    items.add(FormItemState())
                }
            }
        }
    }

    LaunchedEffect(categoriesByType) {
        if (selectedCategoryId == null && categoriesByType.isNotEmpty()) {
            selectedCategoryId = categoriesByType.first().id
        }
    }

    val totalAmount = items.sumOf { it.subtotal }

    BackHandler {
        if (items.any { it.name.isNotBlank() } || counterparty.isNotBlank()) {
            showDiscardDialog = true
        } else {
            onNavigateBack()
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Buang Perubahan?") },
            text = { Text("Catatan belum disimpan. Apakah kamu yakin ingin keluar?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDiscardDialog = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.extendedColors.danger)
                ) {
                    Text("Buang")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Lanjut Mengisi")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (editTransactionId != null) "Edit Catatan" else if (isIncome) "Catat Pemasukan" else "Catat Pengeluaran",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (items.any { it.name.isNotBlank() } || counterparty.isNotBlank()) {
                            showDiscardDialog = true
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isIncome) MaterialTheme.extendedColors.incomeContainer.copy(alpha = 0.4f) else MaterialTheme.extendedColors.expenseContainer.copy(alpha = 0.4f)
                )
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface,
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Total ${if (isIncome) "Pemasukan" else "Pengeluaran"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = MoneyFormatter.format(totalAmount),
                                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isIncome) MaterialTheme.extendedColors.income else MaterialTheme.extendedColors.expense
                            )
                        }

                        Text(
                            text = "${items.size} Barang",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    AppPrimaryButton(
                        text = if (editTransactionId != null) "Simpan Perubahan" else "Simpan Catatan",
                        onClick = {
                            val validItems = items.filter { it.name.isNotBlank() || it.subtotal > 0 }
                            if (validItems.isEmpty()) return@AppPrimaryButton

                            val txItems = validItems.mapIndexed { idx, itemState ->
                                TransactionItemEntity(
                                    id = itemState.id,
                                    transactionId = editTransactionId ?: UUID.randomUUID().toString(),
                                    userId = "default_umkm_user",
                                    productId = itemState.productId,
                                    name = itemState.name.ifBlank { "Barang ${idx + 1}" },
                                    qty = itemState.qty,
                                    unit = itemState.unit,
                                    unitPrice = itemState.unitPrice,
                                    subtotal = itemState.subtotal,
                                    position = idx
                                )
                            }

                            viewModel.saveTransaction(
                                id = editTransactionId,
                                type = type,
                                categoryId = selectedCategoryId,
                                occurredAt = occurredAt,
                                counterparty = counterparty.ifBlank { null },
                                paymentMethod = paymentMethod,
                                note = note.ifBlank { null },
                                items = txItems,
                                autoUpdateStock = autoUpdateStock
                            )
                            onNavigateBack()
                        },
                        modifier = Modifier.testTag("save_transaction_button")
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("transaction_form_content"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Type Switcher
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isIncome) MaterialTheme.extendedColors.income else androidx.compose.ui.graphics.Color.Transparent)
                            .clickable { type = "income" }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = null,
                                tint = if (isIncome) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Pemasukan",
                                fontWeight = FontWeight.Bold,
                                color = if (isIncome) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (!isIncome) MaterialTheme.extendedColors.expense else androidx.compose.ui.graphics.Color.Transparent)
                            .clickable { type = "expense" }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = if (!isIncome) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Pengeluaran",
                                fontWeight = FontWeight.Bold,
                                color = if (!isIncome) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Date & Counterparty
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Tanggal: ${DateUtils.formatFullDate(occurredAt)} • ${DateUtils.formatTime(occurredAt)}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        OutlinedTextField(
                            value = counterparty,
                            onValueChange = { counterparty = it },
                            label = { Text(if (isIncome) "Nama Pembeli / Pelanggan (opsional)" else "Nama Supplier / Toko (opsional)") },
                            placeholder = { Text(if (isIncome) "Contoh: Bu Anita" else "Contoh: Toko Grosir Jaya") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }
                }
            }

            // Categories Row
            item {
                Column {
                    Text(
                        text = "Kategori",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(categoriesByType) { cat ->
                            val isSelected = selectedCategoryId == cat.id
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategoryId = cat.id },
                                label = { Text(cat.name) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = CategoryIconHelper.getIcon(cat.icon),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                shape = RoundedCornerShape(50),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = if (isIncome) MaterialTheme.extendedColors.incomeContainer else MaterialTheme.extendedColors.expenseContainer
                                )
                            )
                        }
                    }
                }
            }

            // Item Editor Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Rincian Barang (${items.size})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Subtotal Otomatis",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Dynamic Item rows
            itemsIndexed(items) { index, item ->
                ItemCardEditor(
                    index = index,
                    item = item,
                    availableProducts = availableProducts,
                    isIncome = isIncome,
                    canDelete = items.size > 1,
                    onItemChange = { updated ->
                        items[index] = updated
                    },
                    onDelete = {
                        if (items.size > 1) {
                            items.removeAt(index)
                        }
                    }
                )
            }

            // Add Item button
            item {
                OutlinedButton(
                    onClick = {
                        items.add(FormItemState(name = "", qty = 1.0, unit = "pcs", unitPrice = 0L))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("add_item_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("+ Tambah Barang Lain", fontWeight = FontWeight.SemiBold)
                }
            }

            // Payment Method
            item {
                Column {
                    Text(
                        text = "Metode Pembayaran",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("cash" to "Tunai", "transfer" to "Transfer", "qris" to "QRIS", "other" to "Lainnya").forEach { (methodKey, label) ->
                            val isSelected = paymentMethod == methodKey
                            FilterChip(
                                selected = isSelected,
                                onClick = { paymentMethod = methodKey },
                                label = { Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                shape = RoundedCornerShape(50)
                            )
                        }
                    }
                }
            }

            // Auto Stock Checkbox
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { autoUpdateStock = !autoUpdateStock },
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = autoUpdateStock,
                            onCheckedChange = { autoUpdateStock = it },
                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (isIncome) "Kurangi stok otomatis" else "Tambah stok otomatis",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isIncome) "Stok produk terkait akan berkurang sesuai jumlah terjual" else "Stok produk terkait akan bertambah ke gudang",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Note Field
            item {
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Catatan Tambahan (opsional)") },
                    placeholder = { Text("Contoh: Belanja mingguan di pasar") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 2
                )
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ItemCardEditor(
    index: Int,
    item: FormItemState,
    availableProducts: List<ProductEntity>,
    isIncome: Boolean,
    canDelete: Boolean,
    onItemChange: (FormItemState) -> Unit,
    onDelete: () -> Unit
) {
    var expandedAutocomplete by remember { mutableStateOf(false) }
    val filteredProducts = if (item.name.isNotBlank()) {
        availableProducts.filter { it.name.contains(item.name, ignoreCase = true) }.take(5)
    } else emptyList()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("item_card_editor_$index"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Barang #${index + 1}",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                if (canDelete) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Hapus item",
                            tint = MaterialTheme.extendedColors.danger,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            ExposedDropdownMenuBox(
                expanded = expandedAutocomplete && filteredProducts.isNotEmpty(),
                onExpandedChange = { expandedAutocomplete = it }
            ) {
                OutlinedTextField(
                    value = item.name,
                    onValueChange = { newName ->
                        onItemChange(item.copy(name = newName))
                        expandedAutocomplete = newName.isNotBlank()
                    },
                    label = { Text("Nama Barang *") },
                    placeholder = { Text("Ketik nama atau pilih produk...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                if (filteredProducts.isNotEmpty()) {
                    ExposedDropdownMenu(
                        expanded = expandedAutocomplete,
                        onDismissRequest = { expandedAutocomplete = false }
                    ) {
                        filteredProducts.forEach { product ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(product.name, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            text = "Stok: ${product.stockQty} ${product.unit} • Harga: ${MoneyFormatter.format(if (isIncome) product.sellPrice else product.buyPrice)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Inventory2,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                onClick = {
                                    onItemChange(
                                        item.copy(
                                            productId = product.id,
                                            name = product.name,
                                            unit = product.unit,
                                            unitPrice = if (isIncome) product.sellPrice else product.buyPrice
                                        )
                                    )
                                    expandedAutocomplete = false
                                }
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                QtyStepper(
                    qty = item.qty,
                    unit = item.unit,
                    onQtyChange = { newQty ->
                        onItemChange(item.copy(qty = newQty))
                    },
                    modifier = Modifier.weight(1.2f)
                )

                OutlinedTextField(
                    value = item.unit,
                    onValueChange = { newUnit ->
                        onItemChange(item.copy(unit = newUnit))
                    },
                    label = { Text("Satuan") },
                    modifier = Modifier.weight(0.8f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            MoneyField(
                value = item.unitPrice,
                onValueChange = { newPrice ->
                    onItemChange(item.copy(unitPrice = newPrice))
                },
                label = "Harga per ${item.unit}"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Subtotal:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = MoneyFormatter.format(item.subtotal),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
