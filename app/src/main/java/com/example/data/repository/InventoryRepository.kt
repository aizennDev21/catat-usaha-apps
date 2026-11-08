package com.example.data.repository

import com.example.data.local.dao.OutboxDao
import com.example.data.local.dao.ProductDao
import com.example.data.local.dao.UserDao
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.ProductWithMovements
import com.example.data.local.entity.StockMovementEntity
import com.example.data.local.entity.SyncOutboxEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class InventoryRepository(
    private val productDao: ProductDao,
    private val userDao: UserDao,
    private val outboxDao: OutboxDao
) {
    fun getAllProducts(): Flow<List<ProductEntity>> =
        productDao.getAllProducts()

    fun getLowStockProducts(): Flow<List<ProductEntity>> =
        productDao.getLowStockProducts()

    suspend fun getProductById(id: String): ProductEntity? =
        productDao.getProductById(id)

    fun getProductWithMovements(id: String): Flow<ProductWithMovements?> =
        productDao.getProductWithMovements(id)

    suspend fun searchProductsByName(query: String): List<ProductEntity> =
        productDao.searchProductsByName(query)

    suspend fun saveProduct(
        id: String = UUID.randomUUID().toString(),
        name: String,
        sku: String?,
        category: String?,
        unit: String,
        stockQty: Double,
        minStock: Double,
        buyPrice: Long,
        sellPrice: Long,
        note: String?
    ): String {
        val user = userDao.getUserProfileOnce()
        val userId = user?.userId ?: "user_default_sari"
        val now = System.currentTimeMillis()

        val product = ProductEntity(
            id = id,
            userId = userId,
            name = name.trim(),
            sku = sku?.trim()?.ifBlank { null },
            category = category?.trim()?.ifBlank { null },
            unit = unit.trim().ifBlank { "pcs" },
            stockQty = stockQty,
            minStock = minStock,
            buyPrice = buyPrice,
            sellPrice = sellPrice,
            note = note?.trim()?.ifBlank { null },
            isActive = true,
            createdAt = now,
            updatedAt = now,
            deletedAt = null,
            syncStatus = "pending"
        )

        productDao.insertProduct(product)

        outboxDao.insertMutation(
            SyncOutboxEntity(
                id = UUID.randomUUID().toString(),
                entity = "product",
                entityId = id,
                op = "upsert",
                payload = """{"id":"$id","name":"$name"}""",
                createdAt = now
            )
        )

        return id
    }

    suspend fun adjustStock(
        productId: String,
        deltaQty: Double,
        type: String, // "in", "out", "adjust"
        referenceType: String = "manual", // "manual", "opname"
        note: String? = null
    ) {
        val product = productDao.getProductById(productId) ?: return
        val user = userDao.getUserProfileOnce()
        val userId = user?.userId ?: "user_default_sari"
        val now = System.currentTimeMillis()

        val newStock = when (type) {
            "in" -> product.stockQty + deltaQty
            "out" -> maxOf(0.0, product.stockQty - deltaQty)
            "adjust" -> deltaQty // For opname, deltaQty is the exact physical stock counted
            else -> product.stockQty
        }

        val movement = StockMovementEntity(
            id = UUID.randomUUID().toString(),
            userId = userId,
            productId = productId,
            type = type,
            qty = if (type == "adjust") (newStock - product.stockQty) else deltaQty,
            referenceType = referenceType,
            referenceId = null,
            note = note ?: when (referenceType) {
                "opname" -> "Stok Opname (Penyesuaian Fisik)"
                else -> if (type == "in") "Penambahan Stok Manual" else "Pengurangan Stok Manual"
            },
            occurredAt = now,
            createdAt = now,
            updatedAt = now,
            deletedAt = null,
            syncStatus = "pending"
        )

        productDao.insertStockMovement(movement)
        productDao.updateStockQty(productId, newStock, now)

        outboxDao.insertMutation(
            SyncOutboxEntity(
                id = UUID.randomUUID().toString(),
                entity = "stock_movement",
                entityId = movement.id,
                op = "upsert",
                payload = """{"product_id":"$productId","type":"$type","qty":${movement.qty}}""",
                createdAt = now
            )
        )
    }

    suspend fun markOutOfStock(productId: String) {
        adjustStock(
            productId = productId,
            deltaQty = 0.0,
            type = "adjust",
            referenceType = "opname",
            note = "Ditandai habis manual"
        )
    }

    suspend fun deleteProduct(id: String) {
        val now = System.currentTimeMillis()
        productDao.softDeleteProduct(id, now)
        outboxDao.insertMutation(
            SyncOutboxEntity(
                id = UUID.randomUUID().toString(),
                entity = "product",
                entityId = id,
                op = "delete",
                payload = """{"id":"$id","deleted_at":$now}""",
                createdAt = now
            )
        )
    }
}
