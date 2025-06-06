package com.example.mobileappproyect_android.data

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.example.mobileappproyect_android.R
import java.time.LocalDate

enum class SubscriptionStatus {
    ACTIVE, PAUSED, CANCELED, PENDING_PAYMENT
}

@Composable
fun SubscriptionStatus.localizedName(): String {
    return when (this) {
        SubscriptionStatus.ACTIVE -> stringResource(R.string.subscription_status_active)
        SubscriptionStatus.PAUSED -> stringResource(R.string.subscription_status_paused)
        SubscriptionStatus.CANCELED -> stringResource(R.string.subscription_status_canceled)
        SubscriptionStatus.PENDING_PAYMENT -> stringResource(R.string.subscription_status_pending_payment)
    }
}

@Entity(tableName = "subscriptions")
data class Subscription(
    @PrimaryKey var id: String, // Made var for no-arg constructor if needed, or keep val if using Option 2
    var name: String,
    var imageUrl: String?,
    var renewalDate: LocalDate,
    var baseCost: Double,
    var baseCurrency: String,
    var status: SubscriptionStatus
) {
    @Ignore
    var cost: Double = 0.0

    @Ignore
    var currencySymbol: String = ""

    // No-arg constructor for Room (and other frameworks if needed)
    // You might not even need this explicitly if all primary constructor args have defaults,
    // but KSP can be picky.
    constructor() : this(
        id = "", // Default value
        name = "", // Default value
        imageUrl = null, // Default value
        renewalDate = LocalDate.now(), // Default value
        baseCost = 0.0, // Default value
        baseCurrency = "USD", // Default value
        status = SubscriptionStatus.ACTIVE // Default value
    )
    // If you need a constructor for creating instances with ignored fields in your app code:
    // This constructor is NOT for Room.
    @Ignore
    constructor(
        id: String,
        name: String,
        imageUrl: String?,
        renewalDate: LocalDate,
        baseCost: Double,
        baseCurrency: String,
        status: SubscriptionStatus,
        cost: Double, // This is the ignored field
        currencySymbol: String // This is the ignored field
    ) : this(id, name, imageUrl, renewalDate, baseCost, baseCurrency, status) {
        this.cost = cost
        this.currencySymbol = currencySymbol
    }
}