package com.example.mobileappproyect_android.data

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import java.time.LocalDate
import androidx.compose.ui.res.stringResource
import com.example.mobileappproyect_android.R

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