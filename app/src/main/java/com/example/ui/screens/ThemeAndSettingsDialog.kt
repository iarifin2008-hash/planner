package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.data.model.UserAccount
import com.example.data.model.UserProfile
import com.example.ui.theme.*

@Composable
fun ThemeAndSettingsDialog(
    profile: UserProfile?,
    userAccount: UserAccount? = null,
    isSyncing: Boolean = false,
    syncStatusMessage: String = "",
    onDismiss: () -> Unit,
    onSaveThemeSettings: (themeId: String, fontColorId: String, fontSizeScale: Float) -> Unit,
    onSaveProfile: (name: String, pin: String, avatarId: String, isPinEnabled: Boolean) -> Unit,
    onTriggerSync: () -> Unit = {},
    onLogoutAccount: () -> Unit = {},
    onConnectNewDevice: (String) -> Unit = {},
    isVoiceServiceRunning: Boolean = false,
    onToggleVoiceService: (Boolean) -> Unit = {},
    onTriggerVoiceTest: () -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Tema, 1: Font, 2: Multi-Device Sync, 3: Asisten Suara & Profil

    var currentThemeId by remember { mutableStateOf(profile?.themePreset ?: "SHARK_BLUE") }
    var currentFontColorId by remember { mutableStateOf(profile?.fontColorPreset ?: "DEEP_CHARCOAL") }
    var currentFontScale by remember { mutableFloatStateOf(profile?.fontSizeScale ?: 1.0f) }

    var userName by remember { mutableStateOf(profile?.name ?: "Sobat Cuan") }
    var userPin by remember { mutableStateOf(profile?.pinHash ?: "1234") }
    var isPinEnabled by remember { mutableStateOf(profile?.isPinEnabled ?: true) }

    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Pengaturan & Multi-Device",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = PastelSkyDark
                        )
                        Text(
                            text = "Kustomisasi warna pastel, font, multi-device & asisten suara",
                            fontSize = 10.sp,
                            color = TextSecondaryMuted
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Navigation Tabs
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = PastelSkySurface,
                    contentColor = PastelSkyDark,
                    edgePadding = 4.dp,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Tema", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Font Teks", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Multi-Device", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = { Text("Asisten & PIN", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        0 -> ThemePresetTab(
                            selectedThemeId = currentThemeId,
                            onSelectTheme = { currentThemeId = it }
                        )
                        1 -> FontSettingsTab(
                            selectedFontColorId = currentFontColorId,
                            currentFontScale = currentFontScale,
                            onSelectFontColor = { currentFontColorId = it },
                            onChangeFontScale = { currentFontScale = it }
                        )
                        2 -> MultiDeviceSyncTab(
                            userAccount = userAccount,
                            isSyncing = isSyncing,
                            syncStatusMessage = syncStatusMessage,
                            onTriggerSync = onTriggerSync,
                            onConnectNewDevice = onConnectNewDevice,
                            onLogout = onLogoutAccount
                        )
                        3 -> VoiceAndProfileTab(
                            name = userName,
                            pin = userPin,
                            isPinEnabled = isPinEnabled,
                            isVoiceActive = isVoiceServiceRunning,
                            onNameChange = { userName = it },
                            onPinChange = { userPin = it },
                            onPinToggle = { isPinEnabled = it },
                            onToggleVoice = onToggleVoiceService,
                            onTestVoice = onTriggerVoiceTest
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Save Action
                Button(
                    onClick = {
                        onSaveThemeSettings(currentThemeId, currentFontColorId, currentFontScale)
                        onSaveProfile(userName, userPin, "shark_happy", isPinEnabled)
                        Toast.makeText(context, "Pengaturan berhasil disimpan", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PastelSkyPrimary),
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Terapkan & Simpan", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ThemePresetTab(
    selectedThemeId: String,
    onSelectTheme: (String) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text(
                text = "Pilih Nuansa Warna Aplikasi:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark
            )
        }

        items(ThemePreset.entries.size) { index ->
            val preset = ThemePreset.entries[index]
            val isSelected = preset.id.equals(selectedThemeId, ignoreCase = true)

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = if (isSelected) PastelSkyLight else Color(0xFFF8FAFC)),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(if (isSelected) PastelSkyPrimary else Color(0xFFE2E8F0))
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectTheme(preset.id) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(preset.primary)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = preset.displayName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                            Text(
                                text = "Nuansa ${preset.displayName}",
                                fontSize = 10.sp,
                                color = TextSecondaryMuted
                            )
                        }
                    }
                    RadioButton(
                        selected = isSelected,
                        onClick = { onSelectTheme(preset.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FontSettingsTab(
    selectedFontColorId: String,
    currentFontScale: Float,
    onSelectFontColor: (String) -> Unit,
    onChangeFontScale: (Float) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text(
                text = "1. Pilihan Warna Font Teks:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark
            )
        }

        items(FontColorPreset.entries.size) { index ->
            val preset = FontColorPreset.entries[index]
            val isSelected = preset.id.equals(selectedFontColorId, ignoreCase = true)

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = if (isSelected) PastelSkyLight else Color(0xFFF8FAFC)),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(if (isSelected) PastelSkyPrimary else Color(0xFFE2E8F0))
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectFontColor(preset.id) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(preset.primaryColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = preset.displayName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = preset.primaryColor
                        )
                    }
                    RadioButton(
                        selected = isSelected,
                        onClick = { onSelectFontColor(preset.id) }
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "2. Besar Kecil Font Teks:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimaryDark
            )
        }

        items(FontSizePreset.entries.size) { index ->
            val sizePreset = FontSizePreset.entries[index]
            val isSelected = kotlin.math.abs(currentFontScale - sizePreset.scale) < 0.05f

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = if (isSelected) PastelMintLight else Color(0xFFF8FAFC)),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(if (isSelected) PastelMintSavings else Color(0xFFE2E8F0))
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onChangeFontScale(sizePreset.scale) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = sizePreset.label,
                            fontSize = (12 * sizePreset.scale).sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimaryDark
                        )
                        Text(
                            text = "Contoh: Rp 1.250.000 (Skala ${sizePreset.scale}x)",
                            fontSize = (10 * sizePreset.scale).sp,
                            color = TextSecondaryMuted
                        )
                    }
                    RadioButton(
                        selected = isSelected,
                        onClick = { onChangeFontScale(sizePreset.scale) },
                        colors = RadioButtonDefaults.colors(selectedColor = PastelMintSavings)
                    )
                }
            }
        }
    }
}

