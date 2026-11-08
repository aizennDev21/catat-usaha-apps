package com.example.data.remote

import com.example.data.local.entity.UserProfileEntity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Cerminan satu baris tabel `user_profiles` di Supabase.
 * Nama di @SerialName harus SAMA PERSIS dengan nama kolom di tabel.
 */
@Serializable
data class RemoteProfile(
    @SerialName("user_id") val userId: String,
    @SerialName("full_name") val fullName: String,
    @SerialName("business_name") val businessName: String,
    val phone: String? = null,
    @SerialName("business_type") val businessType: String? = null
)

fun UserProfileEntity.toRemote() = RemoteProfile(
    userId = userId,
    fullName = fullName,
    businessName = businessName,
    phone = phone.ifBlank { null },
    businessType = businessType
)