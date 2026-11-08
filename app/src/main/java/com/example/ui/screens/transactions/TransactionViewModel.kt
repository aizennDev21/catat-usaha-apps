package com.example.ui.screens.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.TransactionItemEntity
import com.example.data.local.entity.TransactionWithItems
import com.example.data.local.entity.UserProfileEntity
import com.example.data.repository.InventoryRepository
import com.example.data.repository.TransactionRepository
import com.example.data.repository.UserRepository
import com.example.util.DateUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class TransactionListUiState(
    val transactions: List<TransactionWithItems> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val filterType: String = "all", // "all", "income", "expense"
    val filterPeriod: String = "all", // "all", "today", "7days", "month"
    val filterCategoryId: String? = null,
    val searchQuery: String = "",
    val totalIncome: Long = 0L,
    val totalExpense: Long = 0L,
    val lastDeletedId: String? = null
)

private data class FilterParams(
    val type: String,
    val period: String,
    val categoryId: String?,
    val query: String
)

class TransactionViewModel(
    private val transactionRepository: TransactionRepository,
    private val inventoryRepository: InventoryRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _filterType = MutableStateFlow("all")
    private val _filterPeriod = MutableStateFlow("all")
    private val _filterCategoryId = MutableStateFlow<String?>(null)
    private val _searchQuery = MutableStateFlow("")

    private val _recentlyDeletedId = MutableStateFlow<String?>(null)

    private val filters = combine(
        _filterType,
        _filterPeriod,
        _filterCategoryId,
        _searchQuery
    ) { fType, fPeriod, fCat, query ->
        FilterParams(fType, fPeriod, fCat, query)
    }

    val listUiState: StateFlow<TransactionListUiState> = combine(
        transactionRepository.getAllTransactions(),
        transactionRepository.getAllCategories(),
        filters
    ) { allTx, categories, filter ->
        val (startTime, endTime) = when (filter.period) {
            "today" -> Pair(DateUtils.getStartOfDay(), DateUtils.getEndOfDay())
            "7days" -> Pair(System.currentTimeMillis() - (7 * 86400000L), DateUtils.getEndOfDay())
            "month" -> Pair(DateUtils.getStartOfMonth(), DateUtils.getEndOfDay())
            else -> Pair(0L, Long.MAX_VALUE)
        }

        val filtered = allTx.filter { txWithItems ->
            val tx = txWithItems.transaction
            val matchesType = filter.type == "all" || tx.type == filter.type
            val matchesPeriod = tx.occurredAt in startTime..endTime
            val matchesCat = filter.categoryId == null || tx.categoryId == filter.categoryId
            val matchesQuery = filter.query.isBlank() ||
                    (tx.counterparty?.contains(filter.query, ignoreCase = true) == true) ||
                    (tx.note?.contains(filter.query, ignoreCase = true) == true) ||
                    txWithItems.items.any { it.name.contains(filter.query, ignoreCase = true) }

            matchesType && matchesPeriod && matchesCat && matchesQuery
        }

        val income = filtered.filter { it.transaction.type == "income" }.sumOf { it.transaction.totalAmount }
        val expense = filtered.filter { it.transaction.type == "expense" }.sumOf { it.transaction.totalAmount }

        TransactionListUiState(
            transactions = filtered,
            categories = categories,
            filterType = filter.type,
            filterPeriod = filter.period,
            filterCategoryId = filter.categoryId,
            searchQuery = filter.query,
            totalIncome = income,
            totalExpense = expense,
            lastDeletedId = _recentlyDeletedId.value
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TransactionListUiState()
    )

    fun setFilterType(type: String) { _filterType.value = type }
    fun setFilterPeriod(period: String) { _filterPeriod.value = period }
    fun setFilterCategory(catId: String?) { _filterCategoryId.value = catId }
    fun setSearchQuery(query: String) { _searchQuery.value = query }

    fun getCategoriesByType(type: String): Flow<List<CategoryEntity>> =
        transactionRepository.getCategoriesByType(type)

    fun getAllProducts(): Flow<List<ProductEntity>> =
        inventoryRepository.getAllProducts()

    fun getUserProfile(): Flow<UserProfileEntity?> =
        userRepository.getUserProfile()

    suspend fun getTransactionById(id: String): TransactionWithItems? =
        transactionRepository.getTransactionById(id)

    fun saveTransaction(
        id: String?,
        type: String,
        categoryId: String?,
        occurredAt: Long,
        counterparty: String?,
        paymentMethod: String,
        note: String?,
        items: List<TransactionItemEntity>,
        autoUpdateStock: Boolean
    ) {
        viewModelScope.launch {
            val txId = id ?: UUID.randomUUID().toString()
            transactionRepository.saveTransaction(
                id = txId,
                type = type,
                categoryId = categoryId,
                occurredAt = occurredAt,
                counterparty = counterparty,
                paymentMethod = paymentMethod,
                note = note,
                items = items,
                autoUpdateStock = autoUpdateStock
            )
        }
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch {
            _recentlyDeletedId.value = id
            transactionRepository.deleteTransaction(id)
        }
    }

    fun restoreLastDeleted() {
        val id = _recentlyDeletedId.value ?: return
        viewModelScope.launch {
            transactionRepository.restoreTransaction(id)
            _recentlyDeletedId.value = null
        }
    }
}
