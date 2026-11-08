package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey val userId: String,
    val fullName: String,
    val businessName: String,
    val phone: String,
    val email: String? = null,
    val passwordHash: String? = null,
    val businessType: String = "Warung & Sembako",
    val avatarUrl: String? = null,
    val currency: String = "IDR",
    val locale: String = "id_ID",
    val themeMode: String = "system", // "light", "dark", "system"
    val deductStockDefault: Boolean = true,
    val addStockDefault: Boolean = true,
    val lowStockNotification: Boolean = true,
    val pinLock: String? = null,
    val isLoggedIn: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val type: String, // "income", "expense"
    val icon: String,
    val color: String, // hex
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val syncStatus: String = "synced" // "synced", "pending", "conflict"
)

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val sku: String? = null,
    val category: String? = null,
    val unit: String = "pcs",
    val stockQty: Double = 0.0,
    val minStock: Double = 0.0,
    val buyPrice: Long = 0L,
    val sellPrice: Long = 0L,
    val note: String? = null,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val syncStatus: String = "synced"
)

@Entity(tableName = "stock_movements")
data class StockMovementEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val productId: String,
    val type: String, // "in", "out", "adjust"
    val qty: Double,
    // --- Ditambahkan: kolom ini WAJIB diisi di tabel Supabase (NOT NULL, tanpa default),
    // supaya riwayat stok bisa disinkronkan. ---
    val stockBefore: Double = 0.0,
    val stockAfter: Double = 0.0,
    val unitPrice: Long = 0L,
    // --- akhir tambahan ---
    val referenceType: String? = null, // "transaction", "manual", "opname"
    val referenceId: String? = null,
    val note: String? = null,
    val occurredAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val syncStatus: String = "synced"
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val type: String, // "income", "expense"
    val categoryId: String? = null,
    val occurredAt: Long = System.currentTimeMillis(),
    val counterparty: String? = null, // customer for income, supplier for expense
    val paymentMethod: String = "cash", // "cash", "transfer", "qris", "other"
    val totalAmount: Long = 0L,
    val note: String? = null,
    val attachmentUrl: String? = null,
    val sourceType: String? = "manual", // "manual", "shopping_list"
    val sourceId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val syncStatus: String = "synced"
)

@Entity(tableName = "transaction_items")
data class TransactionItemEntity(
    @PrimaryKey val id: String,
    val transactionId: String,
    val userId: String,
    val productId: String? = null,
    val name: String,
    val qty: Double,
    val unit: String = "pcs",
    val unitPrice: Long,
    val subtotal: Long,
    val position: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val syncStatus: String = "synced"
)

@Entity(tableName = "shopping_lists")
data class ShoppingListEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val plannedDate: Long? = null,
    val status: String = "open", // "open", "done", "cancelled"
    val linkedTransactionId: String? = null,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val syncStatus: String = "synced"
)

@Entity(tableName = "shopping_items")
data class ShoppingItemEntity(
    @PrimaryKey val id: String,
    val listId: String,
    val userId: String,
    val productId: String? = null,
    val name: String,
    val qty: Double,
    val unit: String = "pcs",
    val estimatedPrice: Long = 0L,
    val actualPrice: Long? = null,
    val isChecked: Boolean = false,
    val checkedAt: Long? = null,
    val position: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val syncStatus: String = "synced"
)

@Entity(tableName = "sync_outbox")
data class SyncOutboxEntity(
    @PrimaryKey val id: String, // mutation_id UUID
    val entity: String, // "transaction", "product", "category", "profile", "stock_movement"
    val entityId: String,
    val op: String, // "upsert", "delete"
    val payload: String, // JSON (tidak dipakai lagi, SyncRepository baca langsung dari Room)
    val baseVersion: Int = 0,
    val attempts: Int = 0,
    val lastError: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val nextRetryAt: Long? = null
)