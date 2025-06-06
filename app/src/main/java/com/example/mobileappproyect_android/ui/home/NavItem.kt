package com.example.mobileappproyect_android.ui.home

// Mantén tus importaciones actuales, solo asegúrate de estas:
import androidx.activity.result.launch
import androidx.compose.animation.core.copy
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.runtime.collectAsState // Opcional si usas collectAsStateWithLifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle // Recomendado
import com.example.mobileappproyect_android.ui.theme.MobileAppProyectAndroidTheme

// ... el resto de tus importaciones ...
import androidx.compose.material3.Card // Asegúrate de tener esta para SubscriptionItem
import androidx.compose.material3.CardDefaults // Para la elevación de la Card
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
import androidx.compose.ui.geometry.isEmpty
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.error
import androidx.compose.ui.text.font.FontWeight // Para el texto en SubscriptionItem
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.substring
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.mobileappproyect_android.R
import com.example.mobileappproyect_android.data.Subscription
import com.example.mobileappproyect_android.data.SubscriptionStatus
import com.example.mobileappproyect_android.data.localizedName
import kotlinx.coroutines.launch
import kotlin.text.isNotEmpty
import kotlin.text.lowercase
import kotlin.text.replace
import kotlin.text.substring
import kotlin.text.uppercase


// Data class for Navigation Drawer Items
data class NavItem(
    // Ya no almacenamos el string literal aquí si lo vamos a resolver en el Composable
    // En su lugar, podríamos tener un ID de recurso o un identificador para buscar el string.
    // Pero para simplificar y dado que se usa directamente en el @Composable HomeScreen,
    // podemos resolver los strings al crear la lista `navItems`.
    val label: String, // Este será el string ya resuelto
    val icon: ImageVector,
    val route: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel = viewModel(),
    onSubscriptionClick: (String) -> Unit,
    onAddSubscriptionClick: () -> Unit,
    navController: NavController
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val subscriptionsList by homeViewModel.subscriptions.collectAsStateWithLifecycle()
    val currentSortCriteria by homeViewModel.sortCriteria.collectAsStateWithLifecycle()

    // Resuelve los strings para NavItem aquí, ya que estamos en un @Composable
    val homeLabel = stringResource(R.string.home_nav_item_home)
    val addSubscriptionLabel = stringResource(R.string.home_nav_item_add_subscription)
    val settingsLabel = stringResource(R.string.home_nav_item_settings)

    val navItems = remember(homeLabel, addSubscriptionLabel, settingsLabel) { // Re-calcula si los strings cambian
        listOf(
            NavItem(homeLabel, Icons.Filled.Home, "home_screen"),
            NavItem(addSubscriptionLabel, Icons.Filled.AddCircle, "add_subscription"),
            NavItem(settingsLabel, Icons.Filled.Settings, "settings")
        )
    }

    var selectedItemIndex by remember { mutableStateOf(0) }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    // Resuelve los strings para sortOptions aquí
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

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(16.dp))
                Text(
                    stringResource(R.string.home_drawer_title_menu), // "Menu"
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(16.dp))
                navItems.forEachIndexed { index, item ->
                    NavigationDrawerItem(
                        label = { Text(item.label) }, // item.label ya es el string resuelto
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
                        // La contentDescription debería ser única y descriptiva
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
                    title = { Text(stringResource(R.string.home_title)) }, // "My Subscriptions"
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch {
                                if (drawerState.isClosed) drawerState.open() else drawerState.close()
                            }
                        }) {
                            Icon(
                                Icons.Filled.Menu,
                                contentDescription = stringResource(R.string.home_drawer_open_description) // "Open Drawer"
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
                                    contentDescription = stringResource(R.string.home_sort_subscriptions_description) // "Sort Subscriptions"
                                )
                            }

                            ExposedDropdownMenu(
                                expanded = sortMenuExpanded,
                                onDismissRequest = { sortMenuExpanded = false },
                                modifier = Modifier.width(220.dp) // O un ancho más dinámico
                            ) {
                                sortOptions.forEach { (criteria, label) -> // label ya es el string resuelto
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
                                                    contentDescription = stringResource(R.string.home_sort_selected_description) // "Selected sort criteria"
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
                        contentDescription = stringResource(R.string.home_fab_add_subscription_description) // "Add Subscription"
                    )
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
                    Text(stringResource(R.string.home_no_subscriptions)) // "No subscriptions added yet..."
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(subscriptionsList, key = { it.id }) { subscription ->
                        val isSoon = homeViewModel.isRenewalSoon(subscription.renewalDate)
                        SubscriptionItem(
                            subscription = subscription,
                            isRenewalSoon = isSoon,
                            onClick = { onSubscriptionClick(subscription.id) }
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun SubscriptionItem(
    subscription: Subscription,
    isRenewalSoon: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = subscription.imageUrl,
                // Content description más específico para la imagen
                contentDescription = stringResource(R.string.subscription_item_logo_description, subscription.name),
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(id = R.drawable.ic_placeholder_image), // Asumo que esto no cambia con el idioma
                error = painterResource(id = R.drawable.ic_placeholder_image) // Asumo que esto no cambia con el idioma
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = subscription.name, // El nombre viene de los datos, no de strings.xml
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    // Usar stringResource para el prefijo "Renews: "
                    text = stringResource(R.string.subscription_item_renews_prefix) + " ${subscription.renewalDate}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isRenewalSoon) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subscription.status.localizedName(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, // Color del texto del estado
                    modifier = Modifier
                        .background(
                            // El color del fondo del estado depende de la lógica, no de strings.xml
                            color = statusColor(subscription.status).copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                // El símbolo de moneda y el costo vienen de los datos
                text = "${subscription.currencySymbol}${String.format("%.2f", subscription.cost)}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

