package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.*

@Database(
    entities = [
        WalletItem::class,
        UserAccount::class,
        UserProfile::class,
        BudgetMonth::class,
        IncomeItem::class,
        SavingItem::class,
        FixedExpenseItem::class,
        VariableExpenseItem::class,
        SubscriptionItem::class,
        DailyExpenseItem::class,
        MonthlyRecap::class,
        BudgetPlanAllocation::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun budgetDao(): BudgetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "money_planner_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
