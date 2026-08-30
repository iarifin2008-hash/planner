package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wallet_items")
data class WalletItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,              // e.g., "Uang Cash", "Saldo DANA", "Saldo Rekening", "GoPay", "ShopeePay"
    val type: String = "CASH",     // "CASH", "E_WALLET", "BANK", "OTHER"
    val balance: Double = 0.0,     // Total saldo sekarang (bisa diisi manual atau terpotong transaksi)
    val colorHex: String = "#6599B8",
    val iconName: String = "cash", // "cash", "dana", "bank", "wallet", "card"
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_accounts")
data class UserAccount(
    @PrimaryKey val id: Int = 1,
    val email: String = "",
    val passwordHash: String = "",
    val displayName: String = "Sobat Cuan",
    val syncCode: String = "CUAN-7701", // 8-char pairing key for multi-device sync e.g. CUAN-8821
    val deviceId: String = "",
    val deviceName: String = "Android Device",
    val isLoggedIn: Boolean = false,
    val isCloudSyncEnabled: Boolean = true,
    val lastSyncedAt: Long = 0L,
    val syncStatusMessage: String = "Siap Sinkronisasi Multi-Device",
    val createdAt: Long = System.currentTimeMillis()
)

data class SyncPayload(
    val syncCode: String = "",
    val userEmail: String = "",
    val deviceId: String = "",
    val deviceName: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val userProfile: UserProfile? = null,
    val months: List<BudgetMonth> = emptyList(),
    val wallets: List<WalletItem> = emptyList(),
    val incomes: List<IncomeItem> = emptyList(),
    val savings: List<SavingItem> = emptyList(),
    val fixedExpenses: List<FixedExpenseItem> = emptyList(),
    val variableExpenses: List<VariableExpenseItem> = emptyList(),
    val subscriptions: List<SubscriptionItem> = emptyList(),
    val dailyExpenses: List<DailyExpenseItem> = emptyList(),
    val monthlyRecaps: List<MonthlyRecap> = emptyList(),
    val budgetPlanAllocations: List<BudgetPlanAllocation> = emptyList()
)

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val name: String = "Sobat Cuan",
    val pinHash: String = "1234",
    val avatarId: String = "shark_happy",
    val isPinEnabled: Boolean = true,
    val themePreset: String = "SHARK_BLUE",       // SHARK_BLUE, SWEET_ROSE, MINT_SAGE, LAVENDER_DREAM, SUNSET_PEACH, DARK_SLATE
    val fontColorPreset: String = "DEEP_CHARCOAL",// DEEP_CHARCOAL, NAVY_MIDNIGHT, ESPRESSO, PLUM_VIOLET, TEAL_FOREST
    val fontSizeScale: Float = 1.0f,              // 0.88f (Small), 1.0f (Normal), 1.15f (Large), 1.30f (Extra Large)
    val useManualBalance: Boolean = false,
    val manualBalance: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "budget_months")
data class BudgetMonth(
    @PrimaryKey val monthId: String, // e.g., "2026-01", "2026-07"
    val monthName: String,           // e.g., "Januari", "Juli"
    val year: Int,                   // e.g., 2026
    val notes: String = "",
    val isClosed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "income_items")
