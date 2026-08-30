package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.*
import com.example.data.repository.BudgetRepository
import com.example.parser.BankTranscriptParser
import com.example.parser.ParsedTransaction
import com.example.ui.components.ChartSlice
import com.example.ui.theme.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BudgetViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: BudgetRepository

    // User Profile & Preferences
    val userProfile: StateFlow<UserProfile?>
    val userAccount: StateFlow<UserAccount?>

    private val _isAppUnlocked = MutableStateFlow(false)
    val isAppUnlocked: StateFlow<Boolean> = _isAppUnlocked.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncStatusMessage = MutableStateFlow("Siap Sinkronisasi Multi-Device")
    val syncStatusMessage: StateFlow<String> = _syncStatusMessage.asStateFlow()

    private val _currentMonthId = MutableStateFlow("2026-01")
    val currentMonthId: StateFlow<String> = _currentMonthId.asStateFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = BudgetRepository(db.budgetDao())
        userProfile = repository.userProfile.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )
        userAccount = repository.userAccount.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

        viewModelScope.launch {
            repository.initializeDefaultDataIfEmpty()
        }

        // Auto unlock if user has logged in and PIN is not enabled, or if no PIN set
        viewModelScope.launch {
            repository.userProfile.collectLatest { profile ->
                if (profile != null && !profile.isPinEnabled) {
                    _isAppUnlocked.value = true
                }
            }
        }
    }

    val allMonths: StateFlow<List<BudgetMonth>> = repository.allMonths.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allRecaps: StateFlow<List<MonthlyRecap>> = repository.allRecaps.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Wallets & Multi-Anggaran Sumber Kas State
    val wallets: StateFlow<List<WalletItem>> = repository.allWallets.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val totalWalletBalance: StateFlow<Double> = wallets.map { list ->
        list.sumOf { it.balance }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )

    // Data streams for active month
    val incomes: StateFlow<List<IncomeItem>> = _currentMonthId.flatMapLatest { repository.getIncomesForMonth(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savings: StateFlow<List<SavingItem>> = _currentMonthId.flatMapLatest { repository.getSavingsForMonth(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val fixedExpenses: StateFlow<List<FixedExpenseItem>> = _currentMonthId.flatMapLatest { repository.getFixedExpensesForMonth(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val variableExpenses: StateFlow<List<VariableExpenseItem>> = _currentMonthId.flatMapLatest { repository.getVariableExpensesForMonth(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val subscriptions: StateFlow<List<SubscriptionItem>> = _currentMonthId.flatMapLatest { repository.getSubscriptionsForMonth(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dailyExpenses: StateFlow<List<DailyExpenseItem>> = _currentMonthId.flatMapLatest { repository.getDailyExpensesForMonth(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allocations: StateFlow<List<BudgetPlanAllocation>> = _currentMonthId.flatMapLatest { mId ->
        repository.getAllocationsForMonth(mId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Financial Planning Percentage Allocation Calculations
    val allocationCalculations: StateFlow<List<AllocationCalculationResult>> = kotlinx.coroutines.flow.combine(
        listOf(
            allocations,
            incomes,
            fixedExpenses,
            variableExpenses,
            savings,
            subscriptions,
            dailyExpenses
        )
    ) { flows ->
        @Suppress("UNCHECKED_CAST")
        val allocList = flows[0] as List<BudgetPlanAllocation>
        @Suppress("UNCHECKED_CAST")
        val incList = flows[1] as List<IncomeItem>
        @Suppress("UNCHECKED_CAST")
        val fixList = flows[2] as List<FixedExpenseItem>
        @Suppress("UNCHECKED_CAST")
        val varList = flows[3] as List<VariableExpenseItem>
        @Suppress("UNCHECKED_CAST")
        val savList = flows[4] as List<SavingItem>
        @Suppress("UNCHECKED_CAST")
        val subList = flows[5] as List<SubscriptionItem>
        @Suppress("UNCHECKED_CAST")
        val dailyList = flows[6] as List<DailyExpenseItem>

        val totalIncome = incList.sumOf { it.amount }
        val fixedSpent = fixList.sumOf { it.actualAmount }
        val varSpent = varList.sumOf { it.actualAmount } + dailyList.sumOf { it.totalAmount }
        val savSpent = savList.sumOf { it.actualAmount }
        val subSpent = subList.sumOf { it.actualAmount }

        allocList.map { alloc ->
            val actualSpent = when (alloc.categoryKey) {
                "FIXED" -> fixedSpent
                "VARIABLE" -> varSpent
                "SAVINGS" -> savSpent
                "SUBSCRIPTION" -> subSpent
                else -> 0.0
            }
            val maxAllowance = totalIncome * (alloc.targetPercent / 100.0)
            val remaining = maxAllowance - actualSpent
            val usagePercent = if (maxAllowance > 0.0) (actualSpent / maxAllowance) * 100.0 else 0.0
            val isNear = usagePercent >= 80.0 && usagePercent < 100.0
            val isExceeded = actualSpent > maxAllowance && maxAllowance > 0.0
            val excess = if (isExceeded) actualSpent - maxAllowance else 0.0

            AllocationCalculationResult(
                allocation = alloc,
                totalIncome = totalIncome,
                maxAllowanceAmount = maxAllowance,
                actualSpentAmount = actualSpent,
                remainingAmount = remaining,
                usagePercentOfPlan = usagePercent,
                isNearMax = isNear,
                isExceeded = isExceeded,
                excessAmount = excess
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Auto-seed default allocations when switching to a month without any allocations
    init {
        viewModelScope.launch {
            _currentMonthId.collectLatest { mId ->
                val existing = repository.getAllocationsForMonthOnce(mId)
                if (existing.isEmpty()) {
                    repository.insertAllocations(
                        listOf(
                            BudgetPlanAllocation(monthId = mId, categoryKey = "FIXED", title = "Kebutuhan Pokok (Fixed Cost)", targetPercent = 50.0, colorHex = "#6599B8"),
                            BudgetPlanAllocation(monthId = mId, categoryKey = "VARIABLE", title = "Kebutuhan Variabel & Jajan", targetPercent = 25.0, colorHex = "#F4A261"),
                            BudgetPlanAllocation(monthId = mId, categoryKey = "SAVINGS", title = "Tabungan & Investasi", targetPercent = 20.0, colorHex = "#74C69D"),
                            BudgetPlanAllocation(monthId = mId, categoryKey = "SUBSCRIPTION", title = "Langganan & Cicilan", targetPercent = 5.0, colorHex = "#A594F9")
                        )
                    )
                }
            }
        }
    }

    // Financial Overview Calculation
    val financialOverview: StateFlow<FinancialOverview> = combine(
        combine(incomes, savings, fixedExpenses) { inc, sav, fix -> Triple(inc, sav, fix) },
        combine(variableExpenses, subscriptions, dailyExpenses) { varExp, sub, daily -> Triple(varExp, sub, daily) },
        userProfile
    ) { (inc, sav, fix), (varExp, sub, daily), profile ->
        val totalIncome = inc.sumOf { it.amount }
        val totalSavingPlanned = sav.sumOf { it.plannedAmount }
        val totalSavingActual = sav.sumOf { it.actualAmount }

        val totalFixedPlanned = fix.sumOf { it.plannedAmount }
        val totalFixedActual = fix.sumOf { it.actualAmount }

        val totalVarPlanned = varExp.sumOf { it.plannedAmount }
        val totalVarActual = varExp.sumOf { it.actualAmount }

        val totalSubPlanned = sub.sumOf { it.plannedAmount }
        val totalSubActual = sub.sumOf { it.actualAmount }

        val totalDaily = daily.sumOf { it.totalAmount }

        val totalActualExpense = totalFixedActual + totalVarActual + totalSubActual + totalDaily
        val remainingBalance = totalIncome - (totalActualExpense + totalSavingActual)
        val remainingBudgetPercent = if (totalIncome > 0.0) ((remainingBalance / totalIncome) * 100.0).coerceIn(0.0, 100.0) else 100.0
        val savingsRatePercent = if (totalIncome > 0.0) ((totalSavingActual / totalIncome) * 100.0) else 0.0

        val useManual = profile?.useManualBalance ?: false
        val manualVal = profile?.manualBalance ?: 0.0
        val effectiveBal = if (useManual) manualVal else remainingBalance

        FinancialOverview(
            totalIncome = totalIncome,
            totalSavingPlanned = totalSavingPlanned,
            totalSavingActual = totalSavingActual,
            totalFixedPlanned = totalFixedPlanned,
            totalFixedActual = totalFixedActual,
            totalVariablePlanned = totalVarPlanned,
            totalVariableActual = totalVarActual,
            totalSubPlanned = totalSubPlanned,
            totalSubActual = totalSubActual,
            totalDailyExpense = totalDaily,
            totalActualExpense = totalActualExpense,
            remainingBalance = remainingBalance,
            remainingBudgetPercent = remainingBudgetPercent,
            savingsRatePercent = savingsRatePercent,
            useManualBalance = useManual,
            effectiveBalance = effectiveBal
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FinancialOverview())

    // Category Budget Status Calculations (Limits, Remaining Allowance, Warning Alerts)
    val fixedExpenseStatus: StateFlow<CategoryBudgetStatus> = fixedExpenses.map { items ->
        val planned = items.sumOf { it.plannedAmount }
        val actual = items.sumOf { it.actualAmount }
        calculateCategoryStatus("Pengeluaran Tetap", planned, actual)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), calculateCategoryStatus("Pengeluaran Tetap", 0.0, 0.0))

    val variableExpenseStatus: StateFlow<CategoryBudgetStatus> = variableExpenses.map { items ->
        val planned = items.sumOf { it.plannedAmount }
        val actual = items.sumOf { it.actualAmount }
        calculateCategoryStatus("Pengeluaran Variabel", planned, actual)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), calculateCategoryStatus("Pengeluaran Variabel", 0.0, 0.0))

    val subscriptionStatus: StateFlow<CategoryBudgetStatus> = subscriptions.map { items ->
        val planned = items.sumOf { it.plannedAmount }
        val actual = items.sumOf { it.actualAmount }
        calculateCategoryStatus("Langganan & Cicilan", planned, actual)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), calculateCategoryStatus("Langganan & Cicilan", 0.0, 0.0))

    val dailyExpenseStatus: StateFlow<CategoryBudgetStatus> = combine(dailyExpenses, variableExpenses) { daily, varExp ->
        // Daily expense target is estimated from variable budget or daily records
        val dailyActual = daily.sumOf { it.totalAmount }
        val plannedTarget = if (varExp.any { it.title.contains("Jajan", ignoreCase = true) || it.title.contains("Makan", ignoreCase = true) }) {
            varExp.filter { it.title.contains("Jajan", ignoreCase = true) || it.title.contains("Makan", ignoreCase = true) }.sumOf { it.plannedAmount }
        } else {
            1000000.0 // Default reasonable target for daily
        }
        calculateCategoryStatus("Jajan & Belanja Harian", plannedTarget, dailyActual)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), calculateCategoryStatus("Jajan & Belanja Harian", 1000000.0, 0.0))

    private fun calculateCategoryStatus(title: String, planned: Double, actual: Double): CategoryBudgetStatus {
        val remaining = (planned - actual).coerceAtLeast(0.0)
        val usagePercent = if (planned > 0.0) ((actual / planned) * 100.0) else if (actual > 0) 100.0 else 0.0
        val isNear = usagePercent >= 80.0 && usagePercent < 100.0
        val isOver = actual > planned && planned > 0.0
        val excess = if (isOver) actual - planned else 0.0

        return CategoryBudgetStatus(
            categoryTitle = title,
            plannedLimit = planned,
            actualSpent = actual,
            remainingAllowance = remaining,
            usagePercentage = usagePercent,
            isNearLimit = isNear,
            isOverBudget = isOver,
            excessAmount = excess
        )
    }

    // Chart Slices: Income
    val incomeChartSlices: StateFlow<List<ChartSlice>> = incomes.map { list ->
        val colors = listOf(Color(0xFF6599B8), Color(0xFF74C69D), Color(0xFFF4A261), Color(0xFFA594F9), Color(0xFFE2847A))
        list.mapIndexed { idx, item ->
            ChartSlice(
                label = item.source,
                value = item.amount,
                color = colors[idx % colors.size]
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Chart Slices: Savings
    val savingsChartSlices: StateFlow<List<ChartSlice>> = savings.map { list ->
        val colors = listOf(Color(0xFF74C69D), Color(0xFF52B788), Color(0xFF40916C), Color(0xFF2D6A4F), Color(0xFF95D5B2))
        list.mapIndexed { idx, item ->
            ChartSlice(
                label = item.title,
                value = item.actualAmount,
                color = colors[idx % colors.size]
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Ranking of Expenses
    val categoryRankings: StateFlow<List<CategoryRank>> = combine(
        combine(fixedExpenses, variableExpenses) { fix, varExp -> Pair(fix, varExp) },
        combine(subscriptions, dailyExpenses) { sub, daily -> Pair(sub, daily) }
    ) { (fix, varExp), (sub, daily) ->
        val map = mutableMapOf<String, Double>()
        val countMap = mutableMapOf<String, Int>()

        fix.forEach {
            map[it.title] = (map[it.title] ?: 0.0) + it.actualAmount
            countMap[it.title] = (countMap[it.title] ?: 0) + 1
        }
        varExp.forEach {
            map[it.title] = (map[it.title] ?: 0.0) + it.actualAmount
            countMap[it.title] = (countMap[it.title] ?: 0) + 1
        }
        sub.forEach {
            map[it.title] = (map[it.title] ?: 0.0) + it.actualAmount
            countMap[it.title] = (countMap[it.title] ?: 0) + 1
        }
        daily.forEach {
            val key = if (it.category.isNotBlank()) it.category else it.title
            map[key] = (map[key] ?: 0.0) + it.totalAmount
            countMap[key] = (countMap[key] ?: 0) + 1
        }

        val totalAllExpense = map.values.sum()
        map.map { (cat, amount) ->
            CategoryRank(
                categoryName = cat,
                totalAmount = amount,
                percentageOfExpense = if (totalAllExpense > 0) (amount / totalAllExpense) * 100.0 else 0.0,
                transactionCount = countMap[cat] ?: 1
            )
        }.sortedByDescending { it.totalAmount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Actions
    fun setAppUnlocked(unlocked: Boolean) {
        _isAppUnlocked.value = unlocked
    }

    fun selectMonth(monthId: String) {
        _currentMonthId.value = monthId
    }

    fun createNewMonth(name: String, year: Int, copyPreviousBudget: Boolean = true) {
        viewModelScope.launch {
            val monthNum = getMonthNumber(name)
            val newMonthId = String.format(Locale.US, "%04d-%02d", year, monthNum)
            val newMonth = BudgetMonth(
                monthId = newMonthId,
                monthName = name,
                year = year,
                notes = "Budget $name $year"
            )
            repository.insertMonth(newMonth)

            if (copyPreviousBudget) {
                val prevFix = fixedExpenses.value
                val prevVar = variableExpenses.value
                val prevSav = savings.value
                val prevSub = subscriptions.value
                val prevInc = incomes.value

                repository.insertFixedExpenses(prevFix.map { it.copy(id = 0, monthId = newMonthId, actualAmount = 0.0) })
                repository.insertVariableExpenses(prevVar.map { it.copy(id = 0, monthId = newMonthId, actualAmount = 0.0) })
                repository.insertSavings(prevSav.map { it.copy(id = 0, monthId = newMonthId, actualAmount = 0.0) })
                repository.insertSubscriptions(prevSub.map { it.copy(id = 0, monthId = newMonthId, actualAmount = 0.0) })
                repository.insertIncomes(prevInc.map { it.copy(id = 0, monthId = newMonthId) })
            }
            _currentMonthId.value = newMonthId
        }
    }

    private fun getMonthNumber(name: String): Int {
        return when (name.lowercase()) {
            "januari", "january" -> 1
            "februari", "february" -> 2
            "maret", "march" -> 3
            "april" -> 4
            "mei", "may" -> 5
            "juni", "june" -> 6
            "juli", "july" -> 7
            "agustus", "august" -> 8
            "september", "september" -> 9
            "oktober", "october" -> 10
            "november", "november" -> 11
            "desember", "december" -> 12
            else -> 1
        }
    }

    // Update Manual Balance Override
    fun setManualBalance(useManual: Boolean, manualAmount: Double) {
        viewModelScope.launch {
            val current = userProfile.value ?: UserProfile()
            repository.saveUserProfile(
                current.copy(
                    useManualBalance = useManual,
                    manualBalance = manualAmount
                )
            )
        }
    }

    // Update Appearance / Theme Settings
    fun updateThemeSettings(
        themePresetId: String,
        fontColorPresetId: String,
        fontSizeScale: Float
    ) {
        viewModelScope.launch {
            val current = userProfile.value ?: UserProfile()
            repository.saveUserProfile(
                current.copy(
                    themePreset = themePresetId,
                    fontColorPreset = fontColorPresetId,
                    fontSizeScale = fontSizeScale
                )
            )
        }
    }

    // Wallet & Multi-Anggaran Management
    fun addWallet(name: String, type: String, initialBalance: Double, colorHex: String = "#6599B8", iconName: String = "cash") {
        viewModelScope.launch {
            repository.insertWallet(
                WalletItem(
                    name = name.trim(),
                    type = type,
                    balance = initialBalance,
                    colorHex = colorHex,
                    iconName = iconName,
                    isDefault = false
                )
            )
            triggerCloudSync()
        }
    }

    fun updateWallet(item: WalletItem) {
        viewModelScope.launch {
            repository.updateWallet(item)
            triggerCloudSync()
        }
    }

    fun updateWalletBalanceManual(walletId: Long, newBalance: Double) {
        viewModelScope.launch {
            repository.updateWalletBalance(walletId, newBalance)
            triggerCloudSync()
        }
    }

    fun deleteWallet(item: WalletItem) {
        viewModelScope.launch {
            repository.deleteWallet(item)
            triggerCloudSync()
        }
    }

    private suspend fun adjustWalletBalance(walletName: String, delta: Double) {
        try {
            val all = repository.getAllWalletsOnce()
            val match = all.find { it.name.equals(walletName.trim(), ignoreCase = true) }
                ?: all.find { it.isDefault }
                ?: all.firstOrNull()
            if (match != null) {
                val newBal = match.balance + delta
                repository.updateWalletBalance(match.id, newBal)
            }
        } catch (e: Exception) {
            // Non-blocking
        }
    }

    // CRUD Income
    fun addIncome(source: String, type: String, amount: Double, date: String, walletName: String = "Saldo Rekening") {
        viewModelScope.launch {
            repository.insertIncome(
                IncomeItem(
                    monthId = _currentMonthId.value,
                    source = source,
                    type = type,
                    amount = amount,
                    date = date.ifEmpty { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()) },
                    walletName = walletName
                )
            )
            adjustWalletBalance(walletName, amount)
            triggerCloudSync()
        }
    }

    fun updateIncome(item: IncomeItem, oldAmount: Double = item.amount, oldWallet: String = item.walletName) {
        viewModelScope.launch {
            repository.updateIncome(item)
            if (oldWallet.equals(item.walletName, ignoreCase = true)) {
                adjustWalletBalance(item.walletName, item.amount - oldAmount)
            } else {
                adjustWalletBalance(oldWallet, -oldAmount)
                adjustWalletBalance(item.walletName, item.amount)
            }
            triggerCloudSync()
        }
    }

    fun deleteIncome(item: IncomeItem) {
        viewModelScope.launch {
            repository.deleteIncome(item)
            adjustWalletBalance(item.walletName, -item.amount)
            triggerCloudSync()
        }
    }

    // CRUD Savings
    fun addSaving(title: String, priority: String, planned: Double, actual: Double, date: String = "", walletName: String = "Saldo Rekening") {
        viewModelScope.launch {
            repository.insertSaving(
                SavingItem(
                    monthId = _currentMonthId.value,
                    title = title,
                    priority = priority,
                    plannedAmount = planned,
                    actualAmount = actual,
                    date = date.ifEmpty { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()) },
                    walletName = walletName
                )
            )
            if (actual > 0) {
                adjustWalletBalance(walletName, -actual)
            }
            triggerCloudSync()
        }
    }

    fun updateSaving(item: SavingItem, oldActual: Double = item.actualAmount, oldWallet: String = item.walletName) {
        viewModelScope.launch {
            repository.updateSaving(item)
            if (oldWallet.equals(item.walletName, ignoreCase = true)) {
                adjustWalletBalance(item.walletName, -(item.actualAmount - oldActual))
            } else {
                adjustWalletBalance(oldWallet, oldActual)
                adjustWalletBalance(item.walletName, -item.actualAmount)
            }
            triggerCloudSync()
        }
    }

    fun deleteSaving(item: SavingItem) {
        viewModelScope.launch {
            repository.deleteSaving(item)
            if (item.actualAmount > 0) {
                adjustWalletBalance(item.walletName, item.actualAmount)
            }
            triggerCloudSync()
        }
    }

    // CRUD Fixed Expense
    fun addFixedExpense(title: String, priority: String, planned: Double, actual: Double, date: String = "", walletName: String = "Saldo Rekening") {
        viewModelScope.launch {
            repository.insertFixedExpense(
                FixedExpenseItem(
                    monthId = _currentMonthId.value,
                    title = title,
                    priority = priority,
                    plannedAmount = planned,
                    actualAmount = actual,
                    date = date.ifEmpty { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()) },
                    walletName = walletName
                )
            )
            if (actual > 0) {
                adjustWalletBalance(walletName, -actual)
            }
            triggerCloudSync()
        }
    }

    fun updateFixedExpense(item: FixedExpenseItem, oldActual: Double = item.actualAmount, oldWallet: String = item.walletName) {
        viewModelScope.launch {
            repository.updateFixedExpense(item)
            if (oldWallet.equals(item.walletName, ignoreCase = true)) {
                adjustWalletBalance(item.walletName, -(item.actualAmount - oldActual))
            } else {
                adjustWalletBalance(oldWallet, oldActual)
                adjustWalletBalance(item.walletName, -item.actualAmount)
            }
            triggerCloudSync()
        }
    }

    fun deleteFixedExpense(item: FixedExpenseItem) {
        viewModelScope.launch {
            repository.deleteFixedExpense(item)
            if (item.actualAmount > 0) {
                adjustWalletBalance(item.walletName, item.actualAmount)
            }
            triggerCloudSync()
        }
    }

    // CRUD Variable Expense
    fun addVariableExpense(title: String, priority: String, planned: Double, actual: Double, date: String = "", walletName: String = "Saldo DANA") {
        viewModelScope.launch {
            repository.insertVariableExpense(
                VariableExpenseItem(
                    monthId = _currentMonthId.value,
                    title = title,
                    priority = priority,
                    plannedAmount = planned,
                    actualAmount = actual,
                    date = date.ifEmpty { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()) },
                    walletName = walletName
                )
            )
            if (actual > 0) {
                adjustWalletBalance(walletName, -actual)
            }
            triggerCloudSync()
        }
    }

    fun updateVariableExpense(item: VariableExpenseItem, oldActual: Double = item.actualAmount, oldWallet: String = item.walletName) {
        viewModelScope.launch {
            repository.updateVariableExpense(item)
            if (oldWallet.equals(item.walletName, ignoreCase = true)) {
                adjustWalletBalance(item.walletName, -(item.actualAmount - oldActual))
            } else {
                adjustWalletBalance(oldWallet, oldActual)
                adjustWalletBalance(item.walletName, -item.actualAmount)
            }
            triggerCloudSync()
        }
    }

    fun deleteVariableExpense(item: VariableExpenseItem) {
        viewModelScope.launch {
            repository.deleteVariableExpense(item)
            if (item.actualAmount > 0) {
                adjustWalletBalance(item.walletName, item.actualAmount)
            }
            triggerCloudSync()
        }
    }

    // CRUD Subscription
    fun addSubscription(title: String, priority: String, planned: Double, actual: Double, date: String = "", walletName: String = "Saldo Rekening") {
        viewModelScope.launch {
            repository.insertSubscription(
                SubscriptionItem(
                    monthId = _currentMonthId.value,
                    title = title,
                    priority = priority,
                    plannedAmount = planned,
                    actualAmount = actual,
                    date = date.ifEmpty { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()) },
                    walletName = walletName
                )
            )
            if (actual > 0) {
                adjustWalletBalance(walletName, -actual)
            }
            triggerCloudSync()
        }
    }

    fun updateSubscription(item: SubscriptionItem, oldActual: Double = item.actualAmount, oldWallet: String = item.walletName) {
        viewModelScope.launch {
            repository.updateSubscription(item)
            if (oldWallet.equals(item.walletName, ignoreCase = true)) {
                adjustWalletBalance(item.walletName, -(item.actualAmount - oldActual))
            } else {
                adjustWalletBalance(oldWallet, oldActual)
                adjustWalletBalance(item.walletName, -item.actualAmount)
            }
            triggerCloudSync()
        }
    }

    fun deleteSubscription(item: SubscriptionItem) {
        viewModelScope.launch {
            repository.deleteSubscription(item)
            if (item.actualAmount > 0) {
                adjustWalletBalance(item.walletName, item.actualAmount)
            }
            triggerCloudSync()
        }
    }

    // CRUD Daily Expense
    fun addDailyExpense(date: String, title: String, category: String, quantity: Int, unitPrice: Double, walletName: String = "Uang Cash") {
        viewModelScope.launch {
            val total = quantity * unitPrice
            repository.insertDailyExpense(
                DailyExpenseItem(
                    monthId = _currentMonthId.value,
                    date = date.ifEmpty { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()) },
                    title = title,
                    category = category,
                    quantity = quantity,
                    unitPrice = unitPrice,
                    totalAmount = total,
                    walletName = walletName
                )
            )
            if (total > 0) {
                adjustWalletBalance(walletName, -total)
            }
            triggerCloudSync()
        }
    }

    fun updateDailyExpense(item: DailyExpenseItem, oldTotal: Double = item.totalAmount, oldWallet: String = item.walletName) {
        viewModelScope.launch {
            repository.updateDailyExpense(item)
            if (oldWallet.equals(item.walletName, ignoreCase = true)) {
                adjustWalletBalance(item.walletName, -(item.totalAmount - oldTotal))
            } else {
                adjustWalletBalance(oldWallet, oldTotal)
                adjustWalletBalance(item.walletName, -item.totalAmount)
            }
            triggerCloudSync()
        }
    }

    fun deleteDailyExpense(item: DailyExpenseItem) {
        viewModelScope.launch {
            repository.deleteDailyExpense(item)
            if (item.totalAmount > 0) {
                adjustWalletBalance(item.walletName, item.totalAmount)
            }
            triggerCloudSync()
        }
    }

    // AI Voice Assistant Processing
    fun analyzeVoiceTranscript(transcript: String, onResult: (VoiceTransactionAnalysis) -> Unit) {
        viewModelScope.launch {
            val analysis = com.example.util.GeminiVoiceService.analyzeVoiceTransaction(transcript)
            onResult(analysis)
        }
    }

    // Execute Voice Transaction and automatically allocate & deduct budget & wallet
    fun executeVoiceTransaction(analysis: VoiceTransactionAnalysis, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val mId = _currentMonthId.value
            val itemDate = analysis.date.ifEmpty { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()) }
            val chosenWallet = analysis.walletName.ifEmpty { "Uang Cash" }

            when (analysis.targetCategory) {
                "VARIABLE" -> {
                    repository.insertVariableExpense(
                        VariableExpenseItem(
                            monthId = mId,
                            title = analysis.itemTitle,
                            priority = analysis.priority.ifEmpty { "Medium" },
                            plannedAmount = 0.0,
                            actualAmount = analysis.amount,
                            date = itemDate,
                            walletName = chosenWallet
                        )
                    )
                    adjustWalletBalance(chosenWallet, -analysis.amount)
                }
                "DAILY" -> {
                    repository.insertDailyExpense(
                        DailyExpenseItem(
                            monthId = mId,
                            date = itemDate,
                            title = analysis.itemTitle,
                            category = analysis.subCategory.ifEmpty { "Jajan" },
                            quantity = analysis.quantity.coerceAtLeast(1),
                            unitPrice = analysis.amount,
                            totalAmount = analysis.amount,
                            notes = "Dicatat via Asisten Suara (Sumber: $chosenWallet)",
                            walletName = chosenWallet
                        )
                    )
                    repository.insertVariableExpense(
                        VariableExpenseItem(
                            monthId = mId,
                            title = analysis.itemTitle,
                            priority = "Medium",
                            plannedAmount = 0.0,
                            actualAmount = analysis.amount,
                            date = itemDate,
                            walletName = chosenWallet
                        )
                    )
                    adjustWalletBalance(chosenWallet, -analysis.amount)
                }
                "FIXED" -> {
                    repository.insertFixedExpense(
                        FixedExpenseItem(
                            monthId = mId,
                            title = analysis.itemTitle,
                            priority = analysis.priority.ifEmpty { "High" },
                            plannedAmount = 0.0,
                            actualAmount = analysis.amount,
                            date = itemDate,
                            walletName = chosenWallet
                        )
                    )
                    adjustWalletBalance(chosenWallet, -analysis.amount)
                }
                "SAVINGS" -> {
                    repository.insertSaving(
                        SavingItem(
                            monthId = mId,
                            title = analysis.itemTitle,
                            priority = analysis.priority.ifEmpty { "High" },
                            plannedAmount = 0.0,
                            actualAmount = analysis.amount,
                            date = itemDate,
                            walletName = chosenWallet
                        )
                    )
                    adjustWalletBalance(chosenWallet, -analysis.amount)
                }
                "SUBSCRIPTION" -> {
                    repository.insertSubscription(
                        SubscriptionItem(
                            monthId = mId,
                            title = analysis.itemTitle,
                            priority = analysis.priority.ifEmpty { "Low" },
                            plannedAmount = 0.0,
                            actualAmount = analysis.amount,
                            date = itemDate,
                            walletName = chosenWallet
                        )
                    )
                    adjustWalletBalance(chosenWallet, -analysis.amount)
                }
                "INCOME" -> {
                    repository.insertIncome(
                        IncomeItem(
                            monthId = mId,
                            source = analysis.itemTitle,
                            type = analysis.subCategory.ifEmpty { "Pemasukan" },
                            amount = analysis.amount,
                            date = itemDate,
                            walletName = chosenWallet
                        )
                    )
                    adjustWalletBalance(chosenWallet, analysis.amount)
                }
                else -> {
                    repository.insertDailyExpense(
                        DailyExpenseItem(
                            monthId = mId,
                            date = itemDate,
                            title = analysis.itemTitle,
                            category = "Jajan",
                            quantity = 1,
                            unitPrice = analysis.amount,
                            totalAmount = analysis.amount,
                            notes = "Dicatat via Suara (Sumber: $chosenWallet)",
                            walletName = chosenWallet
                        )
                    )
                    repository.insertVariableExpense(
                        VariableExpenseItem(
                            monthId = mId,
                            title = analysis.itemTitle,
                            priority = "Medium",
                            plannedAmount = 0.0,
                            actualAmount = analysis.amount,
                            date = itemDate,
                            walletName = chosenWallet
                        )
                    )
                    adjustWalletBalance(chosenWallet, -analysis.amount)
                }
            }
            // Auto cloud sync
            triggerCloudSync()
            onComplete()
        }
    }

    // Batch Import from Bank / DANA Transcript / PDF / Screenshot
    fun importTransactions(transactions: List<ParsedTransaction>) {
        viewModelScope.launch {
            val mId = _currentMonthId.value
            for (t in transactions) {
                if (!t.isChecked) continue
                when (t.targetSection) {
                    "INCOME" -> {
                        repository.insertIncome(
                            IncomeItem(
                                monthId = mId,
                                source = t.description,
                                type = t.suggestedCategory,
                                amount = t.amount,
                                date = t.date
                            )
                        )
                    }
                    "SAVING" -> {
                        repository.insertSaving(
                            SavingItem(
                                monthId = mId,
                                title = t.description,
                                priority = "High",
                                plannedAmount = t.amount,
                                actualAmount = t.amount
                            )
                        )
                    }
                    "FIXED" -> {
                        repository.insertFixedExpense(
                            FixedExpenseItem(
                                monthId = mId,
                                title = t.description,
                                priority = "High",
                                plannedAmount = t.amount,
                                actualAmount = t.amount
                            )
                        )
                    }
                    "SUBSCRIPTION" -> {
                        repository.insertSubscription(
                            SubscriptionItem(
                                monthId = mId,
                                title = t.description,
                                priority = "Low",
                                plannedAmount = t.amount,
                                actualAmount = t.amount
                            )
                        )
                    }
                    "VARIABLE" -> {
                        repository.insertVariableExpense(
                            VariableExpenseItem(
                                monthId = mId,
                                title = t.description,
                                priority = "Medium",
                                plannedAmount = t.amount,
                                actualAmount = t.amount
                            )
                        )
                    }
                    else -> {
                        // Daily Tracker
                        repository.insertDailyExpense(
                            DailyExpenseItem(
                                monthId = mId,
                                date = t.date,
                                title = t.description,
                                category = t.suggestedCategory,
                                quantity = 1,
                                unitPrice = t.amount,
                                totalAmount = t.amount
                            )
                        )
                    }
                }
            }
        }
    }

    // Save Monthly Recap
    fun saveMonthlyRecap(notes: String = "") {
        viewModelScope.launch {
            val overview = financialOverview.value
            val mId = _currentMonthId.value
            val currentMonth = repository.getMonthById(mId)
            val recap = MonthlyRecap(
                monthId = mId,
                monthName = currentMonth?.monthName ?: "Bulan $mId",
                year = currentMonth?.year ?: 2026,
                totalIncome = overview.totalIncome,
                totalSavings = overview.totalSavingActual,
                totalFixedExpense = overview.totalFixedActual,
                totalVariableExpense = overview.totalVariableActual,
                totalSubscription = overview.totalSubActual,
                totalDailyExpense = overview.totalDailyExpense,
                totalExpense = overview.totalActualExpense,
                remainingBalance = overview.remainingBalance,
                savingsRatePercent = overview.savingsRatePercent,
                expenseRatePercent = if (overview.totalIncome > 0.0) (overview.totalActualExpense / overview.totalIncome) * 100.0 else 0.0,
                recapNotes = notes
            )
            repository.saveRecap(recap)
        }
    }

    // Update Profile & PIN
    fun updateProfile(name: String, pin: String, avatarId: String, isPinEnabled: Boolean) {
        viewModelScope.launch {
            val current = userProfile.value ?: UserProfile()
            repository.saveUserProfile(
                current.copy(
                    name = name,
                    pinHash = pin,
                    avatarId = avatarId,
                    isPinEnabled = isPinEnabled
                )
            )
            if (isPinEnabled) {
                // If user enables PIN, keep unlocked until lock is requested or restart
            } else {
                _isAppUnlocked.value = true
            }
        }
    }

    fun lockApp() {
        _isAppUnlocked.value = false
    }

    // Budget Plan Percentage Allocation Operations
    fun updateAllocationPercent(id: Long, newPercent: Double) {
        viewModelScope.launch {
            val current = allocations.value.find { it.id == id } ?: return@launch
            repository.updateAllocation(current.copy(targetPercent = newPercent.coerceIn(0.0, 100.0)))
        }
    }

    fun addCustomAllocation(title: String, percent: Double, colorHex: String = "#74C69D") {
        viewModelScope.launch {
            repository.insertAllocation(
                BudgetPlanAllocation(
                    monthId = _currentMonthId.value,
                    categoryKey = "CUSTOM_${System.currentTimeMillis()}",
                    title = title,
                    targetPercent = percent.coerceIn(0.0, 100.0),
                    colorHex = colorHex
                )
            )
        }
    }

    fun deleteAllocation(item: BudgetPlanAllocation) {
        viewModelScope.launch {
            repository.deleteAllocation(item)
        }
    }

    fun resetAllocationsToDefault() {
        viewModelScope.launch {
            val mId = _currentMonthId.value
            repository.clearAllocationsForMonth(mId)
            repository.insertAllocations(
                listOf(
                    BudgetPlanAllocation(monthId = mId, categoryKey = "FIXED", title = "Kebutuhan Pokok (Fixed Cost)", targetPercent = 50.0, colorHex = "#6599B8"),
                    BudgetPlanAllocation(monthId = mId, categoryKey = "VARIABLE", title = "Kebutuhan Variabel & Jajan", targetPercent = 25.0, colorHex = "#F4A261"),
                    BudgetPlanAllocation(monthId = mId, categoryKey = "SAVINGS", title = "Tabungan & Investasi", targetPercent = 20.0, colorHex = "#74C69D"),
                    BudgetPlanAllocation(monthId = mId, categoryKey = "SUBSCRIPTION", title = "Langganan & Cicilan", targetPercent = 5.0, colorHex = "#A594F9")
                )
            )
        }
    }

    // ==========================================
    // Multi-Device Cloud Sync & Account Engine
    // ==========================================

    fun registerAccount(name: String, email: String, password: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                _isSyncing.value = true
                _syncStatusMessage.value = "Mendaftarkan akun & membuat kunci sinkronisasi..."

                val (deviceId, deviceName) = com.example.util.CloudSyncService.getDeviceIdentifier(getApplication())
                val generatedSyncCode = com.example.util.CloudSyncService.generateDeviceSyncCode(email)

                val account = UserAccount(
                    id = 1,
                    email = email.trim().lowercase(),
                    passwordHash = password,
                    displayName = name.ifBlank { "Sobat Cuan" },
                    syncCode = generatedSyncCode,
                    deviceId = deviceId,
                    deviceName = deviceName,
                    isLoggedIn = true,
                    isCloudSyncEnabled = true,
                    lastSyncedAt = System.currentTimeMillis(),
                    syncStatusMessage = "Akun terdaftar. Sinkronisasi aktif!"
                )
                repository.saveUserAccount(account)

                val profile = userProfile.value ?: UserProfile()
                repository.saveUserProfile(
                    profile.copy(
                        name = name.ifBlank { "Sobat Cuan" },
                        pinHash = if (password.length == 4 && password.all { it.isDigit() }) password else profile.pinHash
                    )
                )

                // Push initial state to cloud
                com.example.util.CloudSyncService.uploadToCloud(getApplication(), generatedSyncCode, email)

                _isAppUnlocked.value = true
                _isSyncing.value = false
                _syncStatusMessage.value = "🟢 Tersinkronisasi Multi-Device ($generatedSyncCode)"
                onResult(true, "Akun berhasil dibuat! Kode Sinkronisasi Perangkat: $generatedSyncCode")
            } catch (e: Exception) {
                _isSyncing.value = false
                _syncStatusMessage.value = "Gagal membuat akun: ${e.message}"
                onResult(false, e.message ?: "Gagal membuat akun")
            }
        }
    }

    fun loginAccount(email: String, password: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                _isSyncing.value = true
                _syncStatusMessage.value = "Menghubungkan akun & sinkronisasi data cloud..."

                val (deviceId, deviceName) = com.example.util.CloudSyncService.getDeviceIdentifier(getApplication())
                val syncCode = com.example.util.CloudSyncService.generateDeviceSyncCode(email)

                // Try to download cloud data for this account
                val downloadRes = com.example.util.CloudSyncService.downloadFromCloud(getApplication(), syncCode)
                if (downloadRes.isSuccess) {
                    val payload = downloadRes.getOrNull()
                    val account = UserAccount(
                        id = 1,
                        email = email.trim().lowercase(),
                        passwordHash = password,
                        displayName = payload?.userProfile?.name ?: "Sobat Cuan",
                        syncCode = syncCode,
                        deviceId = deviceId,
                        deviceName = deviceName,
                        isLoggedIn = true,
                        isCloudSyncEnabled = true,
                        lastSyncedAt = System.currentTimeMillis(),
                        syncStatusMessage = "🟢 Tersinkronisasi dengan device lain ($syncCode)"
                    )
                    repository.saveUserAccount(account)
                    _isAppUnlocked.value = true
                    _isSyncing.value = false
                    _syncStatusMessage.value = "🟢 Tersinkronisasi ($syncCode)"
                    onResult(true, "Login berhasil! Seluruh data tersinkronisasi dari device lain.")
                } else {
                    // Create local session with this email
                    val account = UserAccount(
                        id = 1,
                        email = email.trim().lowercase(),
                        passwordHash = password,
                        displayName = "Sobat Cuan",
                        syncCode = syncCode,
                        deviceId = deviceId,
                        deviceName = deviceName,
                        isLoggedIn = true,
                        isCloudSyncEnabled = true,
                        lastSyncedAt = System.currentTimeMillis(),
                        syncStatusMessage = "🟢 Siap sinkronisasi"
                    )
                    repository.saveUserAccount(account)
                    com.example.util.CloudSyncService.uploadToCloud(getApplication(), syncCode, email)
                    _isAppUnlocked.value = true
                    _isSyncing.value = false
                    onResult(true, "Login berhasil! Kode sinkronisasi: $syncCode")
                }
            } catch (e: Exception) {
                _isSyncing.value = false
                _syncStatusMessage.value = "Gagal login: ${e.message}"
                onResult(false, e.message ?: "Gagal login")
            }
        }
    }

    fun connectWithSyncCode(code: String, onResult: (Boolean, String) -> Unit) {
        val cleanCode = code.trim().uppercase()
        if (cleanCode.isBlank()) {
            onResult(false, "Kode sinkronisasi tidak boleh kosong")
            return
        }

        viewModelScope.launch {
            try {
                _isSyncing.value = true
                _syncStatusMessage.value = "Mengunduh data akun dari kode $cleanCode..."

                val (deviceId, deviceName) = com.example.util.CloudSyncService.getDeviceIdentifier(getApplication())
                val result = com.example.util.CloudSyncService.downloadFromCloud(getApplication(), cleanCode)

                if (result.isSuccess) {
                    val payload = result.getOrNull()
                    val account = UserAccount(
                        id = 1,
                        email = payload?.userEmail ?: "$cleanCode@cuan.local",
                        displayName = payload?.userProfile?.name ?: "Sobat Cuan",
                        syncCode = cleanCode,
                        deviceId = deviceId,
                        deviceName = deviceName,
                        isLoggedIn = true,
                        isCloudSyncEnabled = true,
                        lastSyncedAt = System.currentTimeMillis(),
                        syncStatusMessage = "🟢 Tersambung & Tersinkronisasi Multi-Device ($cleanCode)"
                    )
                    repository.saveUserAccount(account)
                    _isAppUnlocked.value = true
                    _isSyncing.value = false
                    _syncStatusMessage.value = "🟢 Tersinkronisasi ($cleanCode)"
                    onResult(true, "Berhasil terhubung! Data dari device lain berhasil dimuat.")
                } else {
                    _isSyncing.value = false
                    _syncStatusMessage.value = "Kode $cleanCode tidak ditemukan di cloud."
                    onResult(false, "Kode sinkronisasi '$cleanCode' belum terdaftar atau belum pernah diunggah.")
                }
            } catch (e: Exception) {
                _isSyncing.value = false
                onResult(false, e.message ?: "Gagal menghubungkan device")
            }
        }
    }

    fun triggerCloudSync(onComplete: ((Boolean, String) -> Unit)? = null) {
        viewModelScope.launch {
            val account = userAccount.value ?: repository.getUserAccountOnce()
            val syncCode = account?.syncCode?.ifBlank { "CUAN-7701" } ?: "CUAN-7701"
            val email = account?.email ?: "user@planner.id"

            _isSyncing.value = true
            _syncStatusMessage.value = "🔄 Menyinkronkan data multi-device..."

            // 1. First push local updates to cloud
            val uploadRes = com.example.util.CloudSyncService.uploadToCloud(getApplication(), syncCode, email)
            // 2. Fetch latest changes
            val downloadRes = com.example.util.CloudSyncService.downloadFromCloud(getApplication(), syncCode)

            _isSyncing.value = false
            val timeStr = SimpleDateFormat("HH:mm:ss", Locale("id", "ID")).format(Date())

            if (uploadRes.isSuccess || downloadRes.isSuccess) {
                _syncStatusMessage.value = "🟢 Tersinkronisasi Multi-Device ($timeStr)"
                account?.let {
                    repository.saveUserAccount(it.copy(lastSyncedAt = System.currentTimeMillis(), syncStatusMessage = "🟢 Tersinkronisasi Multi-Device ($timeStr)"))
                }
                onComplete?.invoke(true, "Data berhasil disinkronisasi ke seluruh device!")
            } else {
                _syncStatusMessage.value = "⚠️ Sinkronisasi offline (Lokal tersimpan aman)"
                onComplete?.invoke(false, "Sinkronisasi offline. Data tersimpan di database lokal.")
            }
        }
    }

    fun logoutAccount() {
        viewModelScope.launch {
            val current = userAccount.value ?: UserAccount()
            repository.saveUserAccount(current.copy(isLoggedIn = false))
            _isAppUnlocked.value = false
        }
    }
}
