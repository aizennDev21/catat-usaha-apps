package com.example.data.repository

import com.example.data.local.dao.OutboxDao
import com.example.data.local.dao.ProductDao
import com.example.data.local.dao.ShoppingDao
import com.example.data.local.dao.UserDao
import com.example.data.local.entity.ShoppingItemEntity
import com.example.data.local.entity.ShoppingListEntity
import com.example.data.local.entity.ShoppingListWithItems
import com.example.data.local.entity.SyncOutboxEntity
import com.example.data.local.entity.TransactionItemEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class ShoppingRepository(
    private val shoppingDao: ShoppingDao,
    private val productDao: ProductDao,
    private val userDao: UserDao,
    private val outboxDao: OutboxDao,
    private val transactionRepository: TransactionRepository
) {
    fun getAllShoppingLists(): Flow<List<ShoppingListWithItems>> =
        shoppingDao.getAllShoppingLists()

    fun getShoppingListById(id: String): Flow<ShoppingListWithItems?> =
        shoppingDao.getShoppingListById(id)

    suspend fun saveShoppingList(
        id: String = UUID.randomUUID().toString(),
        title: String,
        plannedDate: Long?,
        note: String?,
        items: List<ShoppingItemEntity>
    ): String {
        val user = userDao.getUserProfileOnce()
        val userId = user?.userId ?: "user_default_sari"
        val now = System.currentTimeMillis()

        val list = ShoppingListEntity(
            id = id,
            userId = userId,
            title = title.trim(),
            plannedDate = plannedDate,
            status = "open",
            linkedTransactionId = null,
            note = note?.trim()?.ifBlank { null },
            createdAt = now,
            updatedAt = now,
            deletedAt = null,
            syncStatus = "pending"
        )

        val listItems = items.mapIndexed { index, item ->
            item.copy(
                id = if (item.id.isBlank()) UUID.randomUUID().toString() else item.id,
                listId = id,
                userId = userId,
                position = index,
                createdAt = now,
                updatedAt = now,
                syncStatus = "pending"
            )
        }

        shoppingDao.insertShoppingList(list)
        shoppingDao.insertShoppingItems(listItems)

        outboxDao.insertMutation(
            SyncOutboxEntity(
                id = UUID.randomUUID().toString(),
                entity = "shopping_list",
                entityId = id,
                op = "upsert",
                payload = """{"id":"$id","title":"$title"}""",
                createdAt = now
            )
        )

        return id
    }

    suspend fun createListFromLowStock(title: String = "Belanja Restok Barang"): String {
        val user = userDao.getUserProfileOnce()
        val userId = user?.userId ?: "user_default_sari"
        val now = System.currentTimeMillis()

        val listId = UUID.randomUUID().toString()
        val list = ShoppingListEntity(
            id = listId,
            userId = userId,
            title = title,
            plannedDate = now + 86400000L, // Tomorrow
            status = "open",
            note = "Dibuat otomatis dari stok menipis & habis",
            createdAt = now,
            updatedAt = now
        )

        // Find products with low or 0 stock
        val products = productDao.searchProductsByName("")
        val lowStockProducts = products.filter { it.stockQty <= it.minStock }

        val items = lowStockProducts.mapIndexed { index, product ->
            val suggestedQty = if (product.stockQty <= 0) {
                maxOf(5.0, product.minStock * 2)
            } else {
                maxOf(1.0, (product.minStock * 2) - product.stockQty)
            }
            ShoppingItemEntity(
                id = UUID.randomUUID().toString(),
                listId = listId,
                userId = userId,
                productId = product.id,
                name = product.name,
                qty = suggestedQty,
                unit = product.unit,
                estimatedPrice = product.buyPrice,
                actualPrice = null,
                isChecked = false,
                position = index,
                createdAt = now,
                updatedAt = now
            )
        }

        shoppingDao.insertShoppingList(list)
        if (items.isNotEmpty()) {
            shoppingDao.insertShoppingItems(items)
        }

        return listId
    }

    suspend fun toggleItemChecked(itemId: String, isChecked: Boolean) {
        shoppingDao.setItemChecked(itemId, isChecked)
    }

    suspend fun convertToExpenseTransaction(
        listId: String,
        supplierName: String?,
        paymentMethod: String = "cash",
        autoAddStock: Boolean = true
    ): String {
        val list = shoppingDao.getShoppingListById(listId)
        // Convert checked items or all items
        val now = System.currentTimeMillis()
        val txId = UUID.randomUUID().toString()

        // Create transaction items from shopping items
        val itemsToConvert = list?.let { flow ->
            // We get the transaction items
        }

        return txId
    }

    suspend fun deleteShoppingList(id: String) {
        val now = System.currentTimeMillis()
        shoppingDao.softDeleteShoppingList(id, now)
        shoppingDao.softDeleteItemsByListId(id, now)
        outboxDao.insertMutation(
            SyncOutboxEntity(
                id = UUID.randomUUID().toString(),
                entity = "shopping_list",
                entityId = id,
                op = "delete",
                payload = """{"id":"$id","deleted_at":$now}""",
                createdAt = now
            )
        )
    }
}
