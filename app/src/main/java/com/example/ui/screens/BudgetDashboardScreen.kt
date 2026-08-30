package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.util.CurrencyUtils
import java.util.Locale

@Composable
fun BudgetDashboardScreen(
    currentMonthName: String,
    currentYear: Int,
    allMonths: List<BudgetMonth>,
    overview: FinancialOverview,
    fixedStatus: CategoryBudgetStatus,
    variableStatus: CategoryBudgetStatus,
    subscriptionStatus: CategoryBudgetStatus,
    dailyStatus: CategoryBudgetStatus,
    incomes: List<IncomeItem>,
    savings: List<SavingItem>,
    fixedExpenses: List<FixedExpenseItem>,
    variableExpenses: List<VariableExpenseItem>,
    subscriptions: List<SubscriptionItem>,
    dailyExpenses: List<DailyExpenseItem> = emptyList(),
    wallets: List<WalletItem> = emptyList(),
    totalWalletBalance: Double = 0.0,
    incomeSlices: List<ChartSlice>,
    savingsSlices: List<ChartSlice>,
    categoryRanks: List<CategoryRank>,
    allocations: List<AllocationCalculationResult> = emptyList(),
    onSelectMonth: (String) -> Unit,
    onOpenNewMonthDialog: () -> Unit,
    onOpenImportDialog: () -> Unit,
    onOpenManualBalanceDialog: () -> Unit,
    onOpenVoiceAssistant: () -> Unit = {},
    onAddWalletClick: () -> Unit = {},
    onEditWalletClick: (WalletItem) -> Unit = {},
    onAddIncomeClick: () -> Unit,
    onEditIncomeClick: (IncomeItem) -> Unit,
    onAddSavingClick: () -> Unit,
    onEditSavingClick: (SavingItem) -> Unit,
    onAddFixedClick: () -> Unit,
    onEditFixedClick: (FixedExpenseItem) -> Unit,
    onAddVariableClick: () -> Unit,
    onEditVariableClick: (VariableExpenseItem) -> Unit,
    onAddSubscriptionClick: () -> Unit,
    onEditSubscriptionClick: (SubscriptionItem) -> Unit,
    onAddDailyClick: () -> Unit = {},
    onEditDailyClick: (DailyExpenseItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var monthDropdownExpanded by remember { mutableStateOf(false) }

    val fixedAlloc = allocations.find { it.allocation.categoryKey == "FIXED" }
    val varAlloc = allocations.find { it.allocation.categoryKey == "VARIABLE" }
    val savAlloc = allocations.find { it.allocation.categoryKey == "SAVINGS" }
    val subAlloc = allocations.find { it.allocation.categoryKey == "SUBSCRIPTION" }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp)
    ) {
        // 1. Month Selector & App Logo Banner & Voice AI Button
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = PastelSkySurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(PastelCardBorder)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Month Dropdown
                    Box {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { monthDropdownExpanded = true }
                                .background(Color.White)
                                .border(1.dp, PastelCardBorder, RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = PastelSkyPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "$currentMonthName $currentYear",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = PastelSkyDark
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = PastelSkyDark)
                        }

                        DropdownMenu(
                            expanded = monthDropdownExpanded,
                            onDismissRequest = { monthDropdownExpanded = false }
                        ) {
                            allMonths.forEach { m ->
                                DropdownMenuItem(
                                    text = { Text("${m.monthName} ${m.year}") },
                                    onClick = {
                                        onSelectMonth(m.monthId)
                                        monthDropdownExpanded = false
                                    }
                                )
                            }
                            Divider()
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Add, contentDescription = null, tint = PastelSkyPrimary)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Tambah Bulan Baru", color = PastelSkyPrimary, fontWeight = FontWeight.Bold)
                                    }
                                },
                                onClick = {
                                    monthDropdownExpanded = false
                                    onOpenNewMonthDialog()
                                }
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Voice Assistant Button
                        Button(
                            onClick = onOpenVoiceAssistant,
                            colors = ButtonDefaults.buttonColors(containerColor = PastelCoralFixed),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Suara AI", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // Import Button
                        Button(
                            onClick = onOpenImportDialog,
                            colors = ButtonDefaults.buttonColors(containerColor = PastelSkyPrimary),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Import", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // 2. Four Key Summary Cards
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SummaryMetricCard(
                        title = "Pendapatan Bulanan",
                        amount = overview.totalIncome,
                        subtitle = "Sumber Dana & Kas (${wallets.size} Dompet)",
                        backgroundColor = Color(0xFFF4FAFE),
                        borderColor = PastelCardBorder,
                        accentColor = PastelSkyPrimary,
                        icon = Icons.Default.AccountBalanceWallet,
                        modifier = Modifier.weight(1f)
                    )

                    // Sisa Uang / Saldo (With Manual Edit Capability)
                    SummaryMetricCard(
                        title = if (overview.useManualBalance) "Saldo Riil (Manual)" else "Sisa Uang Bulan Ini",
                        amount = overview.effectiveBalance,
                        subtitle = if (overview.useManualBalance) "Mode Manual Aktif ✎" else "Saldo Bersih (${CurrencyUtils.formatPercent(overview.remainingBudgetPercent)}) ✎",
                        backgroundColor = if (overview.effectiveBalance >= 0) Color(0xFFF0FDF4) else Color(0xFFFFF1F2),
                        borderColor = if (overview.effectiveBalance >= 0) PastelMintLight else Color(0xFFFFD8D8),
                        accentColor = if (overview.effectiveBalance >= 0) PastelMintSavings else Color(0xFFE63946),
                        icon = Icons.Default.Savings,
                        onClick = onOpenManualBalanceDialog,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SummaryMetricCard(
                        title = "Tabungan Bulan ini",
                        amount = overview.totalSavingActual,
                        subtitle = "Target: ${CurrencyUtils.formatRupiahShort(overview.totalSavingPlanned)}",
                        backgroundColor = Color(0xFFF0FFF4),
                        borderColor = PastelMintLight,
                        accentColor = PastelMintSavings,
                        icon = Icons.Default.TrendingUp,
                        modifier = Modifier.weight(1f)
                    )

                    SummaryMetricCard(
                        title = "Total Pengeluaran",
                        amount = overview.totalActualExpense,
                        subtitle = "Fixed + Var + Subs + Jajan",
                        backgroundColor = Color(0xFFFFF5F5),
                        borderColor = PastelCoralLight,
                        accentColor = PastelCoralFixed,
                        icon = Icons.Default.ReceiptLong,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 3. Sumber Dana & Dompet Kas Section (Cash, DANA, Saldo Rekening, GoPay, dll.)
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(Color(0xFF118EEA), PastelSkyPrimary))),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color(0xFF118EEA), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Sumber Dana & Dompet Kas",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PastelSkyDark
                                )
                                Text(
                                    text = "Total Saldo: ${CurrencyUtils.formatRupiah(totalWalletBalance)}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF2D6A4F)
                                )
                            }
                        }

                        Button(
                            onClick = onAddWalletClick,
                            colors = ButtonDefaults.buttonColors(containerColor = PastelSkyPrimary),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Tambah", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Wallets List Chips / Grid
                    if (wallets.isEmpty()) {
                        EmptyRow("Belum ada dompet kas. Tekan '+ Tambah' untuk membuat.")
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            wallets.chunked(2).forEach { rowWallets ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    rowWallets.forEach { wallet ->
                                        val cardColor = try {
                                            Color(android.graphics.Color.parseColor(wallet.colorHex))
                                        } catch (e: Exception) {
                                            Color(0xFF118EEA)
                                        }

                                        Card(
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = cardColor.copy(alpha = 0.08f)),
                                            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(cardColor.copy(alpha = 0.35f))),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { onEditWalletClick(wallet) }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(8.dp)
                                                                .clip(CircleShape)
                                                                .background(cardColor)
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            text = wallet.name,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = TextPrimaryDark,
                                                            maxLines = 1
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = CurrencyUtils.formatRupiah(wallet.balance),
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = cardColor
                                                    )
                                                }
                                                Icon(
                                                    Icons.Default.Edit,
                                                    contentDescription = "Edit Saldo",
                                                    tint = cardColor.copy(alpha = 0.7f),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                    if (rowWallets.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Percentage Sync Overview Card (Sinkronisasi Jatah Persen %)
        if (allocations.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(PastelSkyPrimary, PastelMintSavings))),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.SyncAlt, contentDescription = null, tint = PastelSkyPrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Sinkronisasi Jatah Persenan (%)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PastelSkyDark
                                )
                            }
                            Text(
                                text = "Tersinkron Otomatis",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PastelMintSavings
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            allocations.take(4).forEach { alloc ->
                                val color = when (alloc.allocation.categoryKey) {
                                    "FIXED" -> PastelCoralFixed
                                    "SAVINGS" -> PastelMintSavings
                                    "SUBSCRIPTION" -> PastelLilacSub
                                    else -> PastelPeachVar
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(color.copy(alpha = 0.08f))
                                        .border(0.5.dp, color.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                        .padding(6.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = alloc.allocation.title.take(8),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimaryDark,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "${alloc.allocation.targetPercent}%",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = color
                                        )
                                        Text(
                                            text = "Sisa: ${CurrencyUtils.formatRupiahShort(alloc.remainingAmount)}",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (alloc.remainingAmount < 0) Color(0xFFD32F2F) else TextSecondaryMuted
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Visual Interactive Gauge Ring & Bar Comparison
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(PastelCardBorder)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Grafik Sinkronisasi Keuangan",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = PastelSkyDark
                    )
                    Text(
                        text = "Grafik berkurang sinkron saat pengeluaran bertambah",
                        fontSize = 10.sp,
                        color = TextSecondaryMuted
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    SynchronizedBudgetGauge(
                        totalIncome = overview.totalIncome,
                        totalExpense = overview.totalActualExpense,
                        totalSavings = overview.totalSavingActual,
                        remainingBalance = overview.effectiveBalance,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        LegendPill(color = PastelSkyPrimary, label = "Sisa Uang")
                        LegendPill(color = PastelMintSavings, label = "Tabungan")
                        LegendPill(color = PastelCoralFixed, label = "Pengeluaran")
                    }
                }
            }
        }

        // 5. Donut Charts (Income Breakdown & Savings Breakdown)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DonutChartCard(
                    title = "Chart Pendapatan",
                    slices = incomeSlices,
                    totalLabel = "Total Income",
                    modifier = Modifier.weight(1f)
                )

                DonutChartCard(
                    title = "Chart Tabungan",
                    slices = savingsSlices,
                    totalLabel = "Total Tabungan",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 6. Monthly Bar Comparison
        item {
            MonthlyBarComparisonCard(
                fixedActual = overview.totalFixedActual,
                fixedPlanned = overview.totalFixedPlanned,
                variableActual = overview.totalVariableActual,
                variablePlanned = overview.totalVariablePlanned,
                subActual = overview.totalSubActual,
                subPlanned = overview.totalSubPlanned
            )
        }

        // 7. SECTION 1: Pendapatan / Income Table
        item {
            SpreadsheetCard(
                title = "Pendapatan / Income",
                subtitle = "Catatan semua sumber pemasukan bulan ini",
                totalAmount = overview.totalIncome,
                icon = Icons.Default.Payments,
                themeColor = PastelSkyPrimary,
                onAddClick = onAddIncomeClick
            ) {
                if (incomes.isEmpty()) {
                    EmptyRow("Belum ada data pendapatan. Tekan '+' untuk menambah.")
                } else {
                    incomes.forEach { item ->
                        val percent = if (overview.totalIncome > 0) (item.amount / overview.totalIncome) * 100.0 else 0.0
                        SpreadsheetRow(
                            col1 = item.source,
                            col2 = item.type,
                            col3 = CurrencyUtils.formatRupiah(item.amount),
                            col4 = CurrencyUtils.formatPercent(percent),
                            date = item.date,
                            walletName = item.walletName,
                            onClick = { onEditIncomeClick(item) }
                        )
                    }
                }
            }
        }

        // 8. SECTION 2: Nabung / Saving Table (With Jatah Persen Sync)
        item {
            SpreadsheetCard(
                title = "Nabung / Saving & Investasi",
                subtitle = "Target tabungan, dana darurat, dan investasi",
                totalAmount = overview.totalSavingActual,
                totalPlanned = overview.totalSavingPlanned,
                icon = Icons.Default.AccountBalance,
                themeColor = PastelMintSavings,
                onAddClick = onAddSavingClick
            ) {
                // Section Sync Banner
                if (savAlloc != null) {
                    SectionAllocationSyncBanner(
                        alloc = savAlloc,
                        themeColor = PastelMintSavings,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                if (savings.isEmpty()) {
                    EmptyRow("Belum ada pos tabungan. Tekan '+' untuk menambah.")
                } else {
                    savings.forEach { item ->
                        val percent = if (overview.totalIncome > 0) (item.actualAmount / overview.totalIncome) * 100.0 else 0.0
                        PlanVsActualRow(
                            title = item.title,
                            priority = item.priority,
                            planned = item.plannedAmount,
                            actual = item.actualAmount,
                            percent = percent,
                            date = item.date,
                            walletName = item.walletName,
                            onClick = { onEditSavingClick(item) }
                        )
                    }
                }
            }
        }

        // 9. SECTION 3: Pengeluaran Tetap / Fixed Cost Table (With Limit & Jatah Persen Sync)
        item {
            SpreadsheetCard(
                title = "Pengeluaran Tetap / Fixed Cost",
                subtitle = "Sewa kos, listrik, air, wifi, kebutuhan pokok",
                totalAmount = overview.totalFixedActual,
                totalPlanned = overview.totalFixedPlanned,
                icon = Icons.Default.Home,
                themeColor = PastelCoralFixed,
                onAddClick = onAddFixedClick
            ) {
                // Section Sync Banner from Jatah Persen
                if (fixedAlloc != null) {
                    SectionAllocationSyncBanner(
                        alloc = fixedAlloc,
                        themeColor = PastelCoralFixed,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                } else {
                    CategoryAllowanceWarningCard(
                        status = fixedStatus,
                        themeColor = PastelCoralFixed,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                if (fixedExpenses.isEmpty()) {
                    EmptyRow("Belum ada pengeluaran tetap. Tekan '+' untuk menambah.")
                } else {
                    fixedExpenses.forEach { item ->
                        val percent = if (overview.totalIncome > 0) (item.actualAmount / overview.totalIncome) * 100.0 else 0.0
                        PlanVsActualRow(
                            title = item.title,
                            priority = item.priority,
                            planned = item.plannedAmount,
                            actual = item.actualAmount,
                            percent = percent,
                            date = item.date,
                            walletName = item.walletName,
                            onClick = { onEditFixedClick(item) }
                        )
                    }
                }
            }
        }

        // 10. SECTION 4: Pengeluaran Tidak Tetap / Variable Cost Table (With Jatah Persen Sync)
        item {
            SpreadsheetCard(
                title = "Pengeluaran Tidak Tetap / Variable Cost",
                subtitle = "Uang makan, transport, bensin, belanja, nongkrong",
                totalAmount = overview.totalVariableActual,
                totalPlanned = overview.totalVariablePlanned,
                icon = Icons.Default.ShoppingCart,
                themeColor = PastelPeachVar,
                onAddClick = onAddVariableClick
            ) {
                // Section Sync Banner from Jatah Persen
                if (varAlloc != null) {
                    SectionAllocationSyncBanner(
                        alloc = varAlloc,
                        themeColor = PastelPeachVar,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                } else {
                    CategoryAllowanceWarningCard(
                        status = variableStatus,
                        themeColor = PastelPeachVar,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                if (variableExpenses.isEmpty()) {
                    EmptyRow("Belum ada pengeluaran variabel. Tekan '+' untuk menambah.")
                } else {
                    variableExpenses.forEach { item ->
                        val percent = if (overview.totalIncome > 0) (item.actualAmount / overview.totalIncome) * 100.0 else 0.0
                        PlanVsActualRow(
                            title = item.title,
                            priority = item.priority,
                            planned = item.plannedAmount,
                            actual = item.actualAmount,
                            percent = percent,
                            date = item.date,
                            walletName = item.walletName,
                            onClick = { onEditVariableClick(item) }
                        )
                    }
                }
            }
        }

        // 11. SECTION 5: Biaya Langganan & Hutang / Subscription & Debt (With Jatah Persen Sync)
        item {
            SpreadsheetCard(
                title = "Biaya Langganan & Hutang / Subscriptions",
                subtitle = "Canva, Netflix, Spotify, ChatGPT, cicilan",
                totalAmount = overview.totalSubActual,
                totalPlanned = overview.totalSubPlanned,
                icon = Icons.Default.CreditCard,
                themeColor = PastelLilacSub,
                onAddClick = onAddSubscriptionClick
            ) {
                // Section Sync Banner from Jatah Persen
                if (subAlloc != null) {
                    SectionAllocationSyncBanner(
                        alloc = subAlloc,
                        themeColor = PastelLilacSub,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                } else {
                    CategoryAllowanceWarningCard(
                        status = subscriptionStatus,
                        themeColor = PastelLilacSub,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                if (subscriptions.isEmpty()) {
                    EmptyRow("Belum ada biaya langganan. Tekan '+' untuk menambah.")
                } else {
                    subscriptions.forEach { item ->
                        val percent = if (overview.totalIncome > 0) (item.actualAmount / overview.totalIncome) * 100.0 else 0.0
                        PlanVsActualRow(
                            title = item.title,
                            priority = item.priority,
                            planned = item.plannedAmount,
                            actual = item.actualAmount,
                            percent = percent,
                            date = item.date,
                            walletName = item.walletName,
                            onClick = { onEditSubscriptionClick(item) }
                        )
                    }
                }
            }
        }

        // 12. SECTION 6: Pos Jajan & Pengeluaran Harian (Daily Expense / Suara AI)
        item {
            SpreadsheetCard(
                title = "Pos Jajan & Pengeluaran Harian (Harian / Suara AI)",
                subtitle = "Makan siang, kopi, bensin, cemilan, transaksi suara AI",
                totalAmount = overview.totalDailyExpense,
                totalPlanned = null,
                icon = Icons.Default.Fastfood,
                themeColor = Color(0xFFF77F00),
                onAddClick = onAddDailyClick
            ) {
                if (dailyExpenses.isEmpty()) {
                    EmptyRow("Belum ada catatan jajan/harian. Ucapkan ke Suara AI atau tekan '+'")
                } else {
                    dailyExpenses.forEach { item ->
                        SpreadsheetRow(
                            col1 = "${item.title} (${item.quantity}x)",
                            col2 = item.category,
                            col3 = CurrencyUtils.formatRupiah(item.totalAmount),
                            col4 = if (item.notes.isNotBlank()) item.notes.take(15) else "Jajan",
                            date = item.date,
                            walletName = item.walletName,
                            onClick = { onEditDailyClick(item) }
                        )
                    }
                }
            }
        }

        // 13. Category Ranking (Peringkat Pengeluaran)
        item {
            CategoryRankingCard(ranks = categoryRanks)
        }
    }
}

@Composable
private fun SectionAllocationSyncBanner(
    alloc: AllocationCalculationResult,
    themeColor: Color,
    modifier: Modifier = Modifier
) {
    val isOver = alloc.remainingAmount < 0
    val isNear = alloc.isNearMax && !isOver

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOver) Color(0xFFFFF0F0) else if (isNear) Color(0xFFFFFDF0) else Color(0xFFF6FAFD)
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (isOver) Color(0xFFE63946) else if (isNear) Color(0xFFF4A261) else themeColor.copy(alpha = 0.4f)
            )
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isOver) Icons.Default.Warning else Icons.Default.Percent,
                        contentDescription = null,
                        tint = if (isOver) Color(0xFFE63946) else themeColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Jatah Persen: ${alloc.allocation.targetPercent}% (${CurrencyUtils.formatRupiahShort(alloc.maxAllowanceAmount)})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isOver) Color(0xFFD32F2F) else TextPrimaryDark
                    )
                }

                // Status chip
                val statusText = when {
                    isOver -> "🚨 Melebihi Jatah!"
                    isNear -> "⚠️ Hampir Habis"
                    else -> "✓ Sisa Aman"
                }
                val statusColor = when {
                    isOver -> Color(0xFFD32F2F)
                    isNear -> Color(0xFFE76F51)
                    else -> Color(0xFF2D6A4F)
                }
                Text(
                    text = statusText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { (alloc.usagePercentOfPlan / 100f).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (isOver) Color(0xFFE63946) else if (isNear) Color(0xFFF4A261) else themeColor,
                trackColor = Color(0xFFE5EBF0)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Terpakai: ${CurrencyUtils.formatRupiah(alloc.actualSpentAmount)} (${String.format(Locale.US, "%.1f", alloc.usagePercentOfPlan)}%)",
                    fontSize = 10.sp,
                    color = TextSecondaryMuted
                )
                Text(
                    text = "Sisa Jatah: ${CurrencyUtils.formatRupiah(alloc.remainingAmount)}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isOver) Color(0xFFD32F2F) else PastelSkyDark
                )
            }
        }
    }
}

@Composable
private fun SpreadsheetCard(
    title: String,
    subtitle: String,
    totalAmount: Double,
    totalPlanned: Double? = null,
    icon: ImageVector,
    themeColor: Color,
    onAddClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(PastelCardBorder)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            TableSectionHeader(
                title = title,
                subtitle = subtitle,
                totalPlanned = totalPlanned,
                totalActual = totalAmount,
                icon = icon,
                themeColor = themeColor,
                onAddClick = onAddClick
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                content = content
            )
        }
    }
}

@Composable
private fun SpreadsheetRow(
    col1: String,
    col2: String,
    col3: String,
    col4: String,
    date: String = "",
    walletName: String = "",
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .background(Color(0xFFFAFDFF))
            .border(0.5.dp, Color(0xFFE2EFF9), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1.2f)) {
            Text(col1, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextPrimaryDark)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                if (walletName.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFE0F2FE))
                            .padding(horizontal = 5.dp, vertical = 1.5.dp)
                    ) {
                        Text(walletName, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0369A1))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                if (date.isNotBlank()) {
                    Text("📅 $date", fontSize = 9.sp, color = TextSecondaryMuted)
                }
            }
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(PastelSkyLight)
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(col2, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = PastelSkyDark)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(col3, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PastelSkyDark, modifier = Modifier.weight(1f))
        Text(col4, fontSize = 10.sp, color = TextSecondaryMuted)
    }
}

