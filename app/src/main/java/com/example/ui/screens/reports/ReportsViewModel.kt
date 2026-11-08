package com.example.ui.screens.reports

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.dao.CategorySummary
import com.example.data.local.dao.TopProductSummary
import com.example.data.local.entity.TransactionWithItems
import com.example.data.local.entity.UserProfileEntity
import com.example.data.repository.TransactionRepository
import com.example.data.repository.UserRepository
import com.example.util.CsvExporter
import com.example.util.DateUtils
import com.example.util.PdfExporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ReportsUiState(
    val userProfile: UserProfileEntity? = null,
    val selectedPeriod: String = "month", // "today", "7days", "month", "year"
    val periodLabel: String = "Bulan Ini",
    val totalIncome: Long = 0L,
    val totalExpense: Long = 0L,
    val netProfit: Long = 0L,
    val transactionCount: Int = 0,
    val transactions: List<TransactionWithItems> = emptyList(),
    val expenseCategories: List<CategorySummary> = emptyList(),
    val topProducts: List<TopProductSummary> = emptyList()
)

class ReportsViewModel(
    private val transactionRepository: TransactionRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _selectedPeriod = MutableStateFlow("month")
    val selectedPeriod: StateFlow<String> = _selectedPeriod.asStateFlow()

    val uiState: StateFlow<ReportsUiState> = combine(
        userRepository.getUserProfile(),
        _selectedPeriod,
        transactionRepository.getAllTransactions()
    ) { user, period, allTx ->
        val (startTime, endTime, label) = when (period) {
            "today" -> Triple(DateUtils.getStartOfDay(), DateUtils.getEndOfDay(), "Hari Ini")
            "7days" -> Triple(System.currentTimeMillis() - (7 * 86400000L), DateUtils.getEndOfDay(), "7 Hari Terakhir")
            "year" -> Triple(DateUtils.getStartOfYear(), DateUtils.getEndOfDay(), "Tahun Ini")
            else -> Triple(DateUtils.getStartOfMonth(), DateUtils.getEndOfDay(), "Bulan Ini")
        }

        val filtered = allTx.filter { it.transaction.occurredAt in startTime..endTime }
        val income = filtered.filter { it.transaction.type == "income" }.sumOf { it.transaction.totalAmount }
        val expense = filtered.filter { it.transaction.type == "expense" }.sumOf { it.transaction.totalAmount }

        // Aggregate category breakdown
        val expenseMap = mutableMapOf<String, Pair<Long, Int>>()
        filtered.filter { it.transaction.type == "expense" }.forEach { txWithItems ->
            val catName = txWithItems.transaction.categoryId ?: "Belanja Umum"
            val prev = expenseMap[catName] ?: Pair(0L, 0)
            expenseMap[catName] = Pair(prev.first + txWithItems.transaction.totalAmount, prev.second + 1)
        }
        val categorySummaries = expenseMap.map { (catId, pair) ->
            CategorySummary(
                categoryId = catId,
                total = pair.first,
                count = pair.second
            )
        }.sortedByDescending { it.total }

        // Aggregate top sold items
        val productSalesMap = mutableMapOf<String, Pair<Double, Long>>()
        filtered.filter { it.transaction.type == "income" }.forEach { txWithItems ->
            txWithItems.items.forEach { item ->
                val curr = productSalesMap[item.name] ?: Pair(0.0, 0L)
                productSalesMap[item.name] = Pair(curr.first + item.qty, curr.second + item.subtotal)
            }
        }
        val topProducts = productSalesMap.map { (name, stats) ->
            TopProductSummary(
                name = name,
                totalQty = stats.first,
                totalRevenue = stats.second
            )
        }.sortedByDescending { it.totalRevenue }.take(5)

        ReportsUiState(
            userProfile = user,
            selectedPeriod = period,
            periodLabel = label,
            totalIncome = income,
            totalExpense = expense,
            netProfit = income - expense,
            transactionCount = filtered.size,
            transactions = filtered,
            expenseCategories = categorySummaries,
            topProducts = topProducts
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ReportsUiState()
    )

    fun selectPeriod(period: String) {
        _selectedPeriod.value = period
    }

    fun exportPdf(context: Context) {
        val state = uiState.value
        val businessName = state.userProfile?.businessName ?: "Warung Bu Sari"
        val file = PdfExporter.generateAndSharePdf(
            context = context,
            businessName = businessName,
            periodText = state.periodLabel,
            transactions = state.transactions,
            totalIncome = state.totalIncome,
            totalExpense = state.totalExpense,
            detailedPerItem = true
        )
        if (file != null) {
            PdfExporter.shareFile(context, file, "application/pdf")
        }
    }

    fun exportCsv(context: Context) {
        val state = uiState.value
        val businessName = state.userProfile?.businessName ?: "Warung Bu Sari"
        val file = CsvExporter.generateAndShareCsv(
            context = context,
            businessName = businessName,
            periodText = state.periodLabel,
            transactions = state.transactions,
            detailedPerItem = true
        )
        if (file != null) {
            PdfExporter.shareFile(context, file, "text/csv")
        }
    }
}
