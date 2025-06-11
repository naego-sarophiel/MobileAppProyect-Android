package com.example.mobileappproyect_android.ui.home

// Mantén tus importaciones actuales, solo asegúrate de estas:
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items // Asegúrate que esta está
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog // NECESARIO PARA EL DIÁLOGO
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button // NECESARIO PARA EL DIÁLOGO
import androidx.compose.material3.ButtonDefaults // NECESARIO PARA EL DIÁLOGO
import androidx.lifecycle.compose.collectAsStateWithLifecycle

// ... el resto de tus importaciones ...
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.error
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.request.ImageRequest
import coil.compose.AsyncImage
import com.example.mobileappproyect_android.R
// Import SubscriptionUiModel and remove/comment out old Subscription import if it was here
import com.example.mobileappproyect_android.ui.home.SubscriptionUiModel // <-- IMPORT THIS
// import com.example.mobileappproyect_android.data.Subscription // <-- REMOVE OR COMMENT OUT if previously used for SubscriptionItem
import com.example.mobileappproyect_android.data.SubscriptionStatus
import com.example.mobileappproyect_android.data.localizedName // Asegúrate que está importado
import com.example.mobileappproyect_android.ui.theme.MobileAppProyectAndroidTheme
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter // Para formatear fecha
import java.time.format.FormatStyle     // Para formatear fecha
import java.util.Locale                 // Para formatear costo


