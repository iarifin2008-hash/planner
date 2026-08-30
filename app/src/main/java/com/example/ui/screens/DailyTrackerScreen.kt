package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.data.model.DailyExpenseItem
import com.example.ui.theme.*
import com.example.util.CurrencyUtils

@Composable
fun DailyTrackerScreen(
    dailyExpenses: List<DailyExpenseItem>,
    onAddClick: () -> Unit,
    onEditClick: (DailyExpenseItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Semua") }

    val categories = listOf("Semua", "Jajan", "Makan", "Belanja", "Transport", "Hiburan", "Lainnya")

    val filteredList = remember(dailyExpenses, searchQuery, selectedCategory) {
        dailyExpenses.filter { item ->
            val matchesCategory = if (selectedCategory == "Semua") true else item.category.equals(selectedCategory, ignoreCase = true)
            val matchesSearch = if (searchQuery.isBlank()) true else {
                item.title.contains(searchQuery, ignoreCase = true) ||
                item.category.contains(searchQuery, ignoreCase = true) ||
                item.notes.contains(searchQuery, ignoreCase = true) ||
                item.walletName.contains(searchQuery, ignoreCase = true)
            }
            matchesCategory && matchesSearch
        }
    }

    val totalJajan = dailyExpenses.sumOf { it.totalAmount }
    val totalCount = dailyExpenses.size
    val totalBelanjaOnly = dailyExpenses.filter { it.category.equals("Belanja", ignoreCase = true) }.sumOf { it.totalAmount }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Summary row matching reference screenshots (Total Jajan, Berapa kali Jajan, Total Belanja)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Total Jajan Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PastelSkySurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(PastelCardBorder)),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Total Jajan", fontSize = 11.sp, color = TextSecondaryMuted, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(CurrencyUtils.formatRupiah(totalJajan), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PastelSkyDark)
                }
            }

            // Counter Card (Berapa kali Jajan)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PastelSkySurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(PastelCardBorder)),
                modifier = Modifier.weight(0.9f)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Frekuensi Jajan", fontSize = 11.sp, color = TextSecondaryMuted, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("$totalCount Kali", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = PastelSkyDark)
                }
            }

            // Total Belanja Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PastelSkySurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(PastelCardBorder)),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Total Belanja", fontSize = 11.sp, color = TextSecondaryMuted, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(CurrencyUtils.formatRupiah(totalBelanjaOnly), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PastelSkyDark)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Table Header Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(PastelCardBorder)),
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Tracker Jajan Selama 1 Bulan",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = PastelSkyDark
                        )
                        Text(
                            text = "Catat pengeluaran mikro harian & sumber dana terpakai",
                            fontSize = 10.sp,
                            color = TextSecondaryMuted
                        )
                    }
                    Button(
                        onClick = onAddClick,
                        colors = ButtonDefaults.buttonColors(containerColor = PastelSkyPrimary),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tambah", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search & Filter Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Cari jajan / sumber dana...", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp), tint = TextSecondaryMuted) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(14.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color(0xFFE2EFF9),
                            focusedBorderColor = PastelSkyPrimary
                        ),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Category Chips Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = selectedCategory == cat
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PastelSkyPrimary,
                                selectedLabelColor = Color.White
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = Color(0xFFE2EFF9),
                                selectedBorderColor = PastelSkyPrimary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Column Headers
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(PastelSkyLight)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Tgl", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PastelSkyDark, modifier = Modifier.width(48.dp))
                    Text("Keterangan & Dompet", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PastelSkyDark, modifier = Modifier.weight(1f))
                    Text("Qty", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PastelSkyDark, modifier = Modifier.width(32.dp))
                    Text("Harga", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PastelSkyDark, modifier = Modifier.width(55.dp))
                    Text("Total", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PastelSkyDark, modifier = Modifier.width(62.dp))
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (filteredList.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (searchQuery.isNotBlank() || selectedCategory != "Semua") "Tidak ada transaksi jajan yang sesuai filter."
                            else "Belum ada catatan jajan harian bulan ini.\nTekan '+ Tambah' untuk mencatat.",
                            fontSize = 12.sp,
                            color = TextCaption,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filteredList) { item ->
                            DailyExpenseTableRow(item = item, onClick = { onEditClick(item) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyExpenseTableRow(
    item: DailyExpenseItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .background(Color(0xFFFAFDFF))
            .border(0.5.dp, Color(0xFFE2EFF9), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = item.date.take(5), // dd/MM
            fontSize = 10.sp,
            color = TextSecondaryMuted,
            modifier = Modifier.width(48.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimaryDark,
                maxLines = 1
            )
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 1.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(PastelSkyLight)
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = item.category,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = PastelSkyDark
                    )
                }
                if (item.walletName.isNotBlank()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFFE0F2FE))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = item.walletName,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0369A1)
                        )
                    }
                }
            }
        }

        Text(
            text = "${item.quantity}x",
            fontSize = 10.sp,
            color = TextSecondaryMuted,
            modifier = Modifier.width(32.dp)
        )

        Text(
            text = CurrencyUtils.formatRupiahShort(item.unitPrice),
            fontSize = 10.sp,
            color = TextSecondaryMuted,
            modifier = Modifier.width(55.dp)
        )

        Text(
            text = CurrencyUtils.formatRupiahShort(item.totalAmount),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = PastelSkyDark,
            modifier = Modifier.width(62.dp)
        )
    }
}
