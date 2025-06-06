package com.example.mobileappproyect_android.ui.home

import android.app.Application
import android.util.Log
import androidx.compose.animation.core.copy
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobileappproyect_android.data.AppDatabase
import com.example.mobileappproyect_android.data.ExchangeRateManager
import com.example.mobileappproyect_android.data.SettingsManager
// Import your new SubscriptionEntity and SubscriptionUiModel
import com.example.mobileappproyect_android.data.SubscriptionEntity // For DAO interactions
import com.example.mobileappproyect_android.ui.home.SubscriptionUiModel // For UI and ViewModel state
import com.example.mobileappproyect_android.ui.home.toEntity // Mapper
import com.example.mobileappproyect_android.ui.home.toUiModel // Mapper
import com.example.mobileappproyect_android.data.SubscriptionDao
import com.example.mobileappproyect_android.data.SubscriptionStatus
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Currency
import java.util.Locale
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.text.firstOrNull
import kotlin.text.lowercase
import kotlin.text.sumOf

enum class SortCriteria {
    RENEWAL_DATE_ASC,
    PRICE_ASC,
    PRICE_DESC,
    NAME_ASC
}

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsManager = SettingsManager(application)
    private val exchangeRateManager = ExchangeRateManager()
    private val subscriptionDao: SubscriptionDao = AppDatabase.getDatabase(application).subscriptionDao()

    private val _sortCriteria = MutableStateFlow(SortCriteria.RENEWAL_DATE_ASC)
    val sortCriteria: StateFlow<SortCriteria> = _sortCriteria.asStateFlow()

    // Flow from DAO now returns Entities
    private val rawSubscriptionsFromDb: Flow<List<SubscriptionEntity>> = subscriptionDao.getAllSubscriptions()

    // This StateFlow will now hold SubscriptionUiModel
    val subscriptions: StateFlow<List<SubscriptionUiModel>> =
        combine(
            settingsManager.currencyFlow,
            exchangeRateManager.exchangeRates,
            rawSubscriptionsFromDb, // Now Flow<List<SubscriptionEntity>>
            _sortCriteria
        ) { selectedCurrencySymbol, _, dbSubsAsEntities, currentSortCriteria -> // rates parameter no longer directly used here if conversion is inside toUiModel logic
            val uiModelSubscriptions = dbSubsAsEntities.map { entity ->
                val conversionResult = exchangeRateManager.convertCurrency(
                    amount = entity.baseCost,
                    fromCurrency = entity.baseCurrency,
                    toCurrencySymbol = selectedCurrencySymbol
                )
                if (conversionResult != null) {
                    entity.toUiModel(calculatedCost = conversionResult.first, calculatedCurrencySymbol = conversionResult.second)
                } else {
                    // Fallback: use base cost and currency if conversion fails
                    entity.toUiModel(calculatedCost = entity.baseCost, calculatedCurrencySymbol = entity.baseCurrency)
                }
            }
            // Now sort the List<SubscriptionUiModel>
            when (currentSortCriteria) {
                SortCriteria.RENEWAL_DATE_ASC -> uiModelSubscriptions.sortedBy { it.renewalDate }
                SortCriteria.PRICE_ASC -> uiModelSubscriptions.sortedBy { it.cost } // Sort by 'cost' on UiModel
                SortCriteria.PRICE_DESC -> uiModelSubscriptions.sortedByDescending { it.cost }
                SortCriteria.NAME_ASC -> uiModelSubscriptions.sortedBy { it.name.lowercase() }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val monthlyTotalSpend: StateFlow<String> = combine(
        subscriptions, // This is now Flow<List<SubscriptionUiModel>>
        settingsManager.currencyFlow // currencyFlow might be redundant if symbol is in UiModel
    ) { subsUiModelList, currentCurrencySymbolUsedForFormatting ->
        val total = subsUiModelList
            .filter { it.isActiveNow() } // Updated extension function below
            .sumOf { it.cost } // Use 'cost' from SubscriptionUiModel
        // Use the currency symbol from the first active sub, or the setting if none.
        // Or directly use currentCurrencySymbolUsedForFormatting if that's preferred.
        val symbolToFormatWith = subsUiModelList.firstOrNull { it.isActiveNow() }?.currencySymbol ?: currentCurrencySymbolUsedForFormatting
        formatCurrency(total, symbolToFormatWith)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = formatCurrency(0.0, settingsManager.currencyFlow.toString()) // Initial value from settings
    )

    var selectedSubscriptionForEdit by mutableStateOf<SubscriptionUiModel?>(null)
        private set

    init {
        viewModelScope.launch {
            val currentSubsEntities = rawSubscriptionsFromDb.first()
            if (currentSubsEntities.isEmpty()) {
                Log.d("HomeViewModel", "Database is empty, loading sample subscriptions into DB.")
                loadSampleSubscriptionsIntoDb()
            }

            val initialSelectedCurrency = settingsManager.currencyFlow.first()
            exchangeRateManager.fetchRatesIfNeeded("USD") // Base for conversions
            if (initialSelectedCurrency != "USD" && initialSelectedCurrency.length == 3) {
                exchangeRateManager.fetchRatesIfNeeded(initialSelectedCurrency)
            }
        }
    }

    private suspend fun loadSampleSubscriptionsIntoDb() {
        val sampleSubsEntities = listOf(
            SubscriptionEntity(
                id = "1",
                name = "Streaming Service A",
                imageUrl = "https://via.placeholder.com/150/FF0000/FFFFFF?Text=ServiceA",
                renewalDate = LocalDate.now().plusDays(3),
                baseCost = 12.99,
                baseCurrency = "USD",
                status = SubscriptionStatus.ACTIVE
            ),
            SubscriptionEntity(
                id = "2",
                name = "Music Platform B",
                imageUrl = "https://via.placeholder.com/150/00FF00/FFFFFF?Text=ServiceB",
                renewalDate = LocalDate.now().plusDays(8),
                baseCost = 9.99,
                baseCurrency = "USD",
                status = SubscriptionStatus.PENDING_PAYMENT
            ),
            SubscriptionEntity(
                id = "3",
                name = "Cloud Storage C",
                imageUrl = null, // Placeholder if no image
                renewalDate = LocalDate.now().plusDays(30),
                baseCost = 5.00,
                baseCurrency = "EUR", // Example of different base currency
                status = SubscriptionStatus.PAUSED
            ),
            SubscriptionEntity(
                id = "4",
                name = "Gaming Subscription D",
                imageUrl = "https://via.placeholder.com/150/0000FF/FFFFFF?Text=ServiceD",
                renewalDate = LocalDate.now().plusDays(6),
                baseCost = 15.00,
                baseCurrency = "USD",
                status = SubscriptionStatus.CANCELED
            )
        )
        subscriptionDao.insertAllSubscriptions(sampleSubsEntities)
    }

    fun deleteSubscription(subscriptionId: String) {
        viewModelScope.launch {
            subscriptionDao.deleteSubscriptionById(subscriptionId)
        }
    }

    // Extension function for SubscriptionUiModel
    private fun SubscriptionUiModel.isActiveNow(): Boolean {
        return this.status == SubscriptionStatus.ACTIVE
    }

    // formatCurrency remains largely the same, but ensure it handles symbols correctly
    private fun formatCurrency(amount: Double, currencyIdentifier: String): String {
        val defaultLocale = Locale.getDefault() // Consider user's locale for formatting
        val numberFormat = NumberFormat.getCurrencyInstance(defaultLocale)
        try {
            // If identifier is a 3-letter currency code (e.g., "USD", "EUR")
            if (currencyIdentifier.length == 3 && currencyIdentifier.all { it.isLetter() }) {
                val currency = Currency.getInstance(currencyIdentifier.uppercase(Locale.ROOT))
                numberFormat.currency = currency
                return numberFormat.format(amount)
            }
        } catch (e: IllegalArgumentException) {
            Log.w("HomeViewModel", "Invalid currency code: $currencyIdentifier", e)
        }

        // If identifier is a symbol (e.g., "$", "€")
        // This part can be tricky because symbols are locale-dependent.
        // The most reliable way is to use the currency code if available.
        // Forcing a symbol might not always match user expectations or device locale.
        val cleanAmountString = String.format(Locale.US, "%.2f", amount) // Use US locale for consistent decimal formatting
        return if (currencyIdentifier.length == 1 && !currencyIdentifier[0].isLetterOrDigit()) {
            // Simple symbol prefixing
            "$currencyIdentifier$cleanAmountString"
        } else {
            // Fallback to USD formatting if identifier is not a known code or simple symbol
            numberFormat.currency = Currency.getInstance("USD") // Default to USD or user's local currency
            val formatted = numberFormat.format(amount)
            // If a symbol was provided but it wasn't a code, try to replace the default symbol
            if (currencyIdentifier.length == 1 && currencyIdentifier != numberFormat.currency.symbol) {
                return formatted.replace(numberFormat.currency.symbol, currencyIdentifier)
            }
            return formatted
        }
    }


    fun setSortCriteria(criteria: SortCriteria) {
        _sortCriteria.value = criteria
    }

    // This method now operates on renewalDate from SubscriptionUiModel
    fun isRenewalSoon(renewalDate: LocalDate, daysThreshold: Int = 7): Boolean {
        val today = LocalDate.now()
        val daysUntilRenewal = ChronoUnit.DAYS.between(today, renewalDate)
        return daysUntilRenewal in 0 until daysThreshold
    }

    // This method now takes and sets SubscriptionUiModel
    fun onSubscriptionSelectedForEdit(subscriptionUiModel: SubscriptionUiModel?) {
        selectedSubscriptionForEdit = subscriptionUiModel?.copy() // copy() on UiModel is fine
    }

    fun clearSelectedSubscription() {
        selectedSubscriptionForEdit = null
    }

    // Takes SubscriptionUiModel from the UI (e.g., an edit screen)
    fun updateSubscription(updatedSubscriptionUiModel: SubscriptionUiModel) {
        viewModelScope.launch {
            val entityToUpdate = updatedSubscriptionUiModel.toEntity() // Convert to entity before saving
            subscriptionDao.updateSubscription(entityToUpdate)
        }
        clearSelectedSubscription()
    }

    // Takes SubscriptionUiModel from the UI (e.g., an add screen)
    fun addSubscription(newSubscriptionUiModel: SubscriptionUiModel) {
        viewModelScope.launch {
            // Generate a unique ID if it's a new subscription and ID is blank
            val uiModelWithId = if (newSubscriptionUiModel.id.isBlank()) {
                newSubscriptionUiModel.copy(id = java.util.UUID.randomUUID().toString())
            } else {
                newSubscriptionUiModel
            }
            val entityToAdd = uiModelWithId.toEntity() // Convert to entity
            subscriptionDao.insertSubscription(entityToAdd)
        }
    }
}