// Data class for Navigation Drawer Items
data class NavItem(
    val label: String,
    val icon: ImageVector,
    val route: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel = viewModel(),
    onSubscriptionClick: (SubscriptionUiModel) -> Unit, // Para editar/ver detalles
    onAddSubscriptionClick: () -> Unit,
    navController: NavController
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    // subscriptionsList is now List<SubscriptionUiModel>
    val subscriptionsList by homeViewModel.subscriptions.collectAsStateWithLifecycle()
    val currentSortCriteria by homeViewModel.sortCriteria.collectAsStateWithLifecycle()
    val monthlyTotal by homeViewModel.monthlyTotalSpend.collectAsStateWithLifecycle()

    val homeLabel = stringResource(R.string.home_nav_item_home)
    val addSubscriptionLabel = stringResource(R.string.home_nav_item_add_subscription)
    val settingsLabel = stringResource(R.string.home_nav_item_settings)

    val navItems = remember(homeLabel, addSubscriptionLabel, settingsLabel) {
        listOf(
            NavItem(homeLabel, Icons.Filled.Home, "home_screen"),
            NavItem(addSubscriptionLabel, Icons.Filled.AddCircle, "add_subscription"),
            NavItem(settingsLabel, Icons.Filled.Settings, "settings")
        )
    }

    var selectedItemIndex by remember { mutableStateOf(0) }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    val sortByRenewalLabel = stringResource(R.string.sort_by_renewal_soonest)
    val sortByPriceLowHighLabel = stringResource(R.string.sort_by_price_low_high)
    val sortByPriceHighLowLabel = stringResource(R.string.sort_by_price_high_low)
    val sortByNameAzLabel = stringResource(R.string.sort_by_name_az)

    val sortOptions = remember(sortByRenewalLabel, sortByPriceLowHighLabel, sortByPriceHighLowLabel, sortByNameAzLabel) {
        listOf(
            SortCriteria.RENEWAL_DATE_ASC to sortByRenewalLabel,
            SortCriteria.PRICE_ASC to sortByPriceLowHighLabel,
            SortCriteria.PRICE_DESC to sortByPriceHighLowLabel,
            SortCriteria.NAME_ASC to sortByNameAzLabel
        )
    }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var subscriptionIdToDelete by remember { mutableStateOf<String?>(null) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(16.dp))
                Text(
                    stringResource(R.string.home_drawer_title_menu),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(16.dp))
                navItems.forEachIndexed { index, item ->
                    NavigationDrawerItem(
                        label = { Text(item.label) },
                        selected = index == selectedItemIndex,
                        onClick = {
                            selectedItemIndex = index
                            scope.launch { drawerState.close() }
                            if (item.route == "settings") {
                                navController.navigate(item.route)
                            } else if (item.route == "add_subscription") {
                                onAddSubscriptionClick()
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.home_title)) },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch {
                                if (drawerState.isClosed) drawerState.open() else drawerState.close()
                            }
                        }) {
                            Icon(
                                Icons.Filled.Menu,
                                contentDescription = stringResource(R.string.home_drawer_open_description)
                            )
                        }
                    },
                    actions = {
                        ExposedDropdownMenuBox(
                            expanded = sortMenuExpanded,
                            onExpandedChange = { sortMenuExpanded = !sortMenuExpanded }
                        ) {
                            IconButton(
                                onClick = { sortMenuExpanded = true },
                                modifier = Modifier.menuAnchor()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sort,
                                    contentDescription = stringResource(R.string.home_sort_subscriptions_description)
                                )
                            }
                            ExposedDropdownMenu(
                                expanded = sortMenuExpanded,
                                onDismissRequest = { sortMenuExpanded = false },
                                modifier = Modifier.width(220.dp)
                            ) {
                                sortOptions.forEach { (criteria, label) ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = label,
                                                modifier = Modifier.fillMaxWidth(),
                                                textAlign = TextAlign.Start
                                            )
                                        },
                                        onClick = {
                                            homeViewModel.setSortCriteria(criteria)
                                            sortMenuExpanded = false
                                        },
                                        leadingIcon = if (currentSortCriteria == criteria) {
                                            {
                                                Icon(
                                                    Icons.Filled.Check,
                                                    contentDescription = stringResource(R.string.home_sort_selected_description)
                                                )
                                            }
                                        } else null,
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = onAddSubscriptionClick) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = stringResource(R.string.home_fab_add_subscription_description)
                    )
                }
            },
            bottomBar = {
                if (subscriptionsList.isNotEmpty()) {
                    BottomAppBar(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${stringResource(R.string.home_monthly_total_label)}: $monthlyTotal",
                                style = MaterialTheme.typography.titleMedium,
                                fontSize = 18.sp
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            if (subscriptionsList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.home_no_subscriptions))
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 72.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // items now receives List<SubscriptionUiModel>
                    items(subscriptionsList, key = { it.id }) { subscriptionUiModel ->
                        val isSoon = homeViewModel.isRenewalSoon(subscriptionUiModel.renewalDate)
                        SubscriptionItem(
                            subscription = subscriptionUiModel, // Pass SubscriptionUiModel
                            isRenewalSoon = isSoon,
                            onClick = { onSubscriptionClick(subscriptionUiModel) },
                            onDeleteClick = { subId ->
                                subscriptionIdToDelete = subId
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }

            if (showDeleteDialog && subscriptionIdToDelete != null) {
                AlertDialog(
                    onDismissRequest = {
                        showDeleteDialog = false
                        subscriptionIdToDelete = null
                    },
                    title = { Text(stringResource(R.string.delete_subscription_dialog_title)) },
                    text = { Text(stringResource(R.string.delete_subscription_dialog_message)) },
                    confirmButton = {
                        Button(
                            onClick = {
                                subscriptionIdToDelete?.let { homeViewModel.deleteSubscription(it) }
                                showDeleteDialog = false
                                subscriptionIdToDelete = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text(stringResource(R.string.delete_action))
                        }
                    },
                    dismissButton = {
                        Button(onClick = {
                            showDeleteDialog = false
                            subscriptionIdToDelete = null
                        }) {
                            Text(stringResource(R.string.cancel_action))
                        }
                    }
                )
            }
        }
    }
}


@Composable
fun SubscriptionItem(
    subscription: SubscriptionUiModel,
    isRenewalSoon: Boolean,
    onClick: () -> Unit,
    onDeleteClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                // Use Coil's ImageRequest.Builder
                model = ImageRequest.Builder(LocalContext.current) // <--- Use coil.request.ImageRequest
                    .data(subscription.imageUrl)
                    .crossfade(true)
                    .placeholder(R.drawable.ic_placeholder_image) // Make sure R.drawable.ic_placeholder_image exists
                    .error(R.drawable.ic_placeholder_image)
                    .build(), // This .build() is a method of coil.request.ImageRequest.Builder
                contentDescription = stringResource(R.string.subscription_item_logo_description, subscription.name), // Make sure R.string.subscription_item_logo_description exists
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = subscription.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                val formattedRenewalDate = subscription.renewalDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
                Text(
                    text = "${stringResource(R.string.subscription_item_renews_prefix)} $formattedRenewalDate",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isRenewalSoon) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${subscription.currencySymbol}${String.format(Locale.getDefault(), "%.2f", subscription.cost)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subscription.status.localizedName(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .background(
                            // Assuming statusColor is defined elsewhere and takes SubscriptionStatus
                            color = statusColor(subscription.status).copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            IconButton(
                onClick = { onDeleteClick(subscription.id) }
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.subscription_item_delete_description),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun statusColor(status: SubscriptionStatus): Color {
    return when (status) {
        SubscriptionStatus.ACTIVE -> Color.Green
        SubscriptionStatus.PAUSED -> Color.Gray
        SubscriptionStatus.CANCELED -> MaterialTheme.colorScheme.error
        SubscriptionStatus.PENDING_PAYMENT -> Color(0xFFFFA500) // Orange
    }
}

@Preview(showBackground = true, name = "HomeScreen Light")
@Composable
fun HomeScreenPreview() {
    MobileAppProyectAndroidTheme(darkTheme = false) {
        HomeScreen(
            onSubscriptionClick = {},
            onAddSubscriptionClick = {},
            navController = rememberNavController()
            // HomeViewModel will provide an empty list or sample SubscriptionUiModels for preview
        )
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, name = "HomeScreen Dark")
@Composable
fun HomeScreenDarkPreview() {
    MobileAppProyectAndroidTheme(darkTheme = true) {
        HomeScreen(
            onSubscriptionClick = {},
            onAddSubscriptionClick = {},
            navController = rememberNavController()
            // HomeViewModel will provide an empty list or sample SubscriptionUiModels for preview
        )
    }
}

@Preview(showBackground = true, name = "SubscriptionItem Renewal Soon")
@Composable
fun SubscriptionItemPreviewRenewalSoon() {
    MobileAppProyectAndroidTheme {
        SubscriptionItem(
            // CHANGED to SubscriptionUiModel
            subscription = SubscriptionUiModel(
                id = "1",
                name = "Netflix Premium",
                imageUrl = null,
                renewalDate = LocalDate.now().plusDays(3),
                baseCost = 15.99,       // Base cost from entity
                baseCurrency = "USD",   // Base currency from entity
                cost = 15.99,           // Calculated/display cost
                currencySymbol = "$",   // Display currency symbol
                status = SubscriptionStatus.ACTIVE
            ),
            isRenewalSoon = true,
            onClick = {},
            onDeleteClick = {}
        )
    }
}

@Preview(showBackground = true, name = "SubscriptionItem Normal")
@Composable
fun SubscriptionItemPreviewNormal() {
    MobileAppProyectAndroidTheme {
        SubscriptionItem(
            // CHANGED to SubscriptionUiModel
            subscription = SubscriptionUiModel(
                id = "2",
                name = "Spotify Family",
                imageUrl = null,
                renewalDate = LocalDate.now().plusMonths(1),
                baseCost = 14.99,
                baseCurrency = "USD",
                cost = 14.99,
                currencySymbol = "$",
                status = SubscriptionStatus.ACTIVE
            ),
            isRenewalSoon = false,
            onClick = {},
            onDeleteClick = {}
        )
    }
}