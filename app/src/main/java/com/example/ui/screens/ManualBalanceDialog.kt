package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.example.ui.theme.*
import com.example.util.CurrencyUtils

@Composable
fun ManualBalanceDialog(
    useManualBalance: Boolean,
    currentManualBalance: Double,
    calculatedBalance: Double,
    onDismiss: () -> Unit,
    onSave: (useManual: Boolean, manualAmount: Double) -> Unit
) {
    var isManualMode by remember { mutableStateOf(useManualBalance) }
    var amountText by remember { mutableStateOf(if (currentManualBalance > 0) currentManualBalance.toLong().toString() else calculatedBalance.toLong().toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(PastelSkyLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = PastelSkyPrimary)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Edit Saldo Uang",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = PastelSkyDark
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Pilih apakah Anda ingin menghitung sisa uang secara otomatis dari kalkulasi anggaran atau memasukkan nominal saldo riil dompet secara manual.",
                    fontSize = 12.sp,
                    color = TextSecondaryMuted
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Mode Selection Options
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = if (!isManualMode) PastelMintLight else Color(0xFFF8FAFC)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Otomatis dari Kalkulasi Budget", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                            Text("Pemasukan - (Pengeluaran + Tabungan): ${CurrencyUtils.formatRupiah(calculatedBalance)}", fontSize = 11.sp, color = TextSecondaryMuted)
                        }
                        RadioButton(
                            selected = !isManualMode,
                            onClick = { isManualMode = false }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isManualMode) PastelSkyLight else Color(0xFFF8FAFC)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Input Manual Saldo Riil", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                            Text("Masukkan nominal uang yang saat ini benar-benar Anda miliki", fontSize = 11.sp, color = TextSecondaryMuted)
                        }
                        RadioButton(
                            selected = isManualMode,
                            onClick = { isManualMode = true }
                        )
                    }
                }

                if (isManualMode) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { if (it.all { char -> char.isDigit() }) amountText = it },
                        label = { Text("Jumlah Uang Riil (Rp)") },
                        leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Batal")
                    }

                    Button(
                        onClick = {
                            val parsedAmount = amountText.toDoubleOrNull() ?: calculatedBalance
                            onSave(isManualMode, parsedAmount)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PastelSkyPrimary),
                        modifier = Modifier.weight(1.5f)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Terapkan")
                    }
                }
            }
        }
    }
}
