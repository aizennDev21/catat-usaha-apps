package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.ui.graphics.vector.ImageVector

object CategoryIconHelper {
    fun getIcon(iconName: String?): ImageVector {
        return when (iconName?.lowercase()) {
            "storefront" -> Icons.Default.Storefront
            "shopping_basket" -> Icons.Default.ShoppingBasket
            "shopping_bag" -> Icons.Default.ShoppingBag
            "receipt_long" -> Icons.Default.ReceiptLong
            "groups" -> Icons.Default.Groups
            "local_shipping" -> Icons.Default.LocalShipping
            "add_card" -> Icons.Default.AddCard
            "trending_up" -> Icons.Default.TrendingUp
            "more_horiz" -> Icons.Default.MoreHoriz
            else -> Icons.Default.Category
        }
    }
}
