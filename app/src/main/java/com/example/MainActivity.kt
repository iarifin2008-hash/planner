package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.*
import com.example.service.BackgroundVoiceService
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.viewmodel.BudgetViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: BudgetViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
            val themePreset = ThemePreset.fromId(userProfile?.themePreset ?: "SHARK_BLUE")
            val fontColorPreset = FontColorPreset.fromId(userProfile?.fontColorPreset ?: "DEEP_CHARCOAL")
            val fontScale = userProfile?.fontSizeScale ?: 1.0f

            MyApplicationTheme(
                themePreset = themePreset,
                fontColorPreset = fontColorPreset,
                fontSizeScale = fontScale
            ) {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: BudgetViewModel) {
    val context = LocalContext.current

    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val userAccount by viewModel.userAccount.collectAsStateWithLifecycle()
    val isAppUnlocked by viewModel.isAppUnlocked.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val syncStatusMessage by viewModel.syncStatusMessage.collectAsStateWithLifecycle()

    val currentMonthId by viewModel.currentMonthId.collectAsStateWithLifecycle()
    val allMonths by viewModel.allMonths.collectAsStateWithLifecycle()
    val allRecaps by viewModel.allRecaps.collectAsStateWithLifecycle()

    val overview by viewModel.financialOverview.collectAsStateWithLifecycle()
    val fixedStatus by viewModel.fixedExpenseStatus.collectAsStateWithLifecycle()
    val variableStatus by viewModel.variableExpenseStatus.collectAsStateWithLifecycle()
    val subscriptionStatus by viewModel.subscriptionStatus.collectAsStateWithLifecycle()
    val dailyStatus by viewModel.dailyExpenseStatus.collectAsStateWithLifecycle()

    val incomes by viewModel.incomes.collectAsStateWithLifecycle()
    val savings by viewModel.savings.collectAsStateWithLifecycle()
    val fixedExpenses by viewModel.fixedExpenses.collectAsStateWithLifecycle()
    val variableExpenses by viewModel.variableExpenses.collectAsStateWithLifecycle()
    val subscriptions by viewModel.subscriptions.collectAsStateWithLifecycle()
    val dailyExpenses by viewModel.dailyExpenses.collectAsStateWithLifecycle()

    val wallets by viewModel.wallets.collectAsStateWithLifecycle()
    val totalWalletBalance by viewModel.totalWalletBalance.collectAsStateWithLifecycle()

    val incomeSlices by viewModel.incomeChartSlices.collectAsStateWithLifecycle()
    val savingsSlices by viewModel.savingsChartSlices.collectAsStateWithLifecycle()
    val categoryRanks by viewModel.categoryRankings.collectAsStateWithLifecycle()
    val allocationCalcs by viewModel.allocationCalculations.collectAsStateWithLifecycle()

    // Navigation Tab state (0: Planner / Pos Anggaran, 1: Jatah Persen (%), 2: Tracker Jajan, 3: Rekapan)
    var selectedTab by remember { mutableIntStateOf(0) }

    // Dialog States
    var showImportDialog by remember { mutableStateOf(false) }
    var showNewMonthDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showManualBalanceDialog by remember { mutableStateOf(false) }
    var showVoiceAssistantDialog by remember { mutableStateOf(false) }
    var showWalletDialog by remember { mutableStateOf(false) }
    var activeWalletToEdit by remember { mutableStateOf<WalletItem?>(null) }

    var isVoiceServiceRunning by remember { mutableStateOf(false) }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val recordAudioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        if (recordAudioGranted) {
            BackgroundVoiceService.start(context)
            isVoiceServiceRunning = true
            Toast.makeText(context, "Asisten suara latar belakang aktif! Ucapkan 'Hai Plenner'", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, "Izin mikrofon dibutuhkan untuk fitur asisten suara latar belakang", Toast.LENGTH_SHORT).show()
        }
    }

    fun startVoiceService() {
        val permissionsToRequest = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissionsToRequest.toTypedArray())
    }

    fun stopVoiceService() {
        BackgroundVoiceService.stop(context)
        isVoiceServiceRunning = false
        Toast.makeText(context, "Asisten suara dihentikan", Toast.LENGTH_SHORT).show()
    }

    var activeIncomeToEdit by remember { mutableStateOf<IncomeItem?>(null) }
    var showIncomeDialog by remember { mutableStateOf(false) }

    var activeSavingToEdit by remember { mutableStateOf<SavingItem?>(null) }
    var showSavingDialog by remember { mutableStateOf(false) }

    var activeFixedToEdit by remember { mutableStateOf<FixedExpenseItem?>(null) }
    var showFixedDialog by remember { mutableStateOf(false) }

    var activeVariableToEdit by remember { mutableStateOf<VariableExpenseItem?>(null) }
    var showVariableDialog by remember { mutableStateOf(false) }

    var activeSubToEdit by remember { mutableStateOf<SubscriptionItem?>(null) }
    var showSubDialog by remember { mutableStateOf(false) }

    var activeDailyToEdit by remember { mutableStateOf<DailyExpenseItem?>(null) }
    var showDailyDialog by remember { mutableStateOf(false) }

    val currentMonthObj = allMonths.find { it.monthId == currentMonthId }
    val currentMonthName = currentMonthObj?.monthName ?: "Januari"
    val currentYear = currentMonthObj?.year ?: 2026

    // 1. Account / PIN Lock / Multi-Device Initial Setup Screen Check
    val needsAuth = !isAppUnlocked || (userAccount == null && userProfile?.name?.isBlank() == true)

    if (needsAuth) {
        AuthLockScreen(
            userProfile = userProfile,
            userAccount = userAccount,
            isSyncing = isSyncing,
            syncStatusMessage = syncStatusMessage,
            onUnlocked = { viewModel.setAppUnlocked(true) },
            onRegisterUser = { name, email, password ->
                viewModel.registerAccount(name, email, password) { success, msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }
            },
            onLoginUser = { email, password ->
                viewModel.loginAccount(email, password) { success, msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }
            },
            onConnectCode = { code ->
                viewModel.connectWithSyncCode(code) { success, msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }
            },
            onResetPin = {
                viewModel.updateProfile(userProfile?.name ?: "Sobat Cuan", "1234", "shark_happy", isPinEnabled = true)
                viewModel.setAppUnlocked(true)
            }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.cute_cashier_logo_1787992779159),
                            contentDescription = "Logo Aplikasi",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, PastelSkyPrimary, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Money Planner",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PastelSkyDark
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                if (isSyncing) {
                                    CircularProgressIndicator(modifier = Modifier.size(10.dp), strokeWidth = 1.5.dp, color = PastelSkyDark)
                                } else {
                                    Icon(
                                        Icons.Default.CloudDone,
                                        contentDescription = "Multi-device Terhubung",
                                        tint = Color(0xFF2D6A4F),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Halo, ${userProfile?.name ?: userAccount?.displayName ?: "Sobat Cuan"} • ${userAccount?.syncCode ?: "CUAN-7701"}",
                                fontSize = 10.sp,
                                color = TextSecondaryMuted
                            )
                        }
                    }
                },
                actions = {
                    // Quick Voice Assistant Button
                    IconButton(onClick = { showVoiceAssistantDialog = true }) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = "Asisten Suara AI",
                            tint = if (isVoiceServiceRunning) Color(0xFF2D6A4F) else PastelCoralFixed
                        )
                    }
                    // Quick Multi-Device Cloud Sync
                    IconButton(onClick = {
                        viewModel.triggerCloudSync { success, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.Default.Sync, contentDescription = "Sinkronkan Multi-Device", tint = PastelSkyDark)
                    }
                    if (userProfile?.isPinEnabled == true) {
                        IconButton(onClick = { viewModel.lockApp() }) {
                            Icon(Icons.Default.Lock, contentDescription = "Kunci Aplikasi Sekarang", tint = PastelSkyDark)
                        }
                    }
                    IconButton(onClick = { showImportDialog = true }) {
                        Icon(Icons.Default.CloudUpload, contentDescription = "Import Mutasi", tint = PastelSkyDark)
                    }
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Pengaturan Tema & Multi-Device", tint = PastelSkyDark)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundCream,
                    titleContentColor = PastelSkyDark
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showVoiceAssistantDialog = true },
                containerColor = PastelSkyPrimary,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Mic, contentDescription = "Asisten Suara AI") },
                text = { Text("Hai Plenner", fontWeight = FontWeight.Bold) },
                shape = RoundedCornerShape(18.dp)
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {
                        Icon(
                            if (selectedTab == 0) Icons.Filled.PieChart else Icons.Outlined.PieChart,
                            contentDescription = "Planner"
                        )
                    },
                    label = { Text("Pos Anggaran", fontSize = 10.sp, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PastelSkyPrimary,
                        selectedTextColor = PastelSkyPrimary,
                        indicatorColor = PastelSkyLight
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        Icon(
                            if (selectedTab == 1) Icons.Filled.Percent else Icons.Outlined.Percent,
                            contentDescription = "Jatah Persen"
                        )
                    },
                    label = { Text("Jatah Persen", fontSize = 10.sp, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PastelSkyPrimary,
                        selectedTextColor = PastelSkyPrimary,
                        indicatorColor = PastelSkyLight
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = {
                        Icon(
                            if (selectedTab == 2) Icons.Filled.Fastfood else Icons.Outlined.Fastfood,
                            contentDescription = "Tracker Jajan"
                        )
                    },
                    label = { Text("Tracker Jajan", fontSize = 10.sp, fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PastelPeachVar,
                        selectedTextColor = PastelPeachVar,
                        indicatorColor = PastelPeachLight
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = {
                        Icon(
                            if (selectedTab == 3) Icons.Filled.Assessment else Icons.Outlined.Assessment,
                            contentDescription = "Rekapan"
                        )
                    },
                    label = { Text("Rekapan", fontSize = 10.sp, fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PastelMintSavings,
                        selectedTextColor = PastelMintSavings,
                        indicatorColor = PastelMintLight
                    )
                )
            }
        },
        containerColor = BackgroundCream
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> {
                    BudgetDashboardScreen(
                        currentMonthName = currentMonthName,
                        currentYear = currentYear,
                        allMonths = allMonths,
                        overview = overview,
                        fixedStatus = fixedStatus,
                        variableStatus = variableStatus,
                        subscriptionStatus = subscriptionStatus,
                        dailyStatus = dailyStatus,
                        incomes = incomes,
                        savings = savings,
                        fixedExpenses = fixedExpenses,
                        variableExpenses = variableExpenses,
                        subscriptions = subscriptions,
                        dailyExpenses = dailyExpenses,
                        wallets = wallets,
                        totalWalletBalance = totalWalletBalance,
                        incomeSlices = incomeSlices,
                        savingsSlices = savingsSlices,
                        categoryRanks = categoryRanks,
                        allocations = allocationCalcs,
                        onSelectMonth = { viewModel.selectMonth(it) },
                        onOpenNewMonthDialog = { showNewMonthDialog = true },
                        onOpenImportDialog = { showImportDialog = true },
                        onOpenManualBalanceDialog = { showManualBalanceDialog = true },
                        onOpenVoiceAssistant = { showVoiceAssistantDialog = true },
                        onAddWalletClick = {
                            activeWalletToEdit = null
                            showWalletDialog = true
                        },
                        onEditWalletClick = {
                            activeWalletToEdit = it
                            showWalletDialog = true
                        },
                        onAddIncomeClick = {
                            activeIncomeToEdit = null
                            showIncomeDialog = true
                        },
                        onEditIncomeClick = {
                            activeIncomeToEdit = it
                            showIncomeDialog = true
                        },
                        onAddSavingClick = {
                            activeSavingToEdit = null
                            showSavingDialog = true
                        },
                        onEditSavingClick = {
                            activeSavingToEdit = it
                            showSavingDialog = true
                        },
                        onAddFixedClick = {
                            activeFixedToEdit = null
                            showFixedDialog = true
                        },
                        onEditFixedClick = {
                            activeFixedToEdit = it
                            showFixedDialog = true
                        },
                        onAddVariableClick = {
                            activeVariableToEdit = null
                            showVariableDialog = true
                        },
                        onEditVariableClick = {
                            activeVariableToEdit = it
                            showVariableDialog = true
                        },
                        onAddSubscriptionClick = {
                            activeSubToEdit = null
                            showSubDialog = true
                        },
                        onEditSubscriptionClick = {
                            activeSubToEdit = it
                            showSubDialog = true
                        },
                        onAddDailyClick = {
                            activeDailyToEdit = null
                            showDailyDialog = true
                        },
                        onEditDailyClick = {
                            activeDailyToEdit = it
                            showDailyDialog = true
                        }
                    )
                }
                1 -> {
                    BudgetPlanningScreen(
                        currentMonthName = currentMonthName,
                        calculations = allocationCalcs,
                        totalIncome = overview.totalIncome,
                        onUpdatePercent = { id, p -> viewModel.updateAllocationPercent(id, p) },
                        onAddCustomAllocation = { title, p, color -> viewModel.addCustomAllocation(title, p, color) },
                        onDeleteAllocation = { item -> viewModel.deleteAllocation(item) },
                        onResetDefaults = { viewModel.resetAllocationsToDefault() },
                        onApplyPreset = { preset -> viewModel.applyBudgetPreset(preset) }
                    )
                }
                2 -> {
                    DailyTrackerScreen(
                        dailyExpenses = dailyExpenses,
                        onAddClick = {
                            activeDailyToEdit = null
                            showDailyDialog = true
                        },
                        onEditClick = {
                            activeDailyToEdit = it
                            showDailyDialog = true
                        }
                    )
                }
                3 -> {
                    MonthlyRecapScreen(
                        monthName = currentMonthName,
                        year = currentYear,
                        overview = overview,
                        savedRecaps = allRecaps,
                        onSaveRecapClick = { notes -> viewModel.saveMonthlyRecap(notes) },
                        onCreateNewMonthClick = { showNewMonthDialog = true }
                    )
                }
            }
        }
    }

    // === DIALOGS ===

    // 0. AI Voice Assistant Dialog
    if (showVoiceAssistantDialog) {
        VoiceExpenseAssistantDialog(
            onDismiss = { showVoiceAssistantDialog = false },
            onConfirmTransaction = { analysis ->
                viewModel.executeVoiceTransaction(analysis) {
                    Toast.makeText(context, "Pengeluaran Rp ${analysis.amount.toLong()} berhasil dicatat & disinkronisasi!", Toast.LENGTH_SHORT).show()
                }
                showVoiceAssistantDialog = false
            }
        )
    }

    // 1. Import Bank / DANA Mutasi Transcript Dialog (PDF / Screenshot / Text)
    if (showImportDialog) {
        TranscriptImportDialog(
            currentMonthId = currentMonthId,
            onDismiss = { showImportDialog = false },
            onImportConfirmed = { transactions ->
                viewModel.importTransactions(transactions)
            }
        )
    }

    // 2. New Month Dialog
    if (showNewMonthDialog) {
        NewMonthDialog(
            onDismiss = { showNewMonthDialog = false },
            onCreate = { monthName, year, copyPrevious ->
                viewModel.createNewMonth(monthName, year, copyPrevious)
                showNewMonthDialog = false
            }
        )
    }

    // 3. Theme, Font Color, Multi-Device Sync & Voice Settings Dialog
    if (showSettingsDialog) {
        ThemeAndSettingsDialog(
            profile = userProfile,
            userAccount = userAccount,
            isSyncing = isSyncing,
            syncStatusMessage = syncStatusMessage,
            onDismiss = { showSettingsDialog = false },
            onSaveThemeSettings = { themeId, fontColorId, fontSizeScale ->
                viewModel.updateThemeSettings(themeId, fontColorId, fontSizeScale)
            },
            onSaveProfile = { name, pin, avatar, isPinEnabled ->
                viewModel.updateProfile(name, pin, avatar, isPinEnabled)
            },
            onTriggerSync = {
                viewModel.triggerCloudSync { success, msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            },
            onConnectNewDevice = { code ->
                viewModel.connectWithSyncCode(code) { success, msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }
            },
            onLogoutAccount = {
                viewModel.logoutAccount()
                showSettingsDialog = false
            },
            isVoiceServiceRunning = isVoiceServiceRunning,
            onToggleVoiceService = { enable ->
                if (enable) startVoiceService() else stopVoiceService()
            },
            onTriggerVoiceTest = {
                showSettingsDialog = false
                showVoiceAssistantDialog = true
            }
        )
    }

    // 4. Manual Balance Override Dialog
    if (showManualBalanceDialog) {
        ManualBalanceDialog(
            useManualBalance = overview.useManualBalance,
            currentManualBalance = overview.effectiveBalance,
            calculatedBalance = overview.remainingBalance,
            onDismiss = { showManualBalanceDialog = false },
            onSave = { useManual, manualAmount ->
                viewModel.setManualBalance(useManual, manualAmount)
            }
        )
    }

    // 5. Add/Edit Income Dialog
    if (showIncomeDialog) {
        AddEditIncomeDialog(
            item = activeIncomeToEdit,
            availableWallets = wallets,
            onDismiss = { showIncomeDialog = false },
            onSave = { src, type, amt, dt, wName ->
                if (activeIncomeToEdit == null) {
                    viewModel.addIncome(src, type, amt, dt, wName)
                } else {
                    viewModel.updateIncome(activeIncomeToEdit!!.copy(source = src, type = type, amount = amt, date = dt, walletName = wName))
                }
                showIncomeDialog = false
            },
            onDelete = if (activeIncomeToEdit != null) {
                {
                    viewModel.deleteIncome(activeIncomeToEdit!!)
                    showIncomeDialog = false
                }
            } else null
        )
    }

    // 6. Add/Edit Saving Dialog
    if (showSavingDialog) {
        AddEditBudgetSectionDialog(
            sectionTitle = "Tabungan & Investasi",
            titleValue = activeSavingToEdit?.title ?: "",
            priorityValue = activeSavingToEdit?.priority ?: "High",
            plannedValue = activeSavingToEdit?.plannedAmount ?: 0.0,
            actualValue = activeSavingToEdit?.actualAmount ?: 0.0,
            dateValue = activeSavingToEdit?.date ?: "",
            walletValue = activeSavingToEdit?.walletName ?: "",
            availableWallets = wallets,
            isEdit = activeSavingToEdit != null,
            onDismiss = { showSavingDialog = false },
            onSave = { title, priority, planned, actual, date, wName ->
                if (activeSavingToEdit == null) {
                    viewModel.addSaving(title, priority, planned, actual, date, wName)
                } else {
                    viewModel.updateSaving(activeSavingToEdit!!.copy(title = title, priority = priority, plannedAmount = planned, actualAmount = actual, date = date, walletName = wName))
                }
                showSavingDialog = false
            },
            onDelete = if (activeSavingToEdit != null) {
                {
                    viewModel.deleteSaving(activeSavingToEdit!!)
                    showSavingDialog = false
                }
            } else null
        )
    }

    // 7. Add/Edit Fixed Expense Dialog
    if (showFixedDialog) {
        AddEditBudgetSectionDialog(
            sectionTitle = "Pengeluaran Tetap (Fixed Cost)",
            titleValue = activeFixedToEdit?.title ?: "",
            priorityValue = activeFixedToEdit?.priority ?: "High",
            plannedValue = activeFixedToEdit?.plannedAmount ?: 0.0,
            actualValue = activeFixedToEdit?.actualAmount ?: 0.0,
            dateValue = activeFixedToEdit?.date ?: "",
            walletValue = activeFixedToEdit?.walletName ?: "",
            availableWallets = wallets,
            isEdit = activeFixedToEdit != null,
            onDismiss = { showFixedDialog = false },
            onSave = { title, priority, planned, actual, date, wName ->
                if (activeFixedToEdit == null) {
                    viewModel.addFixedExpense(title, priority, planned, actual, date, wName)
                } else {
                    viewModel.updateFixedExpense(activeFixedToEdit!!.copy(title = title, priority = priority, plannedAmount = planned, actualAmount = actual, date = date, walletName = wName))
                }
                showFixedDialog = false
            },
            onDelete = if (activeFixedToEdit != null) {
                {
                    viewModel.deleteFixedExpense(activeFixedToEdit!!)
                    showFixedDialog = false
                }
            } else null
        )
    }

    // 8. Add/Edit Variable Expense Dialog
    if (showVariableDialog) {
        AddEditBudgetSectionDialog(
            sectionTitle = "Pengeluaran Variabel",
            titleValue = activeVariableToEdit?.title ?: "",
            priorityValue = activeVariableToEdit?.priority ?: "Medium",
            plannedValue = activeVariableToEdit?.plannedAmount ?: 0.0,
            actualValue = activeVariableToEdit?.actualAmount ?: 0.0,
            dateValue = activeVariableToEdit?.date ?: "",
            walletValue = activeVariableToEdit?.walletName ?: "",
            availableWallets = wallets,
            isEdit = activeVariableToEdit != null,
            onDismiss = { showVariableDialog = false },
            onSave = { title, priority, planned, actual, date, wName ->
                if (activeVariableToEdit == null) {
                    viewModel.addVariableExpense(title, priority, planned, actual, date, wName)
                } else {
                    viewModel.updateVariableExpense(activeVariableToEdit!!.copy(title = title, priority = priority, plannedAmount = planned, actualAmount = actual, date = date, walletName = wName))
                }
                showVariableDialog = false
            },
            onDelete = if (activeVariableToEdit != null) {
                {
                    viewModel.deleteVariableExpense(activeVariableToEdit!!)
                    showVariableDialog = false
                }
            } else null
        )
    }

    // 9. Add/Edit Subscription Dialog
    if (showSubDialog) {
        AddEditBudgetSectionDialog(
            sectionTitle = "Langganan & Cicilan",
            titleValue = activeSubToEdit?.title ?: "",
            priorityValue = activeSubToEdit?.priority ?: "Low",
            plannedValue = activeSubToEdit?.plannedAmount ?: 0.0,
            actualValue = activeSubToEdit?.actualAmount ?: 0.0,
            dateValue = activeSubToEdit?.date ?: "",
            walletValue = activeSubToEdit?.walletName ?: "",
            availableWallets = wallets,
            isEdit = activeSubToEdit != null,
            onDismiss = { showSubDialog = false },
            onSave = { title, priority, planned, actual, date, wName ->
                if (activeSubToEdit == null) {
                    viewModel.addSubscription(title, priority, planned, actual, date, wName)
                } else {
                    viewModel.updateSubscription(activeSubToEdit!!.copy(title = title, priority = priority, plannedAmount = planned, actualAmount = actual, date = date, walletName = wName))
                }
                showSubDialog = false
            },
            onDelete = if (activeSubToEdit != null) {
                {
                    viewModel.deleteSubscription(activeSubToEdit!!)
                    showSubDialog = false
                }
            } else null
        )
    }

    // 10. Add/Edit Daily Expense Dialog
    if (showDailyDialog) {
        AddEditDailyExpenseDialog(
            item = activeDailyToEdit,
            availableWallets = wallets,
            onDismiss = { showDailyDialog = false },
            onSave = { date, title, category, quantity, unitPrice, wName ->
                if (activeDailyToEdit == null) {
                    viewModel.addDailyExpense(date, title, category, quantity, unitPrice, wName)
                } else {
                    viewModel.updateDailyExpense(activeDailyToEdit!!.copy(date = date, title = title, category = category, quantity = quantity, unitPrice = unitPrice, totalAmount = quantity * unitPrice, walletName = wName))
                }
                showDailyDialog = false
            },
            onDelete = if (activeDailyToEdit != null) {
                {
                    viewModel.deleteDailyExpense(activeDailyToEdit!!)
                    showDailyDialog = false
                }
            } else null
        )
    }

    // 11. Add/Edit Wallet / Sumber Kas Dialog
    if (showWalletDialog) {
        AddEditWalletDialog(
            item = activeWalletToEdit,
            onDismiss = { showWalletDialog = false },
            onSave = { name, type, balance, colorHex, iconName ->
                if (activeWalletToEdit == null) {
                    viewModel.addWallet(name, type, balance, colorHex, iconName)
                } else {
                    viewModel.updateWallet(activeWalletToEdit!!.copy(name = name, type = type, balance = balance, colorHex = colorHex, iconName = iconName))
                }
                showWalletDialog = false
            },
            onDelete = if (activeWalletToEdit != null) {
                {
                    viewModel.deleteWallet(activeWalletToEdit!!)
                    showWalletDialog = false
                }
            } else null
        )
    }
}
