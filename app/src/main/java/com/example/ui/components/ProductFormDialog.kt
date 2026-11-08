package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.ProductEntity
import com.example.util.MoneyFormatter

@Composable
fun ProductFormDialog(
    initialProduct: ProductEntity? = null,
    onDismiss: () -> Unit,
    onSave: (
        id: String?,
        name: String,
        sku: String?,
        category: String?,
        unit: String,
        stockQty: Double,
        minStock: Double,
        buyPrice: Long,
        sellPrice: Long,
        note: String?
    ) -> Unit
) {
    var name by remember { mutableStateOf(initialProduct?.name ?: "") }
    var sku by remember { mutableStateOf(initialProduct?.sku ?: "") }
    var category by remember { mutableStateOf(initialProduct?.category ?: "Sembako") }
    var unit by remember { mutableStateOf(initialProduct?.unit ?: "pcs") }
    var stockQtyInput by remember { mutableStateOf(if (initialProduct != null) "${initialProduct.stockQty}" else "0") }
    var minStockInput by remember { mutableStateOf(if (initialProduct != null) "${initialProduct.minStock}" else "5") }
    var buyPrice by remember { mutableStateOf(initialProduct?.buyPrice ?: 0L) }
    var sellPrice by remember { mutableStateOf(initialProduct?.sellPrice ?: 0L) }
    var note by remember { mutableStateOf(initialProduct?.note ?: "") }

    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialProduct != null) "Edit Produk" else "Tambah Produk Baru",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (it.isNotBlank()) isError = false
                    },
                    label = { Text("Nama Produk / Barang *") },
                    isError = isError,
                    supportingText = if (isError) { { Text("Nama produk tidak boleh kosong") } } else null,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Kategori") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Satuan (kg/pcs)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = sku,
                        onValueChange = { sku = it },
                        label = { Text("Kode SKU (opsional)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = minStockInput,
                        onValueChange = { minStockInput = it },
                        label = { Text("Batas Menipis") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                if (initialProduct == null) {
                    OutlinedTextField(
                        value = stockQtyInput,
                        onValueChange = { stockQtyInput = it },
                        label = { Text("Stok Awal ($unit)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                MoneyField(
                    value = buyPrice,
                    onValueChange = { buyPrice = it },
                    label = "Harga Beli (Modal)"
                )

                MoneyField(
                    value = sellPrice,
                    onValueChange = { sellPrice = it },
                    label = "Harga Jual"
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Catatan Produk (opsional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        isError = true
                        return@Button
                    }
                    val stock = stockQtyInput.toDoubleOrNull() ?: 0.0
                    val minStock = minStockInput.toDoubleOrNull() ?: 5.0
                    onSave(
                        initialProduct?.id,
                        name,
                        sku,
                        category,
                        unit,
                        stock,
                        minStock,
                        buyPrice,
                        sellPrice,
                        note
                    )
                },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Simpan Produk")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
