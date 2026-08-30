package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.*
import com.example.ui.theme.*
import com.example.util.CurrencyUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AddEditIncomeDialog(
    item: IncomeItem? = null,
    availableWallets: List<WalletItem> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (source: String, type: String, amount: Double, date: String, walletName: String) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var source by remember { mutableStateOf(item?.source ?: "") }
    var type by remember { mutableStateOf(item?.type ?: "Utama") }
    var amountText by remember { mutableStateOf(if (item != null && item.amount > 0) String.format(Locale.US, "%.0f", item.amount) else "") }
    var date by remember { mutableStateOf(item?.date ?: SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())) }
    var selectedWallet by remember {
        mutableStateOf(
            item?.walletName ?: availableWallets.firstOrNull()?.name ?: "Saldo Rekening"
        )
    }

    val typeOptions = listOf("Utama", "Sampingan", "Bonus", "Passive", "Lainnya")
    val walletOptions = if (availableWallets.isNotEmpty()) {
        availableWallets.map { it.name }
    } else {
        listOf("Saldo Rekening", "Saldo DANA", "Uang Cash", "GoPay")
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (item == null) "Tambah Pendapatan" else "Edit Pendapatan",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = PastelSkyDark
                )
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = source,
                    onValueChange = { source = it },
                    label = { Text("Sumber (cth: Gaji, Freelance, Bonus)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))

                Text("Tipe Pendapatan", fontSize = 12.sp, color = TextSecondaryMuted)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    typeOptions.take(3).forEach { opt ->
                        FilterChip(
                            selected = type == opt,
                            onClick = { type = opt },
                            label = { Text(opt, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Wallet / Dompet Tujuan Penerimaan
                Text("Masuk ke Rekening / Dompet Mana?", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = PastelSkyDark)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    walletOptions.take(3).forEach { wName ->
                        FilterChip(
                            selected = selectedWallet.equals(wName, ignoreCase = true),
                            onClick = { selectedWallet = wName },
                            label = { Text(wName, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { char -> char.isDigit() } },
                    label = { Text("Jumlah Nominal (Rp)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Tanggal (dd/mm/yyyy)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onDelete != null) {
                        TextButton(onClick = onDelete) {
                            Text("Hapus", color = Color(0xFFE63946))
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    TextButton(onClick = onDismiss) {
                        Text("Batal", color = TextSecondaryMuted)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amt = amountText.toDoubleOrNull() ?: 0.0
                            if (source.isNotBlank() && amt > 0) {
                                onSave(source, type, amt, date, selectedWallet)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PastelSkyPrimary)
                    ) {
                        Text("Simpan")
                    }
                }
            }
        }
    }
}

@Composable
fun AddEditBudgetSectionDialog(
    sectionTitle: String,
    titleValue: String = "",
    priorityValue: String = "Medium",
    plannedValue: Double = 0.0,
    actualValue: Double = 0.0,
    dateValue: String = "",
    walletValue: String = "",
    availableWallets: List<WalletItem> = emptyList(),
    isEdit: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (title: String, priority: String, planned: Double, actual: Double, date: String, walletName: String) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var title by remember { mutableStateOf(titleValue) }
    var priority by remember { mutableStateOf(priorityValue) }
    var plannedText by remember { mutableStateOf(if (plannedValue > 0) String.format(Locale.US, "%.0f", plannedValue) else "") }
    var actualText by remember { mutableStateOf(if (actualValue > 0) String.format(Locale.US, "%.0f", actualValue) else "") }
    var date by remember {
        mutableStateOf(
            if (dateValue.isNotBlank()) dateValue else SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        )
    }
    var selectedWallet by remember {
        mutableStateOf(
            if (walletValue.isNotBlank()) walletValue
            else if (sectionTitle.contains("Variabel", ignoreCase = true) || sectionTitle.contains("Jajan", ignoreCase = true)) "Saldo DANA"
            else "Saldo Rekening"
        )
    }

    val priorities = listOf("High", "Medium", "Low")
    val walletOptions = if (availableWallets.isNotEmpty()) {
        availableWallets.map { it.name }
    } else {
        listOf("Saldo Rekening", "Saldo DANA", "Uang Cash", "GoPay")
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (!isEdit) "Tambah $sectionTitle" else "Edit $sectionTitle",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = PastelSkyDark
                )
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Keterangan / Nama Pos") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Sumber Anggaran / Dompet yang Dipakai/Terpotong
                Text("Potong dari Sumber Uang / Saldo:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = PastelSkyDark)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    walletOptions.take(3).forEach { wName ->
                        FilterChip(
                            selected = selectedWallet.equals(wName, ignoreCase = true),
                            onClick = { selectedWallet = wName },
                            label = { Text(wName, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Tanggal Transaksi (dd/mm/yyyy)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text("Prioritas Anggaran", fontSize = 12.sp, color = TextSecondaryMuted)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    priorities.forEach { p ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { Text(p, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = plannedText,
                    onValueChange = { plannedText = it.filter { char -> char.isDigit() } },
                    label = { Text("Rencana Budget (Rp)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = actualText,
                    onValueChange = { actualText = it.filter { char -> char.isDigit() } },
                    label = { Text("Aktual / Realisasi Terpakai (Rp)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onDelete != null) {
                        TextButton(onClick = onDelete) {
                            Text("Hapus", color = Color(0xFFE63946))
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    TextButton(onClick = onDismiss) {
                        Text("Batal", color = TextSecondaryMuted)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val p = plannedText.toDoubleOrNull() ?: 0.0
                            val a = actualText.toDoubleOrNull() ?: 0.0
                            if (title.isNotBlank()) {
                                onSave(title, priority, p, a, date, selectedWallet)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PastelSkyPrimary)
                    ) {
                        Text("Simpan")
                    }
                }
            }
        }
    }
}

@Composable
fun AddEditDailyExpenseDialog(
    item: DailyExpenseItem? = null,
    availableWallets: List<WalletItem> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (date: String, title: String, category: String, quantity: Int, unitPrice: Double, walletName: String) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var title by remember { mutableStateOf(item?.title ?: "") }
    var category by remember { mutableStateOf(item?.category ?: "Jajan") }
    var date by remember { mutableStateOf(item?.date ?: SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())) }
    var quantityText by remember { mutableStateOf(item?.quantity?.toString() ?: "1") }
    var unitPriceText by remember { mutableStateOf(if (item != null && item.unitPrice > 0) String.format(Locale.US, "%.0f", item.unitPrice) else "") }
    var selectedWallet by remember {
        mutableStateOf(item?.walletName ?: "Uang Cash")
    }

    val categories = listOf("Jajan", "Makan", "Transport", "Belanja", "Hiburan", "Top Up", "Lainnya")
    val walletOptions = if (availableWallets.isNotEmpty()) {
        availableWallets.map { it.name }
    } else {
        listOf("Uang Cash", "Saldo DANA", "Saldo Rekening", "GoPay")
    }

    val qty = quantityText.toIntOrNull() ?: 1
    val price = unitPriceText.toDoubleOrNull() ?: 0.0
    val total = qty * price

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (item == null) "Catat Jajan & Belanja Harian" else "Edit Jajan Harian",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = PastelSkyDark
                )
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Tanggal (dd/mm/yyyy)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Keterangan (cth: Batagor, Kopi, Bensin)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Wallet / Sumber Anggaran Pembayaran
                Text("Pakai Uang / Saldo Mana?", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = PastelSkyDark)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    walletOptions.take(3).forEach { wName ->
                        FilterChip(
                            selected = selectedWallet.equals(wName, ignoreCase = true),
                            onClick = { selectedWallet = wName },
                            label = { Text(wName, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text("Kategori", fontSize = 12.sp, color = TextSecondaryMuted)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.take(4).forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 10.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { quantityText = it.filter { c -> c.isDigit() } },
                        label = { Text("Jumlah (Qty)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = unitPriceText,
                        onValueChange = { unitPriceText = it.filter { c -> c.isDigit() } },
                        label = { Text("Harga Satuan (Rp)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1.5f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Total Preview Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = PastelSkyLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Total Biaya Terpotong:", fontSize = 11.sp, color = TextPrimaryDark)
                            Text("Sumber: $selectedWallet", fontSize = 10.sp, color = TextSecondaryMuted)
                        }
                        Text(
                            CurrencyUtils.formatRupiah(total),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = PastelSkyDark
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onDelete != null) {
                        TextButton(onClick = onDelete) {
                            Text("Hapus", color = Color(0xFFE63946))
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    TextButton(onClick = onDismiss) {
                        Text("Batal", color = TextSecondaryMuted)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isNotBlank() && price > 0) {
                                onSave(date, title, category, qty, price, selectedWallet)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PastelSkyPrimary)
                    ) {
                        Text("Simpan")
                    }
                }
            }
        }
    }
}

@Composable
fun AddEditWalletDialog(
    item: WalletItem? = null,
    onDismiss: () -> Unit,
    onSave: (name: String, type: String, balance: Double, colorHex: String, iconName: String) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(item?.name ?: "") }
    var type by remember { mutableStateOf(item?.type ?: "E_WALLET") }
    var balanceText by remember {
        mutableStateOf(
            if (item != null) String.format(Locale.US, "%.0f", item.balance) else ""
        )
    }
    var selectedColor by remember { mutableStateOf(item?.colorHex ?: "#118EEA") }
    var selectedIcon by remember { mutableStateOf(item?.iconName ?: "wallet") }

    val walletTypes = listOf(
        "CASH" to "Uang Tunai",
        "E_WALLET" to "E-Wallet",
        "BANK" to "Rekening Bank",
        "SAVINGS" to "Tabungan / Investasi"
    )

    val colorOptions = listOf("#74C69D", "#118EEA", "#6599B8", "#00AED6", "#E63946", "#F4A261", "#9B5DE5")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (item == null) "Tambah Sumber Uang / Dompet" else "Edit Sumber Anggaran",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = PastelSkyDark
                )
                Text(
                    text = "Kelola Uang Cash, Saldo DANA, Rekening Bank, dll. dengan nominal saldo manual.",
                    fontSize = 11.sp,
                    color = TextSecondaryMuted
                )
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama Dompet (cth: Saldo DANA, Uang Cash, Rekening BCA)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text("Tipe Sumber Dana", fontSize = 12.sp, color = TextSecondaryMuted)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    walletTypes.take(2).forEach { (tKey, tLabel) ->
                        FilterChip(
                            selected = type == tKey,
                            onClick = { type = tKey },
                            label = { Text(tLabel, fontSize = 11.sp) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    walletTypes.drop(2).forEach { (tKey, tLabel) ->
                        FilterChip(
                            selected = type == tKey,
                            onClick = { type = tKey },
                            label = { Text(tLabel, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = balanceText,
                    onValueChange = { balanceText = it.filter { c -> c.isDigit() } },
                    label = { Text("Total Nominal Saldo Saat Ini (Rp)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("Pilih Warna Label", fontSize = 12.sp, color = TextSecondaryMuted)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    colorOptions.forEach { hex ->
                        val parsed = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Color.Gray }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = parsed,
                            modifier = Modifier
                                .size(28.dp)
                                .let {
                                    if (selectedColor == hex) {
                                        it.border(2.dp, Color.Black, RoundedCornerShape(8.dp))
                                    } else it
                                },
                            onClick = { selectedColor = hex }
                        ) {}
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onDelete != null) {
                        TextButton(onClick = onDelete) {
                            Text("Hapus", color = Color(0xFFE63946))
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    TextButton(onClick = onDismiss) {
                        Text("Batal", color = TextSecondaryMuted)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val bal = balanceText.toDoubleOrNull() ?: 0.0
                            if (name.isNotBlank()) {
                                onSave(name, type, bal, selectedColor, selectedIcon)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PastelSkyPrimary)
                    ) {
                        Text("Simpan")
                    }
                }
            }
        }
    }
}

@Composable
fun NewMonthDialog(
    onDismiss: () -> Unit,
    onCreate: (monthName: String, year: Int, copyPrevious: Boolean) -> Unit
) {
    val months = listOf(
        "Januari", "Februari", "Maret", "April", "Mei", "Juni",
        "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    )
    var selectedMonth by remember { mutableStateOf("Februari") }
    var yearText by remember { mutableStateOf("2026") }
    var copyPrevious by remember { mutableStateOf(true) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Buat Budget Bulan Baru",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = PastelSkyDark
                )
                Text(
                    text = "Mulai perencanaan keuangan untuk bulan berikutnya",
                    fontSize = 11.sp,
                    color = TextSecondaryMuted
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text("Pilih Bulan (Lengkap 12 Bulan)", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextPrimaryDark)
                Spacer(modifier = Modifier.height(6.dp))
                // Row 1: Jan - Apr
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    months.take(4).forEach { m ->
                        FilterChip(
                            modifier = Modifier.weight(1f),
                            selected = selectedMonth == m,
                            onClick = { selectedMonth = m },
                            label = { Text(m.take(3), fontSize = 11.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
                        )
                    }
                }
                // Row 2: Mei - Agu
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    months.drop(4).take(4).forEach { m ->
                        FilterChip(
                            modifier = Modifier.weight(1f),
                            selected = selectedMonth == m,
                            onClick = { selectedMonth = m },
                            label = { Text(m.take(3), fontSize = 11.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
                        )
                    }
                }
                // Row 3: Sep - Des
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    months.drop(8).take(4).forEach { m ->
                        FilterChip(
                            modifier = Modifier.weight(1f),
                            selected = selectedMonth == m,
                            onClick = { selectedMonth = m },
                            label = { Text(m.take(3), fontSize = 11.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = yearText,
                    onValueChange = { yearText = it.filter { c -> c.isDigit() } },
                    label = { Text("Tahun") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = copyPrevious,
                        onCheckedChange = { copyPrevious = it }
                    )
                    Text(
                        "Salin pos anggaran tetap dari bulan sebelumnya",
                        fontSize = 11.sp,
                        color = TextPrimaryDark
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal", color = TextSecondaryMuted)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val yr = yearText.toIntOrNull() ?: 2026
                            onCreate(selectedMonth, yr, copyPrevious)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PastelSkyPrimary)
                    ) {
                        Text("Buat Bulan")
                    }
                }
            }
        }
    }
}
