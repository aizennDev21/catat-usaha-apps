package com.example.ui.screens.shopping

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.ShoppingItemEntity
import com.example.data.local.entity.ShoppingListWithItems
import com.example.ui.components.AppPrimaryButton
import com.example.ui.components.MoneyField
import com.example.ui.components.QtyStepper
import com.example.ui.theme.extendedColors
import com.example.util.MoneyFormatter
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingDetailScreen(
    listId: String,
    viewModel: ShoppingViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listWithItems by viewModel.getShoppingListById(listId).collectAsState(initial = null)
    val scope = rememberCoroutineScope()

    var showAddItemDialog by remember { mutableStateOf(false) }
    var newItemName by remember { mutableStateOf("") }
    var newItemQty by remember { mutableStateOf(1.0) }
    var newItemUnit by remember { mutableStateOf("pcs") }
    var newItemPrice by remember { mutableStateOf(0L) }

    var showConvertDialog by remember { mutableStateOf(false) }
    var supplierName by remember { mutableStateOf("") }

    if (showAddItemDialog && listWithItems != null) {
        AlertDialog(
            onDismissRequest = { showAddItemDialog = false },
            title = { Text("Tambah Barang Belanja", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newItemName,
                        onValueChange = { newItemName = it },
                        label = { Text("Nama Barang *") },
                        placeholder = { Text("Contoh: Beras Ramos") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        QtyStepper(
                            qty = newItemQty,
                            unit = newItemUnit,
                            onQtyChange = { newItemQty = it },
                            modifier = Modifier.weight(1.2f)
                        )
                        OutlinedTextField(
                            value = newItemUnit,
                            onValueChange = { newItemUnit = it },
                            label = { Text("Satuan") },
                            modifier = Modifier.weight(0.8f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }

                    MoneyField(
                        value = newItemPrice,
                        onValueChange = { newItemPrice = it },
                        label = "Perkiraan Harga Satuan"
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newItemName.isNotBlank()) {
                            val currentItems = listWithItems!!.items.toMutableList()
                            currentItems.add(
                                ShoppingItemEntity(
                                    id = UUID.randomUUID().toString(),
                                    listId = listId,
                                    userId = listWithItems!!.shoppingList.userId,
                                    name = newItemName,
                                    qty = newItemQty,
                                    unit = newItemUnit,
                                    estimatedPrice = newItemPrice,
                                    position = currentItems.size
                                )
                            )
                            viewModel.saveShoppingList(
                                id = listId,
                                title = listWithItems!!.shoppingList.title,
                                plannedDate = listWithItems!!.shoppingList.plannedDate,
                                note = listWithItems!!.shoppingList.note,
                                items = currentItems
                            )
                            newItemName = ""
                            newItemQty = 1.0
                            newItemPrice = 0L
                            showAddItemDialog = false
                        }
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Tambah")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddItemDialog = false }) {
                    Text("Batal")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showConvertDialog && listWithItems != null) {
        AlertDialog(
            onDismissRequest = { showConvertDialog = false },
            title = { Text("Catat Sebagai Pengeluaran?", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Barang belanjaan yang diceklis akan otomatis diposting ke Pengeluaran dan stoknya ditambahkan ke toko.")
                    OutlinedTextField(
                        value = supplierName,
                        onValueChange = { supplierName = it },
                        label = { Text("Nama Toko / Tempat Belanja") },
                        placeholder = { Text("Contoh: Pasar Induk") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            viewModel.convertShoppingListToExpense(listWithItems!!, supplierName)
                            showConvertDialog = false
                            onNavigateToTransactions()
                        }
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Simpan Pengeluaran")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConvertDialog = false }) {
                    Text("Batal")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = listWithItems?.shoppingList?.title ?: "Daftar Belanja",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.deleteShoppingList(listId)
                        onNavigateBack()
                    }) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.extendedColors.danger)
                    }
                }
            )
        },
        bottomBar = {
            listWithItems?.let { detail ->
                val checkedCount = detail.items.count { it.isChecked }
                val totalEstimated = detail.items.sumOf { (it.qty * it.estimatedPrice).toLong() }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
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
                                    text = "$checkedCount dari ${detail.items.size} Barang Dibeli",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = MoneyFormatter.format(totalEstimated),
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Button(
                                onClick = { showConvertDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                enabled = checkedCount > 0,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(imageVector = Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Jadikan Pengeluaran")
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddItemDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_item_to_shopping_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Tambah Barang")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (listWithItems == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val detail = listWithItems!!
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .testTag("shopping_detail_content"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (detail.items.isEmpty()) {
                    item {
                        Text(
                            text = "Daftar belanja masih kosong. Tekan tombol + untuk menambah barang.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 20.dp)
                        )
                    }
                } else {
                    items(detail.items, key = { it.id }) { item ->
                        ShoppingItemRow(
                            item = item,
                            onToggleChecked = { isChecked ->
                                viewModel.toggleItemChecked(item.id, isChecked)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ShoppingItemRow(
    item: ShoppingItemEntity,
    onToggleChecked: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onToggleChecked(!item.isChecked) },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isChecked) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = item.isChecked,
                onCheckedChange = onToggleChecked,
                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    color = if (item.isChecked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${item.qty} ${item.unit} • Est: ${MoneyFormatter.format(item.estimatedPrice)}/${item.unit}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = MoneyFormatter.format((item.qty * item.estimatedPrice).toLong()),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None
                ),
                color = if (item.isChecked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
            )
        }
    }
}
