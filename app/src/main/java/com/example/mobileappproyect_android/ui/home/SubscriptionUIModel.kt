package com.example.mobileappproyect_android.ui.home // Or your chosen package

import com.example.mobileappproyect_android.data.SubscriptionEntity
import com.example.mobileappproyect_android.data.SubscriptionStatus
import java.time.LocalDate

// Data class for UI and ViewModel internal state
data class SubscriptionUiModel(
    val id: String,
    val name: String,
    val imageUrl: String?,
    val renewalDate: LocalDate,
    val baseCost: Double,       // Keep baseCost for editing purposes
    val baseCurrency: String,   // Keep baseCurrency for editing
    val status: SubscriptionStatus,
    val cost: Double,           // Calculated/Display cost
    val currencySymbol: String  // Calculated/Display currency symbol
)

// Extension function to map from SubscriptionEntity to SubscriptionUiModel
fun SubscriptionEntity.toUiModel(calculatedCost: Double, calculatedCurrencySymbol: String): SubscriptionUiModel {
    return SubscriptionUiModel(
        id = this.id,
        name = this.name,
        imageUrl = this.imageUrl,
        renewalDate = this.renewalDate,
        baseCost = this.baseCost,       // Preserve original base cost from entity
        baseCurrency = this.baseCurrency, // Preserve original base currency
        status = this.status,
        cost = calculatedCost,          // Set the calculated display cost
        currencySymbol = calculatedCurrencySymbol // Set the display currency symbol
    )
}

// Extension function to map from SubscriptionUiModel back to SubscriptionEntity for saving
fun SubscriptionUiModel.toEntity(): SubscriptionEntity {
    return SubscriptionEntity(
        id = this.id,
        name = this.name,
        imageUrl = this.imageUrl,
        renewalDate = this.renewalDate,
        baseCost = this.baseCost,       // Use baseCost from UiModel when saving
        baseCurrency = this.baseCurrency, // Use baseCurrency from UiModel
        status = this.status
    )
}