data class IncomeItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val monthId: String,
    val source: String,             // e.g., "Pekerjaan", "Bisnis", "Trading", "Freelance"
    val type: String = "Utama",      // "Utama", "Sampingan", "Bonus", "Passive"
    val amount: Double,
    val date: String = "",
    val walletName: String = "Saldo Rekening", // Sumber/Tujuan Saldo
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "saving_items")
data class SavingItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val monthId: String,
    val title: String,              // e.g., "Dana Darurat", "Saham", "Travelling", "Beli Laptop"
    val priority: String = "High",   // "High", "Medium", "Low"
    val plannedAmount: Double,      // Rencana
    val actualAmount: Double,       // Aktual
    val targetTotal: Double = 0.0,  // Target tabungan kumulatif jika ada
    val date: String = "",          // Tanggal transaksi e.g. "29/08/2026"
    val walletName: String = "Saldo Rekening", // Sumber Anggaran yang Dipakai
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "fixed_expense_items")
data class FixedExpenseItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val monthId: String,
    val title: String,              // e.g., "Sewa Kos", "Bayar Listrik", "Bayar Air", "Wifi"
    val priority: String = "High",
    val plannedAmount: Double,      // Rencana / Batas Maksimal
    val actualAmount: Double,       // Aktual
    val date: String = "",          // Tanggal transaksi e.g. "29/08/2026"
    val walletName: String = "Saldo Rekening", // Sumber Anggaran yang Dipakai
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "variable_expense_items")
data class VariableExpenseItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val monthId: String,
    val title: String,              // e.g., "Uang Makan", "Transport / Bensin", "Jajan", "Belanja"
    val priority: String = "Medium",
    val plannedAmount: Double,      // Rencana / Batas Maksimal
    val actualAmount: Double,       // Aktual
    val date: String = "",          // Tanggal transaksi e.g. "29/08/2026"
    val walletName: String = "Saldo DANA", // Sumber Anggaran yang Dipakai
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "subscription_items")
data class SubscriptionItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val monthId: String,
    val title: String,              // e.g., "Canva Pro", "ChatGPT", "Spotify", "Netflix", "Cicilan"
    val priority: String = "Low",
    val plannedAmount: Double,      // Rencana / Batas Maksimal
    val actualAmount: Double,       // Aktual
    val date: String = "",          // Tanggal transaksi e.g. "29/08/2026"
    val walletName: String = "Saldo Rekening", // Sumber Anggaran yang Dipakai
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "daily_expense_items")
data class DailyExpenseItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val monthId: String,
    val date: String,               // e.g., "01/01/2026"
    val title: String,              // e.g., "Batagor", "Matcha", "Beli Gas", "Dimsum Mentai"
    val category: String,           // "Jajan", "Makan", "Transport", "Belanja", "Hiburan", "Top Up", "Lainnya"
    val quantity: Int = 1,
    val unitPrice: Double,
    val totalAmount: Double,
    val notes: String = "",
    val walletName: String = "Uang Cash", // Sumber Anggaran yang Dipakai
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "monthly_recaps")
data class MonthlyRecap(
    @PrimaryKey val monthId: String,
    val monthName: String,
    val year: Int,
    val totalIncome: Double,
    val totalSavings: Double,
    val totalFixedExpense: Double,
    val totalVariableExpense: Double,
    val totalSubscription: Double,
    val totalDailyExpense: Double,
    val totalExpense: Double,
    val remainingBalance: Double,
    val savingsRatePercent: Double,
    val expenseRatePercent: Double,
    val recapNotes: String = "",
    val savedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "budget_plan_allocations")
data class BudgetPlanAllocation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val monthId: String,
    val categoryKey: String, // "FIXED", "VARIABLE", "SAVINGS", "SUBSCRIPTION", or custom
    val title: String,       // e.g., "Kebutuhan Pokok / Fixed Cost", "Jajan & Kebutuhan Variabel", "Tabungan & Investasi", "Langganan & Hiburan"
    val targetPercent: Double, // Persen saja (%) misal 50.0, 30.0, 20.0
    val colorHex: String = "#6599B8",
    val createdAt: Long = System.currentTimeMillis()
)

data class AllocationCalculationResult(
    val allocation: BudgetPlanAllocation,
    val totalIncome: Double,
    val maxAllowanceAmount: Double, // totalIncome * (targetPercent / 100)
    val actualSpentAmount: Double,  // total pengeluaran aktual di kategori ini
    val remainingAmount: Double,    // maxAllowanceAmount - actualSpentAmount
    val usagePercentOfPlan: Double, // (actualSpentAmount / maxAllowanceAmount) * 100
    val isNearMax: Boolean,         // usage >= 80% && < 100%
    val isExceeded: Boolean,        // usage >= 100%
    val excessAmount: Double        // max(0, actualSpent - maxAllowance)
)

data class CategoryRank(
    val categoryName: String,
    val totalAmount: Double,
    val percentageOfExpense: Double,
    val transactionCount: Int
)

data class CategoryBudgetStatus(
    val categoryTitle: String,
    val plannedLimit: Double,
    val actualSpent: Double,
    val remainingAllowance: Double,
    val usagePercentage: Double,
    val isNearLimit: Boolean,       // >= 80% and < 100%
    val isOverBudget: Boolean,      // >= 100%
    val excessAmount: Double
)

data class FinancialOverview(
    val totalIncome: Double = 0.0,
    val totalSavingPlanned: Double = 0.0,
    val totalSavingActual: Double = 0.0,
    val totalFixedPlanned: Double = 0.0,
    val totalFixedActual: Double = 0.0,
    val totalVariablePlanned: Double = 0.0,
    val totalVariableActual: Double = 0.0,
    val totalSubPlanned: Double = 0.0,
    val totalSubActual: Double = 0.0,
    val totalDailyExpense: Double = 0.0,
    val totalActualExpense: Double = 0.0,
    val remainingBalance: Double = 0.0,
    val remainingBudgetPercent: Double = 100.0,
    val savingsRatePercent: Double = 0.0,
    val useManualBalance: Boolean = false,
    val effectiveBalance: Double = 0.0
)

data class VoiceTransactionAnalysis(
    val rawTranscript: String = "",
    val itemTitle: String = "",
    val amount: Double = 0.0,
    val date: String = "",
    val targetCategory: String = "DAILY", // "DAILY", "VARIABLE", "FIXED", "SAVINGS", "SUBSCRIPTION", "INCOME"
    val subCategory: String = "Jajan",
    val priority: String = "Medium",
    val quantity: Int = 1,
    val walletName: String = "Uang Cash", // Sumber Anggaran yang Terpotong
    val explanation: String = "",
    val deductionImpact: String = ""
)

