package com.example.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.home.HomeViewModel
import com.example.ui.screens.inventory.InventoryScreen
import com.example.ui.screens.inventory.InventoryViewModel
import com.example.ui.screens.inventory.ProductDetailScreen
import com.example.ui.screens.profile.ProfileScreen
import com.example.ui.screens.profile.ProfileViewModel
import com.example.ui.screens.reports.ReportsScreen
import com.example.ui.screens.reports.ReportsViewModel
import com.example.ui.screens.shopping.ShoppingDetailScreen
import com.example.ui.screens.shopping.ShoppingListScreen
import com.example.ui.screens.shopping.ShoppingViewModel
import com.example.ui.screens.transactions.TransactionDetailScreen
import com.example.ui.screens.transactions.TransactionFormScreen
import com.example.ui.screens.transactions.TransactionListScreen
import com.example.ui.screens.transactions.TransactionViewModel
import com.example.ui.theme.extendedColors
import kotlinx.coroutines.launch

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Home : Screen("home", "Beranda", Icons.Default.Home)
    data object Transactions : Screen("transactions", "Catatan", Icons.Default.ReceiptLong)
    data object Inventory : Screen("inventory", "Stok", Icons.Default.Inventory2)
    data object Reports : Screen("reports", "Analitik", Icons.AutoMirrored.Filled.TrendingUp)
    data object Shopping : Screen("shopping", "Belanja", Icons.Default.ShoppingCart)
    data object Profile : Screen("profile", "Profil", Icons.Default.Person)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    homeViewModel: HomeViewModel,
    transactionViewModel: TransactionViewModel,
    inventoryViewModel: InventoryViewModel,
    shoppingViewModel: ShoppingViewModel,
    reportsViewModel: ReportsViewModel,
    profileViewModel: ProfileViewModel,
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isDark = isSystemInDarkTheme()

    var showQuickActionSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    val bottomNavItems = listOf(
        Screen.Home,
        Screen.Transactions,
        Screen.Inventory,
        Screen.Reports,
        Screen.Profile
    )

    val isTopLevelDestination = bottomNavItems.any { it.route == currentRoute }

    if (showQuickActionSheet) {
        ModalBottomSheet(
            onDismissRequest = { showQuickActionSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Pilih Aksi Cepat",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Catat transaksi kas atau rencanakan belanja dagangan",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Income Option
                QuickActionItem(
                    title = "Catat Pemasukan (Penjualan)",
                    subtitle = "Pelanggan bayar tunai / transfer / QRIS",
                    icon = Icons.Default.ArrowUpward,
                    iconBgColor = MaterialTheme.extendedColors.income.copy(alpha = 0.15f),
                    iconTint = MaterialTheme.extendedColors.income,
                    onClick = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            showQuickActionSheet = false
                            navController.navigate("transaction_form?type=income")
                        }
                    }
                )

                // Expense Option
                QuickActionItem(
                    title = "Catat Pengeluaran (Belanja)",
                    subtitle = "Belanja barang dagangan, operasional, ongkos",
                    icon = Icons.Default.ArrowDownward,
                    iconBgColor = MaterialTheme.extendedColors.expense.copy(alpha = 0.15f),
                    iconTint = MaterialTheme.extendedColors.expense,
                    onClick = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            showQuickActionSheet = false
                            navController.navigate("transaction_form?type=expense")
                        }
                    }
                )

                // Shopping List Option
                QuickActionItem(
                    title = "Buat Daftar Belanja Pasar",
                    subtitle = "Checklist barang yang harus dibeli ke supplier",
                    icon = Icons.Default.ShoppingCart,
                    iconBgColor = MaterialTheme.extendedColors.accent.copy(alpha = 0.15f),
                    iconTint = MaterialTheme.extendedColors.accent,
                    onClick = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            showQuickActionSheet = false
                            navController.navigate(Screen.Shopping.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    Scaffold(
        bottomBar = {
            if (isTopLevelDestination) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    bottomNavItems.forEach { screen ->
                        val isSelected = currentRoute == screen.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = screen.title
                                )
                            },
                            label = {
                                Text(
                                    text = screen.title,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.extendedColors.accent,
                                selectedTextColor = MaterialTheme.extendedColors.accent,
                                indicatorColor = MaterialTheme.extendedColors.accentContainer.copy(alpha = 0.6f),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.testTag("nav_item_${screen.route}")
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (isTopLevelDestination) {
                FloatingActionButton(
                    onClick = { showQuickActionSheet = true },
                    containerColor = MaterialTheme.extendedColors.accent,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .shadow(6.dp, CircleShape, spotColor = MaterialTheme.extendedColors.accent)
                        .testTag("quick_action_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Aksi Cepat",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToTransactions = { navController.navigate(Screen.Transactions.route) },
                    onNavigateToTransactionDetail = { id -> navController.navigate("transaction_detail/$id") },
                    onNavigateToEditTransaction = { id -> navController.navigate("transaction_form?type=income&editId=$id") },
                    onNavigateToReports = { navController.navigate(Screen.Reports.route) },
                    onNavigateToInventory = { navController.navigate(Screen.Inventory.route) },
                    onNavigateToShopping = { navController.navigate(Screen.Shopping.route) },
                    onNavigateToProfile = { navController.navigate(Screen.Profile.route) }
                )
            }

            composable(Screen.Transactions.route) {
                TransactionListScreen(
                    viewModel = transactionViewModel,
                    onNavigateToDetail = { id -> navController.navigate("transaction_detail/$id") },
                    onNavigateToEdit = { id -> navController.navigate("transaction_form?type=income&editId=$id") },
                    onNavigateToExport = { navController.navigate(Screen.Reports.route) },
                    onAddNewTransaction = { type -> navController.navigate("transaction_form?type=$type") }
                )
            }

            composable(
                route = "transaction_form?type={type}&editId={editId}",
                arguments = listOf(
                    navArgument("type") {
                        type = NavType.StringType
                        defaultValue = "income"
                    },
                    navArgument("editId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val type = backStackEntry.arguments?.getString("type") ?: "income"
                val editId = backStackEntry.arguments?.getString("editId")
                TransactionFormScreen(
                    initialType = type,
                    editTransactionId = editId,
                    viewModel = transactionViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = "transaction_detail/{id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("id") ?: ""
                TransactionDetailScreen(
                    transactionId = id,
                    viewModel = transactionViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEdit = { editId -> navController.navigate("transaction_form?type=income&editId=$editId") }
                )
            }

            composable(Screen.Inventory.route) {
                InventoryScreen(
                    viewModel = inventoryViewModel,
                    onNavigateToProductDetail = { id -> navController.navigate("product_detail/$id") },
                    onNavigateToShopping = { navController.navigate(Screen.Shopping.route) }
                )
            }

            composable(
                route = "product_detail/{id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("id") ?: ""
                ProductDetailScreen(
                    productId = id,
                    viewModel = inventoryViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Shopping.route) {
                ShoppingListScreen(
                    viewModel = shoppingViewModel,
                    onNavigateToDetail = { id -> navController.navigate("shopping_detail/$id") }
                )
            }

            composable(
                route = "shopping_detail/{id}",
                arguments = listOf(navArgument("id") { type = NavType.StringType })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("id") ?: ""
                ShoppingDetailScreen(
                    listId = id,
                    viewModel = shoppingViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToTransactions = { navController.navigate(Screen.Transactions.route) }
                )
            }

            composable(Screen.Reports.route) {
                ReportsScreen(
                    viewModel = reportsViewModel
                )
            }

            composable(Screen.Profile.route) {
                ProfileScreen(
                    viewModel = profileViewModel
                )
            }
        }
    }
}

@Composable
private fun QuickActionItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconBgColor: Color,
    iconTint: Color,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = if (isDark) androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(iconBgColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
