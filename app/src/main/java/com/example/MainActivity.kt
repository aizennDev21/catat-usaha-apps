package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AppDatabase
import com.example.data.repository.InventoryRepository
import com.example.data.repository.ShoppingRepository
import com.example.data.repository.SyncRepository
import com.example.data.repository.TransactionRepository
import com.example.data.repository.UserRepository
import com.example.ui.navigation.AppNavigation
import com.example.ui.screens.auth.AuthScreen
import com.example.ui.screens.auth.AuthViewModel
import com.example.ui.screens.home.HomeViewModel
import com.example.ui.screens.inventory.InventoryViewModel
import com.example.ui.screens.profile.ProfileViewModel
import com.example.ui.screens.reports.ReportsViewModel
import com.example.ui.screens.shopping.ShoppingViewModel
import com.example.ui.screens.transactions.TransactionViewModel
import com.example.ui.theme.CatatUsahaTheme
import com.example.ui.theme.extendedColors

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = AppDatabase.getInstance(applicationContext)

        val outboxDao = db.outboxDao()
        val userDao = db.userDao()
        val productDao = db.productDao()
        val categoryDao = db.categoryDao()
        val transactionDao = db.transactionDao()
        val shoppingDao = db.shoppingDao()

        val userRepository = UserRepository(
            userDao = userDao,
            outboxDao = outboxDao,
            categoryDao = categoryDao,
            clearLocalData = { withContext(Dispatchers.IO) { db.clearAllTables() } }
        )
        val transactionRepository = TransactionRepository(transactionDao, productDao, categoryDao, userDao, outboxDao)
        val inventoryRepository = InventoryRepository(productDao, userDao, outboxDao)
        val shoppingRepository = ShoppingRepository(shoppingDao, productDao, userDao, outboxDao, transactionRepository)
        val syncRepository = SyncRepository(outboxDao, categoryDao, transactionDao, productDao, userDao)

        val homeViewModel = HomeViewModel(transactionRepository, inventoryRepository, shoppingRepository, userRepository, syncRepository)
        val transactionViewModel = TransactionViewModel(transactionRepository, inventoryRepository, userRepository)
        val inventoryViewModel = InventoryViewModel(inventoryRepository, shoppingRepository)
        val shoppingViewModel = ShoppingViewModel(shoppingRepository, transactionRepository, inventoryRepository)
        val reportsViewModel = ReportsViewModel(transactionRepository, userRepository)
        val profileViewModel = ProfileViewModel(userRepository, syncRepository)
        val authViewModel = AuthViewModel(userRepository, syncRepository)

        setContent {
            val userProfile by userRepository.getUserProfile().collectAsState(initial = null)
            val isDark = when (userProfile?.themeMode) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }

            val isUserLoggedIn = userProfile != null && userProfile?.isLoggedIn == true
            var isUnlocked by remember { mutableStateOf(userProfile?.pinLock.isNullOrBlank()) }

            CatatUsahaTheme(darkTheme = isDark) {
                if (!isUserLoggedIn) {
                    AuthScreen(viewModel = authViewModel)
                } else if (userProfile?.pinLock != null && !isUnlocked) {
                    PinLockScreen(
                        correctPin = userProfile!!.pinLock!!,
                        businessName = userProfile?.businessName ?: "Warung Bu Sari",
                        onUnlocked = { isUnlocked = true }
                    )
                } else {
                    AppNavigation(
                        homeViewModel = homeViewModel,
                        transactionViewModel = transactionViewModel,
                        inventoryViewModel = inventoryViewModel,
                        shoppingViewModel = shoppingViewModel,
                        reportsViewModel = reportsViewModel,
                        profileViewModel = profileViewModel
                    )
                }
            }
        }
    }
}

@Composable
fun PinLockScreen(
    correctPin: String,
    businessName: String,
    onUnlocked: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    fun onNumberPress(num: String) {
        if (enteredPin.length < 4) {
            val newPin = enteredPin + num
            enteredPin = newPin
            isError = false
            if (newPin.length == 4) {
                if (newPin == correctPin) {
                    onUnlocked()
                } else {
                    isError = true
                    enteredPin = ""
                }
            }
        }
    }

    fun onBackspace() {
        if (enteredPin.isNotEmpty()) {
            enteredPin = enteredPin.dropLast(1)
            isError = false
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = businessName,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = if (isError) "PIN Salah, coba lagi" else "Masukkan 4 angka PIN untuk membuka",
                style = MaterialTheme.typography.bodyMedium,
                color = if (isError) MaterialTheme.extendedColors.danger else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // PIN Dots Indicator
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                repeat(4) { idx ->
                    val isFilled = idx < enteredPin.length
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .background(
                                if (isFilled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                CircleShape
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Numeric Keypad
            val keys = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("", "0", "DEL")
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                keys.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        row.forEach { key ->
                            if (key.isBlank()) {
                                Spacer(modifier = Modifier.size(64.dp))
                            } else if (key == "DEL") {
                                Surface(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .clickable { onBackspace() },
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = CircleShape
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Backspace,
                                            contentDescription = "Hapus",
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            } else {
                                Surface(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .clickable { onNumberPress(key) },
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = CircleShape
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = key,
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 22.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
