package com.example.service

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.data.db.AppDatabase
import com.example.data.model.*
import com.example.util.CurrencyUtils
import com.example.util.GeminiVoiceService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*

class BackgroundVoiceService : Service(), TextToSpeech.OnInitListener {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false

    private var isCurrentlyListening = false
    private var isAwaitingSpecificExpense = false // True after saying "Hai Planner"
    private var restartListeningRunnable: Runnable? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "BackgroundVoiceService created")
        _isServiceActive.value = true

        // Initialize Indonesian Text-to-Speech
        try {
            textToSpeech = TextToSpeech(applicationContext, this)
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing TTS: ${e.message}")
        }

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildForegroundNotification("Siaga mendengarkan kode 'Hai Planner'...", null))

        initSpeechRecognizer()
        startListeningLoop()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech?.setLanguage(Locale("id", "ID"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                textToSpeech?.language = Locale.US
            }
            isTtsReady = true
            speakFeedback("Asisten Suara Planner aktif. Katakan Hai Planner untuk mencatat pengeluaran.")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when (action) {
            ACTION_STOP_SERVICE -> {
                speakFeedback("Asisten suara dinonaktifkan.")
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_TRIGGER_LISTEN -> {
                isAwaitingSpecificExpense = true
                speakFeedback("Ya, silakan sebutkan transaksi pengeluaran Anda.")
                mainHandler.postDelayed({ restartListeningNow() }, 1500)
            }
        }
        return START_STICKY
    }

    private fun initSpeechRecognizer() {
        mainHandler.post {
            try {
                speechRecognizer?.destroy()
                if (SpeechRecognizer.isRecognitionAvailable(this)) {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
                        setRecognitionListener(createRecognitionListener())
                    }
                } else {
                    Log.e(TAG, "Speech recognition not available on device")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating speech recognizer: ${e.message}")
            }
        }
    }

    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isCurrentlyListening = true
                _isListeningState.value = true
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                isCurrentlyListening = false
                _isListeningState.value = false
            }

            override fun onError(error: Int) {
                isCurrentlyListening = false
                _isListeningState.value = false
                Log.d(TAG, "SpeechRecognizer error: $error")
                // Schedule restart of continuous listening
                scheduleRestartListening(1500)
            }

            override fun onResults(results: Bundle?) {
                isCurrentlyListening = false
                _isListeningState.value = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val spokenText = matches?.firstOrNull()?.trim() ?: ""

                if (spokenText.isNotBlank()) {
                    _lastSpokenText.value = spokenText
                    handleSpokenInput(spokenText)
                } else {
                    scheduleRestartListening(1000)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val partial = matches?.firstOrNull()?.trim() ?: ""
                if (partial.isNotBlank()) {
                    _lastSpokenText.value = partial
                    checkImmediateHotword(partial)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    private fun checkImmediateHotword(text: String) {
        val lower = text.lowercase(Locale.getDefault())
        if (!isAwaitingSpecificExpense && isHotwordContained(lower)) {
            vibrateDevice()
        }
    }

    private fun isHotwordContained(text: String): Boolean {
        val lower = text.lowercase(Locale.getDefault())
        return lower.contains("hai planner") ||
                lower.contains("hai plenner") ||
                lower.contains("halo planner") ||
                lower.contains("halo plenner") ||
                lower.contains("hey planner") ||
                lower.contains("plenner") ||
                lower.contains("planner catat") ||
                lower.contains("catat planner") ||
                lower.contains("hai plan")
    }

    private fun handleSpokenInput(spokenText: String) {
        val lower = spokenText.lowercase(Locale.getDefault())
        Log.d(TAG, "Handling spoken input: $spokenText (awaiting: $isAwaitingSpecificExpense)")

        // 1. Check if hotword is spoken
        if (isHotwordContained(lower)) {
            vibrateDevice()

            // Check if user spoke the hotword AND the transaction in one sentence
            // Example: "Hai planner beli nasi padang 25 ribu"
            val cleanAfterHotword = spokenText
                .replace(Regex("""(?i)\b(hai|halo|hey)?\s*(planner|plenner|plan)\b"""), "")
                .replace(Regex("""(?i)\b(tolong|catatkan|catat)\b"""), "")
                .trim()

            if (cleanAfterHotword.length > 4 && containsNumbersOrMoneyWords(cleanAfterHotword)) {
                // Direct complete voice transaction!
                processTransactionFromSpeech(cleanAfterHotword)
            } else {
                // Wake up and prompt user for transaction
                isAwaitingSpecificExpense = true
                speakFeedback("Hai! Silakan sebutkan transaksi pengeluaran Anda...")
                updateNotification("🎙️ Hai Planner aktif! Sebutkan pengeluaran...", null)
                mainHandler.postDelayed({ restartListeningNow() }, 1800)
            }
            return
        }

        // 2. If previously awakened by "Hai Planner", process whatever is spoken now
        if (isAwaitingSpecificExpense) {
            isAwaitingSpecificExpense = false
            if (containsNumbersOrMoneyWords(spokenText)) {
                processTransactionFromSpeech(spokenText)
            } else {
                speakFeedback("Maaf, nominal transaksi tidak terdeteksi. Silakan coba lagi dengan mengucapkan nominalnya.")
                scheduleRestartListening(2000)
            }
            return
        }

        // 3. Direct transaction detection without hotword (e.g. "Beli bensin 20 ribu")
        if (containsNumbersOrMoneyWords(spokenText) && spokenText.length > 5) {
            processTransactionFromSpeech(spokenText)
            return
        }

        // Otherwise continue listening
        scheduleRestartListening(1200)
    }

    private fun containsNumbersOrMoneyWords(text: String): Boolean {
        val lower = text.lowercase(Locale.getDefault())
        val hasDigits = Regex("""\d+""").containsMatchIn(text)
        val hasMoneyWords = lower.contains("ribu") || lower.contains("rb") || lower.contains("k") ||
                lower.contains("juta") || lower.contains("jt") || lower.contains("ratus") ||
                lower.contains("puluh") || lower.contains("rupiah") || lower.contains("beli") ||
                lower.contains("bayar") || lower.contains("nabung")
        return hasDigits || hasMoneyWords
    }

    private fun processTransactionFromSpeech(transcript: String) {
        updateNotification("🤖 Memproses suara & sinkronisasi pos anggaran...", null)

        serviceScope.launch(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(applicationContext)
                val dao = db.budgetDao()

                // Determine target month (current month or default 2026-01)
                val cal = Calendar.getInstance()
                val currentYear = cal.get(Calendar.YEAR)
                val currentMonthNum = cal.get(Calendar.MONTH) + 1
                val monthIdFormatted = String.format(Locale.US, "%04d-%02d", currentYear, currentMonthNum)

                var targetMonth = dao.getMonthById(monthIdFormatted)
                if (targetMonth == null) {
                    targetMonth = dao.getMonthById("2026-01") ?: run {
                        val newMonth = BudgetMonth(
                            monthId = monthIdFormatted,
                            monthName = SimpleDateFormat("MMMM", Locale("id", "ID")).format(Date()),
                            year = currentYear
                        )
                        dao.insertMonth(newMonth)
                        newMonth
                    }
                }

                val mId = targetMonth.monthId
                val defaultDate = SimpleDateFormat("dd/MM/yyyy", Locale("id", "ID")).format(Date())

                // Analyze using Gemini AI / Indonesian offline NLP
                val analysis = GeminiVoiceService.analyzeVoiceTransaction(transcript, defaultDate)

                if (analysis.amount <= 0 || analysis.itemTitle.isBlank()) {
                    withContext(Dispatchers.Main) {
                        speakFeedback("Maaf, tidak dapat mengenali nominal dari '$transcript'. Ucapkan contoh: Beli kopi 25 ribu.")
                        scheduleRestartListening(2000)
                    }
                    return@launch
                }

                // 2. Synchronize directly into Pos Anggaran (Room Database) & Wallet deduction
                val itemDate = if (analysis.date.isNotBlank()) analysis.date else defaultDate
                val chosenWallet = analysis.walletName.ifEmpty { "Uang Cash" }
                var posName = "Pengeluaran Harian"

                // Adjust Wallet Balance helper
                val allWallets = dao.getAllWalletsOnce()
                val targetWallet = allWallets.find { it.name.equals(chosenWallet.trim(), ignoreCase = true) }
                    ?: allWallets.find { it.isDefault }
                    ?: allWallets.firstOrNull()

                if (targetWallet != null) {
                    val delta = if (analysis.targetCategory == "INCOME") analysis.amount else -analysis.amount
                    dao.updateWalletBalance(targetWallet.id, targetWallet.balance + delta)
                }

                when (analysis.targetCategory) {
                    "FIXED" -> {
                        posName = "Pengeluaran Tetap (Fixed Cost)"
                        dao.insertFixedExpense(
                            FixedExpenseItem(
                                monthId = mId,
                                title = analysis.itemTitle,
                                priority = analysis.priority.ifEmpty { "High" },
                                plannedAmount = 0.0,
                                actualAmount = analysis.amount,
                                date = itemDate,
                                walletName = chosenWallet
                            )
                        )
                    }
                    "SAVINGS" -> {
                        posName = "Pos Tabungan & Investasi"
                        dao.insertSaving(
                            SavingItem(
                                monthId = mId,
                                title = analysis.itemTitle,
                                priority = analysis.priority.ifEmpty { "High" },
                                plannedAmount = 0.0,
                                actualAmount = analysis.amount,
                                date = itemDate,
                                walletName = chosenWallet
                            )
                        )
                    }
                    "SUBSCRIPTION" -> {
                        posName = "Pos Langganan Digital"
                        dao.insertSubscription(
                            SubscriptionItem(
                                monthId = mId,
                                title = analysis.itemTitle,
                                priority = analysis.priority.ifEmpty { "Low" },
                                plannedAmount = 0.0,
                                actualAmount = analysis.amount,
                                date = itemDate,
                                walletName = chosenWallet
                            )
                        )
                    }
                    "INCOME" -> {
                        posName = "Pemasukan Kas"
                        dao.insertIncome(
                            IncomeItem(
                                monthId = mId,
                                source = analysis.itemTitle,
                                type = analysis.subCategory.ifEmpty { "Pemasukan" },
                                amount = analysis.amount,
                                date = itemDate,
                                walletName = chosenWallet
                            )
                        )
                    }
                    else -> {
                        // Pos Jajan & Pengeluaran Harian (Daily Tracker + Variable Expense)
                        posName = "Pos Jajan & Belanja"
                        dao.insertDailyExpense(
                            DailyExpenseItem(
                                monthId = mId,
                                date = itemDate,
                                title = analysis.itemTitle,
                                category = analysis.subCategory.ifEmpty { "Jajan" },
                                quantity = 1,
                                unitPrice = analysis.amount,
                                totalAmount = analysis.amount,
                                notes = "Otomatis via Suara AI ('Hai Planner') - Sumber: $chosenWallet",
                                walletName = chosenWallet
                            )
                        )
                        dao.insertVariableExpense(
                            VariableExpenseItem(
                                monthId = mId,
                                title = analysis.itemTitle,
                                priority = "Medium",
                                plannedAmount = 0.0,
                                actualAmount = analysis.amount,
                                date = itemDate,
                                walletName = chosenWallet
                            )
                        )
                    }
                }

                _lastSyncedTransaction.value = analysis

                // Feedback with Wallet name
                val formattedAmt = CurrencyUtils.formatRupiah(analysis.amount)
                val speechMessage = if (analysis.targetCategory == "INCOME") {
                    "Pemasukan ${analysis.itemTitle} sebesar $formattedAmt berhasil dicatat dan menambah $chosenWallet!"
                } else {
                    "Tercatat! ${analysis.itemTitle} sebesar $formattedAmt berhasil masuk ke $posName dan memotong $chosenWallet."
                }

                withContext(Dispatchers.Main) {
                    vibrateDevice()
                    speakFeedback(speechMessage)
                    updateNotification(
                        "✅ Berhasil: ${analysis.itemTitle} ($formattedAmt)",
                        "Masuk $posName & terpotong dari $chosenWallet."
                    )
                    scheduleRestartListening(3500)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error storing voice transaction: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    speakFeedback("Terjadi kesalahan saat menyimpan transaksi.")
                    scheduleRestartListening(2000)
                }
            }
        }
    }

    private fun speakFeedback(message: String) {
        if (isTtsReady && textToSpeech != null) {
            try {
                textToSpeech?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "VOICE_ID_${System.currentTimeMillis()}")
            } catch (e: Exception) {
                Log.e(TAG, "TTS speak failed: ${e.message}")
            }
        }
    }

    private fun vibrateDevice() {
        try {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(120)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Vibration failed: ${e.message}")
        }
    }

    private fun startListeningLoop() {
        mainHandler.post {
            restartListeningNow()
        }
    }

    private fun restartListeningNow() {
        if (!isServiceActive.value) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Cannot start listening: RECORD_AUDIO permission missing")
            return
        }

        try {
            speechRecognizer?.cancel()
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "id-ID")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
            }
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error restarting speech listener: ${e.message}")
            scheduleRestartListening(2000)
        }
    }

    private fun scheduleRestartListening(delayMillis: Long) {
        restartListeningRunnable?.let { mainHandler.removeCallbacks(it) }
        restartListeningRunnable = Runnable {
            restartListeningNow()
        }
        mainHandler.postDelayed(restartListeningRunnable!!, delayMillis)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Asisten Suara Planner (Hai Planner)",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Layanan siaga deteksi suara 'Hai Planner' untuk pencatatan transaksi otomatis"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(title: String, subtitle: String?): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerListenIntent = Intent(this, BackgroundVoiceService::class.java).apply {
            action = ACTION_TRIGGER_LISTEN
        }
        val triggerPendingIntent = PendingIntent.getService(
            this, 1, triggerListenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, BackgroundVoiceService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 2, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🎙️ Asisten Suara Hai Planner Aktif")
            .setContentText(title)
            .setSubText(subtitle ?: "Katakan 'Hai Planner' kapan saja")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(android.R.drawable.ic_btn_speak_now, "Bicara", triggerPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Matikan", stopPendingIntent)
            .build()
    }

    private fun updateNotification(title: String, subtitle: String?) {
        val notification = buildForegroundNotification(title, subtitle)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "BackgroundVoiceService destroyed")
        _isServiceActive.value = false
        _isListeningState.value = false

        restartListeningRunnable?.let { mainHandler.removeCallbacks(it) }
        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.e(TAG, "Error destroying speech recognizer: ${e.message}")
        }
        try {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping TTS: ${e.message}")
        }
        serviceJob.cancel()
    }

    companion object {
        const val TAG = "BackgroundVoiceService"
        const val CHANNEL_ID = "planner_voice_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_STOP_SERVICE = "com.example.service.STOP_VOICE_SERVICE"
        const val ACTION_TRIGGER_LISTEN = "com.example.service.TRIGGER_LISTEN"

        private val _isServiceActive = MutableStateFlow(false)
        val isServiceActive = _isServiceActive.asStateFlow()

        private val _isListeningState = MutableStateFlow(false)
        val isListeningState = _isListeningState.asStateFlow()

        private val _lastSpokenText = MutableStateFlow("")
        val lastSpokenText = _lastSpokenText.asStateFlow()

        private val _lastSyncedTransaction = MutableStateFlow<VoiceTransactionAnalysis?>(null)
        val lastSyncedTransaction = _lastSyncedTransaction.asStateFlow()

        fun start(context: Context) {
            val intent = Intent(context, BackgroundVoiceService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(context, intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, BackgroundVoiceService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.startService(intent)
        }

        fun triggerVoiceInput(context: Context) {
            val intent = Intent(context, BackgroundVoiceService::class.java).apply {
                action = ACTION_TRIGGER_LISTEN
            }
            context.startService(intent)
        }
    }
}
