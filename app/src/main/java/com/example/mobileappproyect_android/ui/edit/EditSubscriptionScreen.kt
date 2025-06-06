package com.example.mobileappproyect_android.ui.edit

import android.Manifest
import android.app.DatePickerDialog
import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.mobileappproyect_android.R
import com.example.mobileappproyect_android.ui.home.SubscriptionUiModel
import com.example.mobileappproyect_android.data.SubscriptionStatus
import com.example.mobileappproyect_android.ui.theme.MobileAppProyectAndroidTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun EditSubscriptionScreen(
    subscriptionToEdit: SubscriptionUiModel?,
    onSaveSubscription: (SubscriptionUiModel) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var name by remember(subscriptionToEdit) { mutableStateOf(subscriptionToEdit?.name ?: "") }
    var baseCostString by remember(subscriptionToEdit) { mutableStateOf(subscriptionToEdit?.baseCost?.toString() ?: "") }
    var baseCurrency by remember(subscriptionToEdit) { mutableStateOf(subscriptionToEdit?.baseCurrency ?: "USD") }
    val baseCurrencyOptions = listOf("USD", "EUR", "GBP", "JPY", "INR")

    var renewalDate by remember(subscriptionToEdit) { mutableStateOf(subscriptionToEdit?.renewalDate ?: LocalDate.now()) }
    var status by remember(subscriptionToEdit) { mutableStateOf(subscriptionToEdit?.status ?: SubscriptionStatus.ACTIVE) }

    // tempImageUri will hold the temporary URI from the picker
    var tempImageUri by remember { mutableStateOf<Uri?>(null) }

    // currentImageSource will hold what AsyncImage should load
    // It can be a Uri (for newly picked image) or a String (for existing file path or web URL)
    var currentImageSource by remember(subscriptionToEdit?.imageUrl, tempImageUri) {
        mutableStateOf<Any?>( // Type is Any? to accommodate Uri or String
            tempImageUri ?: subscriptionToEdit?.imageUrl?.let {
                // If the stored imageUrl is a content URI (old way, less likely now) or a file path
                if (it.startsWith("content://") || it.startsWith("file://") || !it.contains("://")) {
                    // Attempt to parse if it looks like a URI, otherwise assume it's a file path or needs to be treated as a File by Coil
                    try {
                        Uri.parse(it) // For content URIs or file URIs
                    } catch (e: Exception) {
                        it // If parsing fails, it might be a direct file path string or an http url
                    }
                } else {
                    it // Assume http URL or other valid Coil input
                }
            }
        )
    }


    var showDatePicker by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            tempImageUri = it // Store the temporary URI from the picker
            currentImageSource = it // Update display to show the newly picked image immediately
        }
    }

    val readStoragePermission = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        Manifest.permission.READ_EXTERNAL_STORAGE
    } else {
        null
    }
    val permissionState = readStoragePermission?.let { rememberPermissionState(permission = it) }

    val statusActiveLabel = stringResource(R.string.subscription_status_active)
    val statusPausedLabel = stringResource(R.string.subscription_status_paused)
    val statusCanceledLabel = stringResource(R.string.subscription_status_canceled)
    val statusPendingPaymentLabel = stringResource(R.string.subscription_status_pending_payment)

    val subscriptionStatusOptions = remember(statusActiveLabel, statusPausedLabel, statusCanceledLabel, statusPendingPaymentLabel) {
        mapOf(
            SubscriptionStatus.ACTIVE to statusActiveLabel,
            SubscriptionStatus.PAUSED to statusPausedLabel,
            SubscriptionStatus.CANCELED to statusCanceledLabel,
            SubscriptionStatus.PENDING_PAYMENT to statusPendingPaymentLabel
        )
    }
    val currentStatusLabel = subscriptionStatusOptions[status] ?: status.name

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (subscriptionToEdit != null) stringResource(R.string.edit_subscription_title_edit)
                        else stringResource(R.string.edit_subscription_title_add)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.edit_subscription_back_description)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val baseCostValue = baseCostString.toDoubleOrNull()
                        if (name.isNotBlank() && baseCostValue != null) {
                            var finalImageUrl: String? = subscriptionToEdit?.imageUrl

                            // If a new image was picked (tempImageUri is not null)
                            if (tempImageUri != null) {
                                val newFileName = "IMG_${UUID.randomUUID()}.jpg"
                                val copiedImagePath = copyUriToInternalStorage(context, tempImageUri!!, newFileName)

                                if (copiedImagePath != null) {
                                    // If there was an old image, try to delete it from internal storage
                                    subscriptionToEdit?.imageUrl?.let { oldPath ->
                                        // Only delete if it's a file path and not an http URL
                                        if (!oldPath.startsWith("http://") && !oldPath.startsWith("https://") && !oldPath.startsWith("content://")) {
                                            try {
                                                val oldFile = File(oldPath)
                                                if (oldFile.exists() && oldFile.path.contains(context.filesDir.path)) { // Ensure it's in our app's dir
                                                    oldFile.delete()
                                                }
                                            } catch (e: Exception) {
                                                Log.e("EditScreen", "Failed to delete old image: $oldPath", e)
                                            }
                                        }
                                    }
                                    finalImageUrl = copiedImagePath
                                } else {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.edit_subscription_error_saving_image), // Use a string resource
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    // Optionally, do not proceed if image saving fails
                                    // return@IconButton // or handle differently
                                }
                            }

                            val newOrUpdatedSubscriptionUiModel = SubscriptionUiModel(
                                id = subscriptionToEdit?.id ?: "",
                                name = name,
                                imageUrl = finalImageUrl, // Use the new file path or existing/old one
                                renewalDate = renewalDate,
                                baseCost = baseCostValue,
                                baseCurrency = baseCurrency,
                                status = status,
                                cost = baseCostValue,
                                currencySymbol = baseCurrency
                            )
                            onSaveSubscription(newOrUpdatedSubscriptionUiModel)
                        } else {
                            Toast.makeText(
                                context,
                                context.getString(R.string.edit_subscription_error_fill_fields),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = stringResource(R.string.edit_subscription_save_description)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Image Section
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable {
                        if (readStoragePermission == null || permissionState?.status?.isGranted == true) {
                            imagePickerLauncher.launch("image/*")
                        } else {
                            permissionState?.launchPermissionRequest()
                        }
                    }
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {
                // Use currentImageSource for AsyncImage
                if (currentImageSource != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(currentImageSource) // Load either temp Uri, file path, or web URL
                            .crossfade(true)
                            .error(R.drawable.ic_placeholder_image)
                            .placeholder(R.drawable.ic_placeholder_image) // Also good to have a placeholder
                            .build(),
                        contentDescription = stringResource(R.string.edit_subscription_image_description),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Filled.PhotoCamera,
                        contentDescription = stringResource(R.string.edit_subscription_add_image_icon_description),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                stringResource(R.string.edit_subscription_tap_to_change_image),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.edit_subscription_label_name)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                singleLine = true
            )
            LaunchedEffect(Unit) {
                if (subscriptionToEdit == null) focusRequester.requestFocus()
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = baseCostString,
                onValueChange = { baseCostString = it },
                label = { Text(stringResource(R.string.edit_subscription_label_renewal_cost, baseCurrency)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Selector de Moneda Base
            var baseCurrencyExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = baseCurrencyExpanded,
                onExpandedChange = { baseCurrencyExpanded = !baseCurrencyExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = baseCurrency,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.edit_subscription_label_base_currency)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = baseCurrencyExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = baseCurrencyExpanded,
                    onDismissRequest = { baseCurrencyExpanded = false }
                ) {
                    baseCurrencyOptions.forEach { selectionOption ->
                        DropdownMenuItem(
                            text = { Text(selectionOption) },
                            onClick = {
                                baseCurrency = selectionOption
                                baseCurrencyExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Selector de Fecha de Renovación
            OutlinedTextField(
                value = renewalDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                onValueChange = { /* No se cambia directamente */ },
                label = { Text(stringResource(R.string.edit_subscription_label_renewal_date)) },
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(
                            Icons.Filled.CalendarToday,
                            contentDescription = stringResource(R.string.edit_subscription_calendar_icon_description)
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            if (showDatePicker) {
                val calendar = Calendar.getInstance()
                calendar.time = java.util.Date.from(renewalDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
                DatePickerDialog(
                    context,
                    { _, year, month, dayOfMonth ->
                        renewalDate = LocalDate.of(year, month + 1, dayOfMonth)
                        showDatePicker = false
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).apply {
                    setOnDismissListener { showDatePicker = false }
                    show()
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Selector de Estado de Suscripción
            var statusExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = statusExpanded,
                onExpandedChange = { statusExpanded = !statusExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = currentStatusLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.edit_subscription_label_status)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = statusExpanded,
                    onDismissRequest = { statusExpanded = false }
                ) {
                    subscriptionStatusOptions.forEach { (statusEnum, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                status = statusEnum
                                statusExpanded = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// Helper function to copy Uri content to internal storage
private fun copyUriToInternalStorage(context: Context, uri: Uri, newFileName: String): String? {
    try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        // Create a directory for subscription images if it doesn't exist
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
        Log.e("ImageCopy", "IOException: Error copying URI to internal storage", e)
        return null
    } catch (e: SecurityException) {
        Log.e("ImageCopy", "SecurityException: Error copying URI to internal storage", e)
        // This might happen if the URI is no longer valid or accessible
        return null
    }
}

// Preview
@Preview(showBackground = true, name = "Edit Subscription Screen Light")
@Composable
fun EditSubscriptionScreenPreview() {
    MobileAppProyectAndroidTheme {
        EditSubscriptionScreen(
            subscriptionToEdit = null,
            onSaveSubscription = {},
            onNavigateBack = {}
        )
    }
}

@Preview(showBackground = true, name = "Edit Subscription Screen Dark - Editing")
@Composable
fun EditSubscriptionScreenEditPreview() {
    MobileAppProyectAndroidTheme(darkTheme = true) {
        EditSubscriptionScreen(
            subscriptionToEdit = SubscriptionUiModel(
                id = "1",
                name = "Sample Service UI",
                // For preview, you might point to a drawable or keep null
                imageUrl = null, // Or "file:///android_asset/some_image.png" if you add to assets
                renewalDate = LocalDate.now().plusMonths(1),
                baseCost = 9.99,
                baseCurrency = "USD",
                status = SubscriptionStatus.ACTIVE,
                cost = 9.99,
                currencySymbol = "$"
            ),
            onSaveSubscription = {},
            onNavigateBack = {}
        )
    }
}