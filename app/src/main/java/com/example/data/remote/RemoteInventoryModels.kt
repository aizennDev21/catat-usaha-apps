package com.example.data.remote

import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.StockMovementEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Cerminan tabel `products` di Supabase. */
@Serializable
data class RemoteProduct(
    val id: String,
    @SerialName("user_id") val userId: String,
    val name: String,
    val sku: String? = null,
    val category: String? = null,
    val unit: String,
    @SerialName("stock_qty") val stockQty: Double,
    @SerialName("min_stock") val minStock: Double,
    @SerialName("buy_price") val buyPrice: Long,
    @SerialName("sell_price") val sellPrice: Long,
    val note: String? = null,
    @SerialName("is_active") val isActive: Boolean,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long,
    @SerialName("deleted_at") val deletedAt: Long? = null
)

fun ProductEntity.toRemote() = RemoteProduct(
    id = id,
    userId = userId,
    name = name,
    sku = sku,
    category = category,
    unit = unit,
    stockQty = stockQty,
    minStock = minStock,
    buyPrice = buyPrice,
    sellPrice = sellPrice,
    note = note,
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt
)

fun RemoteProduct.toLocal() = ProductEntity(
    id = id,
    userId = userId,
    name = name,
    sku = sku,
    category = category,
    unit = unit,
    stockQty = stockQty,
    minStock = minStock,
    buyPrice = buyPrice,
    sellPrice = sellPrice,
    note = note,
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt
)

/**
 * Cerminan tabel `stock_movements`. Tidak ada kolom `occurred_at` di Supabase (hanya
 * created_at), jadi saat ditarik kembali (toLocal), occurredAt lokal memakai createdAt
 * sebagai perkiraan terdekat -- bukan nilai jam-menit persis kejadian, tapi cukup dekat
 * untuk keperluan riwayat/laporan.
 */
@Serializable
data class RemoteStockMovement(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("product_id") val productId: String,
    val type: String,
    @SerialName("change_qty") val changeQty: Double,
    @SerialName("stock_before") val stockBefore: Double,
    @SerialName("stock_after") val stockAfter: Double,
    @SerialName("unit_price") val unitPrice: Long = 0L,
    val note: String? = null,
    @SerialName("ref_type") val refType: String? = null,
    @SerialName("ref_id") val refId: String? = null,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long,
    @SerialName("deleted_at") val deletedAt: Long? = null
)

fun StockMovementEntity.toRemote() = RemoteStockMovement(
    id = id,
    userId = userId,
    productId = productId,
    type = type,
    changeQty = qty,
    stockBefore = stockBefore,
    stockAfter = stockAfter,
    unitPrice = unitPrice,
    note = note,
    refType = referenceType,
    refId = referenceId,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt
)

fun RemoteStockMovement.toLocal() = StockMovementEntity(
    id = id,
    userId = userId,
    productId = productId,
    type = type,
    qty = changeQty,
    stockBefore = stockBefore,
    stockAfter = stockAfter,
    unitPrice = unitPrice,
    referenceType = refType,
    referenceId = refId,
    note = note,
    occurredAt = createdAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt
)
