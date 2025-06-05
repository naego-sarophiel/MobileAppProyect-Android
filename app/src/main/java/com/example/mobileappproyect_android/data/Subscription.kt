package com.example.mobileappproyect_android.data

import java.time.LocalDate

enum class SubscriptionStatus {
    ACTIVE, PAUSED, CANCELED, PENDING_PAYMENT
}

data class Subscription(
    val id: String,
    var name: String,
    var imageUrl: String?,
    var renewalDate: LocalDate,
    var baseCost: Double, // Precio en la moneda base (ej. USD)
    var baseCurrency: String = "USD", // Moneda en la que se ingresó el baseCost
    var cost: Double, // Este será el coste convertido, usado para mostrar
    var currencySymbol: String = "$", // Símbolo de la moneda mostrada
    var status: SubscriptionStatus = SubscriptionStatus.ACTIVE
)