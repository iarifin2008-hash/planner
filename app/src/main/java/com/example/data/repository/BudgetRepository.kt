package com.example.data.repository

import com.example.data.db.BudgetDao
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

class BudgetRepository(private val dao: BudgetDao) {

    val userProfile: Flow<UserProfile?> = dao.getUserProfile()
    val userAccount: Flow<UserAccount?> = dao.getUserAccount()
    val allMonths: Flow<List<BudgetMonth>> = dao.getAllMonths()
    val allRecaps: Flow<List<MonthlyRecap>> = dao.getAllRecaps()
    val allWallets: Flow<List<WalletItem>> = dao.getAllWallets()

    suspend fun getUserProfileOnce() = dao.getUserProfileOnce()
    suspend fun saveUserProfile(profile: UserProfile) = dao.saveUserProfile(profile)

    suspend fun getUserAccountOnce() = dao.getUserAccountOnce()
    suspend fun saveUserAccount(account: UserAccount) = dao.saveUserAccount(account)

    suspend fun getAllWalletsOnce() = dao.getAllWalletsOnce()
    suspend fun getWalletById(id: Long) = dao.getWalletById(id)
    suspend fun getWalletByName(name: String) = dao.getWalletByName(name)
    suspend fun insertWallet(item: WalletItem) = dao.insertWallet(item)
    suspend fun updateWallet(item: WalletItem) = dao.updateWallet(item)
    suspend fun deleteWallet(item: WalletItem) = dao.deleteWallet(item)
    suspend fun updateWalletBalance(id: Long, newBalance: Double) = dao.updateWalletBalance(id, newBalance)

    fun getIncomesForMonth(monthId: String): Flow<List<IncomeItem>> = dao.getIncomesForMonth(monthId)
    fun getSavingsForMonth(monthId: String): Flow<List<SavingItem>> = dao.getSavingsForMonth(monthId)
    fun getFixedExpensesForMonth(monthId: String): Flow<List<FixedExpenseItem>> = dao.getFixedExpensesForMonth(monthId)
    fun getVariableExpensesForMonth(monthId: String): Flow<List<VariableExpenseItem>> = dao.getVariableExpensesForMonth(monthId)
    fun getSubscriptionsForMonth(monthId: String): Flow<List<SubscriptionItem>> = dao.getSubscriptionsForMonth(monthId)
    fun getDailyExpensesForMonth(monthId: String): Flow<List<DailyExpenseItem>> = dao.getDailyExpensesForMonth(monthId)

    suspend fun insertMonth(month: BudgetMonth) = dao.insertMonth(month)
    suspend fun getMonthById(monthId: String) = dao.getMonthById(monthId)
    suspend fun deleteMonth(month: BudgetMonth) = dao.deleteMonth(month)

    suspend fun insertIncome(item: IncomeItem) = dao.insertIncome(item)
    suspend fun updateIncome(item: IncomeItem) = dao.updateIncome(item)
    suspend fun deleteIncome(item: IncomeItem) = dao.deleteIncome(item)

    suspend fun insertSaving(item: SavingItem) = dao.insertSaving(item)
    suspend fun updateSaving(item: SavingItem) = dao.updateSaving(item)
    suspend fun deleteSaving(item: SavingItem) = dao.deleteSaving(item)

    suspend fun insertFixedExpense(item: FixedExpenseItem) = dao.insertFixedExpense(item)
    suspend fun updateFixedExpense(item: FixedExpenseItem) = dao.updateFixedExpense(item)
    suspend fun deleteFixedExpense(item: FixedExpenseItem) = dao.deleteFixedExpense(item)

    suspend fun insertVariableExpense(item: VariableExpenseItem) = dao.insertVariableExpense(item)
    suspend fun updateVariableExpense(item: VariableExpenseItem) = dao.updateVariableExpense(item)
    suspend fun deleteVariableExpense(item: VariableExpenseItem) = dao.deleteVariableExpense(item)

    suspend fun insertSubscription(item: SubscriptionItem) = dao.insertSubscription(item)
    suspend fun updateSubscription(item: SubscriptionItem) = dao.updateSubscription(item)
    suspend fun deleteSubscription(item: SubscriptionItem) = dao.deleteSubscription(item)

    suspend fun insertDailyExpense(item: DailyExpenseItem) = dao.insertDailyExpense(item)
    suspend fun updateDailyExpense(item: DailyExpenseItem) = dao.updateDailyExpense(item)
    suspend fun deleteDailyExpense(item: DailyExpenseItem) = dao.deleteDailyExpense(item)

