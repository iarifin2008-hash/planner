package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FinancialOverview
import com.example.data.model.MonthlyRecap
import com.example.ui.theme.*
import com.example.util.CurrencyUtils

@Composable
fun MonthlyRecapScreen(
    monthName: String,
    year: Int,
    overview: FinancialOverview,
    savedRecaps: List<MonthlyRecap>,
    onSaveRecapClick: (notes: String) -> Unit,
    onCreateNewMonthClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var recapNotes by remember { mutableStateOf("") }
    var showSaveSuccess by remember { mutableStateOf(false) }

    val needsPercent = if (overview.totalIncome > 0) ((overview.totalFixedActual + overview.totalDailyExpense) / overview.totalIncome) * 100.0 else 0.0
    val wantsPercent = if (overview.totalIncome > 0) ((overview.totalVariableActual + overview.totalSubActual) / overview.totalIncome) * 100.0 else 0.0
    val savingsPercent = overview.savingsRatePercent

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Hero Score Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = PastelSkySurface),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(PastelCardBorder)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Rekapan Keuangan", fontSize = 12.sp, color = TextSecondaryMuted, fontWeight = FontWeight.SemiBold)
                        Text("$monthName $year", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PastelSkyDark)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(PastelMintLight)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (overview.remainingBalance >= 0) "Surplus Sehat ✨" else "Defisit / Kurang ⚠️",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (overview.remainingBalance >= 0) PastelMintSavings else Color(0xFFE63946)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Key Figures Grid
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RecapMetricItem(label = "Total Masuk", amount = overview.totalIncome, color = PastelSkyDark, modifier = Modifier.weight(1f))
                    RecapMetricItem(label = "Total Tabungan", amount = overview.totalSavingActual, color = PastelMintSavings, modifier = Modifier.weight(1f))
                    RecapMetricItem(label = "Total Keluar", amount = overview.totalActualExpense, color = PastelCoralFixed, modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(10.dp))

                Divider(color = Color(0xFFD6E8F5))

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Sisa Uang Bersih:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimaryDark)
                    Text(
                        CurrencyUtils.formatRupiah(overview.remainingBalance),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (overview.remainingBalance >= 0) PastelSkyDark else Color(0xFFD90429)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 50/30/20 Rule Analysis
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(PastelCardBorder)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Evaluasi Rasio Keuangan (Metode 50 / 30 / 20)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = PastelSkyDark
                )
                Text(
                    text = "Perbandingan ideal alokasi kebutuhan, keinginan, dan tabungan",
                    fontSize = 10.sp,
                    color = TextSecondaryMuted
                )

                Spacer(modifier = Modifier.height(12.dp))

                BudgetRatioRow(title = "Kebutuhan Pokok / Needs (Ideal ≤ 50%)", actualPercent = needsPercent, idealPercent = 50.0, color = PastelCoralFixed)
                Spacer(modifier = Modifier.height(8.dp))
                BudgetRatioRow(title = "Keinginan / Wants (Ideal ≤ 30%)", actualPercent = wantsPercent, idealPercent = 30.0, color = PastelPeachVar)
                Spacer(modifier = Modifier.height(8.dp))
                BudgetRatioRow(title = "Tabungan & Investasi / Savings (Ideal ≥ 20%)", actualPercent = savingsPercent, idealPercent = 20.0, color = PastelMintSavings)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Save Recap Form
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(PastelCardBorder)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Simpan Rekapan Akhir Bulan Ini",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = PastelSkyDark
                )
                Text(
                    text = "Data rekapan akan tersimpan aman di database lokal aplikasi",
                    fontSize = 10.sp,
                    color = TextSecondaryMuted
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = recapNotes,
                    onValueChange = { recapNotes = it },
                    placeholder = { Text("Catatan / Evaluasi pengeluaran bulan ini...", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            onSaveRecapClick(recapNotes)
                            showSaveSuccess = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PastelSkyPrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Simpan Rekapan")
                    }

                    FilledTonalButton(
                        onClick = onCreateNewMonthClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.AddCircleOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Bulan Selanjutnya")
                    }
                }

                if (showSaveSuccess) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "✅ Rekapan berhasil disimpan ke database lokal!",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PastelMintSavings
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // History of Saved Recaps
        if (savedRecaps.isNotEmpty()) {
            Text(
                text = "Riwayat Rekapan Tersimpan (${savedRecaps.size})",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = PastelSkyDark,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            savedRecaps.forEach { recap ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(PastelCardBorder)),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("${recap.monthName} ${recap.year}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PastelSkyDark)
                            Text(
                                "Masuk: ${CurrencyUtils.formatRupiahShort(recap.totalIncome)} • Tabungan: ${CurrencyUtils.formatRupiahShort(recap.totalSavings)}",
                                fontSize = 10.sp,
                                color = TextSecondaryMuted
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "Sisa: ${CurrencyUtils.formatRupiah(recap.remainingBalance)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (recap.remainingBalance >= 0) PastelMintSavings else Color(0xFFD90429)
                            )
                            Text(
                                "Tabungan: ${CurrencyUtils.formatPercent(recap.savingsRatePercent)}",
                                fontSize = 9.sp,
                                color = TextSecondaryMuted
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecapMetricItem(
    label: String,
    amount: Double,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(label, fontSize = 10.sp, color = TextSecondaryMuted)
        Spacer(modifier = Modifier.height(2.dp))
        Text(CurrencyUtils.formatRupiahShort(amount), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun BudgetRatioRow(
    title: String,
    actualPercent: Double,
    idealPercent: Double,
    color: Color
) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = TextPrimaryDark)
            Text(
                CurrencyUtils.formatPercent(actualPercent),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFFF1F5F9))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth((actualPercent / 100.0).toFloat().coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
        }
    }
}
