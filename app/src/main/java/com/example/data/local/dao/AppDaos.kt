package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.ProductWithMovements
import com.example.data.local.entity.ShoppingItemEntity
import com.example.data.local.entity.ShoppingListEntity
import com.example.data.local.entity.ShoppingListWithItems
import com.example.data.local.entity.StockMovementEntity
import com.example.data.local.entity.SyncOutboxEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionItemEntity
import com.example.data.local.entity.TransactionWithItems
import com.example.data.local.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow

data class CategorySummary(
    val categoryId: String?,
    val total: Long,
    val count: Int
)

data class TopProductSummary(
    val name: String,
    val totalQty: Double,
    val totalRevenue: Long
)

data class DailySummary(
    val dayTimestamp: Long,
    val income: Long,
    val expense: Long
)

@Dao
interface TransactionDao {
    @Transaction
    @Query("SELECT * FROM transactions WHERE deletedAt IS NULL ORDER BY occurredAt DESC")
    fun getAllTransactionsWithItems(): Flow<List<TransactionWithItems>>

    @Transaction
    @Query("SELECT * FROM transactions WHERE deletedAt IS NULL AND occurredAt BETWEEN :startTime AND :endTime ORDER BY occurredAt DESC")
    fun getTransactionsBetween(startTime: Long, endTime: Long): Flow<List<TransactionWithItems>>

    @Transaction
    @Query("SELECT * FROM transactions WHERE id = :id AND deletedAt IS NULL")
    suspend fun getTransactionById(id: String): TransactionWithItems?

    // --- Ditambahkan untuk SyncRepository: butuh baris mentah, bukan relasi gabungan,
    // supaya tidak bergantung pada bentuk TransactionWithItems yang bisa berubah. ---
    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionEntityById(id: String): TransactionEntity?

    @Query("SELECT * FROM transaction_items WHERE transactionId = :transactionId ORDER BY position ASC")
    suspend fun getItemsByTransactionId(transactionId: String): List<TransactionItemEntity>
    // --- akhir tambahan ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactionItems(items: List<TransactionItemEntity>)

    @Query("UPDATE transactions SET deletedAt = :deletedAt, syncStatus = 'pending' WHERE id = :id")
    suspend fun softDeleteTransaction(id: String, deletedAt: Long = System.currentTimeMillis())

    @Query("UPDATE transaction_items SET deletedAt = :deletedAt, syncStatus = 'pending' WHERE transactionId = :transactionId")
    suspend fun softDeleteItemsByTransactionId(transactionId: String, deletedAt: Long = System.currentTimeMillis())

    @Query("UPDATE transactions SET deletedAt = NULL, syncStatus = 'pending' WHERE id = :id")
    suspend fun restoreTransaction(id: String)

    @Query("UPDATE transaction_items SET deletedAt = NULL, syncStatus = 'pending' WHERE transactionId = :transactionId")
    suspend fun restoreItemsByTransactionId(transactionId: String)

    @Query("SELECT SUM(totalAmount) FROM transactions WHERE type = 'income' AND deletedAt IS NULL AND occurredAt BETWEEN :startTime AND :endTime")
    fun getTotalIncomeBetween(startTime: Long, endTime: Long): Flow<Long?>

    @Query("SELECT SUM(totalAmount) FROM transactions WHERE type = 'expense' AND deletedAt IS NULL AND occurredAt BETWEEN :startTime AND :endTime")
    fun getTotalExpenseBetween(startTime: Long, endTime: Long): Flow<Long?>

    @Query("SELECT COUNT(*) FROM transactions WHERE deletedAt IS NULL AND occurredAt BETWEEN :startTime AND :endTime")
    fun getTransactionCountBetween(startTime: Long, endTime: Long): Flow<Int>

    @Query("""
        SELECT categoryId, SUM(totalAmount) as total, COUNT(*) as count 
        FROM transactions 
        WHERE type = :type AND deletedAt IS NULL AND occurredAt BETWEEN :startTime AND :endTime 
        GROUP BY categoryId 
        ORDER BY total DESC
    """)
    fun getCategorySummaries(type: String, startTime: Long, endTime: Long): Flow<List<CategorySummary>>

    @Query("""
        SELECT ti.name, SUM(ti.qty) as totalQty, SUM(ti.subtotal) as totalRevenue 
        FROM transaction_items ti 
        INNER JOIN transactions t ON ti.transactionId = t.id 
        WHERE t.type = 'income' AND t.deletedAt IS NULL AND ti.deletedAt IS NULL AND t.occurredAt BETWEEN :startTime AND :endTime 
        GROUP BY LOWER(ti.name) 
        ORDER BY totalRevenue DESC 
        LIMIT 10
    """)
    fun getTopSellingProducts(startTime: Long, endTime: Long): Flow<List<TopProductSummary>>
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE deletedAt IS NULL ORDER BY name ASC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id AND deletedAt IS NULL")
    suspend fun getProductById(id: String): ProductEntity?

