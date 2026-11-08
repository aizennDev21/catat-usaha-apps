package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.ShoppingListWithItems
import com.example.data.local.entity.TransactionWithItems
import com.example.ui.theme.GradientDark
import com.example.ui.theme.GradientLight
import com.example.ui.theme.extendedColors
import com.example.util.DateUtils
import com.example.util.MoneyFormatter

/**
 * Modern Finance Hero Balance Card
 * Super-sized number, animated ticker, smooth gradient underline, and period label.
 */
@Composable
fun HeroBalanceCard(
    title: String = "Total Saldo / Keuntungan",
    amount: Long,
    periodLabel: String = "Bulan Ini",
    growthPercentage: String? = "+8.2%",
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val animatedAmount = remember { Animatable(0f) }

    LaunchedEffect(amount) {
        animatedAmount.animateTo(
            targetValue = amount.toFloat(),
            animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
        )
    }

    val displayValue = animatedAmount.value.toLong()
    val isPositive = amount >= 0

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isDark) 0.dp else 6.dp,
                shape = RoundedCornerShape(22.dp),
                spotColor = Color(0x0F000000),
                ambientColor = Color(0x08000000)
            )
            .testTag("hero_balance_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = if (isDark) androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)) else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = periodLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = MoneyFormatter.formatSigned(displayValue, isPositive),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (!growthPercentage.isNullOrBlank()) {
                    val isGrowthPositive = !growthPercentage.startsWith("-")
                    Surface(
                        color = if (isGrowthPositive) MaterialTheme.extendedColors.incomeContainer else MaterialTheme.extendedColors.expenseContainer,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = if (isGrowthPositive) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                contentDescription = null,
                                tint = if (isGrowthPositive) MaterialTheme.extendedColors.income else MaterialTheme.extendedColors.expense,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = growthPercentage,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isGrowthPositive) MaterialTheme.extendedColors.income else MaterialTheme.extendedColors.expense
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Soft Gradient Accent Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (isDark) GradientDark else GradientLight)
            )
        }
    }
}

/**
 * Overview 2x2 Grid Component as specified in Design 2.0
 */
