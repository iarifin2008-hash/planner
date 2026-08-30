package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.AllocationCalculationResult
import com.example.data.model.BudgetPlanAllocation
import com.example.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetPlanningScreen(
    currentMonthName: String,
    calculations: List<AllocationCalculationResult>,
    totalIncome: Double,
    onUpdatePercent: (id: Long, newPercent: Double) -> Unit,
    onAddCustomAllocation: (title: String, percent: Double, colorHex: String) -> Unit,
    onDeleteAllocation: (BudgetPlanAllocation) -> Unit,
    onResetDefaults: () -> Unit,
    onApplyPreset: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currencyFormatter = remember {
        NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
            maximumFractionDigits = 0
        }
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedAllocForEdit by remember { mutableStateOf<BudgetPlanAllocation?>(null) }
    val totalTargetPercent = calculations.sumOf { it.allocation.targetPercent }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Perencanaan & Jatah Persenan",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = PastelSkyDark
                        )
                        Text(
                            text = "Bulan $currentMonthName • Atur batasan persen jatah pengeluaran",
                            fontSize = 11.sp,
                            color = TextSecondaryMuted
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onResetDefaults) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Preset",
                            tint = PastelSkyDark
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PastelSkyLight)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = PastelSkyPrimary,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Tambah Kategori Jatah", fontWeight = FontWeight.Bold) }
            )
        },
        containerColor = BackgroundCream,
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp)
        ) {
            // Header: Income & Allocation Summary Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(PastelCardBorder)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Total Pemasukan Acuan",
                                    fontSize = 12.sp,
                                    color = TextSecondaryMuted
                                )
                                Text(
                                    text = currencyFormatter.format(totalIncome),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = PastelSkyDark
                                )
                            }
                            // Total Percent Badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (totalTargetPercent in 99.0..100.0) PastelMintLight
                                        else if (totalTargetPercent > 100.0) Color(0xFFFFEBEE)
                                        else PastelPeachLight
                                    )
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Total: ${String.format(Locale.US, "%.1f", totalTargetPercent)}%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (totalTargetPercent in 99.0..100.0) PastelMintSavings
                                    else if (totalTargetPercent > 100.0) Color(0xFFD32F2F)
                                    else PastelPeachVar
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "💡 Jatah nominal dihitung otomatis dari total pendapatan dikali persen kategori. Saat Anda mencatat mutasi/pengeluaran, sisa jatah otomatis berkurang dan memberi peringatan jika mendekati batas.",
                            fontSize = 11.sp,
                            color = TextSecondaryMuted,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Preset Rasio Populer (1-Tap):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PastelSkyDark
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("50/30/20", "60/20/20", "70/20/10", "40/30/30").forEach { preset ->
                                OutlinedButton(
                                    onClick = { onApplyPreset(preset) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = PastelSkySurface,
                                        contentColor = PastelSkyDark
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, PastelSkyPrimary.copy(alpha = 0.4f)),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.weight(1f).height(34.dp)
                                ) {
                                    Text(preset, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // List of Category Allocations
            items(calculations, key = { it.allocation.id }) { item ->
                AllocationCategoryCard(
                    calc = item,
                    currencyFormatter = currencyFormatter,
                    onEditPercent = { selectedAllocForEdit = item.allocation },
                    onDelete = { onDeleteAllocation(item.allocation) }
                )
            }
        }
    }

    // Quick Edit Percent Dialog
    if (selectedAllocForEdit != null) {
        val alloc = selectedAllocForEdit!!
        var percentInput by remember { mutableStateOf(alloc.targetPercent.toString()) }

        Dialog(onDismissRequest = { selectedAllocForEdit = null }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Atur Jatah Persenan (%)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = PastelSkyDark
                    )
                    Text(
                        text = alloc.title,
                        fontSize = 13.sp,
                        color = TextSecondaryMuted
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = percentInput,
                        onValueChange = { percentInput = it },
                        label = { Text("Persentase (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        trailingIcon = { Text("%", fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 12.dp)) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Quick slider
                    val currentP = percentInput.toDoubleOrNull() ?: 0.0
                    Slider(
                        value = currentP.toFloat().coerceIn(0f, 100f),
                        onValueChange = { percentInput = String.format(Locale.US, "%.1f", it) },
                        valueRange = 0f..100f,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { selectedAllocForEdit = null }) {
                            Text("Batal")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val p = percentInput.toDoubleOrNull() ?: alloc.targetPercent
                                onUpdatePercent(alloc.id, p)
                                selectedAllocForEdit = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PastelSkyPrimary)
                        ) {
                            Text("Simpan Persen")
                        }
                    }
                }
            }
        }
    }

    // Add Custom Allocation Dialog
    if (showAddDialog) {
        var title by remember { mutableStateOf("") }
        var percentInput by remember { mutableStateOf("10") }

        Dialog(onDismissRequest = { showAddDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Tambah Kategori Perencanaan",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = PastelSkyDark
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Nama Kategori / Jatah Pos") },
                        placeholder = { Text("Misal: Hiburan, Dana Darurat") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = percentInput,
                        onValueChange = { percentInput = it },
                        label = { Text("Target Jatah (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        trailingIcon = { Text("%", fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 12.dp)) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showAddDialog = false }) {
                            Text("Batal")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (title.isNotBlank()) {
                                    val p = percentInput.toDoubleOrNull() ?: 10.0
                                    onAddCustomAllocation(title, p, "#F4A261")
                                    showAddDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PastelSkyPrimary)
                        ) {
                            Text("Tambah")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AllocationCategoryCard(
    calc: AllocationCalculationResult,
    currencyFormatter: NumberFormat,
    onEditPercent: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = (calc.usagePercentOfPlan / 100.0).toFloat().coerceIn(0f, 1f)
    val colorHex = calc.allocation.colorHex
    val parsedColor = try {
        Color(android.graphics.Color.parseColor(colorHex))
    } catch (e: Exception) {
        PastelSkyPrimary
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (calc.isExceeded) Color(0xFFE63946)
                else if (calc.isNearMax) Color(0xFFF4A261)
                else PastelCardBorder
            )
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Title and percentage badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(parsedColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = calc.allocation.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(parsedColor.copy(alpha = 0.15f))
                            .clickable { onEditPercent() }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${String.format(Locale.US, "%.1f", calc.allocation.targetPercent)}%",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = parsedColor
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Persen",
                                tint = parsedColor,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }

                    if (calc.allocation.categoryKey.startsWith("CUSTOM")) {
                        IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Hapus",
                                tint = Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Nominal Max Allowance vs Actual Spent
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Jatah Maksimal", fontSize = 10.sp, color = TextSecondaryMuted)
                    Text(
                        text = currencyFormatter.format(calc.maxAllowanceAmount),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Sudah Terpakai", fontSize = 10.sp, color = TextSecondaryMuted)
                    Text(
                        text = currencyFormatter.format(calc.actualSpentAmount),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (calc.isExceeded) Color(0xFFD32F2F) else TextPrimaryDark
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Sisa Saldo Jatah", fontSize = 10.sp, color = TextSecondaryMuted)
                    Text(
                        text = currencyFormatter.format(calc.remainingAmount),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (calc.remainingAmount < 0) Color(0xFFD32F2F) else PastelMintSavings
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress Bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = when {
                    calc.isExceeded -> Color(0xFFE63946)
                    calc.isNearMax -> Color(0xFFF4A261)
                    else -> parsedColor
                },
                trackColor = Color(0xFFEAEFF5)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Usage percent label & status badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Terpakai: ${String.format(Locale.US, "%.1f", calc.usagePercentOfPlan)}% dari jatah",
                    fontSize = 11.sp,
                    color = TextSecondaryMuted
                )

                when {
                    calc.isExceeded -> {
                        Text(
                            text = "🚨 MELEBIHI JATAH (${currencyFormatter.format(calc.excessAmount)})",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD32F2F)
                        )
                    }
                    calc.isNearMax -> {
                        Text(
                            text = "⚠️ HAMPIR MAKSIMAL",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE76F51)
                        )
                    }
                    else -> {
                        Text(
                            text = "✓ Aman",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PastelMintSavings
                        )
                    }
                }
            }
        }
    }
}
