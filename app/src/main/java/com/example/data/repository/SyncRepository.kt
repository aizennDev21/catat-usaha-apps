package com.example.data.repository

import android.util.Log
import com.example.data.local.dao.CategoryDao
import com.example.data.local.dao.OutboxDao
import com.example.data.local.dao.ProductDao
import com.example.data.local.dao.TransactionDao
import com.example.data.local.dao.UserDao
import com.example.data.local.entity.SyncOutboxEntity
import com.example.data.remote.RemoteCategory
import com.example.data.remote.RemoteProduct
import com.example.data.remote.RemoteStockMovement
import com.example.data.remote.RemoteTransaction
import com.example.data.remote.RemoteTransactionItem
import com.example.data.remote.SoftDelete
import com.example.data.remote.SupabaseProvider
import com.example.data.remote.toLocal
import com.example.data.remote.toRemote
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

sealed class SyncState {
    data object Synced : SyncState()
    data object Syncing : SyncState()
    data object Offline : SyncState()
    data class Error(val message: String) : SyncState()
}

/**
 * Dua arah:
 *
 * PUSH -- kirim baris dari sync_outbox ke Supabase, satu per satu sesuai urutan dibuat.
 * Data TERBARU diambil langsung dari Room (bukan dari kolom payload di outbox), supaya
 * yang terkirim selalu state paling akhir.
 *
 * PULL -- setelah SEMUA push berhasil (outbox kosong), ambil kategori, transaksi, dan produk
 * milik user ini dari Supabase, lalu simpan ke Room. Ini yang membuat data "muncul lagi" di
 * HP setelah Clear Data, ganti HP, atau setelah Room lokal dikosongkan saat ganti akun.
 *
 * Pull SENGAJA dijalankan SETELAH push (bukan sebelum atau paralel), supaya perubahan
 * lokal yang belum terkirim tidak keburu tertimpa oleh versi lama dari server.
 *
 * CATATAN UNTUK LANGKAH BERIKUTNYA: entity yang sudah ditangani: "category", "transaction"
 * (+ "transaction_items" ikut serta), "product", dan "profile" (jaring pengaman saja).
 * Stok (stock_movements) dan belanja masih lokal saja -- pola yang sama tinggal ditiru.
 */
