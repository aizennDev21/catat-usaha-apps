package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

@Immutable
data class ExtendedColors(
    val income: Color,
    val onIncome: Color,
    val incomeContainer: Color,
    val onIncomeContainer: Color,
    val expense: Color,
    val onExpense: Color,
    val expenseContainer: Color,
    val onExpenseContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val danger: Color,
    val success: Color,
    val info: Color,
    val textSecondary: Color,
    val accent: Color,
    val onAccent: Color,
    val accentContainer: Color,
    val gradient: Brush
)

val LocalExtendedColors = staticCompositionLocalOf {
    ExtendedColors(
        income = IncomeLight,
        onIncome = OnIncomeLight,
        incomeContainer = IncomeContainerLight,
        onIncomeContainer = OnIncomeContainerLight,
        expense = ExpenseLight,
        onExpense = OnExpenseLight,
        expenseContainer = ExpenseContainerLight,
        onExpenseContainer = OnExpenseContainerLight,
        warning = WarningLight,
        warningContainer = WarningContainerLight,
        onWarningContainer = OnWarningContainerLight,
        danger = DangerLight,
        success = SuccessLight,
        info = InfoLight,
        textSecondary = TextSecondaryLight,
        accent = AccentLight,
        onAccent = OnAccentLight,
        accentContainer = AccentContainerLight,
        gradient = GradientLight
    )
}

private val DarkExtendedColors = ExtendedColors(
    income = IncomeDark,
    onIncome = OnIncomeDark,
    incomeContainer = IncomeContainerDark,
    onIncomeContainer = OnIncomeContainerDark,
    expense = ExpenseDark,
    onExpense = OnExpenseDark,
    expenseContainer = ExpenseContainerDark,
    onExpenseContainer = OnExpenseContainerDark,
    warning = WarningDark,
    warningContainer = WarningContainerDark,
    onWarningContainer = OnWarningContainerDark,
    danger = DangerDark,
    success = SuccessDark,
    info = InfoDark,
    textSecondary = TextSecondaryDark,
    accent = AccentDark,
    onAccent = OnAccentDark,
    accentContainer = AccentContainerDark,
    gradient = GradientDark
)

private val LightExtendedColors = ExtendedColors(
    income = IncomeLight,
    onIncome = OnIncomeLight,
    incomeContainer = IncomeContainerLight,
    onIncomeContainer = OnIncomeContainerLight,
    expense = ExpenseLight,
    onExpense = OnExpenseLight,
    expenseContainer = ExpenseContainerLight,
    onExpenseContainer = OnExpenseContainerLight,
    warning = WarningLight,
    warningContainer = WarningContainerLight,
    onWarningContainer = OnWarningContainerLight,
    danger = DangerLight,
    success = SuccessLight,
    info = InfoLight,
    textSecondary = TextSecondaryLight,
    accent = AccentLight,
    onAccent = OnAccentLight,
    accentContainer = AccentContainerLight,
    gradient = GradientLight
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = AccentDark,
    onSecondary = OnAccentDark,
    secondaryContainer = AccentContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = AccentLight,
    onSecondary = OnAccentLight,
    secondaryContainer = AccentContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight
)

val MaterialTheme.extendedColors: ExtendedColors
    @Composable
    get() = LocalExtendedColors.current

@Composable
fun CatatUsahaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