    suspend fun insertDailyExpenses(items: List<DailyExpenseItem>) = dao.insertDailyExpenses(items)
    suspend fun insertIncomes(items: List<IncomeItem>) = dao.insertIncomes(items)
    suspend fun insertSavings(items: List<SavingItem>) = dao.insertSavings(items)
    suspend fun insertFixedExpenses(items: List<FixedExpenseItem>) = dao.insertFixedExpenses(items)
    suspend fun insertVariableExpenses(items: List<VariableExpenseItem>) = dao.insertVariableExpenses(items)
    suspend fun insertSubscriptions(items: List<SubscriptionItem>) = dao.insertSubscriptions(items)

    suspend fun saveRecap(recap: MonthlyRecap) = dao.saveRecap(recap)
    suspend fun getRecapForMonth(monthId: String) = dao.getRecapForMonth(monthId)

    // Budget Plan Allocations
    fun getAllocationsForMonth(monthId: String): Flow<List<BudgetPlanAllocation>> = dao.getAllocationsForMonth(monthId)
    suspend fun getAllocationsForMonthOnce(monthId: String): List<BudgetPlanAllocation> = dao.getAllocationsForMonthOnce(monthId)
    suspend fun insertAllocation(item: BudgetPlanAllocation) = dao.insertAllocation(item)
    suspend fun insertAllocations(items: List<BudgetPlanAllocation>) = dao.insertAllocations(items)
    suspend fun updateAllocation(item: BudgetPlanAllocation) = dao.updateAllocation(item)
    suspend fun deleteAllocation(item: BudgetPlanAllocation) = dao.deleteAllocation(item)
    suspend fun clearAllocationsForMonth(monthId: String) = dao.clearAllocationsForMonth(monthId)

