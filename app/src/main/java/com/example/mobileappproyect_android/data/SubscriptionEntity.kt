package com.example.mobileappproyect_android.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

// (Keep SubscriptionStatus enum and its localizedName function if used by entity, or move if only for UI)

@Entity(tableName = "subscriptions")
data class SubscriptionEntity(
    @PrimaryKey val id: String,
    var name: String,
    var imageUrl: String?,
    var renewalDate: LocalDate,
    var baseCost: Double,
    var baseCurrency: String,
    var status: SubscriptionStatus
)