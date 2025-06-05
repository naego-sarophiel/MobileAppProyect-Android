package com.example.mobileappproyect_android.ui.edit

import android.Manifest
import android.app.DatePickerDialog
import android.content.Context
import android.net.Uri
import android.os.Build
import android.widget.DatePicker
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.mobileappproyect_android.R // Your placeholder
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
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import androidx.compose.material3.ExposedDropdownMenuDefaults // Asegúrate que está
import androidx.compose.material3.ExposedDropdownMenuBox // Asegúrate que está
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.DropdownMenuItem

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun EditSubscriptionScreen(
    subscriptionToEdit: Subscription?,
    onSaveSubscription: (Subscription) -> Unit,
    onNavigateBack: () -> Unit
    // Podrías pasar el SettingsManager o el currentDisplayCurrency si el enfoque de edición fuera diferente
) {
    val context = LocalContext.current
    var name by remember(subscriptionToEdit) { mutableStateOf(subscriptionToEdit?.name ?: "") }

    // --- MANEJO DEL PRECIO ---
    // Ahora 'costString' representará el 'baseCost'
    var baseCostString by remember(subscriptionToEdit) { mutableStateOf(subscriptionToEdit?.baseCost?.toString() ?: "") }
    // Asumimos una moneda base por defecto si es nueva, o la de la suscripción existente
    var baseCurrency by remember(subscriptionToEdit) { mutableStateOf(subscriptionToEdit?.baseCurrency ?: "USD") }
    // Podrías tener un Dropdown para cambiar `baseCurrency` si lo deseas. Por ahora, lo mantenemos simple.
    var baseCurrencyExpanded by remember { mutableStateOf(false) } // Estado para controlar si el dropdown está expandido
    val baseCurrencyOptions = listOf("USD", "EUR", "GBP", "JPY", "INR") // Tus monedas base soportadas


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
        null
    }
    val permissionState = readStoragePermission?.let { rememberPermissionState(permission = it) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (subscriptionToEdit != null) stringResource(R.string.edit_subscription) else stringResource(R.string.add_subscription)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val baseCostValue = baseCostString.toDoubleOrNull() // Validar baseCost

                        if (name.isNotBlank() && baseCostValue != null) {
                            val newOrUpdatedSubscription = Subscription(
                                id = subscriptionToEdit?.id ?: UUID.randomUUID().toString(),
                                name = name,
                                imageUrl = imageUri?.toString(),
                                renewalDate = renewalDate,
                                baseCost = baseCostValue, // Guardar el baseCost editado
                                baseCurrency = baseCurrency, // Usar la baseCurrency (fija o seleccionada)
                                cost = 0.0, // Se recalculará por el ViewModel
                                currencySymbol = "", // Se recalculará por el ViewModel
                                status = status
                            )
                            onSaveSubscription(newOrUpdatedSubscription)
                        } else {
                            // Show error message (e.g., using a Snackbar)
                            Toast.makeText(context, "Please fill all fields correctly.", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.Filled.Check, contentDescription = "Save Subscription")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()) // Make content scrollable
        ) {
            // Image Section
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable {
                        if (readStoragePermission == null || permissionState == null || permissionState.status.isGranted) {
                            imagePickerLauncher.launch("image/*")
                        } else {
                            permissionState.launchPermissionRequest()
                        }
                    }
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {
                if (imageUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageUri) // Load from local URI
                            .crossfade(true)
                            .error(R.drawable.ic_placeholder_image)
                            .build(),
                        contentDescription = "Subscription Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Filled.PhotoCamera,
                        contentDescription = "Add Image",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Tap to change image",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.subscription_name)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                singleLine = true
            )
            LaunchedEffect(Unit) { // Request focus on the first field when screen opens
                if (subscriptionToEdit == null) focusRequester.requestFocus()
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = baseCostString,
                onValueChange = { baseCostString = it },
                label = { Text("${stringResource(R.string.renewal_cost)} (${baseCurrency})") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))
            var baseCurrencyExpanded by remember { mutableStateOf(false) }
            val baseCurrencyOptions = listOf("USD", "EUR", "GBP") // Tus monedas base soportadas
            ExposedDropdownMenuBox(
                expanded = baseCurrencyExpanded,
                onExpandedChange = {
                    baseCurrencyExpanded = !baseCurrencyExpanded // Alternar el estado al hacer clic
                },
                modifier = Modifier.fillMaxWidth() // Asegúrate de que el Box ocupe el ancho
            ) {
                OutlinedTextField( // Este es el campo que se muestra
                    value = baseCurrency, // Muestra la moneda base seleccionada
                    onValueChange = {}, // No se cambia directamente aquí
                    readOnly = true,
                    label = { Text("Base Currency") }, // TODO: Usar stringResource
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = baseCurrencyExpanded) },
                    modifier = Modifier
                        .menuAnchor() // IMPORTANTE: Esto ancla el menú al TextField
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = baseCurrencyExpanded,
                    onDismissRequest = { baseCurrencyExpanded = false } // Cerrar si se hace clic fuera
                ) {
                    baseCurrencyOptions.forEach { selectionOption ->
                        DropdownMenuItem(
                            text = { Text(selectionOption) },
                            onClick = {
                                baseCurrency = selectionOption // Actualizar la moneda seleccionada
                                baseCurrencyExpanded = false // Cerrar el menú
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Renewal Date Picker
            OutlinedTextField(
                value = renewalDate.format(DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM)),
                onValueChange = { /* Read Only */ },
                label = { Text("Renewal Date") },
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true },
                trailingIcon = {
                    Icon(Icons.Filled.CalendarToday, "Select Date", Modifier.clickable { showDatePicker = true })
                }
            )

            if (showDatePicker) {
                ShowDatePickerDialog(context, renewalDate) { selectedDate ->
                    renewalDate = selectedDate
                    showDatePicker = false
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Status Dropdown
            var statusExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = statusExpanded,
                onExpandedChange = { statusExpanded = !statusExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = run { // Apply the same logic here
                        val lowerCasedName = status.name.replace("_", " ").lowercase()
                        if (lowerCasedName.isNotEmpty()) {
                            lowerCasedName.substring(0, 1).uppercase(java.util.Locale.getDefault()) + lowerCasedName.substring(1)
                        } else {
                            lowerCasedName
                        }
                    },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.status)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = statusExpanded,
                    onDismissRequest = { statusExpanded = false }
                ) {
                    SubscriptionStatus.values().forEach { selectionOption ->
                        DropdownMenuItem(
                            text = {
                                Text(run { // And here for each item in the dropdown
                                    val lowerCasedOption = selectionOption.name.replace("_", " ").lowercase()
                                    if (lowerCasedOption.isNotEmpty()) {
                                        lowerCasedOption.substring(0, 1).uppercase(java.util.Locale.getDefault()) + lowerCasedOption.substring(1)
                                    } else {
                                        lowerCasedOption
                                    }
                                })
                            },
                            onClick = {
                                status = selectionOption
                                statusExpanded = false
                            }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun ShowDatePickerDialog(
    context: Context,
    initialDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    val year = initialDate.year
    val month = initialDate.monthValue - 1 // Calendar month is 0-indexed
    val day = initialDate.dayOfMonth

    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _: DatePicker, selectedYear: Int, selectedMonth: Int, selectedDayOfMonth: Int ->
                onDateSelected(LocalDate.of(selectedYear, selectedMonth + 1, selectedDayOfMonth))
            }, year, month, day
        )
    }
    // To ensure the dialog is recreated if initialDate changes significantly or context changes
    // though for this specific use case, direct launch is fine.
    LaunchedEffect(datePickerDialog) {
        datePickerDialog.show()
    }
    // Handle dialog dismissal (optional, if you need to react to it)
    DisposableEffect(Unit) {
        onDispose {
            if (datePickerDialog.isShowing) {
                datePickerDialog.dismiss()
            }
        }
    }
}


@Preview(showBackground = true, name = "Edit Screen Light")
@Composable
fun EditSubscriptionScreenPreviewLight() {
    MobileAppProyectAndroidTheme {
        EditSubscriptionScreen(
            subscriptionToEdit = Subscription(
                id = "1",
                name = "Sample Service",
                imageUrl = null, //"https://via.placeholder.com/150",
                renewalDate = LocalDate.now().plusDays(10),
                baseCost = 19.99,
                baseCurrency = "USD",
                currencySymbol = "$",
                cost = 19.99,
                status = SubscriptionStatus.ACTIVE
            ),
            onSaveSubscription = {},
            onNavigateBack = {}
        )
    }
}

@Preview(showBackground = true, name = "Add Screen Light")
@Composable
fun AddSubscriptionScreenPreviewLight() {
    MobileAppProyectAndroidTheme {
        EditSubscriptionScreen(
            subscriptionToEdit = null, // For "Add" mode
            onSaveSubscription = {},
            onNavigateBack = {}
        )
    }
}