    suspend fun initializeDefaultDataIfEmpty() {
        val profile = dao.getUserProfileOnce()
        if (profile == null) {
            dao.saveUserProfile(
                UserProfile(
                    id = 1,
                    name = "Sobat Cuan",
                    pinHash = "1234",
                    avatarId = "shark_cute",
                    isPinEnabled = false // default unlocked or easy to setup
                )
            )
        }

        // Check if there are wallets; seed if empty
        val existingWallets = dao.getAllWalletsOnce()
        if (existingWallets.isEmpty()) {
            dao.insertWallets(
                listOf(
                    WalletItem(name = "Uang Cash", type = "CASH", balance = 500000.0, colorHex = "#74C69D", iconName = "cash", isDefault = false),
                    WalletItem(name = "Saldo DANA", type = "E_WALLET", balance = 350000.0, colorHex = "#118EEA", iconName = "dana", isDefault = false),
                    WalletItem(name = "Saldo Rekening", type = "BANK", balance = 5000000.0, colorHex = "#6599B8", iconName = "bank", isDefault = true),
                    WalletItem(name = "GoPay", type = "E_WALLET", balance = 150000.0, colorHex = "#00AED6", iconName = "wallet", isDefault = false)
                )
            )
        }

        // Check if there are months
        val existingMonth = dao.getMonthById("2026-01")
        if (existingMonth == null) {
            val initialMonth = BudgetMonth(
                monthId = "2026-01",
                monthName = "Januari",
                year = 2026,
                notes = "Budgeting Awal Tahun Shark Edition"
            )
            dao.insertMonth(initialMonth)

            // Seed with sample data exactly matching the reference photos
            dao.insertIncomes(
                listOf(
                    IncomeItem(monthId = "2026-01", source = "Pekerjaan", type = "Utama", amount = 3000000.0, date = "01/01/2026"),
                    IncomeItem(monthId = "2026-01", source = "Bisnis", type = "Sampingan", amount = 1000000.0, date = "05/01/2026"),
                    IncomeItem(monthId = "2026-01", source = "Trading", type = "Sampingan", amount = 500000.0, date = "10/01/2026"),
                    IncomeItem(monthId = "2026-01", source = "Freelance", type = "Sampingan", amount = 350000.0, date = "15/01/2026")
                )
            )

            dao.insertSavings(
                listOf(
                    SavingItem(monthId = "2026-01", title = "Dana Darurat", priority = "High", plannedAmount = 500000.0, actualAmount = 500000.0),
                    SavingItem(monthId = "2026-01", title = "Investasi Saham", priority = "High", plannedAmount = 1000000.0, actualAmount = 1000000.0),
                    SavingItem(monthId = "2026-01", title = "Beli Gadget / Laptop", priority = "Medium", plannedAmount = 400000.0, actualAmount = 400000.0),
                    SavingItem(monthId = "2026-01", title = "Jalan-Jalan / Travelling", priority = "Low", plannedAmount = 150000.0, actualAmount = 150000.0)
                )
            )

            dao.insertFixedExpenses(
                listOf(
                    FixedExpenseItem(monthId = "2026-01", title = "Sewa Kos", priority = "High", plannedAmount = 750000.0, actualAmount = 750000.0),
                    FixedExpenseItem(monthId = "2026-01", title = "Bayar Listrik & Air", priority = "High", plannedAmount = 250000.0, actualAmount = 250000.0),
                    FixedExpenseItem(monthId = "2026-01", title = "Wifi Internet", priority = "High", plannedAmount = 100000.0, actualAmount = 100000.0),
                    FixedExpenseItem(monthId = "2026-01", title = "Iuran Sampah & RT", priority = "Medium", plannedAmount = 50000.0, actualAmount = 50000.0)
                )
            )

            dao.insertVariableExpenses(
                listOf(
                    VariableExpenseItem(monthId = "2026-01", title = "Uang Makan", priority = "High", plannedAmount = 600000.0, actualAmount = 550000.0),
                    VariableExpenseItem(monthId = "2026-01", title = "Uang Transport / Bensin", priority = "High", plannedAmount = 300000.0, actualAmount = 300000.0),
                    VariableExpenseItem(monthId = "2026-01", title = "Jajan & Nongkrong", priority = "Medium", plannedAmount = 200000.0, actualAmount = 250000.0),
                    VariableExpenseItem(monthId = "2026-01", title = "Belanja Kebutuhan Harian", priority = "Medium", plannedAmount = 150000.0, actualAmount = 120000.0)
                )
            )

            dao.insertSubscriptions(
                listOf(
                    SubscriptionItem(monthId = "2026-01", title = "Canva Pro", priority = "Low", plannedAmount = 75000.0, actualAmount = 75000.0),
                    SubscriptionItem(monthId = "2026-01", title = "ChatGPT Plus", priority = "Low", plannedAmount = 200000.0, actualAmount = 200000.0),
                    SubscriptionItem(monthId = "2026-01", title = "Spotify & YouTube", priority = "Low", plannedAmount = 85000.0, actualAmount = 85000.0)
                )
            )

            dao.insertDailyExpenses(
                listOf(
                    DailyExpenseItem(monthId = "2026-01", date = "01/01/2026", title = "Batagor", category = "Jajan", quantity = 1, unitPrice = 10000.0, totalAmount = 10000.0),
                    DailyExpenseItem(monthId = "2026-01", date = "02/01/2026", title = "Gorengan", category = "Jajan", quantity = 10, unitPrice = 2000.0, totalAmount = 20000.0),
                    DailyExpenseItem(monthId = "2026-01", date = "03/01/2026", title = "Matcha Latte", category = "Jajan", quantity = 1, unitPrice = 20000.0, totalAmount = 20000.0),
                    DailyExpenseItem(monthId = "2026-01", date = "04/01/2026", title = "Dimsum Mentai", category = "Makan", quantity = 1, unitPrice = 25000.0, totalAmount = 25000.0),
                    DailyExpenseItem(monthId = "2026-01", date = "05/01/2026", title = "Sempol Ayam", category = "Jajan", quantity = 5, unitPrice = 1000.0, totalAmount = 5000.0),
                    DailyExpenseItem(monthId = "2026-01", date = "06/01/2026", title = "Beli Gas LPG", category = "Belanja", quantity = 1, unitPrice = 22000.0, totalAmount = 22000.0),
                    DailyExpenseItem(monthId = "2026-01", date = "07/01/2026", title = "Top Up GoPay/DANA", category = "Top Up", quantity = 1, unitPrice = 200000.0, totalAmount = 200000.0),
                    DailyExpenseItem(monthId = "2026-01", date = "08/01/2026", title = "Gocar Malioboro", category = "Transport", quantity = 1, unitPrice = 35000.0, totalAmount = 35000.0)
                )
            )

            dao.insertAllocations(
                listOf(
                    BudgetPlanAllocation(monthId = "2026-01", categoryKey = "FIXED", title = "Kebutuhan Pokok (Fixed Cost)", targetPercent = 50.0, colorHex = "#6599B8"),
                    BudgetPlanAllocation(monthId = "2026-01", categoryKey = "VARIABLE", title = "Kebutuhan Variabel & Jajan", targetPercent = 25.0, colorHex = "#F4A261"),
                    BudgetPlanAllocation(monthId = "2026-01", categoryKey = "SAVINGS", title = "Tabungan & Investasi", targetPercent = 20.0, colorHex = "#74C69D"),
                    BudgetPlanAllocation(monthId = "2026-01", categoryKey = "SUBSCRIPTION", title = "Langganan & Cicilan", targetPercent = 5.0, colorHex = "#A594F9")
                )
            )
        }
    }
}
