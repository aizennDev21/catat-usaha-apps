package com.example.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.TransactionWithItems
import com.example.data.local.entity.UserProfileEntity
import com.example.data.repository.InventoryRepository
import com.example.data.repository.ShoppingRepository
import com.example.data.repository.SyncRepository
import com.example.data.repository.SyncState
import com.example.data.repository.TransactionRepository
import com.example.data.repository.UserRepository
import com.example.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val userProfile: UserProfileEntity? = null,
    val selectedPeriod: String = "month", // "today", "7days", "month"
    val totalIncome: Long = 0L,
    val totalExpense: Long = 0L,
    val netProfit: Long = 0L,
    val totalCumulativeBalance: Long = 0L,
    val totalStockValue: Long = 0L,
    val totalProductCount: Int = 0,
    val transactionCount: Int = 0,
    val recentTransactions: List<TransactionWithItems> = emptyList(),
    val lowStockProducts: List<ProductEntity> = emptyList(),
    val syncState: SyncState = SyncState.Synced,
    val pendingSyncCount: Int = 0,
    val lastDeletedTransactionId: String? = null
)

class HomeViewModel(
    private val transactionRepository: TransactionRepository,
    private val inventoryRepository: InventoryRepository,
    private val shoppingRepository: ShoppingRepository,
    private val userRepository: UserRepository,
    private val syncRepository: SyncRepository
) : ViewModel() {

    private val _selectedPeriod = MutableStateFlow("month")
    val selectedPeriod: StateFlow<String> = _selectedPeriod.asStateFlow()

    private val _deletedTxId = MutableStateFlow<String?>(null)

    private val transactionsAndPeriod = combine(
        transactionRepository.getAllTransactions(),
        _selectedPeriod
    ) { txs, period -> Pair(txs, period) }

    private val syncInfo = combine(
        syncRepository.syncState,
        syncRepository.getPendingCount()
    ) { state, count -> Pair(state, count) }

    val uiState: StateFlow<HomeUiState> = combine(
        userRepository.getUserProfile(),
        transactionsAndPeriod,
        inventoryRepository.getAllProducts(),
        inventoryRepository.getLowStockProducts(),
        syncInfo
    ) { user, (allTransactions, period), allProducts, lowStock, (syncState, pendingCount) ->
        val (startTime, endTime) = when (period) {
            "today" -> Pair(DateUtils.getStartOfDay(), DateUtils.getEndOfDay())
            "7days" -> Pair(System.currentTimeMillis() - (7 * 86400000L), DateUtils.getEndOfDay())
            else -> Pair(DateUtils.getStartOfMonth(), DateUtils.getEndOfDay())
        }

        val filteredTransactions = allTransactions.filter {
            it.transaction.occurredAt in startTime..endTime
        }

        val income = filteredTransactions.filter { it.transaction.type == "income" }
            .sumOf { it.transaction.totalAmount }
        val expense = filteredTransactions.filter { it.transaction.type == "expense" }
            .sumOf { it.transaction.totalAmount }

        val allTimeIncome = allTransactions.filter { it.transaction.type == "income" }
            .sumOf { it.transaction.totalAmount }
        val allTimeExpense = allTransactions.filter { it.transaction.type == "expense" }
            .sumOf { it.transaction.totalAmount }
        val cumulativeCash = allTimeIncome - allTimeExpense

        val stockValue = allProducts.sumOf { (it.stockQty * it.buyPrice).toLong() }

        HomeUiState(
            userProfile = user,
            selectedPeriod = period,
            totalIncome = income,
            totalExpense = expense,
            netProfit = income - expense,
            totalCumulativeBalance = cumulativeCash,
            totalStockValue = stockValue,
            totalProductCount = allProducts.size,
            transactionCount = filteredTransactions.size,
            recentTransactions = allTransactions.take(8),
            lowStockProducts = lowStock,
            syncState = syncState,
            pendingSyncCount = pendingCount,
            lastDeletedTransactionId = _deletedTxId.value
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    fun selectPeriod(period: String) {
        _selectedPeriod.value = period
    }

    fun triggerSync() {
        viewModelScope.launch {
            syncRepository.performSync()
        }
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch {
            _deletedTxId.value = id
            transactionRepository.deleteTransaction(id)
        }
    }

    fun restoreLastDeleted() {
        val id = _deletedTxId.value ?: return
        viewModelScope.launch {
            transactionRepository.restoreTransaction(id)
            _deletedTxId.value = null
        }
    }

    fun addLowStockToShoppingList() {
        viewModelScope.launch {
            shoppingRepository.createListFromLowStock("Belanja Restok Barang Menipis")
        }
    }
}
