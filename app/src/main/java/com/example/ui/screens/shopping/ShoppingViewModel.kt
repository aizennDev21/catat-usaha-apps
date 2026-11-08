package com.example.ui.screens.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.ShoppingItemEntity
import com.example.data.local.entity.ShoppingListEntity
import com.example.data.local.entity.ShoppingListWithItems
import com.example.data.local.entity.TransactionItemEntity
import com.example.data.repository.InventoryRepository
import com.example.data.repository.ShoppingRepository
import com.example.data.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class ShoppingViewModel(
    private val shoppingRepository: ShoppingRepository,
    private val transactionRepository: TransactionRepository,
    private val inventoryRepository: InventoryRepository
) : ViewModel() {

    val shoppingLists: StateFlow<List<ShoppingListWithItems>> =
        shoppingRepository.getAllShoppingLists()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    fun getShoppingListById(id: String): Flow<ShoppingListWithItems?> =
        shoppingRepository.getShoppingListById(id)

    fun toggleItemChecked(itemId: String, isChecked: Boolean) {
        viewModelScope.launch {
            shoppingRepository.toggleItemChecked(itemId, isChecked)
        }
    }

    fun createListFromLowStock(title: String = "Belanja Restok Barang") {
        viewModelScope.launch {
            shoppingRepository.createListFromLowStock(title)
        }
    }

    fun saveShoppingList(
        id: String?,
        title: String,
        plannedDate: Long?,
        note: String?,
        items: List<ShoppingItemEntity>
    ) {
        viewModelScope.launch {
            shoppingRepository.saveShoppingList(
                id = id ?: UUID.randomUUID().toString(),
                title = title,
                plannedDate = plannedDate,
                note = note,
                items = items
            )
        }
    }

    fun deleteShoppingList(id: String) {
        viewModelScope.launch {
            shoppingRepository.deleteShoppingList(id)
        }
    }

    suspend fun convertShoppingListToExpense(
        listWithItems: ShoppingListWithItems,
        supplierName: String,
        paymentMethod: String = "cash",
        autoAddStock: Boolean = true
    ): String {
        val checkedItems = listWithItems.items.filter { it.isChecked }.ifEmpty { listWithItems.items }
        val now = System.currentTimeMillis()
        val txId = UUID.randomUUID().toString()

        val transactionItems = checkedItems.mapIndexed { index, shopItem ->
            val finalPrice = shopItem.actualPrice ?: shopItem.estimatedPrice
            TransactionItemEntity(
                id = UUID.randomUUID().toString(),
                transactionId = txId,
                userId = shopItem.userId,
                productId = shopItem.productId,
                name = shopItem.name,
                qty = shopItem.qty,
                unit = shopItem.unit,
                unitPrice = finalPrice,
                subtotal = (shopItem.qty * finalPrice).toLong(),
                position = index
            )
        }

        transactionRepository.saveTransaction(
            id = txId,
            type = "expense",
            categoryId = "cat_belanja_barang",
            occurredAt = now,
            counterparty = supplierName.ifBlank { "Belanja Barang (${listWithItems.shoppingList.title})" },
            paymentMethod = paymentMethod,
            note = "Dikonversi dari daftar belanja: ${listWithItems.shoppingList.title}",
            items = transactionItems,
            autoUpdateStock = autoAddStock
        )

        return txId
    }
}
