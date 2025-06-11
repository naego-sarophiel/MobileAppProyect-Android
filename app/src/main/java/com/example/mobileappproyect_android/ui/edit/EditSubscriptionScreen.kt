package com.example.mobileappproyect_android.ui.edit

import android.Manifest
import android.app.Application
import android.app.DatePickerDialog
// import android.content.Context // No longer needed directly for copyUriToInternalStorage
import android.net.Uri
import android.os.Build
// import android.util.Log // Logging is now in ViewModel
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
import androidx.compose.runtime.* // Keep this for remember, LaunchedEffect etc.
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
// Remove specific remember states if they are now in ViewModel
// import androidx.compose.runtime.getValue
// import androidx.compose.runtime.setValue
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
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.mobileappproyect_android.R
// import com.example.mobileappproyect_android.ui.home.SubscriptionUiModel // VM handles model creation
import com.example.mobileappproyect_android.data.SubscriptionStatus
import com.example.mobileappproyect_android.ui.home.HomeViewModel
import com.example.mobileappproyect_android.ui.theme.MobileAppProyectAndroidTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
// import java.io.File // Handled by VM
// import java.io.FileOutputStream // Handled by VM
// import java.io.IOException // Handled by VM
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
// import java.util.UUID // Handled by VM

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun EditSubscriptionScreen(
    navController: NavController, // For navigation
    editSubscriptionViewModel: EditSubscriptionViewModel // Injected
) {
    val context = LocalContext.current // Still needed for Toast, DatePickerDialog, Permission

    // States from ViewModel
    val pageTitle = editSubscriptionViewModel.pageTitle
    val name = editSubscriptionViewModel.name
    val baseCostString = editSubscriptionViewModel.baseCostString
    val baseCurrency = editSubscriptionViewModel.baseCurrency
    val baseCurrencyOptions = editSubscriptionViewModel.baseCurrencyOptions
    val renewalDate = editSubscriptionViewModel.renewalDate
    val status = editSubscriptionViewModel.status
    val currentImageSource = editSubscriptionViewModel.currentImageSource
    val showDatePicker = editSubscriptionViewModel.showDatePicker

    val focusRequester = remember { FocusRequester() }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        editSubscriptionViewModel.onTempImageUriReceived(uri)
    }

    // Permission for image picking
    val readStoragePermission = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        Manifest.permission.READ_EXTERNAL_STORAGE
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES // For Android 13+
    } else {
        null // No specific permission needed for GetContent on Q, R, S if not using READ_EXTERNAL_STORAGE
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
                title = { Text(pageTitle) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) { // Use NavController
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.edit_subscription_back_description)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        editSubscriptionViewModel.saveSubscription(
                            onSuccess = {
                                Toast.makeText(context, "Subscription saved!", Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                            },
                            onError = { errorMessage ->
                                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                            }
                        )
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
                if (currentImageSource != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(currentImageSource)
                            .crossfade(true)
                            .error(R.drawable.ic_placeholder_image) // Ensure this drawable exists
                            .placeholder(R.drawable.ic_placeholder_image)
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
                onValueChange = { editSubscriptionViewModel.onNameChange(it) },
                label = { Text(stringResource(R.string.edit_subscription_label_name)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                singleLine = true
            )
            // Decide if you want to focus based on if it's new (ViewModel now knows via pageTitle or initialSubscriptionId)
            val addTitle = stringResource(R.string.edit_subscription_title_add)

            LaunchedEffect(pageTitle, addTitle) { // Add 'addTitle' to keys if it could change, though unlikely for resource strings
                if (pageTitle == addTitle) {      // <<< USE THE VARIABLE HERE
                    // Might need a delay for UI to be ready
                    kotlinx.coroutines.delay(100) // Consider if this delay is always necessary
                    focusRequester.requestFocus()
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = baseCostString,
                onValueChange = { editSubscriptionViewModel.onBaseCostChange(it) },
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
                    onValueChange = {}, // VM handles change
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
                                editSubscriptionViewModel.onBaseCurrencyChange(selectionOption)
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
                onValueChange = { /* Not directly changeable here */ },
                label = { Text(stringResource(R.string.edit_subscription_label_renewal_date)) },
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { editSubscriptionViewModel.onShowDatePicker(true) }) {
                        Icon(
                            Icons.Filled.CalendarToday,
                            contentDescription = stringResource(R.string.edit_subscription_calendar_icon_description)
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            if (showDatePicker) {
                val calendar = Calendar.getInstance().apply {
                    time = java.util.Date.from(renewalDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
                }
                DatePickerDialog(
                    context,
                    { _, year, month, dayOfMonth ->
                        editSubscriptionViewModel.onRenewalDateChange(LocalDate.of(year, month + 1, dayOfMonth))
                        editSubscriptionViewModel.onShowDatePicker(false)
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).apply {
                    setOnDismissListener { editSubscriptionViewModel.onShowDatePicker(false) }
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
                    value = currentStatusLabel, // Derived from VM's status
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
                                editSubscriptionViewModel.onStatusChange(statusEnum)
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

// --- Previews will need to be updated to provide a mock/dummy ViewModel ---
@Preview(showBackground = true, name = "Edit Subscription Screen Light (Add)")
@Composable
fun EditSubscriptionScreenAddPreview() {
    MobileAppProyectAndroidTheme {
        // For preview, you'd ideally mock the HomeViewModel or provide a dummy one.
        // This is a simplified approach for preview.
        val dummyHomeViewModel = object : HomeViewModel(Application()) {
            override suspend fun getSubscriptionById(id: String): com.example.mobileappproyect_android.ui.home.SubscriptionUiModel? = null
            override fun addSubscription(subscription: com.example.mobileappproyect_android.ui.home.SubscriptionUiModel) {}
            override fun updateSubscription(subscription: com.example.mobileappproyect_android.ui.home.SubscriptionUiModel) {}
        }
        val dummyApplication = LocalContext.current.applicationContext as Application

        EditSubscriptionScreen(
            navController = NavController(LocalContext.current), // Dummy NavController for preview
            editSubscriptionViewModel = EditSubscriptionViewModel(
                app = dummyApplication,
                homeViewModel = dummyHomeViewModel,
                initialSubscriptionId = "new" // Or null for adding
            )
        )
    }
}

@Preview(showBackground = true, name = "Edit Subscription Screen Dark (Edit)")
@Composable
fun EditSubscriptionScreenEditPreview() {
    MobileAppProyectAndroidTheme(darkTheme = true) {
        val dummyApplication = LocalContext.current.applicationContext as Application
        val existingSub = com.example.mobileappproyect_android.ui.home.SubscriptionUiModel(
            id = "preview-123",
            name = "Netflix Preview",
            baseCost = 15.99,
            baseCurrency = "USD",
            renewalDate = LocalDate.now().plusDays(10),
            status = SubscriptionStatus.ACTIVE,
            imageUrl = null, // or a placeholder R.drawable link if Coil can load it in preview
            cost = 15.99,
            currencySymbol = "$"
        )
        val dummyHomeViewModel = object : HomeViewModel(dummyApplication) {
            override suspend fun getSubscriptionById(id: String): com.example.mobileappproyect_android.ui.home.SubscriptionUiModel? = if (id == "preview-123") existingSub else null
            override fun addSubscription(subscription: com.example.mobileappproyect_android.ui.home.SubscriptionUiModel) {}
            override fun updateSubscription(subscription: com.example.mobileappproyect_android.ui.home.SubscriptionUiModel) {}
        }


        EditSubscriptionScreen(
            navController = NavController(LocalContext.current),
            editSubscriptionViewModel = EditSubscriptionViewModel(
                app = dummyApplication,
                homeViewModel = dummyHomeViewModel,
                initialSubscriptionId = "preview-123"
            )
        )
    }
}