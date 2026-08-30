package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.data.model.VoiceTransactionAnalysis
import com.example.ui.theme.*
import com.example.util.CurrencyUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun VoiceExpenseAssistantDialog(
    onDismiss: () -> Unit,
    onConfirmTransaction: (VoiceTransactionAnalysis) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isListening by remember { mutableStateOf(false) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var transcriptText by remember { mutableStateOf("") }
    var detectedAnalysis by remember { mutableStateOf<VoiceTransactionAnalysis?>(null) }
    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Instant Auto-Sync and Auto-Deduct State
    var autoSyncEnabled by remember { mutableStateOf(true) }
    var isSynchronizedSuccess by remember { mutableStateOf(false) }
    var isEditingMode by remember { mutableStateOf(false) }
    var countdownSeconds by remember { mutableIntStateOf(3) }

    // Editable fields for fine-tuning
    var editTitle by remember { mutableStateOf("") }
    var editAmountText by remember { mutableStateOf("") }
    var editDate by remember { mutableStateOf("") }
    var editCategory by remember { mutableStateOf("VARIABLE") }
    var editWallet by remember { mutableStateOf("Uang Cash") }

    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isListening) 1.25f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    fun executeImmediateSync(analysis: VoiceTransactionAnalysis) {
        onConfirmTransaction(analysis)
        isSynchronizedSuccess = true
        isEditingMode = false
        countdownSeconds = 3

        // Launch auto-dismiss countdown
        scope.launch {
            while (countdownSeconds > 0) {
                delay(1000)
                countdownSeconds--
            }
            onDismiss()
        }
    }

    fun startAnalyzing(text: String) {
        if (text.isBlank()) return
        isAnalyzing = true
        errorMessage = null
        isSynchronizedSuccess = false
        scope.launch {
            try {
                val result = com.example.util.GeminiVoiceService.analyzeVoiceTransaction(text)
                detectedAnalysis = result
                editTitle = result.itemTitle
                editAmountText = if (result.amount > 0) String.format(Locale.US, "%.0f", result.amount) else ""
                editDate = result.date.ifEmpty { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()) }
                editCategory = result.targetCategory
                editWallet = result.walletName.ifEmpty { "Uang Cash" }

                if (autoSyncEnabled && result.amount > 0 && result.itemTitle.isNotBlank()) {
                    executeImmediateSync(result)
                }
            } catch (e: Exception) {
                errorMessage = "Gagal menganalisis: ${e.message}"
            } finally {
                isAnalyzing = false
            }
        }
    }

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            errorMessage = "Perekam suara tidak tersedia di perangkat ini. Silakan ketik atau gunakan tombol contoh suara."
            return
        }

        try {
            speechRecognizer?.destroy()
            val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer = recognizer

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "id-ID")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Sebutkan transaksi, contoh: Beli nasi padang 25 ribu")
            }

            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    isListening = true
                    errorMessage = null
                }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    isListening = false
                }
                override fun onError(error: Int) {
                    isListening = false
                    val msg = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "Suara tidak terdengar jelas, silakan coba lagi."
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Waktu bicara habis."
                        SpeechRecognizer.ERROR_NETWORK -> "Tidak ada jaringan untuk pengenal suara online."
                        else -> "Gagal merekam suara (Kode: $error)."
                    }
                    errorMessage = msg
                }
                override fun onResults(results: Bundle?) {
                    isListening = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val spokenText = matches?.firstOrNull() ?: ""
                    if (spokenText.isNotBlank()) {
                        transcriptText = spokenText
                        startAnalyzing(spokenText)
                    }
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                    if (!partial.isNullOrBlank()) {
                        transcriptText = partial
                    }
                }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            recognizer.startListening(intent)
        } catch (e: Exception) {
            isListening = false
            errorMessage = "Error memulai mikrofon: ${e.message}"
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startListening()
        } else {
            Toast.makeText(context, "Izin mikrofon dibutuhkan untuk merekam suara", Toast.LENGTH_SHORT).show()
        }
    }

    fun requestMicAndListen() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startListening()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val quickVoiceExamples = listOf(
        "Beli nasi padang 25 ribu",
        "Bayar listrik 200 ribu",
        "Nabung reksadana 350 ribu",
        "Langganan Spotify 55 ribu",
        "Beli bensin motor 30 ribu",
        "Ngopi cafe 28 ribu"
    )

    DisposableEffect(Unit) {
        onDispose {
            speechRecognizer?.destroy()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(PastelSkyPrimary, PastelMintSavings))),
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(PastelSkyLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = PastelSkyPrimary, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Asisten Suara AI Keuangan",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = PastelSkyDark
                            )
                            Text(
                                text = "Sebut transaksi -> Otomatis potong pos anggaran",
                                fontSize = 9.sp,
                                color = TextSecondaryMuted
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = TextSecondaryMuted)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Auto-Sync Switch Badge
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (autoSyncEnabled) Color(0xFFF0FDF4) else Color(0xFFF8FAFC))
                        .border(1.dp, if (autoSyncEnabled) PastelMintLight else PastelCardBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (autoSyncEnabled) Icons.Default.Bolt else Icons.Default.SyncDisabled,
                            contentDescription = null,
                            tint = if (autoSyncEnabled) PastelMintSavings else TextSecondaryMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (autoSyncEnabled) "⚡ Sinkronisasi Langsung Aktif" else "Konfirmasi Manual",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (autoSyncEnabled) Color(0xFF2D6A4F) else TextPrimaryDark
                        )
                    }
                    Switch(
                        checked = autoSyncEnabled,
                        onCheckedChange = { autoSyncEnabled = it },
                        modifier = Modifier.scale(0.75f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Microphone Button
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .clip(CircleShape)
                        .background(
                            if (isListening) Brush.radialGradient(listOf(PastelCoralFixed.copy(alpha = 0.4f), Color.Transparent))
                            else Brush.radialGradient(listOf(PastelSkyPrimary.copy(alpha = 0.25f), Color.Transparent))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .scale(pulseScale)
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(
                                if (isListening) Brush.linearGradient(listOf(Color(0xFFFF6B6B), Color(0xFFFF8E72)))
                                else Brush.linearGradient(listOf(PastelSkyPrimary, Color(0xFF4A90E2)))
                            )
                            .clickable {
                                if (isListening) {
                                    speechRecognizer?.stopListening()
                                    isListening = false
                                } else {
                                    requestMicAndListen()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Bicara",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isListening) "🎙️ Mendengarkan... Sebutkan transaksimu!" else "Tekan mikrofon & bicara (cth: 'Beli nasi padang 25 ribu')",
                    fontSize = 11.sp,
                    fontWeight = if (isListening) FontWeight.Bold else FontWeight.Medium,
                    color = if (isListening) Color(0xFFD32F2F) else TextPrimaryDark,
                    textAlign = TextAlign.Center
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = errorMessage ?: "",
                        fontSize = 11.sp,
                        color = Color(0xFFD32F2F),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Transcript Text Input
                OutlinedTextField(
                    value = transcriptText,
                    onValueChange = { transcriptText = it },
                    label = { Text("Transkrip Suara / Teks Ucapan", fontSize = 11.sp) },
                    placeholder = { Text("cth: 'Beli nasi padang 25 ribu'") },
                    trailingIcon = {
                        if (transcriptText.isNotBlank()) {
                            IconButton(onClick = { startAnalyzing(transcriptText) }) {
                                Icon(Icons.Default.Send, contentDescription = "Analisis", tint = PastelSkyPrimary)
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick Indonesian Voice Chips
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Contoh Suara Cepat (Ketuk untuk Uji AI):",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondaryMuted,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickVoiceExamples.take(2).forEach { ex ->
                        SuggestionChip(
                            onClick = {
                                transcriptText = ex
                                startAnalyzing(ex)
                            },
                            label = { Text(ex, fontSize = 9.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickVoiceExamples.drop(2).take(2).forEach { ex ->
                        SuggestionChip(
                            onClick = {
                                transcriptText = ex
                                startAnalyzing(ex)
                            },
                            label = { Text(ex, fontSize = 9.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (isAnalyzing) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = PastelSkyPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AI sedang menganalisis & menyinkronkan pos...", fontSize = 11.sp, color = PastelSkyDark)
                    }
                }

                // 1. CELEBRATION / SUCCESS SYNCHRONIZED CARD (Auto-Sync Mode)
                AnimatedVisibility(visible = isSynchronizedSuccess && !isEditingMode) {
                    val analysis = detectedAnalysis
                    if (analysis != null) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(PastelMintSavings, PastelSkyPrimary))),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 14.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(PastelMintSavings.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PastelMintSavings, modifier = Modifier.size(24.dp))
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "✅ Berhasil Disinkronkan!",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2D6A4F)
                                        )
                                        Text(
                                            text = "Otomatis tersimpan & saldo anggaran terpotong",
                                            fontSize = 10.sp,
                                            color = TextSecondaryMuted
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Divider(color = PastelMintLight)
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Nama Transaksi:", fontSize = 11.sp, color = TextSecondaryMuted)
                                    Text(analysis.itemTitle, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Nominal Terpotong:", fontSize = 11.sp, color = TextSecondaryMuted)
                                    Text(
                                        CurrencyUtils.formatRupiah(analysis.amount),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PastelCoralFixed
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Masuk Pos Anggaran:", fontSize = 11.sp, color = TextSecondaryMuted)
                                    val catLabel = when (analysis.targetCategory) {
                                        "FIXED" -> "Pengeluaran Tetap"
                                        "SAVINGS" -> "Tabungan & Investasi"
                                        "SUBSCRIPTION" -> "Langganan & Cicilan"
                                        "INCOME" -> "Pendapatan / Kas"
                                        else -> "Pengeluaran Tidak Tetap"
                                    }
                                    Text(catLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PastelSkyDark)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Sumber Dompet:", fontSize = 11.sp, color = TextSecondaryMuted)
                                    Text("💳 ${analysis.walletName}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PastelSkyPrimary)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Tanggal:", fontSize = 11.sp, color = TextSecondaryMuted)
                                    Text("📅 ${analysis.date}", fontSize = 10.sp, color = TextPrimaryDark)
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { isEditingMode = true },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Ubah Data", fontSize = 11.sp)
                                    }

                                    Button(
                                        onClick = onDismiss,
                                        colors = ButtonDefaults.buttonColors(containerColor = PastelMintSavings),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1.2f)
                                    ) {
                                        Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Selesai ($countdownSeconds s)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. MANUAL EDIT / CONFIRMATION CARD (If manual mode or user clicked "Ubah Data")
                AnimatedVisibility(visible = (detectedAnalysis != null && !isAnalyzing && (!autoSyncEnabled || isEditingMode))) {
                    val analysis = detectedAnalysis
                    if (analysis != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 14.dp)
                        ) {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF4F9FD)),
                                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(PastelSkyLight, PastelMintLight))),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "✨ Detail Pos Transaksi",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PastelSkyDark
                                        )

                                        val categoryBadgeColor = when (editCategory) {
                                            "FIXED" -> PastelCoralFixed
                                            "SAVINGS" -> PastelMintSavings
                                            "SUBSCRIPTION" -> PastelLilacSub
                                            "INCOME" -> PastelSkyPrimary
                                            else -> PastelPeachVar
                                        }
                                        val categoryBadgeText = when (editCategory) {
                                            "FIXED" -> "Pos: Fixed Cost"
                                            "SAVINGS" -> "Pos: Tabungan"
                                            "SUBSCRIPTION" -> "Pos: Langganan"
                                            "INCOME" -> "Pos: Pendapatan"
                                            else -> "Pos: Variabel / Jajan"
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(categoryBadgeColor.copy(alpha = 0.18f))
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(categoryBadgeText, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = categoryBadgeColor)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Editable Title
                                    OutlinedTextField(
                                        value = editTitle,
                                        onValueChange = { editTitle = it },
                                        label = { Text("Keterangan Barang/Tujuan") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = editAmountText,
                                            onValueChange = { editAmountText = it.filter { c -> c.isDigit() } },
                                            label = { Text("Nominal (Rp)") },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1.2f)
                                        )

                                        OutlinedTextField(
                                            value = editDate,
                                            onValueChange = { editDate = it },
                                            label = { Text("Tanggal") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text("Pilih Pos Anggaran Tujuan:", fontSize = 10.sp, color = TextSecondaryMuted)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        listOf("VARIABLE" to "Variabel", "FIXED" to "Tetap", "SAVINGS" to "Nabung", "SUBSCRIPTION" to "Subs", "INCOME" to "Masuk").forEach { (key, label) ->
                                            FilterChip(
                                                selected = editCategory == key,
                                                onClick = { editCategory = key },
                                                label = { Text(label, fontSize = 9.sp) },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text("Pilih Sumber Dana (Dompet):", fontSize = 10.sp, color = TextSecondaryMuted)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(androidx.compose.foundation.rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf("Uang Cash", "Saldo DANA", "Saldo Rekening", "GoPay", "ShopeePay", "OVO").forEach { wName ->
                                            FilterChip(
                                                selected = editWallet.equals(wName, ignoreCase = true),
                                                onClick = { editWallet = wName },
                                                label = { Text(wName, fontSize = 10.sp) }
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    val amt = editAmountText.toDoubleOrNull() ?: 0.0
                                    if (editTitle.isNotBlank() && amt > 0) {
                                        val finalAnalysis = (detectedAnalysis ?: VoiceTransactionAnalysis()).copy(
                                            itemTitle = editTitle,
                                            amount = amt,
                                            date = editDate,
                                            targetCategory = editCategory,
                                            walletName = editWallet
                                        )
                                        executeImmediateSync(finalAnalysis)
                                    } else {
                                        Toast.makeText(context, "Nama barang dan nominal harus valid", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PastelSkyPrimary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                            ) {
                                Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Simpan & Sinkronkan Sekarang", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
