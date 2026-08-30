package com.example.data.db

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    // User Profile
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun getUserProfile(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profile WHERE id = 1")
    suspend fun getUserProfileOnce(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserProfile(profile: UserProfile)

    // Wallets / Sumber Anggaran Kas
    @Query("SELECT * FROM wallet_items ORDER BY id ASC")
    fun getAllWallets(): Flow<List<WalletItem>>

    @Query("SELECT * FROM wallet_items ORDER BY id ASC")
    suspend fun getAllWalletsOnce(): List<WalletItem>

    @Query("SELECT * FROM wallet_items WHERE id = :id")
    suspend fun getWalletById(id: Long): WalletItem?

    @Query("SELECT * FROM wallet_items WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun getWalletByName(name: String): WalletItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWallet(item: WalletItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWallets(items: List<WalletItem>)

    @Update
    suspend fun updateWallet(item: WalletItem)

    @Delete
    suspend fun deleteWallet(item: WalletItem)

    @Query("UPDATE wallet_items SET balance = :newBalance WHERE id = :id")
    suspend fun updateWalletBalance(id: Long, newBalance: Double)

    // Budget Months
    @Query("SELECT * FROM budget_months ORDER BY monthId DESC")
    fun getAllMonths(): Flow<List<BudgetMonth>>

    @Query("SELECT * FROM budget_months WHERE monthId = :monthId")
    suspend fun getMonthById(monthId: String): BudgetMonth?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMonth(month: BudgetMonth)

    @Delete
    suspend fun deleteMonth(month: BudgetMonth)

    // Income
    @Query("SELECT * FROM income_items WHERE monthId = :monthId ORDER BY id ASC")
    fun getIncomesForMonth(monthId: String): Flow<List<IncomeItem>>

    @Query("SELECT * FROM income_items WHERE monthId = :monthId ORDER BY id ASC")
    suspend fun getIncomesForMonthOnce(monthId: String): List<IncomeItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncome(item: IncomeItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncomes(items: List<IncomeItem>)

    @Update
    suspend fun updateIncome(item: IncomeItem)

    @Delete
    suspend fun deleteIncome(item: IncomeItem)

    // Savings
    @Query("SELECT * FROM saving_items WHERE monthId = :monthId ORDER BY id ASC")
    fun getSavingsForMonth(monthId: String): Flow<List<SavingItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaving(item: SavingItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavings(items: List<SavingItem>)

    @Update
    suspend fun updateSaving(item: SavingItem)

    @Delete
    suspend fun deleteSaving(item: SavingItem)

    // Fixed Expenses
    @Query("SELECT * FROM fixed_expense_items WHERE monthId = :monthId ORDER BY id ASC")
    fun getFixedExpensesForMonth(monthId: String): Flow<List<FixedExpenseItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFixedExpense(item: FixedExpenseItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFixedExpenses(items: List<FixedExpenseItem>)

    @Update
    suspend fun updateFixedExpense(item: FixedExpenseItem)

    @Delete
    suspend fun deleteFixedExpense(item: FixedExpenseItem)

    // Variable Expenses
    @Query("SELECT * FROM variable_expense_items WHERE monthId = :monthId ORDER BY id ASC")
    fun getVariableExpensesForMonth(monthId: String): Flow<List<VariableExpenseItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVariableExpense(item: VariableExpenseItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVariableExpenses(items: List<VariableExpenseItem>)

    @Update
    suspend fun updateVariableExpense(item: VariableExpenseItem)

    @Delete
    suspend fun deleteVariableExpense(item: VariableExpenseItem)

    // Subscriptions
    @Query("SELECT * FROM subscription_items WHERE monthId = :monthId ORDER BY id ASC")
    fun getSubscriptionsForMonth(monthId: String): Flow<List<SubscriptionItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscription(item: SubscriptionItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscriptions(items: List<SubscriptionItem>)

    @Update
    suspend fun updateSubscription(item: SubscriptionItem)

    @Delete
    suspend fun deleteSubscription(item: SubscriptionItem)

    // Daily Expenses
    @Query("SELECT * FROM daily_expense_items WHERE monthId = :monthId ORDER BY id DESC")
    fun getDailyExpensesForMonth(monthId: String): Flow<List<DailyExpenseItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyExpense(item: DailyExpenseItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyExpenses(items: List<DailyExpenseItem>)

    @Update
    suspend fun updateDailyExpense(item: DailyExpenseItem)

    @Delete
    suspend fun deleteDailyExpense(item: DailyExpenseItem)

    // Monthly Recaps
    @Query("SELECT * FROM monthly_recaps ORDER BY monthId DESC")
    fun getAllRecaps(): Flow<List<MonthlyRecap>>

    @Query("SELECT * FROM monthly_recaps WHERE monthId = :monthId")
    suspend fun getRecapForMonth(monthId: String): MonthlyRecap?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveRecap(recap: MonthlyRecap)

    @Delete
    suspend fun deleteRecap(recap: MonthlyRecap)

    // Budget Plan Percentage Allocations
    @Query("SELECT * FROM budget_plan_allocations WHERE monthId = :monthId ORDER BY id ASC")
    fun getAllocationsForMonth(monthId: String): Flow<List<BudgetPlanAllocation>>

    @Query("SELECT * FROM budget_plan_allocations WHERE monthId = :monthId ORDER BY id ASC")
    suspend fun getAllocationsForMonthOnce(monthId: String): List<BudgetPlanAllocation>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllocation(item: BudgetPlanAllocation)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllocations(items: List<BudgetPlanAllocation>)

    @Update
    suspend fun updateAllocation(item: BudgetPlanAllocation)

    @Delete
    suspend fun deleteAllocation(item: BudgetPlanAllocation)

    @Query("DELETE FROM budget_plan_allocations WHERE monthId = :monthId")
    suspend fun clearAllocationsForMonth(monthId: String)

    // Cascade clear for a month if user resets
    @Query("DELETE FROM income_items WHERE monthId = :monthId")
    suspend fun clearIncomesForMonth(monthId: String)

    @Query("DELETE FROM saving_items WHERE monthId = :monthId")
    suspend fun clearSavingsForMonth(monthId: String)

    @Query("DELETE FROM fixed_expense_items WHERE monthId = :monthId")
    suspend fun clearFixedExpensesForMonth(monthId: String)

    @Query("DELETE FROM variable_expense_items WHERE monthId = :monthId")
    suspend fun clearVariableExpensesForMonth(monthId: String)

    @Query("DELETE FROM subscription_items WHERE monthId = :monthId")
    suspend fun clearSubscriptionsForMonth(monthId: String)

    @Query("DELETE FROM daily_expense_items WHERE monthId = :monthId")
    suspend fun clearDailyExpensesForMonth(monthId: String)

    // User Account & Multi-Device Sync Queries
    @Query("SELECT * FROM user_accounts WHERE id = 1")
    fun getUserAccount(): Flow<UserAccount?>

    @Query("SELECT * FROM user_accounts WHERE id = 1")
    suspend fun getUserAccountOnce(): UserAccount?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserAccount(account: UserAccount)

    // Bulk queries for complete Cloud Sync Payload across devices
    @Query("SELECT * FROM budget_months")
    suspend fun getAllMonthsOnce(): List<BudgetMonth>

    @Query("SELECT * FROM income_items")
    suspend fun getAllIncomesOnce(): List<IncomeItem>

    @Query("SELECT * FROM saving_items")
    suspend fun getAllSavingsOnce(): List<SavingItem>

    @Query("SELECT * FROM fixed_expense_items")
    suspend fun getAllFixedExpensesOnce(): List<FixedExpenseItem>

    @Query("SELECT * FROM variable_expense_items")
    suspend fun getAllVariableExpensesOnce(): List<VariableExpenseItem>

    @Query("SELECT * FROM subscription_items")
    suspend fun getAllSubscriptionsOnce(): List<SubscriptionItem>

    @Query("SELECT * FROM daily_expense_items")
    suspend fun getAllDailyExpensesOnce(): List<DailyExpenseItem>

    @Query("SELECT * FROM monthly_recaps")
    suspend fun getAllRecapsOnce(): List<MonthlyRecap>

    @Query("SELECT * FROM budget_plan_allocations")
    suspend fun getAllAllocationsOnce(): List<BudgetPlanAllocation>

    @Transaction
    suspend fun restoreFullSyncPayload(payload: SyncPayload) {
        payload.userProfile?.let { saveUserProfile(it) }
        payload.months.forEach { insertMonth(it) }
        if (payload.wallets.isNotEmpty()) insertWallets(payload.wallets)
        if (payload.incomes.isNotEmpty()) insertIncomes(payload.incomes)
        if (payload.savings.isNotEmpty()) insertSavings(payload.savings)
        if (payload.fixedExpenses.isNotEmpty()) insertFixedExpenses(payload.fixedExpenses)
        if (payload.variableExpenses.isNotEmpty()) insertVariableExpenses(payload.variableExpenses)
        if (payload.subscriptions.isNotEmpty()) insertSubscriptions(payload.subscriptions)
        if (payload.dailyExpenses.isNotEmpty()) insertDailyExpenses(payload.dailyExpenses)
        payload.monthlyRecaps.forEach { saveRecap(it) }
        if (payload.budgetPlanAllocations.isNotEmpty()) insertAllocations(payload.budgetPlanAllocations)
    }
}