    @Transaction
    @Query("SELECT * FROM products WHERE id = :id AND deletedAt IS NULL")
    fun getProductWithMovements(id: String): Flow<ProductWithMovements?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Query("UPDATE products SET stockQty = :newStock, updatedAt = :updatedAt, syncStatus = 'pending' WHERE id = :id")
    suspend fun updateStockQty(id: String, newStock: Double, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE products SET deletedAt = :deletedAt, syncStatus = 'pending' WHERE id = :id")
    suspend fun softDeleteProduct(id: String, deletedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM products WHERE deletedAt IS NULL AND (stockQty <= minStock OR stockQty <= 0) ORDER BY stockQty ASC")
    fun getLowStockProducts(): Flow<List<ProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockMovement(movement: StockMovementEntity)

    // Ditambahkan untuk SyncRepository: ambil satu baris riwayat stok berdasarkan ID.
    @Query("SELECT * FROM stock_movements WHERE id = :id")
    suspend fun getStockMovementById(id: String): StockMovementEntity?

    @Query("SELECT * FROM stock_movements WHERE productId = :productId AND deletedAt IS NULL ORDER BY occurredAt DESC")
    fun getMovementsForProduct(productId: String): Flow<List<StockMovementEntity>>

    @Query("SELECT * FROM products WHERE deletedAt IS NULL AND name LIKE '%' || :query || '%' LIMIT 10")
    suspend fun searchProductsByName(query: String): List<ProductEntity>
}

@Dao
interface ShoppingDao {
    @Transaction
    @Query("SELECT * FROM shopping_lists WHERE deletedAt IS NULL ORDER BY createdAt DESC")
    fun getAllShoppingLists(): Flow<List<ShoppingListWithItems>>

    @Transaction
    @Query("SELECT * FROM shopping_lists WHERE id = :id AND deletedAt IS NULL")
    fun getShoppingListById(id: String): Flow<ShoppingListWithItems?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingList(list: ShoppingListEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingItems(items: List<ShoppingItemEntity>)

    @Update
    suspend fun updateShoppingItem(item: ShoppingItemEntity)

    @Query("UPDATE shopping_items SET isChecked = :isChecked, checkedAt = :checkedAt WHERE id = :itemId")
    suspend fun setItemChecked(itemId: String, isChecked: Boolean, checkedAt: Long? = if (isChecked) System.currentTimeMillis() else null)

    @Query("UPDATE shopping_lists SET status = :status, linkedTransactionId = :txId, updatedAt = :now WHERE id = :listId")
    suspend fun updateListStatus(listId: String, status: String, txId: String? = null, now: Long = System.currentTimeMillis())

    @Query("UPDATE shopping_lists SET deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDeleteShoppingList(id: String, deletedAt: Long = System.currentTimeMillis())

    @Query("UPDATE shopping_items SET deletedAt = :deletedAt WHERE listId = :listId")
    suspend fun softDeleteItemsByListId(listId: String, deletedAt: Long = System.currentTimeMillis())
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE deletedAt IS NULL ORDER BY name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE type = :type AND deletedAt IS NULL ORDER BY name ASC")
    fun getCategoriesByType(type: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: String): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)
}

@Dao
interface UserDao {
    @Query("SELECT * FROM user_profiles WHERE isLoggedIn = 1 LIMIT 1")
    fun getUserProfile(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profiles WHERE isLoggedIn = 1 LIMIT 1")
    suspend fun getUserProfileOnce(): UserProfileEntity?

    @Query("SELECT * FROM user_profiles WHERE email = :identifier OR phone = :identifier LIMIT 1")
    suspend fun findUserByIdentifier(identifier: String): UserProfileEntity?

    @Query("SELECT * FROM user_profiles")
    fun getAllUsers(): Flow<List<UserProfileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: UserProfileEntity)

    @Query("UPDATE user_profiles SET isLoggedIn = 0")
    suspend fun logoutAll()

    @Query("UPDATE user_profiles SET isLoggedIn = 1, updatedAt = :now WHERE userId = :userId")
    suspend fun setLoggedIn(userId: String, now: Long = System.currentTimeMillis())
}

@Dao
interface OutboxDao {
    @Query("SELECT * FROM sync_outbox ORDER BY createdAt ASC")
    fun getAllPendingMutations(): Flow<List<SyncOutboxEntity>>

    @Query("SELECT COUNT(*) FROM sync_outbox")
    fun getPendingMutationCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMutation(mutation: SyncOutboxEntity)

    @Query("DELETE FROM sync_outbox WHERE id = :id")
    suspend fun deleteMutation(id: String)

    @Query("DELETE FROM sync_outbox")
    suspend fun clearAll()
}