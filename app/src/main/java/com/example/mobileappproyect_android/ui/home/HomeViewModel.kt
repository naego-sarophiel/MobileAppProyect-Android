package com.example.mobileappproyect_android.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.mobileappproyect_android.data.Subscription
import com.example.mobileappproyect_android.data.SubscriptionStatus
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import android.app.Application // Si cambias HomeViewModel a AndroidViewModel
import androidx.activity.result.launch
import androidx.compose.animation.core.copy
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.AndroidViewModel // Cambia a AndroidViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.example.mobileappproyect_android.data.ExchangeRateManager
import com.example.mobileappproyect_android.data.SettingsManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.text.lowercase

enum class SortCriteria {
    RENEWAL_DATE_ASC, // Fecha de renovación más cercana primero
    PRICE_ASC,        // Precio más bajo primero
    PRICE_DESC,       // Precio más alto primero
    NAME_ASC          // Opcional: Por nombre
}

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsManager = SettingsManager(application)
    private val exchangeRateManager = ExchangeRateManager() // Instanciar

    // Lista original de suscripciones con precios base
    private val _rawSubscriptions = mutableStateListOf<Subscription>()

    private val _sortCriteria = MutableStateFlow(SortCriteria.RENEWAL_DATE_ASC) // Orden por defecto
    val sortCriteria: StateFlow<SortCriteria> = _sortCriteria

    val subscriptions: StateFlow<List<Subscription>> =
        combine(
            settingsManager.currencyFlow, // Flujo de la moneda seleccionada
            exchangeRateManager.exchangeRates, // Flujo de las tasas de cambio
            snapshotFlow { _rawSubscriptions.toList() }, // Observar cambios en _rawSubscriptions
            _sortCriteria
        ) { selectedCurrencySymbol, rates, rawSubs, currentSortCriteria ->
            val convertedSubscriptions = rawSubs.map { sub -> // Renombrado para claridad
                val conversionResult = exchangeRateManager.convertCurrency(
                    amount = sub.baseCost,
                    fromCurrency = sub.baseCurrency,
                    toCurrencySymbol = selectedCurrencySymbol
                )
                if (conversionResult != null) {
                    sub.copy(cost = conversionResult.first, currencySymbol = conversionResult.second)
                } else {
                    // Fallback: mostrar coste base si la conversión falla o no hay tasas
                    sub.copy(cost = sub.baseCost, currencySymbol = sub.baseCurrency)
                }
            }
            when (currentSortCriteria) {
                SortCriteria.RENEWAL_DATE_ASC -> convertedSubscriptions.sortedBy { it.renewalDate }
                SortCriteria.PRICE_ASC -> convertedSubscriptions.sortedBy { it.cost }
                SortCriteria.PRICE_DESC -> convertedSubscriptions.sortedByDescending { it.cost }
                SortCriteria.NAME_ASC -> convertedSubscriptions.sortedBy { it.name.lowercase() }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun setSortCriteria(criteria: SortCriteria) {
        _sortCriteria.value = criteria
    }

    var selectedSubscriptionForEdit by mutableStateOf<Subscription?>(null)
        private set

    init {
        loadSampleSubscriptions() // Carga los datos base
        viewModelScope.launch {
            // Obtener tasas de cambio iniciales (asumiendo que los precios base están en USD)
            exchangeRateManager.fetchRatesIfNeeded("USD")
        }
    }

    private fun loadSampleSubscriptions() {
        _rawSubscriptions.addAll(
            listOf(
                Subscription("1", "Streaming Service A", "...", LocalDate.now().plusDays(3), 12.99, "USD", 12.99, "$", SubscriptionStatus.ACTIVE),
                Subscription("2", "Music Platform B", "...", LocalDate.now().plusDays(8), 9.99, "USD", 9.99, "$", SubscriptionStatus.PENDING_PAYMENT),
                Subscription("3", "Cloud Storage C", null, LocalDate.now().plusDays(30), 5.00, "USD", 5.00, "$", SubscriptionStatus.PAUSED),
                Subscription("4", "Gaming Subscription D", "https://via.placeholder.com/150/0000FF/FFFFFF?Text=ServiceD", LocalDate.now().plusDays(6), 15.00, "USD", 15.00, "$", SubscriptionStatus.CANCELED)
            )
        )
    }

    fun isRenewalSoon(renewalDate: LocalDate, daysThreshold: Int = 7): Boolean {
        val today = LocalDate.now()
        val daysUntilRenewal = ChronoUnit.DAYS.between(today, renewalDate)
        return daysUntilRenewal in 0 until daysThreshold
    }

    fun onSubscriptionSelectedForEdit(subscription: Subscription?) { // Cambiado a aceptar Subscription?
        selectedSubscriptionForEdit = subscription?.copy() // Crear una copia para la edición
    }

    fun clearSelectedSubscription() {
        selectedSubscriptionForEdit = null
    }

    fun updateSubscription(updatedSubscriptionFromEdit: Subscription) {
        val index = _rawSubscriptions.indexOfFirst { it.id == updatedSubscriptionFromEdit.id }
        if (index != -1) {
            // updatedSubscriptionFromEdit ya tiene baseCost y baseCurrency correctos desde EditScreen
            _rawSubscriptions[index] = updatedSubscriptionFromEdit.copy(
                cost = 0.0, // Será recalculado por el flow 'subscriptions'
                currencySymbol = "" // Será recalculado
            )
        }
        clearSelectedSubscription()
    }

    // Si añades una nueva suscripción
    fun addSubscription(newSubscriptionFromEdit: Subscription) {
        // newSubscriptionFromEdit ya tiene baseCost y baseCurrency
        _rawSubscriptions.add(newSubscriptionFromEdit.copy(
            cost = 0.0, // Será recalculado
            currencySymbol = "" // Será recalculado
        ))
    }
}