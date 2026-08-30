package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CategoryBudgetStatus
import com.example.data.model.CategoryRank
import com.example.ui.theme.*
import com.example.util.CurrencyUtils

@Composable
fun SummaryMetricCard(
    title: String,
    amount: Double,
    subtitle: String? = null,
    backgroundColor: Color = Color.White,
    borderColor: Color = PastelCardBorder,
    accentColor: Color = PastelSkyPrimary,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(borderColor)),
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondaryMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (icon != null) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = CurrencyUtils.formatRupiah(amount),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = TextSecondaryMuted,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Komponen Indikator Jatah Kategori & Peringatan Batas Maksimal
 */
@Composable
fun CategoryAllowanceWarningCard(
    status: CategoryBudgetStatus,
    themeColor: Color,
    modifier: Modifier = Modifier
) {
    val progress = (status.usagePercentage / 100.0).coerceIn(0.0, 1.0).toFloat()
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "Progress")

    val (statusBg, statusBorder, statusTextColor, statusIcon, statusLabel) = when {
        status.isOverBudget -> {
            arrayOf(
                Color(0xFFFFF0F0),
                Color(0xFFFFCCD5),
                Color(0xFFDC2626),
                Icons.Default.Warning,
                "🚨 Melebihi Jatah! Lebih ${CurrencyUtils.formatRupiahShort(status.excessAmount)}"
            )
        }
        status.isNearLimit -> {
            arrayOf(
                Color(0xFFFFFBEB),
                Color(0xFFFDE68A),
                Color(0xFFD97706),
                Icons.Default.Info,
                "⚠️ Mendekati Batas Maksimal (${CurrencyUtils.formatPercent(status.usagePercentage)})"
            )
        }
        else -> {
            arrayOf(
                Color(0xFFF0FDF4),
                Color(0xFFBBF7D0),
                Color(0xFF16A34A),
                Icons.Default.CheckCircle,
                "Sisa Jatah: ${CurrencyUtils.formatRupiah(status.remainingAllowance)} (${CurrencyUtils.formatPercent(status.usagePercentage)} terpakai)"
            )
        }
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = statusBg as Color),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(statusBorder as Color)),
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
                        imageVector = statusIcon as ImageVector,
                        contentDescription = null,
                        tint = statusTextColor as Color,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = statusLabel as String,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusTextColor
                    )
                }

                Text(
                    text = "${CurrencyUtils.formatRupiahShort(status.actualSpent)} / ${CurrencyUtils.formatRupiahShort(status.plannedLimit)}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondaryMuted
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Linear Progress Bar
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = when {
                    status.isOverBudget -> Color(0xFFE63946)
                    status.isNearLimit -> Color(0xFFF4A261)
                    else -> themeColor
                },
                trackColor = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun PriorityBadge(priority: String, modifier: Modifier = Modifier) {
    val (bg, textColor) = when (priority.lowercase()) {
        "high" -> Pair(PriorityHighBg, PriorityHighText)
        "medium", "med" -> Pair(PriorityMedBg, PriorityMedText)
        else -> Pair(PriorityLowBg, PriorityLowText)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = priority,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
fun TableSectionHeader(
    title: String,
    subtitle: String,
    totalPlanned: Double? = null,
    totalActual: Double,
    icon: ImageVector,
    themeColor: Color,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
            .background(themeColor.copy(alpha = 0.15f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(themeColor.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = themeColor, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PastelSkyDark)
                Text(subtitle, fontSize = 10.sp, color = TextSecondaryMuted)
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = CurrencyUtils.formatRupiah(totalActual),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = PastelSkyDark
                )
                if (totalPlanned != null && totalPlanned > 0) {
                    Text(
                        text = "Rencana: ${CurrencyUtils.formatRupiahShort(totalPlanned)}",
                        fontSize = 9.sp,
                        color = TextSecondaryMuted
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onAddClick,
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(themeColor)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah", tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun CategoryRankingCard(
    ranks: List<CategoryRank>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, PastelCardBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Text(
            text = "Peringkat Pengeluaran",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = PastelSkyDark
        )
        Text(
            text = "Kategori terbanyak yang menghabiskan dana",
            fontSize = 10.sp,
            color = TextSecondaryMuted
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (ranks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().height(60.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Belum ada data pengeluaran", fontSize = 11.sp, color = TextCaption)
            }
        } else {
            ranks.take(6).forEachIndexed { index, rank ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(
                                when (index) {
                                    0 -> Color(0xFFFFD166)
                                    1 -> Color(0xFFE2E8F0)
                                    2 -> Color(0xFFE9D8A6)
                                    else -> Color(0xFFF1F5F9)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${index + 1}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = rank.categoryName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimaryDark,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = CurrencyUtils.formatRupiah(rank.totalAmount),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PastelSkyDark
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = "(${CurrencyUtils.formatPercent(rank.percentageOfExpense)})",
                        fontSize = 10.sp,
                        color = TextSecondaryMuted
                    )
                }
            }
        }
    }
}
