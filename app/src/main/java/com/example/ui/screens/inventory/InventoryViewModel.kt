package com.example.ui.screens.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.ProductWithMovements
import com.example.data.repository.InventoryRepository
import com.example.data.repository.ShoppingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class InventoryUiState(
    val products: List<ProductEntity> = emptyList(),
    val searchQuery: String = "",
    val filterStatus: String = "all", // "all", "in_stock", "low_stock", "out_of_stock"
    val totalCount: Int = 0,
    val inStockCount: Int = 0,
    val lowStockCount: Int = 0,
    val outOfStockCount: Int = 0
)

class InventoryViewModel(
    private val inventoryRepository: InventoryRepository,
    private val shoppingRepository: ShoppingRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _filterStatus = MutableStateFlow("all")

    val uiState: StateFlow<InventoryUiState> = combine(
        inventoryRepository.getAllProducts(),
        _searchQuery,
        _filterStatus
    ) { allProducts, query, status ->
        val total = allProducts.size
        val outOfStock = allProducts.count { it.stockQty <= 0 }
        val lowStock = allProducts.count { it.stockQty > 0 && it.stockQty <= it.minStock }
        val inStock = allProducts.count { it.stockQty > it.minStock }

        val filtered = allProducts.filter { product ->
            val matchesQuery = query.isBlank() ||
                    product.name.contains(query, ignoreCase = true) ||
                    (product.sku?.contains(query, ignoreCase = true) == true) ||
                    (product.category?.contains(query, ignoreCase = true) == true)

            val matchesStatus = when (status) {
                "in_stock" -> product.stockQty > product.minStock
                "low_stock" -> product.stockQty > 0 && product.stockQty <= product.minStock
                "out_of_stock" -> product.stockQty <= 0
                else -> true
            }

            matchesQuery && matchesStatus
        }

        InventoryUiState(
            products = filtered,
            searchQuery = query,
            filterStatus = status,
            totalCount = total,
            inStockCount = inStock,
            lowStockCount = lowStock,
            outOfStockCount = outOfStock
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = InventoryUiState()
    )

    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setFilterStatus(status: String) { _filterStatus.value = status }

    fun quickAdjustStock(productId: String, delta: Double, type: String) {
        viewModelScope.launch {
            inventoryRepository.adjustStock(productId, delta, type)
        }
    }

    fun performStockOpname(productId: String, physicalStock: Double, note: String) {
        viewModelScope.launch {
            inventoryRepository.adjustStock(
                productId = productId,
                deltaQty = physicalStock,
                type = "adjust",
                referenceType = "opname",
                note = note
            )
        }
    }

    fun markOutOfStock(productId: String) {
        viewModelScope.launch {
            inventoryRepository.markOutOfStock(productId)
        }
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            inventoryRepository.deleteProduct(productId)
        }
    }

    fun saveProduct(
        id: String?,
        name: String,
        sku: String?,
        category: String?,
        unit: String,
        stockQty: Double,
        minStock: Double,
        buyPrice: Long,
        sellPrice: Long,
        note: String?
    ) {
        viewModelScope.launch {
            inventoryRepository.saveProduct(
                id = id ?: java.util.UUID.randomUUID().toString(),
                name = name,
                sku = sku,
                category = category,
                unit = unit,
                stockQty = stockQty,
                minStock = minStock,
                buyPrice = buyPrice,
                sellPrice = sellPrice,
                note = note
            )
        }
    }

    fun addLowStockToShoppingList() {
        viewModelScope.launch {
            shoppingRepository.createListFromLowStock()
        }
    }

    fun getProductWithMovements(productId: String): Flow<ProductWithMovements?> =
        inventoryRepository.getProductWithMovements(productId)
}
