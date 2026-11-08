package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.CategoryDao
import com.example.data.local.dao.OutboxDao
import com.example.data.local.dao.ProductDao
import com.example.data.local.dao.ShoppingDao
import com.example.data.local.dao.TransactionDao
import com.example.data.local.dao.UserDao
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.ShoppingItemEntity
import com.example.data.local.entity.ShoppingListEntity
import com.example.data.local.entity.StockMovementEntity
import com.example.data.local.entity.SyncOutboxEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransactionItemEntity
import com.example.data.local.entity.UserProfileEntity

@Database(
    entities = [
        UserProfileEntity::class,
        CategoryEntity::class,
        ProductEntity::class,
        StockMovementEntity::class,
        TransactionEntity::class,
        TransactionItemEntity::class,
        ShoppingListEntity::class,
        ShoppingItemEntity::class,
        SyncOutboxEntity::class
    ],
    // v2->v3: hapus data dummy. v3->v4: StockMovementEntity bertambah kolom
    // (stockBefore, stockAfter, unitPrice) supaya riwayat stok bisa disinkronkan.
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun categoryDao(): CategoryDao
    abstract fun productDao(): ProductDao
    abstract fun transactionDao(): TransactionDao
    abstract fun shoppingDao(): ShoppingDao
    abstract fun outboxDao(): OutboxDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "catatusaha.db"
                )
                    // PERINGATAN: ini menghapus SEMUA data lokal setiap versi naik.
                    // Aman selama belum ada pengguna sungguhan. Sebelum rilis publik,
                    // ganti dengan Migration yang benar.
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}