@Composable
fun OverviewGrid2x2(
    incomeAmount: Long,
    expenseAmount: Long,
    stockValue: Long,
    cashAmount: Long,
    onIncomeClick: () -> Unit,
    onExpenseClick: () -> Unit,
    onStockClick: () -> Unit,
    onCashClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Row 1: Pemasukan & Pengeluaran
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OverviewCardItem(
                title = "Pemasukan",
                amount = MoneyFormatter.format(incomeAmount),
                icon = Icons.Default.ArrowUpward,
                iconTint = MaterialTheme.extendedColors.income,
                iconBg = MaterialTheme.extendedColors.income.copy(alpha = 0.15f),
                trendText = "↗ Masuk",
                trendColor = MaterialTheme.extendedColors.income,
                onClick = onIncomeClick,
                testTag = "overview_income_card",
                modifier = Modifier.weight(1f)
            )

            OverviewCardItem(
                title = "Pengeluaran",
                amount = MoneyFormatter.format(expenseAmount),
                icon = Icons.Default.ArrowDownward,
                iconTint = MaterialTheme.extendedColors.expense,
                iconBg = MaterialTheme.extendedColors.expense.copy(alpha = 0.15f),
                trendText = "↘ Keluar",
                trendColor = MaterialTheme.extendedColors.expense,
                onClick = onExpenseClick,
                testTag = "overview_expense_card",
                modifier = Modifier.weight(1f)
            )
        }

        // Row 2: Nilai Stok & Kas Tunai
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OverviewCardItem(
                title = "Nilai Stok",
                amount = MoneyFormatter.format(stockValue),
                icon = Icons.Default.Inventory2,
                iconTint = MaterialTheme.extendedColors.accent,
                iconBg = MaterialTheme.extendedColors.accent.copy(alpha = 0.15f),
                trendText = "📦 Gudang",
                trendColor = MaterialTheme.extendedColors.accent,
                onClick = onStockClick,
                testTag = "overview_stock_card",
                modifier = Modifier.weight(1f)
            )

            OverviewCardItem(
                title = "Kas Tunai",
                amount = MoneyFormatter.format(cashAmount),
                icon = Icons.Default.AccountBalanceWallet,
                iconTint = MaterialTheme.extendedColors.info,
                iconBg = MaterialTheme.extendedColors.info.copy(alpha = 0.15f),
                trendText = "💳 Saldo",
                trendColor = MaterialTheme.extendedColors.info,
                onClick = onCashClick,
                testTag = "overview_cash_card",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun OverviewCardItem(
    title: String,
    amount: String,
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    trendText: String,
    trendColor: Color,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    Card(
        modifier = modifier
            .shadow(
                elevation = if (isDark) 0.dp else 4.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = Color(0x0A000000),
                ambientColor = Color(0x05000000)
            )
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .testTag(testTag),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = if (isDark) androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)) else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(iconBg, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    text = trendText,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = trendColor
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = amount,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.2).sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Modern Finance AI Business Coach Card
 */
@Composable
fun AiBusinessCoachCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isDark) 0.dp else 4.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = Color(0x0F6366F1),
                ambientColor = Color(0x056366F1)
            )
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .testTag("ai_coach_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.extendedColors.accentContainer.copy(alpha = 0.45f)
        ),
        border = if (isDark) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.extendedColors.accent.copy(alpha = 0.2f)) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(MaterialTheme.extendedColors.accent.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.extendedColors.accent,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column {
                    Text(
                        text = "Asisten Usaha ✨",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Tips hemat, evaluasi laba & belanja pintar",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.extendedColors.accent,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * Modern Transaction Card with Soft UI & tinted icon
 */
@Composable
fun TransactionCard(
    txWithItems: TransactionWithItems,
    categoryName: String?,
    categoryIcon: String?,
    categoryColor: String?,
    onClick: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val tx = txWithItems.transaction
    val isIncome = tx.type == "income"
    var showMenu by remember { mutableStateOf(false) }

    val defaultBg = if (isIncome) MaterialTheme.extendedColors.income.copy(alpha = 0.15f) else MaterialTheme.extendedColors.expense.copy(alpha = 0.15f)
    val defaultTint = if (isIncome) MaterialTheme.extendedColors.income else MaterialTheme.extendedColors.expense

    val iconBgColor = remember(categoryColor, defaultBg) {
        if (!categoryColor.isNullOrBlank()) {
            try {
                Color(android.graphics.Color.parseColor(categoryColor)).copy(alpha = 0.15f)
            } catch (e: Exception) {
                defaultBg
            }
        } else {
            defaultBg
        }
    }

    val iconTint = remember(categoryColor, defaultTint) {
        if (!categoryColor.isNullOrBlank()) {
            try {
                Color(android.graphics.Color.parseColor(categoryColor))
            } catch (e: Exception) {
                defaultTint
            }
        } else {
            defaultTint
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isDark) 0.dp else 3.dp,
                shape = RoundedCornerShape(18.dp),
                spotColor = Color(0x08000000),
                ambientColor = Color(0x04000000)
            )
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .testTag("transaction_card_${tx.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = if (isDark) androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon Circle (15% opacity background)
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(iconBgColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = CategoryIconHelper.getIcon(categoryIcon),
                    contentDescription = categoryName ?: "Kategori",
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Center Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                val title = tx.counterparty ?: if (txWithItems.items.isNotEmpty()) {
                    txWithItems.items.joinToString(", ") { it.name }.take(30)
                } else {
                    categoryName ?: (if (isIncome) "Penjualan" else "Belanja")
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = categoryName ?: (if (isIncome) "Penjualan" else "Pengeluaran"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = DateUtils.formatTime(tx.occurredAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right Info: Signed Amount
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = MoneyFormatter.formatSigned(tx.totalAmount, isIncome),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.sp
                    ),
                    color = if (isIncome) MaterialTheme.extendedColors.income else MaterialTheme.extendedColors.expense
                )

                if (txWithItems.items.isNotEmpty()) {
                    Text(
                        text = "${txWithItems.items.size} item",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (onEdit != null || onDelete != null) {
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Opsi",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        if (onEdit != null) {
                            DropdownMenuItem(
                                text = { Text("Edit Catatan") },
                                onClick = {
                                    showMenu = false
                                    onEdit()
                                }
                            )
                        }
                        if (onDelete != null) {
                            DropdownMenuItem(
                                text = { Text("Hapus", color = MaterialTheme.extendedColors.danger) },
                                onClick = {
                                    showMenu = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Product Card for Stock Management
 */
@Composable
fun ProductCard(
    product: ProductEntity,
    onClick: () -> Unit,
    onQuickAdjustStock: (Double, String) -> Unit,
    onAddToShoppingList: () -> Unit,
    onMenuAction: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isDark) 0.dp else 3.dp,
                shape = RoundedCornerShape(18.dp),
                spotColor = Color(0x08000000),
                ambientColor = Color(0x04000000)
            )
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .testTag("product_card_${product.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = if (isDark) androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)) else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (!product.sku.isNullOrBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = product.sku,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        if (!product.category.isNullOrBlank()) {
                            Text(
                                text = product.category,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                StockBadge(stockQty = product.stockQty, minStock = product.minStock)

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Opsi Produk",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Stok Opname (Penyesuaian Fisik)") },
                            onClick = {
                                showMenu = false
                                onMenuAction("opname")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Tandai Habis") },
                            onClick = {
                                showMenu = false
                                onMenuAction("out_of_stock")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Tambah ke Daftar Belanja") },
                            onClick = {
                                showMenu = false
                                onAddToShoppingList()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Edit Produk") },
                            onClick = {
                                showMenu = false
                                onMenuAction("edit")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Hapus Produk", color = MaterialTheme.extendedColors.danger) },
                            onClick = {
                                showMenu = false
                                onMenuAction("delete")
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Stock and Price Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    val stockText = if (product.stockQty % 1.0 == 0.0) "${product.stockQty.toLong()}" else "%.1f".format(product.stockQty)
                    val minText = if (product.minStock % 1.0 == 0.0) "${product.minStock.toLong()}" else "%.1f".format(product.minStock)
                    Text(
                        text = "Stok: $stockText ${product.unit} (min $minText)",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Jual: ${MoneyFormatter.format(product.sellPrice)} • Beli: ${MoneyFormatter.format(product.buyPrice)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Quick Stock buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (product.stockQty <= product.minStock) {
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onAddToShoppingList() },
                            color = MaterialTheme.extendedColors.warningContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddShoppingCart,
                                    contentDescription = "Belanja",
                                    tint = MaterialTheme.extendedColors.warning,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Belanja",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.extendedColors.warning
                                )
                            }
                        }
                    }

                    OutlinedIconButton(
                        onClick = { onQuickAdjustStock(1.0, "out") },
                        modifier = Modifier.size(36.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Kurangi 1",
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    OutlinedIconButton(
                        onClick = { onQuickAdjustStock(1.0, "in") },
                        modifier = Modifier.size(36.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Tambah 1",
                            tint = MaterialTheme.extendedColors.income,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Shopping List Card
 */
@Composable
fun ShoppingListCard(
    listWithItems: ShoppingListWithItems,
    onClick: () -> Unit,
    onConvertToExpense: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val list = listWithItems.shoppingList
    val items = listWithItems.items
    val checkedCount = items.count { it.isChecked }
    val totalEstimated = items.sumOf { (it.qty * it.estimatedPrice).toLong() }
    val isAllDone = items.isNotEmpty() && checkedCount == items.size

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isDark) 0.dp else 3.dp,
                shape = RoundedCornerShape(18.dp),
                spotColor = Color(0x08000000),
                ambientColor = Color(0x04000000)
            )
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .testTag("shopping_list_card_${list.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = if (isDark) androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)) else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = list.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (list.plannedDate != null) {
                        Text(
                            text = "Rencana: ${DateUtils.formatFullDate(list.plannedDate)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    color = if (isAllDone || list.status == "done") MaterialTheme.extendedColors.incomeContainer else MaterialTheme.extendedColors.accentContainer,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = if (isAllDone || list.status == "done") "✓ Selesai" else "$checkedCount / ${items.size} Barang",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (isAllDone || list.status == "done") MaterialTheme.extendedColors.income else MaterialTheme.extendedColors.accent,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (items.isNotEmpty()) {
                val progress = if (items.isNotEmpty()) checkedCount.toFloat() / items.size.toFloat() else 0f
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.extendedColors.accent,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Estimasi Total",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = MoneyFormatter.format(totalEstimated),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (checkedCount > 0 && list.status != "done") {
                        Button(
                            onClick = onConvertToExpense,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.extendedColors.accent
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Jadikan Pengeluaran",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
        }
    }
}
