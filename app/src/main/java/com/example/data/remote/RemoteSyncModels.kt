package com.example.data.remote

import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionItemEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Cerminan tabel `categories` di Supabase.
 * Field lokal `syncStatus` sengaja TIDAK ada di sini, karena itu status lokal saja
 * (kolomnya tidak ada di Supabase).
 */
@Serializable
data class RemoteCategory(
    val id: String,
    @SerialName("user_id") val userId: String,
    val name: String,
    val type: String,
    val icon: String,
    val color: String,
    @SerialName("is_default") val isDefault: Boolean,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long,
    @SerialName("deleted_at") val deletedAt: Long? = null
)

/**
 * Cerminan tabel `transactions`.
 * `attachmentUrl` dan `sourceId` di entity lokal BELUM punya kolom di Supabase,
 * jadi sengaja tidak dikirim. Kalau nanti dipakai, tambah dulu kolomnya lewat
 * `alter table transactions add column ...`, baru tambahkan ke sini.
 */
@Serializable
data class RemoteTransaction(
    val id: String,
    @SerialName("user_id") val userId: String,
    val type: String,
    @SerialName("category_id") val categoryId: String? = null,
    @SerialName("occurred_at") val occurredAt: Long,
    val counterparty: String? = null,
    @SerialName("payment_method") val paymentMethod: String,
    @SerialName("total_amount") val totalAmount: Long,
    val note: String? = null,
    @SerialName("source_type") val sourceType: String? = null,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long,
    @SerialName("deleted_at") val deletedAt: Long? = null
)

/** Cerminan tabel `transaction_items`. */
@Serializable
data class RemoteTransactionItem(
    val id: String,
    @SerialName("transaction_id") val transactionId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("product_id") val productId: String? = null,
    val name: String,
    val qty: Double,
    val unit: String,
    @SerialName("unit_price") val unitPrice: Long,
    val subtotal: Long,
    val position: Int,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long,
    @SerialName("deleted_at") val deletedAt: Long? = null
)

/** Payload kecil untuk soft-delete: hanya mengisi kolom deleted_at lewat .update(). */
@Serializable
data class SoftDelete(@SerialName("deleted_at") val deletedAt: Long)

fun CategoryEntity.toRemote() = RemoteCategory(
    id = id,
    userId = userId,
    name = name,
    type = type,
    icon = icon,
    color = color,
    isDefault = isDefault,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt
)

fun TransactionEntity.toRemote() = RemoteTransaction(
    id = id,
    userId = userId,
    type = type,
    categoryId = categoryId,
    occurredAt = occurredAt,
    counterparty = counterparty,
    paymentMethod = paymentMethod,
    totalAmount = totalAmount,
    note = note,
    sourceType = sourceType,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt
)

fun TransactionItemEntity.toRemote() = RemoteTransactionItem(
    id = id,
    transactionId = transactionId,
    userId = userId,
    productId = productId,
    name = name,
    qty = qty,
    unit = unit,
    unitPrice = unitPrice,
    subtotal = subtotal,
    position = position,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt
)

// ------------------------------------------------------- arah sebaliknya (pull)

/** syncStatus tidak diisi di sini, jadi otomatis memakai default "synced" di entity. */
fun RemoteCategory.toLocal() = CategoryEntity(
    id = id,
    userId = userId,
    name = name,
    type = type,
    icon = icon,
    color = color,
    isDefault = isDefault,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt
)

fun RemoteTransaction.toLocal() = TransactionEntity(
    id = id,
    userId = userId,
    type = type,
    categoryId = categoryId,
    occurredAt = occurredAt,
    counterparty = counterparty,
    paymentMethod = paymentMethod,
    totalAmount = totalAmount,
    note = note,
    sourceType = sourceType,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt
)

fun RemoteTransactionItem.toLocal() = TransactionItemEntity(
    id = id,
    transactionId = transactionId,
    userId = userId,
    productId = productId,
    name = name,
    qty = qty,
    unit = unit,
    unitPrice = unitPrice,
    subtotal = subtotal,
    position = position,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt
)