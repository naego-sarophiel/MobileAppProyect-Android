package com.example.mobileappproyect_android.ui.edit

import android.Manifest
import android.app.DatePickerDialog
// import android.content.Context // No se usa directamente en la función principal después del cambio
import android.net.Uri
import android.os.Build
// import android.widget.DatePicker // No se usa directamente
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
// import androidx.compose.foundation.Image // No se usa directamente
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
// import androidx.compose.runtime.getValue // No es necesario si se usa by
// import androidx.compose.runtime.setValue // No es necesario si se usa by
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
// import androidx.compose.ui.graphics.Color // No se usa directamente
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
// import androidx.compose.ui.res.painterResource // No se usa directamente aquí
import androidx.compose.ui.res.stringResource // ¡IMPORTANTE!
// import androidx.compose.ui.semantics.dismiss // No se usa directamente
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.mobileappproyect_android.R // ¡IMPORTANTE para R.string!
import com.example.mobileappproyect_android.data.Subscription
import com.example.mobileappproyect_android.data.SubscriptionStatus
import com.example.mobileappproyect_android.ui.theme.MobileAppProyectAndroidTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.UUID
// import com.google.accompanist.permissions.isGranted // Duplicado
// import com.google.accompanist.permissions.rememberPermissionState // Duplicado
// import androidx.compose.material3.ExposedDropdownMenuDefaults // No es necesario si se usa el que viene por defecto
// import androidx.compose.material3.ExposedDropdownMenuBox // No es necesario si se usa el que viene por defecto
// import androidx.compose.material3.DropdownMenuItem // No es necesario si se usa el que viene por defecto

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun EditSubscriptionScreen(
    subscriptionToEdit: Subscription?,
    onSaveSubscription: (Subscription) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var name by remember(subscriptionToEdit) { mutableStateOf(subscriptionToEdit?.name ?: "") }
    var baseCostString by remember(subscriptionToEdit) { mutableStateOf(subscriptionToEdit?.baseCost?.toString() ?: "") }
    var baseCurrency by remember(subscriptionToEdit) { mutableStateOf(subscriptionToEdit?.baseCurrency ?: "USD") }
    // var baseCurrencyExpanded by remember { mutableStateOf(false) } // Se define más abajo si es necesario
    val baseCurrencyOptions = listOf("USD", "EUR", "GBP", "JPY", "INR") // Símbolos, generalmente no localizados directamente

    var renewalDate by remember(subscriptionToEdit) { mutableStateOf(subscriptionToEdit?.renewalDate ?: LocalDate.now()) }
    var imageUri by remember(subscriptionToEdit) { mutableStateOf<Uri?>(subscriptionToEdit?.imageUrl?.let { Uri.parse(it) }) }
    var status by remember(subscriptionToEdit) { mutableStateOf(subscriptionToEdit?.status ?: SubscriptionStatus.ACTIVE) }

    var showDatePicker by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { imageUri = it }
    }

    val readStoragePermission = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        Manifest.permission.READ_EXTERNAL_STORAGE
    } else {
        null // Para Android Q y superior, no se necesita permiso para GetContent si solo lees tu propia app o de MediaStore
    }
    val permissionState = readStoragePermission?.let { rememberPermissionState(permission = it) }

    // --- Strings para los estados de suscripción ---
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
    val currentStatusLabel = subscriptionStatusOptions[status] ?: status.name // Fallback al nombre del enum

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
                            val newOrUpdatedSubscription = Subscription(
                                id = subscriptionToEdit?.id ?: UUID.randomUUID().toString(),
                                name = name,
                                imageUrl = imageUri?.toString(),
                                renewalDate = renewalDate,
                                baseCost = baseCostValue,
                                baseCurrency = baseCurrency,
                                cost = 0.0, // Se recalculará
                                currencySymbol = "", // Se recalculará
                                status = status
                            )
                            onSaveSubscription(newOrUpdatedSubscription)
                        } else {
                            Toast.makeText(
                                context,
                                context.getString(R.string.edit_subscription_error_fill_fields), // Usar context.getString para Toast
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
                if (imageUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageUri)
                            .crossfade(true)
                            .error(R.drawable.ic_placeholder_image) // Asumiendo que este placeholder no necesita localización
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
                            text = { Text(selectionOption) }, // Símbolos de moneda generalmente no se localizan
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
                value = renewalDate.format(DateTimeFormatter.ISO_LOCAL_DATE), // Formato estándar
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
                    setOnDismissListener { showDatePicker = false } // Asegurar que se cierra el diálogo
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
                    value = currentStatusLabel, // Mostrar el label localizado
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
                            text = { Text(label) }, // Usar el label localizado
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

// Preview (opcional, pero útil)
@Preview(showBackground = true, name = "Edit Subscription Screen Light")
@Composable
fun EditSubscriptionScreenPreview() {
    MobileAppProyectAndroidTheme {
        EditSubscriptionScreen(
            subscriptionToEdit = null, // Para "Add"
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
            subscriptionToEdit = Subscription( // Datos de ejemplo
                id = "1",
                name = "Sample Service",
                imageUrl = null,
                renewalDate = LocalDate.now().plusMonths(1),
                baseCost = 9.99,
                baseCurrency = "USD",
                cost = 9.99,
                currencySymbol = "$",
                status = SubscriptionStatus.ACTIVE
            ),
            onSaveSubscription = {},
            onNavigateBack = {}
        )
    }
}