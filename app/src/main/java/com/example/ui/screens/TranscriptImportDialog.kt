package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.parser.BankTranscriptParser
import com.example.parser.ParsedTransaction
import com.example.ui.theme.*
import com.example.util.CurrencyUtils

@Composable
fun TranscriptImportDialog(
    currentMonthId: String,
    onDismiss: () -> Unit,
    onImportConfirmed: (List<ParsedTransaction>) -> Unit
) {
    val context = LocalContext.current
    var rawText by remember { mutableStateOf("") }
    var parsedList by remember { mutableStateOf<List<ParsedTransaction>>(emptyList()) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Upload File / Screenshot, 1: Tempel Teks, 2: Hasil Parsing
    var uploadStatusMessage by remember { mutableStateOf<String?>(null) }
    var uploadedFileName by remember { mutableStateOf<String?>(null) }

    // File pickers for PDF and Image
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            uploadedFileName = "Dokumen_Mutasi_Bank.pdf"
            uploadStatusMessage = "Membaca file PDF mutasi..."
            val extracted = BankTranscriptParser.extractTextFromPdf(context, uri)
            rawText = extracted
            parsedList = BankTranscriptParser.parseTranscript(extracted, currentMonthId)
            uploadStatusMessage = "Berhasil membaca ${parsedList.size} transaksi dari file PDF!"
            selectedTab = 2 // Direct to preview
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            uploadedFileName = "Screenshot_Mutasi.jpg"
            uploadStatusMessage = "Memindai screenshot mutasi rekening..."
            val extracted = BankTranscriptParser.extractTextFromScreenshot(context, uri)
            rawText = extracted
            parsedList = BankTranscriptParser.parseTranscript(extracted, currentMonthId)
            uploadStatusMessage = "Berhasil mengekstrak ${parsedList.size} transaksi dari screenshot!"
            selectedTab = 2 // Direct to preview
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .padding(10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Import Mutasi Transkrip",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = PastelSkyDark
                        )
                        Text(
                            text = "Upload PDF / Screenshot atau Tempel Teks Mutasi M-Banking & DANA",
                            fontSize = 11.sp,
                            color = TextSecondaryMuted
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Tab Switcher
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = PastelSkySurface,
                    contentColor = PastelSkyDark,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Upload File / Foto", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Tempel Teks", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = {
                            if (rawText.isNotBlank()) {
                                parsedList = BankTranscriptParser.parseTranscript(rawText, currentMonthId)
                            }
                            selectedTab = 2
                        },
                        text = {
                            Text(
                                "Hasil (${parsedList.size})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                when (selectedTab) {
                    0 -> {
                        // Upload File Tab (PDF & Screenshot)
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            item {
                                Text(
                                    text = "Pilih salah satu metode upload dokumen:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimaryDark
                                )
                            }

                            // 1. Screenshot Upload Option
                            item {
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = PastelSkySurface),
                                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(PastelCardBorder)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { imagePickerLauncher.launch("image/*") }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(PastelSkyLight),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Image, contentDescription = null, tint = PastelSkyPrimary)
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Upload Screenshot Mutasi (Foto/Galeri)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PastelSkyDark)
                                            Text("Scan gambar bukti mutasi m-banking atau DANA", fontSize = 11.sp, color = TextSecondaryMuted)
                                        }
                                        Icon(Icons.Default.CloudUpload, contentDescription = null, tint = PastelSkyPrimary)
                                    }
                                }
                            }

                            // 2. PDF Document Upload Option
                            item {
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = PastelMintLight),
                                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(PastelMintSavings)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { pdfPickerLauncher.launch("application/pdf") }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(Color.White),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = PastelMintSavings)
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Upload File Rekap PDF", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                                            Text("File e-statement bulanan dari bank BCA, Mandiri, BRI, DANA", fontSize = 11.sp, color = TextSecondaryMuted)
                                        }
                                        Icon(Icons.Default.FileOpen, contentDescription = null, tint = PastelMintSavings)
                                    }
                                }
                            }

                            // Preset Demos for quick verification
                            item {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Atau coba uji cepat dengan data mutasi demo:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextSecondaryMuted
                                )
                            }

                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    AssistChip(
                                        onClick = {
                                            rawText = BankTranscriptParser.SAMPLE_DANA_STATEMENT
                                            parsedList = BankTranscriptParser.parseTranscript(rawText, currentMonthId)
                                            selectedTab = 2
                                        },
                                        label = { Text("Contoh E-Wallet DANA", fontSize = 11.sp) },
                                        leadingIcon = { Icon(Icons.Default.AccountBalanceWallet, null, modifier = Modifier.size(14.dp)) }
                                    )
                                    AssistChip(
                                        onClick = {
                                            rawText = BankTranscriptParser.SAMPLE_MBANKING_STATEMENT
                                            parsedList = BankTranscriptParser.parseTranscript(rawText, currentMonthId)
                                            selectedTab = 2
                                        },
                                        label = { Text("Contoh M-Banking", fontSize = 11.sp) },
                                        leadingIcon = { Icon(Icons.Default.AccountBalance, null, modifier = Modifier.size(14.dp)) }
                                    )
                                }
                            }

                            if (uploadStatusMessage != null) {
                                item {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = PastelSkyLight),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PastelSkyPrimary, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(uploadStatusMessage ?: "", fontSize = 11.sp, color = PastelSkyDark)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        // Text Input Tab
                        Column(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = rawText,
                                onValueChange = {
                                    rawText = it
                                    if (it.isNotBlank()) {
                                        parsedList = BankTranscriptParser.parseTranscript(it, currentMonthId)
                                    }
                                },
                                placeholder = {
                                    Text(
                                        "Tempelkan (Paste) hasil transkrip mutasi rekening di sini...\n\nFormat contoh:\n01/01/2026 QRIS Nasi Goreng Rp 25.000 DB\n02/01/2026 Gaji Kantor Rp 4.500.000 CR",
                                        fontSize = 12.sp,
                                        color = TextCaption
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = {
                                    if (rawText.isNotBlank()) {
                                        parsedList = BankTranscriptParser.parseTranscript(rawText, currentMonthId)
                                        selectedTab = 2
                                    }
                                },
                                enabled = rawText.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = PastelSkyPrimary),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Proses & Kelompokkan Transaksi")
                            }
                        }
                    }

                    2 -> {
                        // Results Preview Tab
                        if (parsedList.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = TextCaption, modifier = Modifier.size(48.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Tidak ada transaksi yang terdeteksi.", fontSize = 13.sp, color = TextSecondaryMuted)
                                    Text("Silakan upload dokumen atau tempel teks mutasi.", fontSize = 11.sp, color = TextCaption)
                                }
                            }
                        } else {
                            val checkedItems = parsedList.filter { it.isChecked }
                            val totalImport = checkedItems.sumOf { it.amount }

                            Card(
                                colors = CardDefaults.cardColors(containerColor = PastelSkyLight),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "${checkedItems.size} Transaksi Terpilih",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PastelSkyDark
                                        )
                                        Text(
                                            text = "Siap dimasukkan ke pos anggaran bulan ini",
                                            fontSize = 10.sp,
                                            color = TextSecondaryMuted
                                        )
                                    }
                                    Text(
                                        text = CurrencyUtils.formatRupiah(totalImport),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PastelSkyDark
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                itemsIndexed(parsedList) { index, item ->
                                    ParsedTransactionRow(
                                        item = item,
                                        onToggleCheck = {
                                            parsedList = parsedList.toMutableList().also { list ->
                                                list[index] = item.copy(isChecked = !item.isChecked)
                                            }
                                        },
                                        onChangeSection = { newSection ->
                                            parsedList = parsedList.toMutableList().also { list ->
                                                list[index] = item.copy(targetSection = newSection)
                                            }
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { selectedTab = 0 },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Upload Lagi")
                                }

                                Button(
                                    onClick = {
                                        onImportConfirmed(checkedItems)
                                        onDismiss()
                                    },
                                    enabled = checkedItems.isNotEmpty(),
                                    colors = ButtonDefaults.buttonColors(containerColor = PastelSkyPrimary),
                                    modifier = Modifier.weight(1.5f)
                                ) {
                                    Icon(Icons.Default.DownloadDone, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Import Sekarang")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ParsedTransactionRow(
    item: ParsedTransaction,
    onToggleCheck: () -> Unit,
    onChangeSection: (String) -> Unit
) {
    val sectionLabels = mapOf(
        "DAILY" to "Jajan / Harian",
        "FIXED" to "Pengeluaran Tetap",
        "VARIABLE" to "Pengeluaran Variabel",
        "SAVING" to "Tabungan / Investasi",
        "INCOME" to "Pendapatan / Income",
        "SUBSCRIPTION" to "Langganan"
    )

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isChecked) Color.White else Color(0xFFF8FAFC)
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (item.isChecked) PastelCardBorder else Color(0xFFE2E8F0)
            )
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = item.isChecked,
                onCheckedChange = { onToggleCheck() }
            )

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = item.description,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimaryDark,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = CurrencyUtils.formatRupiah(item.amount),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (item.transactionType == "INCOME") PastelMintSavings else PastelCoralFixed
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.date,
                        fontSize = 10.sp,
                        color = TextSecondaryMuted
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(PastelSkyLight)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = sectionLabels[item.targetSection] ?: item.targetSection,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = PastelSkyDark
                        )
                    }
                }
            }
        }
    }
}
