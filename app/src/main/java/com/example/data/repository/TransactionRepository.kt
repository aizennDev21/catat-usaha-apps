package com.example.data.repository

import com.example.data.local.dao.CategoryDao
import com.example.data.local.dao.CategorySummary
import com.example.data.local.dao.OutboxDao
import com.example.data.local.dao.ProductDao
import com.example.data.local.dao.TopProductSummary
import com.example.data.local.dao.TransactionDao
import com.example.data.local.dao.UserDao
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.StockMovementEntity
import com.example.data.local.entity.SyncOutboxEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionItemEntity
import com.example.data.local.entity.TransactionWithItems
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val productDao: ProductDao,
    private val categoryDao: CategoryDao,
    private val userDao: UserDao,
    private val outboxDao: OutboxDao
) {
    fun getAllTransactions(): Flow<List<TransactionWithItems>> =
        transactionDao.getAllTransactionsWithItems()

    fun getTransactionsBetween(startTime: Long, endTime: Long): Flow<List<TransactionWithItems>> =
        transactionDao.getTransactionsBetween(startTime, endTime)

    suspend fun getTransactionById(id: String): TransactionWithItems? =
        transactionDao.getTransactionById(id)

    fun getAllCategories(): Flow<List<CategoryEntity>> =
        categoryDao.getAllCategories()

    fun getCategoriesByType(type: String): Flow<List<CategoryEntity>> =
        categoryDao.getCategoriesByType(type)

    suspend fun saveTransaction(
        id: String = UUID.randomUUID().toString(),
        type: String, // "income" or "expense"
        categoryId: String?,
        occurredAt: Long,
        counterparty: String?,
        paymentMethod: String,
        note: String?,
        items: List<TransactionItemEntity>,
        autoUpdateStock: Boolean
    ): String {
        val user = userDao.getUserProfileOnce()
        val userId = user?.userId ?: "user_default_sari"
        val now = System.currentTimeMillis()

        val totalAmount = items.sumOf { it.subtotal }

        val transaction = TransactionEntity(
            id = id,
            userId = userId,
            type = type,
            categoryId = categoryId,
            occurredAt = occurredAt,
            counterparty = counterparty?.trim()?.ifBlank { null },
            paymentMethod = paymentMethod,
            totalAmount = totalAmount,
            note = note?.trim()?.ifBlank { null },
            attachmentUrl = null,
            sourceType = "manual",
            createdAt = now,
            updatedAt = now,
            deletedAt = null,
            syncStatus = "pending"
        )

        val transactionItems = items.mapIndexed { index, item ->
            item.copy(
                id = if (item.id.isBlank()) UUID.randomUUID().toString() else item.id,
                transactionId = id,
                userId = userId,
                position = index,
                createdAt = now,
                updatedAt = now,
                syncStatus = "pending"
            )
        }

        // Insert transaction and items
        transactionDao.insertTransaction(transaction)
        transactionDao.insertTransactionItems(transactionItems)

        // Handle auto stock updates if requested
        if (autoUpdateStock) {
            for (item in transactionItems) {
                if (!item.productId.isNullOrBlank()) {
                    val product = productDao.getProductById(item.productId)
                    if (product != null) {
                        val movementType = if (type == "income") "out" else "in"
                        val newStock = if (type == "income") {
                            maxOf(0.0, product.stockQty - item.qty)
                        } else {
                            product.stockQty + item.qty
                        }

                        // Insert stock movement record
                        val movement = StockMovementEntity(
                            id = UUID.randomUUID().toString(),
                            userId = userId,
                            productId = product.id,
                            type = movementType,
                            qty = item.qty,
                            stockBefore = product.stockQty,
                            stockAfter = newStock,
                            unitPrice = item.unitPrice,
                            referenceType = "transaction",
                            referenceId = id,
                            note = if (type == "income") "Penjualan: ${transaction.counterparty ?: "Pelanggan"}" else "Pembelian: ${transaction.counterparty ?: "Supplier"}",
                            occurredAt = occurredAt,
                            createdAt = now,
                            updatedAt = now,
                            syncStatus = "pending"
                        )
                        productDao.insertStockMovement(movement)
                        productDao.updateStockQty(product.id, newStock, now)

                        // Stok produk ini berubah -- antre supaya ikut terkirim ke Supabase.
                        outboxDao.insertMutation(
                            SyncOutboxEntity(
                                id = UUID.randomUUID().toString(),
                                entity = "product",
                                entityId = product.id,
                                op = "upsert",
                                payload = "",
                                createdAt = now
                            )
                        )
                        // Riwayat stok baru ini juga diantre -- lihat pushStockMovement di SyncRepository.
                        outboxDao.insertMutation(
                            SyncOutboxEntity(
                                id = UUID.randomUUID().toString(),
                                entity = "stock_movement",
                                entityId = movement.id,
                                op = "upsert",
                                payload = "",
                                createdAt = now
                            )
                        )
                    }
                }
            }
        }

        // Insert into outbox for syncing
        outboxDao.insertMutation(
            SyncOutboxEntity(
                id = UUID.randomUUID().toString(),
                entity = "transaction",
                entityId = id,
                op = "upsert",
                payload = """{"id":"$id","type":"$type","total":$totalAmount}""",
                createdAt = now
            )
        )

        return id
    }

    suspend fun deleteTransaction(id: String) {
        val now = System.currentTimeMillis()
        transactionDao.softDeleteTransaction(id, now)
        transactionDao.softDeleteItemsByTransactionId(id, now)
        outboxDao.insertMutation(
            SyncOutboxEntity(
                id = UUID.randomUUID().toString(),
                entity = "transaction",
                entityId = id,
                op = "delete",
                payload = """{"id":"$id","deleted_at":$now}""",
                createdAt = now
            )
        )
    }

    suspend fun restoreTransaction(id: String) {
        transactionDao.restoreTransaction(id)
        transactionDao.restoreItemsByTransactionId(id)
    }

    fun getTotalIncomeBetween(startTime: Long, endTime: Long): Flow<Long?> =
        transactionDao.getTotalIncomeBetween(startTime, endTime)

    fun getTotalExpenseBetween(startTime: Long, endTime: Long): Flow<Long?> =
        transactionDao.getTotalExpenseBetween(startTime, endTime)

    fun getTransactionCountBetween(startTime: Long, endTime: Long): Flow<Int> =
        transactionDao.getTransactionCountBetween(startTime, endTime)

    fun getCategorySummaries(type: String, startTime: Long, endTime: Long): Flow<List<CategorySummary>> =
        transactionDao.getCategorySummaries(type, startTime, endTime)

    fun getTopSellingProducts(startTime: Long, endTime: Long): Flow<List<TopProductSummary>> =
        transactionDao.getTopSellingProducts(startTime, endTime)
}