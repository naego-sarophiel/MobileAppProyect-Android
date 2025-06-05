package com.example.mobileappproyect_android.ui.home

// Mantén tus importaciones actuales, solo asegúrate de estas:
import androidx.activity.result.launch
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
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
import kotlinx.coroutines.launch
import kotlin.text.isNotEmpty
import kotlin.text.lowercase
import kotlin.text.replace
import kotlin.text.substring
import kotlin.text.uppercase


// Data class for Navigation Drawer Items
data class NavItem(
    val label: String,
    val icon: ImageVector,
    val route: String // Para navigation si usas NavController for drawer items
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel = viewModel(),
    onSubscriptionClick: (String) -> Unit, // Callback for item click, passes ID
    onAddSubscriptionClick: () -> Unit,      // Callback for FAB click
    navController: NavController
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val subscriptionsList by homeViewModel.subscriptions.collectAsStateWithLifecycle()
    val currentSortCriteria by homeViewModel.sortCriteria.collectAsStateWithLifecycle()
    val navItems = listOf(
        NavItem("Home", Icons.Filled.Home, "home_screen"), // Current screen
        NavItem("Add Subscription", Icons.Filled.AddCircle, "add_subscription"),
        NavItem("Settings", Icons.Filled.Settings, "settings")
    )
    var selectedItemIndex by remember { mutableStateOf(0) } // To highlight selected item
    var sortMenuExpanded by remember { mutableStateOf(false) }
    val sortOptions = listOf( // Opciones para el menú desplegable
        SortCriteria.RENEWAL_DATE_ASC to "Sort by Renewal (Soonest)", // TODO: Usar stringResource
        SortCriteria.PRICE_ASC to "Sort by Price (Low to High)",
        SortCriteria.PRICE_DESC to "Sort by Price (High to Low)",
        SortCriteria.NAME_ASC to "Sort by Name (A-Z)"
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(16.dp))
                Text(
                    "Menu",
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
                    title = { Text("My Subscriptions") },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch {
                                if (drawerState.isClosed) drawerState.open() else drawerState.close()
                            }
                        }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Open Drawer")
                        }
                    },
                    actions = { // --- AÑADIR ACCIÓN DE ORDENACIÓN A LA TOPAPPBAR ---
                        ExposedDropdownMenuBox(
                            expanded = sortMenuExpanded,
                            onExpandedChange = { sortMenuExpanded = !sortMenuExpanded }
                        ) {
                            IconButton(
                                onClick = { sortMenuExpanded = true },
                                modifier = Modifier.menuAnchor() // Anclar el menú al IconButton
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sort,
                                    contentDescription = "Sort Subscriptions" // TODO: Usar stringResource
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
                                                modifier = Modifier.fillMaxWidth(), // Mantenlo
                                                textAlign = TextAlign.Start
                                            )
                                        },
                                        onClick = {
                                            homeViewModel.setSortCriteria(criteria)
                                            sortMenuExpanded = false
                                        },
                                        leadingIcon = if (currentSortCriteria == criteria) {
                                            { Icon(Icons.Filled.Check, contentDescription = "Selected") }
                                        } else null,
                                        // modifier = Modifier.fillMaxWidth(), // Este en el DropdownMenuItem en sí podría no ser necesario si el Text interno ya se expande
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp) // Ajusta este padding
                                        // Prueba incluso con PaddingValues(0.dp) temporalmente para diagnosticar
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
                    Icon(Icons.Filled.Add, contentDescription = "Add Subscription")
                }
            }
        ) { paddingValues ->
            // --- USA subscriptionsList AQUÍ ---
            if (subscriptionsList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No subscriptions added yet. Tap '+' to add one!")
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp) // Añade espacio entre items
                ) {
                    items(subscriptionsList, key = { it.id }) { subscription -> // 'subscription' es cada objeto
                        val isSoon = homeViewModel.isRenewalSoon(subscription.renewalDate)
                        // LLAMADA A LA ÚNICA Y CORRECTA SubscriptionItem
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


// Composable para cada ítem de suscripción (ejemplo básico, necesita estar definido)
// Asumo que tienes un Composable SubscriptionItem.kt, si no, aquí hay una estructura.
// ASEGÚRATE DE QUE ESTE SubscriptionItem ESTÉ DEFINIDO EN ALGÚN LUGAR.
// Lo incluyo aquí para completitud, basado en mi respuesta anterior.
@Composable
fun SubscriptionItem(
    subscription: Subscription, // Asegúrate que este 'Subscription' es tu data class correcta
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
                contentDescription = "${subscription.name} logo",
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape), // O RoundedCornerShape(8.dp)
                contentScale = ContentScale.Crop,
                placeholder = painterResource(id = R.drawable.ic_placeholder_image),
                error = painterResource(id = R.drawable.ic_placeholder_image)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = subscription.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Renews: ${subscription.renewalDate}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isRenewalSoon) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = run {
                        val lowerCasedStatus = subscription.status.name.replace("_", " ").lowercase()
                        if (lowerCasedStatus.isNotEmpty()) {
                            lowerCasedStatus.substring(0, 1).uppercase(java.util.Locale.getDefault()) + lowerCasedStatus.substring(1)
                        } else {
                            lowerCasedStatus
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .background(
                            color = statusColor(subscription.status).copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "${subscription.currencySymbol}${String.format("%.2f", subscription.cost)}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}


