package com.example.mobileappproyect_android.data // O tu paquete de utilidades

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Date

// Interfaz para una API de tasas de cambio (simulada)
interface ExchangeRateApi {
    suspend fun getRates(baseCurrency: String): Map<String, Double> // Ej: {"EUR": 0.92, "GBP": 0.79}
}

// Implementación simulada
class FakeExchangeRateApi : ExchangeRateApi {
    override suspend fun getRates(baseCurrency: String): Map<String, Double> {
        // Simular una llamada de red
        kotlinx.coroutines.delay(500)
        return when (baseCurrency.uppercase()) {
            "USD" -> mapOf("USD" to 1.0, "EUR" to 0.92, "GBP" to 0.79, "JPY" to 150.0, "INR" to 83.0)
            "EUR" -> mapOf("USD" to 1.08, "EUR" to 1.0, "GBP" to 0.85, "JPY" to 163.0, "INR" to 90.0)
            // Añade más si es necesario
            else -> mapOf(baseCurrency.uppercase() to 1.0) // Por defecto, si no hay tasas
        }
    }
}


class ExchangeRateManager(private val exchangeRateApi: ExchangeRateApi = FakeExchangeRateApi()) {
    private val _exchangeRates = MutableStateFlow<Map<String, Double>>(emptyMap())
    val exchangeRates: StateFlow<Map<String, Double>> = _exchangeRates

    // Caché simple para evitar llamadas excesivas
    private var lastFetchedBase: String? = null
    private var lastFetchedTime: Long = 0
    private val cacheDurationMillis = 60 * 60 * 1000 // 1 hora
    private val mutex = Mutex()

    suspend fun fetchRatesIfNeeded(baseCurrency: String = "USD") { // La moneda base de tus datos
        mutex.withLock {
            val currentTime = System.currentTimeMillis()
            if (baseCurrency != lastFetchedBase || (currentTime - lastFetchedTime > cacheDurationMillis) || _exchangeRates.value.isEmpty()) {
                try {
                    val rates = exchangeRateApi.getRates(baseCurrency)
                    _exchangeRates.value = rates
                    lastFetchedBase = baseCurrency
                    lastFetchedTime = currentTime
                    println("Fetched new rates for $baseCurrency: $rates")
                } catch (e: Exception) {
                    // Manejar error de red, quizás reintentar o usar tasas cacheadas si existen
                    println("Error fetching exchange rates: ${e.message}")
                }
            }
        }
    }

    fun convertCurrency(amount: Double, fromCurrency: String, toCurrencySymbol: String): Pair<Double, String>? {
        val rates = _exchangeRates.value
        if (rates.isEmpty()) return null // No hay tasas disponibles

        // Necesitamos un mapa de Símbolo a Código de Moneda para buscar la tasa
        // Este mapa debería ser más robusto en una app real
        val symbolToCodeMap = mapOf("$" to "USD", "€" to "EUR", "£" to "GBP", "¥" to "JPY", "₹" to "INR")
        val toCurrencyCode = symbolToCodeMap[toCurrencySymbol]?.uppercase() ?: return null // Moneda destino no soportada

        // Asumimos que `rates` está basado en "USD" (o la `baseCurrency` con la que se llamó a fetchRates)
        // Si `fromCurrency` no es la base de `rates`, necesitaríamos una conversión indirecta
        // (amount / rates[fromCurrency]) * rates[toCurrencyCode]
        // Para este ejemplo, asumimos que `baseCost` siempre está en `USD` y `rates` también.

        val rateForFromCurrency = rates[fromCurrency.uppercase()] ?: 1.0 // Si la moneda base es la misma, la tasa es 1
        val rateForToCurrency = rates[toCurrencyCode] ?: return null // No hay tasa para la moneda destino

        // Primero convertir `amount` (que está en `fromCurrency`) a la moneda base de `rates` (USD)
        val amountInBaseRateCurrency = amount / rateForFromCurrency
        // Luego convertir de la moneda base de `rates` a `toCurrencyCode`
        val convertedAmount = amountInBaseRateCurrency * rateForToCurrency

        return Pair(convertedAmount, toCurrencySymbol)
    }
}