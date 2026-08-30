package com.example.util

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.VoiceTransactionAnalysis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object GeminiVoiceService {
    private const val TAG = "GeminiVoiceService"
    private const val GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun analyzeVoiceTransaction(
        transcript: String,
        currentDateFormatted: String = SimpleDateFormat("dd/MM/yyyy", Locale("id", "ID")).format(Date())
    ): VoiceTransactionAnalysis = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val aiResult = callGeminiApi(apiKey, transcript, currentDateFormatted)
                if (aiResult != null && aiResult.amount > 0) {
                    return@withContext aiResult
                }
            } catch (e: Exception) {
                Log.e(TAG, "Gemini API error, fallback to offline parser: ${e.message}")
            }
        }

        // Fallback to offline Indonesian NLP parser
        return@withContext parseIndonesianSpeechOffline(transcript, currentDateFormatted)
    }

    private fun callGeminiApi(apiKey: String, transcript: String, currentDate: String): VoiceTransactionAnalysis? {
        val prompt = """
            Kamu adalah Asisten AI Pencatat Keuangan & Pos Anggaran berbahasa Indonesia.
            Tugasmu: Analisis ucapan/suara transaksi keuangan berikut dan ekstrak data ke dalam JSON:
            Ucapan: "$transcript"
            Tanggal default hari ini: "$currentDate"

            Aturan Pos Anggaran (targetCategory):
            - "VARIABLE": Pengeluaran tidak tetap / jajan / harian seperti makanan, minuman, nasi padang, kopi, bensin, cemilan, belanja harian/bulanan, nongkrong, pakaian, servis, pulsa.
            - "FIXED": Pengeluaran tetap/wajib bulanan seperti sewa kos, kontrakan, listrik/PLN, air/PDAM, wifi indihome, asuransi, SPP, cicilan rumah.
            - "SAVINGS": Tabungan, dana darurat, investasi emas, saham, reksadana bibit/ajaib, celengan.
            - "SUBSCRIPTION": Langganan aplikasi, Netflix, Spotify, Canva, YouTube Premium, ChatGPT, tagihan langganan digital.
            - "INCOME": Pemasukan, gaji, bonus, freelance, penjualan, transfer masuk, honor.

            Aturan Sumber Anggaran / Dompet (walletName):
            - "Uang Cash": jika user menyebut cash, tunai, uang fisik, pegang uang.
            - "Saldo DANA": jika user menyebut dana, saldo dana, akun dana.
            - "Saldo Rekening": jika user menyebut rekening, transfer bank, bca, bri, mandiri, bni, jago, bsi, kartu debit.
            - "GoPay": jika user menyebut gopay, gojek.
            - "ShopeePay": jika user menyebut shopeepay, spay.
            - "OVO": jika user menyebut ovo.
            - Jika tidak disebutkan spesifik: gunakan "Uang Cash" untuk jajan/makanan harian, atau "Saldo Rekening" untuk tagihan/tetap/tabungan/gaji, atau "Saldo DANA" untuk belanja online.

            Kembalikan HANYA format JSON valid berikut tanpa markdown formatting tambahan:
            {
              "itemTitle": "nama barang atau tujuan transaksi ringkas",
              "amount": 25000.0,
              "date": "dd/MM/yyyy",
              "targetCategory": "VARIABLE",
              "subCategory": "Makan & Jajan",
              "priority": "Medium",
              "quantity": 1,
              "walletName": "Saldo DANA",
              "explanation": "Penjelasan singkat analisis",
              "deductionImpact": "Anggaran dipotong Rp 25.000 dari Saldo DANA"
            }
        """.trimIndent()

        val jsonBody = JSONObject().apply {
            val contentsArray = JSONArray().apply {
                put(JSONObject().apply {
                    val partsArray = JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    }
                    put("parts", partsArray)
                })
            }
            put("contents", contentsArray)
            val config = JSONObject().apply {
                put("temperature", 0.1)
                val responseFormat = JSONObject().apply {
                    put("type", "json_object")
                }
            }
            put("generationConfig", config)
        }

        val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
        val url = "$GEMINI_URL?key=$apiKey"
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()
        val responseString = response.body?.string() ?: return null

        val rootObj = JSONObject(responseString)
        val candidates = rootObj.optJSONArray("candidates") ?: return null
        val firstCandidate = candidates.optJSONObject(0) ?: return null
        val content = firstCandidate.optJSONObject("content") ?: return null
        val parts = content.optJSONArray("parts") ?: return null
        val textPart = parts.optJSONObject(0)?.optString("text") ?: return null

        val cleanJson = textPart.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val parsed = JSONObject(cleanJson)
        val title = parsed.optString("itemTitle", "Pengeluaran Suara")
        val amt = parsed.optDouble("amount", 0.0)
        val date = parsed.optString("date", currentDate)
        val cat = parsed.optString("targetCategory", "DAILY")
        val sub = parsed.optString("subCategory", "Jajan")
        val prio = parsed.optString("priority", "Medium")
        val qty = parsed.optInt("quantity", 1)
        val wallet = parsed.optString("walletName", if (cat == "FIXED" || cat == "SUBSCRIPTION" || cat == "SAVINGS" || cat == "INCOME") "Saldo Rekening" else "Uang Cash")
        val exp = parsed.optString("explanation", "Transaksi berhasil dianalisis dengan AI Gemini")
        val impact = parsed.optString("deductionImpact", "Saldo & jatah pos anggaran dipotong ${CurrencyUtils.formatRupiah(amt)} dari $wallet")

        return VoiceTransactionAnalysis(
            rawTranscript = transcript,
            itemTitle = title,
            amount = amt,
            date = if (date.isBlank()) currentDate else date,
            targetCategory = cat,
            subCategory = sub,
            priority = prio,
            quantity = qty,
            walletName = wallet,
            explanation = exp,
            deductionImpact = impact
        )
    }

    /**
     * Offline Indonesian Financial NLP Parser for Voice
     * Extremely fast, reliable regex + dictionary parser.
     */
    fun parseIndonesianSpeechOffline(
        transcript: String,
        defaultDate: String = SimpleDateFormat("dd/MM/yyyy", Locale("id", "ID")).format(Date())
    ): VoiceTransactionAnalysis {
        val lower = transcript.lowercase(Locale.getDefault())

        // 1. Extract Amount
        var extractedAmount = 0.0

        // Match patterns like: "50 ribu", "50rb", "50.000", "50k", "1.5 juta", "2jt", "30000", "ceban", "goceng", "gocap", "sejuta", "dua juta setengah"
        val regexKilo = Regex("""(\d+(?:[.,]\d+)?)\s*(?:ribu|rb|k\b)""", RegexOption.IGNORE_CASE)
        val regexJuta = Regex("""(\d+(?:[.,]\d+)?)\s*(?:juta|jt\b)""", RegexOption.IGNORE_CASE)
        val regexJutaSetengah = Regex("""(\d+)\s*(?:juta\s*setengah|jt\s*setengah)\b""", RegexOption.IGNORE_CASE)
        val regexPlainNumber = Regex("""\b(\d{1,3}(?:\.\d{3})+|\d{4,9})\b""")
        val regexWordNumber = Regex("""\b(seceng|noceng|goceng|ceban|ceng|gocap|cepek|gopik|seceng|sepuluh|sebelas|dua belas|lima belas|dua puluh|dua puluh lima|tiga puluh|tiga puluh lima|empat puluh|lima puluh|enam puluh|tujuh puluh|delapan puluh|sembilan puluh|seratus|seratus lima puluh|dua ratus|dua ratus lima puluh|tiga ratus|lima ratus|tujuh ratus lima puluh|sejuta|satu juta|dua juta|setengah juta)\s*(?:ribu|rb)?\b""", RegexOption.IGNORE_CASE)

        when {
            lower.contains("setengah juta") -> extractedAmount = 500000.0
            lower.contains("sejuta") || lower.contains("satu juta") -> {
                if (lower.contains("sejuta setengah") || lower.contains("satu juta setengah")) {
                    extractedAmount = 1500000.0
                } else {
                    extractedAmount = 1000000.0
                }
            }
            regexJutaSetengah.containsMatchIn(lower) -> {
                val match = regexJutaSetengah.find(lower)
                val base = match?.groupValues?.get(1)?.toDoubleOrNull() ?: 1.0
                extractedAmount = (base * 1000000.0) + 500000.0
            }
            regexJuta.containsMatchIn(lower) -> {
                val match = regexJuta.find(lower)
                val numStr = match?.groupValues?.get(1)?.replace(",", ".") ?: "0"
                extractedAmount = (numStr.toDoubleOrNull() ?: 0.0) * 1_000_000.0
            }
            regexKilo.containsMatchIn(lower) -> {
                val match = regexKilo.find(lower)
                val numStr = match?.groupValues?.get(1)?.replace(",", ".") ?: "0"
                extractedAmount = (numStr.toDoubleOrNull() ?: 0.0) * 1_000.0
            }
            lower.contains("ceban") -> extractedAmount = 10000.0
            lower.contains("goceng") -> extractedAmount = 5000.0
            lower.contains("gocap") -> extractedAmount = 50000.0
            lower.contains("seceng") -> extractedAmount = 1000.0
            lower.contains("cepek") -> extractedAmount = 100000.0
            regexPlainNumber.containsMatchIn(lower) -> {
                val match = regexPlainNumber.find(lower)
                val cleanStr = match?.groupValues?.get(1)?.replace(".", "") ?: "0"
                extractedAmount = cleanStr.toDoubleOrNull() ?: 0.0
            }
            regexWordNumber.containsMatchIn(lower) -> {
                when {
                    lower.contains("sepuluh ribu") -> extractedAmount = 10000.0
                    lower.contains("lima belas ribu") -> extractedAmount = 15000.0
                    lower.contains("dua puluh lima ribu") -> extractedAmount = 25000.0
                    lower.contains("dua puluh ribu") -> extractedAmount = 20000.0
                    lower.contains("tiga puluh lima ribu") -> extractedAmount = 35000.0
                    lower.contains("tiga puluh ribu") -> extractedAmount = 30000.0
                    lower.contains("empat puluh ribu") -> extractedAmount = 40000.0
                    lower.contains("lima puluh ribu") -> extractedAmount = 50000.0
                    lower.contains("seratus lima puluh ribu") -> extractedAmount = 150000.0
                    lower.contains("seratus ribu") -> extractedAmount = 100000.0
                    lower.contains("dua ratus lima puluh ribu") -> extractedAmount = 250000.0
                    lower.contains("dua ratus ribu") -> extractedAmount = 200000.0
                    lower.contains("tiga ratus ribu") -> extractedAmount = 300000.0
                    lower.contains("lima ratus ribu") -> extractedAmount = 500000.0
                    lower.contains("tujuh ratus lima puluh ribu") -> extractedAmount = 750000.0
                    lower.contains("dua juta") -> extractedAmount = 2000000.0
                }
            }
        }

        // 2. Extract Date
        var transactionDate = defaultDate
        val cal = Calendar.getInstance()
        if (lower.contains("kemarin")) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
            transactionDate = SimpleDateFormat("dd/MM/yyyy", Locale("id", "ID")).format(cal.time)
        } else if (lower.contains("lusa")) {
            cal.add(Calendar.DAY_OF_YEAR, 2)
            transactionDate = SimpleDateFormat("dd/MM/yyyy", Locale("id", "ID")).format(cal.time)
        } else {
            val dateRegex = Regex("""tanggal\s*(\d{1,2})""", RegexOption.IGNORE_CASE)
            val dateMatch = dateRegex.find(lower)
            if (dateMatch != null) {
                val day = dateMatch.groupValues[1].toIntOrNull() ?: cal.get(Calendar.DAY_OF_MONTH)
                val currentMonth = cal.get(Calendar.MONTH) + 1
                val currentYear = cal.get(Calendar.YEAR)
                transactionDate = String.format(Locale.US, "%02d/%02d/%d", day, currentMonth, currentYear)
            }
        }

        // 3. Extract Category & Title
        var targetCategory = "VARIABLE"
        var subCategory = "Makan & Jajan"
        var priority = "Medium"
        var explanation = "Pengeluaran tidak tetap / jajan terdeteksi dari suara"

        when {
            lower.contains("listrik") || lower.contains("pln") || lower.contains("air") || lower.contains("pdam") ||
            lower.contains("sewa kos") || lower.contains("kosan") || lower.contains("wifi") || lower.contains("indihome") ||
            lower.contains("spp") || lower.contains("kontrakan") || lower.contains("cicilan rumah") || lower.contains("asuransi") -> {
                targetCategory = "FIXED"
                subCategory = "Tagihan Pokok"
                priority = "High"
                explanation = "Dimasukkan ke Pengeluaran Tetap (Fixed Cost) karena merupakan tagihan/kebutuhan pokok bulanan"
            }
            lower.contains("nabung") || lower.contains("tabungan") || lower.contains("investasi") || lower.contains("emas") ||
            lower.contains("bibit") || lower.contains("ajaib") || lower.contains("reksadana") || lower.contains("saham") ||
            lower.contains("dana darurat") || lower.contains("celengan") -> {
                targetCategory = "SAVINGS"
                subCategory = "Tabungan & Investasi"
                priority = "High"
                explanation = "Dimasukkan ke Pos Tabungan & Investasi untuk mencapai target finansial"
            }
            lower.contains("netflix") || lower.contains("spotify") || lower.contains("canva") || lower.contains("chatgpt") ||
            lower.contains("youtube premium") || lower.contains("langganan") || lower.contains("disney") || lower.contains("icloud") -> {
                targetCategory = "SUBSCRIPTION"
                subCategory = "Langganan Digital"
                priority = "Low"
                explanation = "Dimasukkan ke Biaya Langganan & Hiburan Digital berkala"
            }
            lower.contains("gaji") || lower.contains("bonus") || lower.contains("freelance") || lower.contains("penjualan") ||
            lower.contains("transfer masuk") || lower.contains("pendapatan") || lower.contains("dikasih uang") || lower.contains("honor") -> {
                targetCategory = "INCOME"
                subCategory = "Pemasukan"
                priority = "High"
                explanation = "Dicatat sebagai Pendapatan / Pemasukan Kas baru"
            }
            else -> {
                targetCategory = "VARIABLE"
                subCategory = if (lower.contains("makan") || lower.contains("nasi") || lower.contains("kopi") || lower.contains("bensin") || lower.contains("cemilan") || lower.contains("jajan")) "Makan & Jajan" else "Belanja Variabel"
                priority = "Medium"
                explanation = "Dimasukkan ke Pos Pengeluaran Variabel & otomatis memotong anggaran"
            }
        }

        // 4. Extract Payment Wallet Source (Cash, DANA, Rekening, GoPay, ShopeePay, OVO, Seabank, Blu, Jago, LinkAja)
        var detectedWallet = when {
            lower.contains("dana") || lower.contains("saldo dana") -> "Saldo DANA"
            lower.contains("cash") || lower.contains("tunai") || lower.contains("uang fisik") || lower.contains("kantong") -> "Uang Cash"
            lower.contains("gopay") || lower.contains("gojek") || lower.contains("gofood") -> "GoPay"
            lower.contains("shopeepay") || lower.contains("spay") || lower.contains("shopee") -> "ShopeePay"
            lower.contains("ovo") -> "OVO"
            lower.contains("linkaja") -> "LinkAja"
            lower.contains("seabank") -> "SeaBank"
            lower.contains("jago") || lower.contains("bank jago") -> "Bank Jago"
            lower.contains("blu") || lower.contains("jenius") -> "Digital Bank"
            lower.contains("rekening") || lower.contains("bca") || lower.contains("bri") || lower.contains("mandiri") ||
            lower.contains("bni") || lower.contains("bsi") || lower.contains("kartu") || lower.contains("transfer") || lower.contains("m-banking") || lower.contains("mbanking") -> "Saldo Rekening"
            targetCategory == "FIXED" || targetCategory == "SUBSCRIPTION" || targetCategory == "SAVINGS" || targetCategory == "INCOME" -> "Saldo Rekening"
            else -> "Uang Cash"
        }

        // Clean Title
        var cleanTitle = transcript
            .replace(Regex("""(?i)\b(tadi|tadi siang|tadi pagi|tadi malam|hari ini|kemarin|tolong|catat|masukkan|beli|bayar|sebesar|seharga|nominal|rupiah|ribu|rb|k|juta|jt|pakai|lewat|pake|dana|cash|tunai|rekening|bca|bri|mandiri|bni|gopay|ovo|shopeepay|seabank|jago|linkaja|qris)\b"""), "")
            .replace(Regex("""\d+"""), "")
            .replace(Regex("""\s+"""), " ")
            .trim()

        if (cleanTitle.length <= 2) {
            cleanTitle = when (targetCategory) {
                "FIXED" -> "Tagihan Pokok Bulanan"
                "SAVINGS" -> "Nabung / Investasi"
                "SUBSCRIPTION" -> "Langganan Digital"
                "INCOME" -> "Pendapatan Masuk"
                "VARIABLE" -> "Belanja Variabel"
                else -> "Jajan & Makan"
            }
        } else {
            cleanTitle = cleanTitle.split(" ").joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            }
        }

        val deductionImpact = if (targetCategory == "INCOME") {
            "Menambah kas $detectedWallet sebesar ${CurrencyUtils.formatRupiah(extractedAmount)}"
        } else {
            "Memotong $detectedWallet sebesar ${CurrencyUtils.formatRupiah(extractedAmount)}"
        }

        return VoiceTransactionAnalysis(
            rawTranscript = transcript,
            itemTitle = cleanTitle,
            amount = extractedAmount,
            date = transactionDate,
            targetCategory = targetCategory,
            subCategory = subCategory,
            priority = priority,
            quantity = 1,
            walletName = detectedWallet,
            explanation = explanation,
            deductionImpact = deductionImpact
        )
    }
}
