package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.util.CurrencyUtils

data class ChartSlice(
    val label: String,
    val value: Double,
    val color: Color
)

@Composable
fun SynchronizedBudgetGauge(
    totalIncome: Double,
    totalExpense: Double,
    totalSavings: Double,
    remainingBalance: Double,
    modifier: Modifier = Modifier,
    size: Dp = 180.dp
) {
    val remainingPercent = if (totalIncome > 0) {
        ((remainingBalance / totalIncome) * 100.0).coerceIn(0.0, 100.0)
    } else 0.0

    val expensePercent = if (totalIncome > 0) {
        ((totalExpense / totalIncome) * 100.0).coerceIn(0.0, 100.0)
    } else 0.0

    val savingsPercent = if (totalIncome > 0) {
        ((totalSavings / totalIncome) * 100.0).coerceIn(0.0, 100.0)
    } else 0.0

    val animatedRemaining = remember { Animatable(0f) }
    val animatedExpense = remember { Animatable(0f) }
    val animatedSavings = remember { Animatable(0f) }

    LaunchedEffect(remainingPercent, expensePercent, savingsPercent) {
        animatedRemaining.animateTo(remainingPercent.toFloat(), tween(800, easing = FastOutSlowInEasing))
        animatedExpense.animateTo(expensePercent.toFloat(), tween(800, easing = FastOutSlowInEasing))
        animatedSavings.animateTo(savingsPercent.toFloat(), tween(800, easing = FastOutSlowInEasing))
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            val strokeWidth = 18.dp.toPx()
            val arcSize = Size(this.size.width - strokeWidth, this.size.height - strokeWidth)
            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

            // Background track (Light pastel circle)
            drawArc(
                color = Color(0xFFE2EEF8),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth)
            )

            // Remaining Balance (Starts full and depletes as expenses/savings grow)
            val remainingSweep = (animatedRemaining.value / 100f) * 360f
            val savingsSweep = (animatedSavings.value / 100f) * 360f
            val expenseSweep = (animatedExpense.value / 100f) * 360f

            var currentAngle = -90f

            // 1. Sisa Anggaran (Pastel Sky Blue)
            if (remainingSweep > 0) {
                drawArc(
                    color = PastelSkyPrimary,
                    startAngle = currentAngle,
                    sweepAngle = remainingSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                currentAngle += remainingSweep
            }

            // 2. Tabungan (Pastel Mint)
            if (savingsSweep > 0) {
                drawArc(
                    color = PastelMintSavings,
                    startAngle = currentAngle,
                    sweepAngle = savingsSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                currentAngle += savingsSweep
            }

            // 3. Pengeluaran (Pastel Coral)
            if (expenseSweep > 0) {
                drawArc(
                    color = PastelCoralFixed,
                    startAngle = currentAngle,
                    sweepAngle = expenseSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        // Center Content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Sisa Uang",
                fontSize = 11.sp,
                color = TextSecondaryMuted,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = CurrencyUtils.formatRupiah(remainingBalance),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (remainingBalance >= 0) PastelSkyDark else Color(0xFFD90429),
                textAlign = TextAlign.Center
            )
            Text(
                text = CurrencyUtils.formatPercent(remainingPercent),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (remainingPercent > 20) PastelMintSavings else Color(0xFFE63946)
            )
        }
    }
}

@Composable
fun DonutChartCard(
    title: String,
    slices: List<ChartSlice>,
    modifier: Modifier = Modifier,
    totalLabel: String = "Total",
    emptyMessage: String = "Belum ada data"
) {
    val totalValue = slices.sumOf { it.value }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, PastelCardBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = PastelSkyDark,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (totalValue <= 0 || slices.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = emptyMessage,
                    fontSize = 12.sp,
                    color = TextCaption
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Donut Visual
                Box(
                    modifier = Modifier.size(110.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                        val strokeWidth = 14.dp.toPx()
                        val arcSize = Size(this.size.width - strokeWidth, this.size.height - strokeWidth)
                        val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

                        var startAngle = -90f
                        slices.forEach { slice ->
                            val sweep = ((slice.value / totalValue) * 360f).toFloat()
                            if (sweep > 0f) {
                                drawArc(
                                    color = slice.color,
                                    startAngle = startAngle,
                                    sweepAngle = sweep,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = arcSize,
                                    style = Stroke(width = strokeWidth)
                                )
                                startAngle += sweep
                            }
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = totalLabel,
                            fontSize = 9.sp,
                            color = TextSecondaryMuted
                        )
                        Text(
                            text = CurrencyUtils.formatRupiahShort(totalValue),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Legends & Percentages
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    slices.take(4).forEach { slice ->
                        val percent = (slice.value / totalValue) * 100.0
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(slice.color)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = slice.label,
                                fontSize = 11.sp,
                                color = TextPrimaryDark,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = CurrencyUtils.formatPercent(percent),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
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
fun MonthlyBarComparisonCard(
    fixedActual: Double,
    fixedPlanned: Double,
    variableActual: Double,
    variablePlanned: Double,
    subActual: Double,
    subPlanned: Double,
    modifier: Modifier = Modifier
) {
    val maxVal = listOf(fixedActual, fixedPlanned, variableActual, variablePlanned, subActual, subPlanned, 100000.0).maxOrNull() ?: 1.0

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, PastelCardBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Rencana VS Aktual",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = PastelSkyDark
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFBCE0FD)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Rencana", fontSize = 10.sp, color = TextSecondaryMuted)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(PastelSkyPrimary))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Aktual", fontSize = 10.sp, color = TextSecondaryMuted)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Bar groups
        BarGroupItem(label = "Fixed Cost", planned = fixedPlanned, actual = fixedActual, maxVal = maxVal)
        Spacer(modifier = Modifier.height(8.dp))
        BarGroupItem(label = "Variable Cost", planned = variablePlanned, actual = variableActual, maxVal = maxVal)
        Spacer(modifier = Modifier.height(8.dp))
        BarGroupItem(label = "Subscription", planned = subPlanned, actual = subActual, maxVal = maxVal)
    }
}

@Composable
private fun BarGroupItem(
    label: String,
    planned: Double,
    actual: Double,
    maxVal: Double
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = TextPrimaryDark)
            Text(
                "${CurrencyUtils.formatRupiahShort(actual)} / ${CurrencyUtils.formatRupiahShort(planned)}",
                fontSize = 10.sp,
                color = TextSecondaryMuted
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(Color(0xFFF1F5F9))
        ) {
            // Planned bar background
            val plannedFraction = (planned / maxVal).toFloat().coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(plannedFraction)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color(0xFFBCE0FD))
            )
            // Actual bar foreground
            val actualFraction = (actual / maxVal).toFloat().coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(actualFraction)
                    .clip(RoundedCornerShape(5.dp))
                    .background(PastelSkyPrimary)
            )
        }
    }
}
