package com.example.data.repository

import android.util.Log
import android.util.Patterns
import com.example.data.local.dao.CategoryDao
import com.example.data.local.dao.OutboxDao
import com.example.data.local.dao.UserDao
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.SyncOutboxEntity
import com.example.data.local.entity.UserProfileEntity
import com.example.data.remote.RemoteProfile
import com.example.data.remote.SupabaseProvider
import com.example.data.remote.toRemote
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import java.io.IOException
import java.util.UUID

/**
 * Supabase Auth = sumber kebenaran siapa yang login.
 * Room = cache lokal supaya aplikasi tetap cepat dan bisa dipakai offline.
 *
 * clearLocalData dipanggil saat akun yang login BERBEDA dari data lokal
 * (mencegah data user A muncul di akun user B pada HP yang sama).
 */
class UserRepository(
    private val userDao: UserDao,
    private val outboxDao: OutboxDao,
    private val categoryDao: CategoryDao,
    private val clearLocalData: suspend () -> Unit
) {
    private val supabase get() = SupabaseProvider.client

    fun getUserProfile(): Flow<UserProfileEntity?> =
        userDao.getUserProfile()

    suspend fun getUserProfileOnce(): UserProfileEntity? =
        userDao.getUserProfileOnce()

    // ---------------------------------------------------------------- LOGIN

    suspend fun login(identifier: String, password: String): Result<UserProfileEntity> {
        val mail = identifier.trim().lowercase()
        if (!isValidEmail(mail)) {
            return Result.failure(IllegalArgumentException("Masukkan alamat email yang valid"))
        }
        if (password.isBlank()) {
            return Result.failure(IllegalArgumentException("Password tidak boleh kosong"))
        }
        val pass = password

        return authCall {
            supabase.auth.signInWith(Email) {
                this.email = mail
                this.password = pass
            }
            val userId = supabase.auth.currentUserOrNull()?.id
                ?: error("Sesi tidak ditemukan setelah masuk")

            val remote = fetchOrCreateRemoteProfile(userId, mail)
            saveLocalProfile(remote, mail)
        }
    }

    // ------------------------------------------------------------- REGISTER

    suspend fun register(
        fullName: String,
        businessName: String,
        phone: String,
        email: String,
        password: String,
        businessType: String = "Warung & Sembako"
    ): Result<UserProfileEntity> {
        val name = fullName.trim()
        val biz = businessName.trim()
        val tel = phone.trim()
        val mail = email.trim().lowercase()

        if (biz.isBlank()) {
            return Result.failure(IllegalArgumentException("Nama usaha / toko wajib diisi"))
        }
        if (name.isBlank()) {
            return Result.failure(IllegalArgumentException("Nama pemilik wajib diisi"))
        }
        if (!isValidEmail(mail)) {
            return Result.failure(IllegalArgumentException("Email wajib diisi dengan format yang valid"))
        }
        if (password.length < 6) {
            return Result.failure(IllegalArgumentException("Password minimal 6 karakter"))
        }
        val pass = password

        return authCall {
            supabase.auth.signUpWith(Email) {
                this.email = mail
                this.password = pass
            }

            // Kalau "Confirm email" AKTIF di Supabase, signUp berhasil tapi BELUM ada sesi.
            val userId = supabase.auth.currentUserOrNull()?.id
                ?: throw EmailVerificationRequiredException(
                    "Pendaftaran berhasil. Kami mengirim email verifikasi ke $mail. " +
                        "Buka email tersebut, lalu masuk."
                )

            val remote = RemoteProfile(
                userId = userId,
                fullName = name,
                businessName = biz,
                phone = tel.ifBlank { null },
                businessType = businessType
            )
            supabase.postgrest.from("user_profiles").upsert(remote) { onConflict = "user_id" }

            val saved = saveLocalProfile(remote, mail)
            seedDefaultCategories(userId, System.currentTimeMillis())
            saved
        }
    }

    // --------------------------------------------------------------- LOGOUT

    suspend fun logout() {
        try {
            supabase.auth.signOut()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Offline pun tetap boleh keluar secara lokal.
            Log.w(TAG, "signOut gagal, tetap logout lokal", e)
        }
        userDao.logoutAll()
    }

    // ------------------------------------------------------- PROFIL & SETELAN

    suspend fun updateProfile(
        fullName: String,
        businessName: String,
        phone: String,
        avatarUrl: String? = null
    ) {
        val current = userDao.getUserProfileOnce() ?: return
        val now = System.currentTimeMillis()
        val updated = current.copy(
            fullName = fullName.trim(),
            businessName = businessName.trim(),
            phone = phone.trim(),
            avatarUrl = avatarUrl,
            updatedAt = now
        )
        userDao.insertOrUpdateProfile(updated)

        // Coba kirim langsung (biasanya user online saat mengedit profil di menu Profil).
        // Kalau gagal (mis. sedang offline), masukkan ke outbox supaya SyncRepository
        // yang mencoba lagi nanti -- lihat penanganan entity "profile" di SyncRepository.
        try {
            supabase.postgrest.from("user_profiles")
                .upsert(updated.toRemote()) { onConflict = "user_id" }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Update profil belum terkirim (offline?), disimpan ke outbox", e)
            outboxDao.insertMutation(
                SyncOutboxEntity(
                    id = UUID.randomUUID().toString(),
                    entity = "profile",
                    entityId = updated.userId,
                    op = "upsert",
                    payload = "", // SyncRepository membaca langsung dari Room, bukan dari sini
                    createdAt = now
                )
            )
        }
    }

    suspend fun updateSettings(
        themeMode: String? = null,
        deductStockDefault: Boolean? = null,
        addStockDefault: Boolean? = null,
        lowStockNotification: Boolean? = null,
        pinLock: String? = null
    ) {
        val current = userDao.getUserProfileOnce() ?: return
        val now = System.currentTimeMillis()
        val updated = current.copy(
            themeMode = themeMode ?: current.themeMode,
            deductStockDefault = deductStockDefault ?: current.deductStockDefault,
            addStockDefault = addStockDefault ?: current.addStockDefault,
            lowStockNotification = lowStockNotification ?: current.lowStockNotification,
            pinLock = pinLock ?: current.pinLock,
            updatedAt = now
        )
        userDao.insertOrUpdateProfile(updated)
    }

    suspend fun clearPinLock() {
        val current = userDao.getUserProfileOnce() ?: return
        userDao.insertOrUpdateProfile(current.copy(pinLock = null, updatedAt = System.currentTimeMillis()))
    }

    // ------------------------------------------------------------- INTERNAL

    private suspend fun fetchOrCreateRemoteProfile(userId: String, mail: String): RemoteProfile {
        val found = supabase.postgrest.from("user_profiles")
            .select { filter { eq("user_id", userId) } }
            .decodeSingleOrNull<RemoteProfile>()
        if (found != null) return found

        // Akun ada di Auth tapi barisnya belum ada di user_profiles: buat versi minimal.
        val created = RemoteProfile(
            userId = userId,
            fullName = mail.substringBefore("@"),
            businessName = "Usaha Saya",
            businessType = "Warung & Sembako"
        )
        supabase.postgrest.from("user_profiles").upsert(created) { onConflict = "user_id" }
        return created
    }

    private suspend fun saveLocalProfile(remote: RemoteProfile, mail: String): UserProfileEntity {
        // Kalau akun berbeda dari data lokal, kosongkan dulu supaya data tidak tercampur.
        val base = userDao.findUserByIdentifier(mail)?.takeIf { it.userId == remote.userId }
        if (base == null) clearLocalData()

        val now = System.currentTimeMillis()
        val entity = UserProfileEntity(
            userId = remote.userId,
            fullName = remote.fullName,
            businessName = remote.businessName,
            phone = remote.phone ?: "",
            email = mail,
            passwordHash = null, // password TIDAK PERNAH disimpan di HP; Supabase yang mengurusnya
            businessType = remote.businessType ?: "Warung & Sembako",
            avatarUrl = base?.avatarUrl,
            currency = base?.currency ?: "IDR",
            locale = base?.locale ?: "id_ID",
            themeMode = base?.themeMode ?: "system",
            deductStockDefault = base?.deductStockDefault ?: true,
            addStockDefault = base?.addStockDefault ?: true,
            lowStockNotification = base?.lowStockNotification ?: true,
            pinLock = base?.pinLock,
            isLoggedIn = true,
            createdAt = base?.createdAt ?: now,
            updatedAt = now
        )
        userDao.logoutAll()
        userDao.insertOrUpdateProfile(entity)
        return entity
    }

    /**
     * Kategori bawaan dibuat SEKALI SAJA, saat akun baru dibuat lewat register().
     * Sengaja TIDAK dipanggil dari saveLocalProfile(), supaya tidak menulis ulang
     * 7 baris yang sama dan menambah 7 mutasi outbox setiap kali user login.
     * Ditulis ke outbox (bukan langsung push), supaya SyncRepository yang mendorongnya
     * ke Supabase secara konsisten dengan data lain, termasuk saat sedang offline.
     */
    private suspend fun seedDefaultCategories(userId: String, now: Long) {
        fun cat(id: String, name: String, type: String, icon: String, color: String) = CategoryEntity(
            id = id, userId = userId, name = name, type = type,
            icon = icon, color = color, isDefault = true,
            createdAt = now, updatedAt = now
        )
        val categories = listOf(
            cat("cat_penjualan", "Penjualan", "income", "storefront", "#15803D"),
            cat("cat_pemasukan_lain", "Pemasukan Lain", "income", "add_card", "#0369A1"),
            cat("cat_belanja_barang", "Belanja Barang", "expense", "shopping_basket", "#BE123C"),
            cat("cat_operasional", "Operasional", "expense", "receipt_long", "#EA580C"),
            cat("cat_gaji", "Gaji", "expense", "groups", "#7C3AED"),
            cat("cat_transport", "Transport", "expense", "local_shipping", "#D97706"),
            cat("cat_pengeluaran_lain", "Pengeluaran Lain", "expense", "more_horiz", "#64748B")
        )
        categoryDao.insertCategories(categories)
        categories.forEach { c ->
            outboxDao.insertMutation(
                SyncOutboxEntity(
                    id = UUID.randomUUID().toString(),
                    entity = "category",
                    entityId = c.id,
                    op = "upsert",
                    payload = "", // SyncRepository membaca data terbaru langsung dari Room, bukan dari sini
                    createdAt = now
                )
            )
        }
    }

    private fun isValidEmail(s: String) = Patterns.EMAIL_ADDRESS.matcher(s).matches()

    /** Membungkus panggilan jaringan: exception jadi Result.failure dengan pesan ramah. */
    private suspend fun <T> authCall(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e // jangan ditelan, ini cara coroutine dibatalkan
    } catch (e: Exception) {
        Log.e(TAG, "Auth call gagal", e)
        Result.failure(Exception(friendlyMessage(e), e))
    }

    private fun friendlyMessage(e: Throwable): String {
        if (e is EmailVerificationRequiredException) return e.message.orEmpty()
        val msg = e.message.orEmpty().lowercase()
        val offline = generateSequence(e) { it.cause }.any { it is IOException }
        return when {
            "invalid login credentials" in msg -> "Email atau password salah"
            "already registered" in msg || "already been registered" in msg ->
                "Email sudah terdaftar. Silakan masuk."
            "email not confirmed" in msg -> "Email belum diverifikasi. Cek kotak masuk Anda."
            "email_address_invalid" in msg || "invalid email" in msg -> "Alamat email tidak diterima"
            "rate limit" in msg -> "Terlalu banyak percobaan. Coba lagi beberapa menit lagi."
            "weak" in msg || "password should be" in msg -> "Password terlalu lemah (minimal 6 karakter)"
            offline -> "Tidak ada koneksi internet. Periksa jaringan Anda."
            else -> "Terjadi kesalahan. Silakan coba lagi."
        }
    }

    private class EmailVerificationRequiredException(message: String) : Exception(message)

    private companion object {
        const val TAG = "UserRepository"
    }
}