@Composable
private fun MultiDeviceSyncTab(
    userAccount: UserAccount?,
    isSyncing: Boolean,
    syncStatusMessage: String,
    onTriggerSync: () -> Unit,
    onConnectNewDevice: (String) -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    var inputNewSyncCode by remember { mutableStateOf("") }
    val syncCode = userAccount?.syncCode?.ifBlank { "CUAN-7701" } ?: "CUAN-7701"

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PastelSkyLight),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(PastelSkyPrimary)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudDone, contentDescription = null, tint = PastelSkyDark)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Status Sinkronisasi Akun", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PastelSkyDark)
                        }
                        if (isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = PastelSkyDark)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (syncStatusMessage.isNotBlank()) syncStatusMessage else "Tersambung ke Cloud Sync Engine",
                        fontSize = 11.sp,
                        color = PastelSkyDark
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Divider(color = PastelSkyPrimary.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Email Terhubung:", fontSize = 10.sp, color = TextSecondaryMuted)
                    Text(userAccount?.email?.ifBlank { "akun.utama@planner.id" } ?: "akun.utama@planner.id", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimaryDark)

                    Spacer(modifier = Modifier.height(6.dp))

                    Text("Kode Sinkronisasi Perangkat:", fontSize = 10.sp, color = TextSecondaryMuted)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = syncCode,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E3A8A)
                        )
                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Kode Sync", syncCode)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Kode $syncCode disalin!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PastelSkyDark),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Salin Kode", fontSize = 10.sp)
                        }
                    }
                    Text(
                        text = "💡 Masukkan kode ini di HP atau device lain untuk langsung menyambungkan data.",
                        fontSize = 10.sp,
                        color = TextSecondaryMuted,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        item {
            Button(
                onClick = onTriggerSync,
                enabled = !isSyncing,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF74C69D)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(42.dp)
            ) {
                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Sinkronkan Data Sekarang (Sync Now)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFE2E8F0))),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Hubungkan ke Device Lain (Input Kode):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = inputNewSyncCode,
                        onValueChange = { inputNewSyncCode = it.uppercase() },
                        label = { Text("Kode Sinkronisasi Device Lain") },
                        placeholder = { Text("misal: CUAN-9921") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (inputNewSyncCode.isNotBlank()) {
                                onConnectNewDevice(inputNewSyncCode)
                                inputNewSyncCode = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PastelSkyPrimary),
                        modifier = Modifier.fillMaxWidth().height(38.dp)
                    ) {
                        Text("Hubungkan & Timpa dengan Data Kode Ini", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            OutlinedButton(
                onClick = onLogout,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE63946)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Keluar Akun / Hubungkan Akun Lain", fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun VoiceAndProfileTab(
    name: String,
    pin: String,
    isPinEnabled: Boolean,
    isVoiceActive: Boolean,
    onNameChange: (String) -> Unit,
    onPinChange: (String) -> Unit,
    onPinToggle: (Boolean) -> Unit,
    onToggleVoice: (Boolean) -> Unit,
    onTestVoice: () -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Voice Assistant in Background
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = if (isVoiceActive) PastelMintLight else Color(0xFFF8FAFC)),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(if (isVoiceActive) PastelMintSavings else Color(0xFFE2E8F0))
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Mic,
                                contentDescription = null,
                                tint = if (isVoiceActive) PastelMintSavings else TextSecondaryMuted
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    "Asisten Suara 'Hai Planner'",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimaryDark
                                )
                                Text(
                                    if (isVoiceActive) "🟢 Aktif Siaga di Latar Belakang" else "⚪ Nonaktif",
                                    fontSize = 10.sp,
                                    color = if (isVoiceActive) Color(0xFF2D6A4F) else TextSecondaryMuted
                                )
                            }
                        }
                        Switch(
                            checked = isVoiceActive,
                            onCheckedChange = onToggleVoice,
                            colors = SwitchDefaults.colors(checkedThumbColor = PastelMintSavings)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "💡 Katakan 'Hai Planner' atau 'Hai Planner beli makan 25 ribu' kapan saja walaupun aplikasi ditutup. AI akan memproses ucapan, mencatat ke Pos Anggaran, dan memotong saldo secara otomatis.",
                        fontSize = 10.sp,
                        color = TextSecondaryMuted
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onTestVoice,
                        colors = ButtonDefaults.buttonColors(containerColor = PastelSkyPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(36.dp)
                    ) {
                        Icon(Icons.Default.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Tes Bicara Asisten Suara", fontSize = 11.sp)
                    }
                }
            }
        }

        // Profile & Security PIN
        item {
            Text("Pengaturan Profil & PIN:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimaryDark)
        }

        item {
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Nama Pengguna") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(PastelSkySurface)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Kunci Aplikasi dengan PIN", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PastelSkyDark)
                    Text("Meminta PIN 4-digit saat aplikasi dibuka", fontSize = 10.sp, color = TextSecondaryMuted)
                }
                Switch(
                    checked = isPinEnabled,
                    onCheckedChange = onPinToggle
                )
            }
        }

        if (isPinEnabled) {
            item {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) onPinChange(it) },
                    label = { Text("PIN Keamanan (4 Digit)") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