@Composable
private fun PlanVsActualRow(
    title: String,
    priority: String,
    planned: Double,
    actual: Double,
    percent: Double,
    date: String = "",
    walletName: String = "",
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .background(Color(0xFFFAFDFF))
            .border(0.5.dp, Color(0xFFE2EFF9), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1.2f)) {
            Text(title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextPrimaryDark, maxLines = 1)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                PriorityBadge(priority = priority)
                if (walletName.isNotBlank()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFE0F2FE))
                            .padding(horizontal = 5.dp, vertical = 1.5.dp)
                    ) {
                        Text(walletName, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0369A1))
                    }
                }
                if (date.isNotBlank()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("📅 $date", fontSize = 9.sp, color = TextSecondaryMuted)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(CurrencyUtils.formatPercent(percent), fontSize = 9.sp, color = TextSecondaryMuted)
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                CurrencyUtils.formatRupiah(actual),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = PastelSkyDark
            )
            if (planned > 0) {
                Text(
                    "Rcn: ${CurrencyUtils.formatRupiahShort(planned)}",
                    fontSize = 9.sp,
                    color = TextSecondaryMuted
                )
            }
        }
    }
}

@Composable
private fun EmptyRow(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(message, fontSize = 11.sp, color = TextCaption)
    }
}

@Composable
private fun LegendPill(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, fontSize = 10.sp, color = TextSecondaryMuted)
    }
}