class SyncRepository(
    private val outboxDao: OutboxDao,
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao,
    private val productDao: ProductDao,
    private val userDao: UserDao
) {
    private val supabase get() = SupabaseProvider.client

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Synced)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _lastSyncTime = MutableStateFlow(0L)
    val lastSyncTime: StateFlow<Long> = _lastSyncTime.asStateFlow()

    fun getPendingCount(): Flow<Int> = outboxDao.getPendingMutationCount()

    suspend fun performSync(): Boolean {
        val userId = supabase.auth.currentUserOrNull()?.id
        if (userId == null) {
            _syncState.value = SyncState.Offline
            return false
        }

        _syncState.value = SyncState.Syncing

        try {
            pushPending(userId)
            pullCategories(userId)
            pullTransactions(userId)
            pullProducts(userId)
            pullStockMovements(userId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Sinkronisasi gagal", e)
            _syncState.value = SyncState.Error(e.message ?: "Sinkronisasi gagal")
            return false
        }

        _lastSyncTime.value = System.currentTimeMillis()
        _syncState.value = SyncState.Synced
        return true
    }

    // --------------------------------------------------------------- PUSH

    private suspend fun pushPending(userId: String) {
        val pending = outboxDao.getAllPendingMutations().first()
        for (mutation in pending) {
            when (mutation.entity) {
                "category" -> pushCategory(mutation, userId)
                "transaction" -> pushTransaction(mutation, userId)
                "product" -> pushProduct(mutation, userId)
                "stock_movement" -> pushStockMovement(mutation, userId)
                "profile" -> pushProfile(userId)
                else -> Log.w(TAG, "Tipe entity belum didukung sync: ${mutation.entity}")
            }
            outboxDao.deleteMutation(mutation.id)
        }
    }

    /**
     * Jaring pengaman untuk UserRepository.updateProfile(): baris ini hanya masuk outbox
     * kalau push langsung tadi gagal (mis. offline). Tidak ada op "delete" untuk profil.
     */
    private suspend fun pushProfile(userId: String) {
        val local = userDao.getUserProfileOnce() ?: return
        supabase.postgrest.from("user_profiles")
            .upsert(local.toRemote()) { onConflict = "user_id" }
    }

    private suspend fun pushCategory(mutation: SyncOutboxEntity, userId: String) {
        if (mutation.op == "delete") {
            supabase.postgrest.from("categories")
                .update(SoftDelete(System.currentTimeMillis())) {
                    filter {
                        eq("id", mutation.entityId)
                        eq("user_id", userId)
                    }
                }
            return
        }
        val local = categoryDao.getCategoryById(mutation.entityId) ?: return // sudah dihapus lokal
        supabase.postgrest.from("categories")
            .upsert(local.toRemote()) { onConflict = "user_id,id" }
    }

    private suspend fun pushTransaction(mutation: SyncOutboxEntity, userId: String) {
        val id = mutation.entityId
        if (mutation.op == "delete") {
            val now = System.currentTimeMillis()
            supabase.postgrest.from("transactions")
                .update(SoftDelete(now)) {
                    filter {
                        eq("id", id)
                        eq("user_id", userId)
                    }
                }
            supabase.postgrest.from("transaction_items")
                .update(SoftDelete(now)) {
                    filter {
                        eq("transaction_id", id)
                        eq("user_id", userId)
                    }
                }
            return
        }

        val txn = transactionDao.getTransactionEntityById(id) ?: return // sudah dihapus lokal
        val items = transactionDao.getItemsByTransactionId(id)

        supabase.postgrest.from("transactions")
            .upsert(txn.toRemote()) { onConflict = "user_id,id" }

        if (items.isNotEmpty()) {
            supabase.postgrest.from("transaction_items")
                .upsert(items.map { it.toRemote() }) { onConflict = "user_id,id" }
        }
    }

    private suspend fun pushProduct(mutation: SyncOutboxEntity, userId: String) {
        if (mutation.op == "delete") {
            supabase.postgrest.from("products")
                .update(SoftDelete(System.currentTimeMillis())) {
                    filter {
                        eq("id", mutation.entityId)
                        eq("user_id", userId)
                    }
                }
            return
        }
        val local = productDao.getProductById(mutation.entityId) ?: return // sudah dihapus lokal
        supabase.postgrest.from("products")
            .upsert(local.toRemote()) { onConflict = "user_id,id" }
    }

    /** Tidak ada op "delete" -- riwayat stok tidak dihapus, hanya bertambah. */
    private suspend fun pushStockMovement(mutation: SyncOutboxEntity, userId: String) {
        val local = productDao.getStockMovementById(mutation.entityId) ?: return
        supabase.postgrest.from("stock_movements")
            .upsert(local.toRemote()) { onConflict = "user_id,id" }
    }

    // --------------------------------------------------------------- PULL

    private suspend fun pullStockMovements(userId: String) {
        val remote = supabase.postgrest.from("stock_movements")
            .select {
                filter {
                    eq("user_id", userId)
                    exact("deleted_at", null)
                }
            }
            .decodeList<RemoteStockMovement>()

        remote.forEach { productDao.insertStockMovement(it.toLocal()) }
        Log.d(TAG, "Pull riwayat stok: ${remote.size} baris")
    }

    private suspend fun pullProducts(userId: String) {
        val remote = supabase.postgrest.from("products")
            .select {
                filter {
                    eq("user_id", userId)
                    exact("deleted_at", null)
                }
            }
            .decodeList<RemoteProduct>()

        remote.forEach { productDao.insertProduct(it.toLocal()) }
        Log.d(TAG, "Pull produk: ${remote.size} baris")
    }

    private suspend fun pullCategories(userId: String) {
        val remote = supabase.postgrest.from("categories")
            .select {
                filter {
                    eq("user_id", userId)
                    exact("deleted_at", null)
                }
            }
            .decodeList<RemoteCategory>()

        if (remote.isNotEmpty()) {
            categoryDao.insertCategories(remote.map { it.toLocal() })
        }
        Log.d(TAG, "Pull kategori: ${remote.size} baris")
    }

    private suspend fun pullTransactions(userId: String) {
        val remoteTx = supabase.postgrest.from("transactions")
            .select {
                filter {
                    eq("user_id", userId)
                    exact("deleted_at", null)
                }
            }
            .decodeList<RemoteTransaction>()

        val remoteItems = supabase.postgrest.from("transaction_items")
            .select {
                filter {
                    eq("user_id", userId)
                    exact("deleted_at", null)
                }
            }
            .decodeList<RemoteTransactionItem>()

        remoteTx.forEach { transactionDao.insertTransaction(it.toLocal()) }
        if (remoteItems.isNotEmpty()) {
            transactionDao.insertTransactionItems(remoteItems.map { it.toLocal() })
        }
        Log.d(TAG, "Pull transaksi: ${remoteTx.size} transaksi, ${remoteItems.size} item")
    }

    private companion object {
        const val TAG = "SyncRepository"
    }
}