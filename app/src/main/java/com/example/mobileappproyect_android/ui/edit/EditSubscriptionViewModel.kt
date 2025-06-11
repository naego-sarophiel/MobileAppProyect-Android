package com.example.mobileappproyect_android.ui.edit

import android.app.Application
import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.mobileappproyect_android.R
import com.example.mobileappproyect_android.data.SubscriptionStatus
import com.example.mobileappproyect_android.ui.home.HomeViewModel
import com.example.mobileappproyect_android.ui.home.SubscriptionUiModel
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.time.LocalDate
import java.util.UUID

class EditSubscriptionViewModel(
    private val app: Application, // Use 'app' to avoid conflict with context property
    private val homeViewModel: HomeViewModel,
    private val initialSubscriptionId: String?
) : AndroidViewModel(app) {

    // --- State Properties from EditSubscriptionScreen ---
    var pageTitle by mutableStateOf("")
        private set

    var name by mutableStateOf("")
        private set
    var baseCostString by mutableStateOf("")
        private set
    var baseCurrency by mutableStateOf("USD") // Default currency
        private set
    val baseCurrencyOptions = listOf("USD", "EUR", "GBP", "JPY", "INR") // Define directly in VM

    var renewalDate by mutableStateOf(LocalDate.now())
        private set
    var status by mutableStateOf(SubscriptionStatus.ACTIVE)
        private set

    // For image handling
    var tempImageUri by mutableStateOf<Uri?>(null) // URI from picker
        private set
    var currentImageSource by mutableStateOf<Any?>(null) // What Coil loads (Uri or String path/URL)
        private set
    private var existingImageUrl: String? = null // To track original image URL for deletion logic

    var showDatePicker by mutableStateOf(false) // For DatePickerDialog visibility
        private set

    // Permission related (can be observed by the screen if needed, or handled via events)
    // For simplicity, the screen can manage the permission request flow directly
    // and call a VM function if it needs to trigger something based on permission.

    private var originalSubscription: SubscriptionUiModel? = null

    init {
        loadSubscription(initialSubscriptionId)
    }

    private fun loadSubscription(subscriptionId: String?) {
        viewModelScope.launch {
            if (subscriptionId != null && subscriptionId != "new") {
                pageTitle = app.getString(R.string.edit_subscription_title_edit)
                originalSubscription = homeViewModel.getSubscriptionById(subscriptionId) // Assuming this exists
                originalSubscription?.let { sub ->
                    name = sub.name
                    baseCostString = sub.baseCost.toString()
                    baseCurrency = sub.baseCurrency
                    renewalDate = sub.renewalDate
                    status = sub.status
                    currentImageSource = sub.imageUrl // Initial image source
                    existingImageUrl = sub.imageUrl   // Store for potential deletion
                }
            } else {
                pageTitle = app.getString(R.string.edit_subscription_title_add)
                // Fields are already at their defaults for a new subscription
                // Generate a new ID for a new subscription if not editing
                // this.id = UUID.randomUUID().toString() // No, ID is part of originalSubscription or generated on save
            }
        }
    }

    // --- Event Handlers from EditSubscriptionScreen ---
    fun onNameChange(newName: String) {
        name = newName
    }

    fun onBaseCostChange(newCost: String) {
        baseCostString = newCost
    }

    fun onBaseCurrencyChange(newCurrency: String) {
        baseCurrency = newCurrency
    }

    fun onRenewalDateChange(newDate: LocalDate) {
        renewalDate = newDate
    }

    fun onStatusChange(newStatus: SubscriptionStatus) {
        status = newStatus
    }

    fun onTempImageUriReceived(uri: Uri?) {
        tempImageUri = uri
        currentImageSource = uri // Immediately update display to show the newly picked image
    }

    fun onShowDatePicker(show: Boolean) {
        showDatePicker = show
    }

    fun saveSubscription(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val baseCostValue = baseCostString.toDoubleOrNull()
        if (name.isBlank() || baseCostValue == null) {
            onError(app.getString(R.string.edit_subscription_error_fill_fields))
            return
        }

        viewModelScope.launch {
            var finalImageUrl: String? = existingImageUrl // Start with existing or null

            if (tempImageUri != null) { // A new image was picked
                val newFileName = "IMG_${UUID.randomUUID()}.jpg"
                val copiedImagePath = copyUriToInternalStorage(app, tempImageUri!!, newFileName)

                if (copiedImagePath != null) {
                    // If there was an old image, try to delete it from internal storage
                    existingImageUrl?.let { oldPath ->
                        if (!oldPath.startsWith("http://") && !oldPath.startsWith("https://") && !oldPath.startsWith("content://")) {
                            try {
                                val oldFile = File(oldPath)
                                if (oldFile.exists() && oldFile.isFile && oldFile.path.contains(app.filesDir.path)) {
                                    oldFile.delete()
                                    Log.d("EditVM", "Old image deleted: $oldPath")
                                }
                            } catch (e: Exception) {
                                Log.e("EditVM", "Failed to delete old image: $oldPath", e)
                            }
                        }
                    }
                    finalImageUrl = copiedImagePath
                } else {
                    onError(app.getString(R.string.edit_subscription_error_saving_image))
                    return@launch // Stop if image saving fails
                }
            }

            val subscriptionToSave = SubscriptionUiModel(
                id = originalSubscription?.id ?: UUID.randomUUID().toString(), // Use existing ID or generate new
                name = name,
                imageUrl = finalImageUrl,
                renewalDate = renewalDate,
                baseCost = baseCostValue,
                baseCurrency = baseCurrency,
                status = status,
                // These might need recalculation based on your app's logic, or are derived
                cost = baseCostValue, // Assuming baseCost is the main cost
                currencySymbol = baseCurrency // Or derive from currency
            )

            if (originalSubscription != null) { // Editing existing
                homeViewModel.updateSubscription(subscriptionToSave)
            } else { // Adding new
                homeViewModel.addSubscription(subscriptionToSave)
            }
            onSuccess() // Call success callback (which should trigger navigation)
        }
    }

    private fun copyUriToInternalStorage(context: Context, uri: Uri, newFileName: String): String? {
        // (Keep the copyUriToInternalStorage function from your EditSubscriptionScreen.kt here)
        // Or better, move it to a utility class if used elsewhere
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val directory = File(context.filesDir, "subscription_images")
            if (!directory.exists()) {
                directory.mkdirs()
            }
            val file = File(directory, newFileName)
            val outputStream = FileOutputStream(file)
            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            return file.absolutePath
        } catch (e: IOException) {
            Log.e("EditVM", "IOException: Error copying URI: ${e.message}", e)
            return null
        } catch (e: SecurityException) {
            Log.e("EditVM", "SecurityException: Error copying URI: ${e.message}", e)
            return null
        }
    }


    class Factory(
        private val application: Application,
        private val homeViewModel: HomeViewModel
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            if (modelClass.isAssignableFrom(EditSubscriptionViewModel::class.java)) {
                val savedStateHandle = extras.createSavedStateHandle()
                val subscriptionId = savedStateHandle.get<String>("subscriptionId")
                return EditSubscriptionViewModel(application, homeViewModel, subscriptionId) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}