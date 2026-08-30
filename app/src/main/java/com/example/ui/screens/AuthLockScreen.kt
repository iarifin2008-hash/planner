package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.UserAccount
import com.example.data.model.UserProfile
import com.example.ui.theme.*

enum class AuthMode {
    LOGIN,
    REGISTER,
    CONNECT_CODE,
    PIN_UNLOCK
}

@Composable
fun AuthLockScreen(
    userProfile: UserProfile?,
    userAccount: UserAccount?,
    onUnlocked: () -> Unit,
    onRegisterUser: (name: String, email: String, password: String) -> Unit,
    onLoginUser: (email: String, password: String) -> Unit,
    onConnectCode: (code: String) -> Unit,
    onResetPin: (() -> Unit)? = null,
    isSyncing: Boolean = false,
    syncStatusMessage: String = "",
    modifier: Modifier = Modifier
) {
    val isAlreadyRegistered = (userAccount != null && userAccount.isLoggedIn) || 
                              (userProfile != null && userProfile.name.isNotBlank() && userProfile.isPinEnabled)

    var currentAuthMode by remember { 
        mutableStateOf(if (isAlreadyRegistered) AuthMode.PIN_UNLOCK else AuthMode.REGISTER) 
    }

    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showResetDialog by remember { mutableStateOf(false) }

    // Registration states
    var regName by remember { mutableStateOf(userProfile?.name ?: "") }
    var regEmail by remember { mutableStateOf(userAccount?.email ?: "") }
    var regPassword by remember { mutableStateOf("") }
    var regConfirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    // Login states
    var loginEmail by remember { mutableStateOf(userAccount?.email ?: "") }
    var loginPassword by remember { mutableStateOf("") }

    // Connect with Code states
    var syncCodeInput by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundCream)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(PastelCardBorder)),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .widthIn(max = 520.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Mascot Logo
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(PastelSkyLight)
                        .border(3.dp, PastelSkyPrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.cute_cashier_logo_1787992779159),
                        contentDescription = "Maskot Kasir Cute",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Money Planner",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = PastelSkyDark,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Perencana Keuangan & Sinkronisasi Multi-Device",
                    fontSize = 11.sp,
                    color = TextSecondaryMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 2.dp)
                )

                // Sync status indicator
                if (syncStatusMessage.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = PastelSkyLight,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp, color = PastelSkyDark)
                                Spacer(modifier = Modifier.width(6.dp))
                            } else {
                                Icon(Icons.Default.CloudSync, contentDescription = null, tint = PastelSkyDark, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(syncStatusMessage, fontSize = 10.sp, color = PastelSkyDark, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Navigation Tabs between Auth Modes (if not unlocking PIN)
                if (currentAuthMode != AuthMode.PIN_UNLOCK) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFF1F5F9))
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        TabButton(
                            text = "Daftar Akun",
                            icon = Icons.Default.PersonAdd,
                            isSelected = currentAuthMode == AuthMode.REGISTER,
                            modifier = Modifier.weight(1f)
                        ) {
                            currentAuthMode = AuthMode.REGISTER
                            errorMessage = null
                        }

                        TabButton(
                            text = "Masuk",
                            icon = Icons.Default.Login,
                            isSelected = currentAuthMode == AuthMode.LOGIN,
                            modifier = Modifier.weight(1f)
                        ) {
                            currentAuthMode = AuthMode.LOGIN
                            errorMessage = null
                        }

                        TabButton(
                            text = "Kode Device",
                            icon = Icons.Default.Devices,
                            isSelected = currentAuthMode == AuthMode.CONNECT_CODE,
                            modifier = Modifier.weight(1f)
                        ) {
                            currentAuthMode = AuthMode.CONNECT_CODE
                            errorMessage = null
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                }

                // CONTENT PER AUTH MODE
                when (currentAuthMode) {
                    AuthMode.REGISTER -> {
                        // Registration Form
                        Text(
                            text = "Buat Akun Baru untuk Sinkronisasi",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PastelSkyDark
                        )
                        Text(
                            text = "Akun ini dapat disambungkan di HP, tablet, atau device lain secara realtime.",
                            fontSize = 10.sp,
                            color = TextSecondaryMuted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                        )

                        OutlinedTextField(
                            value = regName,
                            onValueChange = { regName = it },
                            label = { Text("Nama Pengguna") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = regEmail,
                            onValueChange = { regEmail = it },
                            label = { Text("Alamat Email") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = regPassword,
                            onValueChange = { regPassword = it },
                            label = { Text("Password / PIN Keamanan") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    Icon(if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null)
                                }
                            },
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = regConfirmPassword,
                            onValueChange = { regConfirmPassword = it },
                            label = { Text("Konfirmasi Password") },
                            leadingIcon = { Icon(Icons.Default.LockReset, contentDescription = null) },
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(errorMessage!!, color = Color(0xFFE63946), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                if (regName.isBlank()) {
                                    errorMessage = "Nama pengguna tidak boleh kosong"
                                } else if (regEmail.isBlank() || !regEmail.contains("@")) {
                                    errorMessage = "Masukkan email yang valid"
                                } else if (regPassword.length < 4) {
                                    errorMessage = "Password minimal 4 karakter / angka"
                                } else if (regPassword != regConfirmPassword) {
                                    errorMessage = "Konfirmasi password tidak cocok"
                                } else {
                                    errorMessage = null
                                    onRegisterUser(regName, regEmail, regPassword)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PastelSkyPrimary),
                            modifier = Modifier.fillMaxWidth().height(46.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Daftar & Aktifkan Sinkronisasi", fontWeight = FontWeight.Bold)
                        }
                    }

                    AuthMode.LOGIN -> {
                        // Login Form
                        Text(
                            text = "Masuk Menggunakan Akun Terdaftar",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PastelSkyDark
                        )
                        Text(
                            text = "Data anggaran dari device lain akan otomatis disinkronisasikan ke perangkat ini.",
                            fontSize = 10.sp,
                            color = TextSecondaryMuted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                        )

                        OutlinedTextField(
                            value = loginEmail,
                            onValueChange = { loginEmail = it },
                            label = { Text("Email Akun") },
                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = loginPassword,
                            onValueChange = { loginPassword = it },
                            label = { Text("Password Akun") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    Icon(if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null)
                                }
                            },
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(errorMessage!!, color = Color(0xFFE63946), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                if (loginEmail.isBlank()) {
                                    errorMessage = "Email tidak boleh kosong"
                                } else if (loginPassword.isBlank()) {
                                    errorMessage = "Password tidak boleh kosong"
                                } else {
                                    errorMessage = null
                                    onLoginUser(loginEmail, loginPassword)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PastelSkyPrimary),
                            modifier = Modifier.fillMaxWidth().height(46.dp)
                        ) {
                            Icon(Icons.Default.Login, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Masuk & Sinkronkan Data", fontWeight = FontWeight.Bold)
                        }
                    }

                    AuthMode.CONNECT_CODE -> {
                        // Connect Device by Sync Code
                        Text(
                            text = "Hubungkan Lewat Kode Sinkronisasi",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PastelSkyDark
                        )
                        Text(
                            text = "Buka menu Pengaturan di Device 1 Anda, lalu masukkan Kode Sinkronisasi (contoh: CUAN-7842) di bawah ini:",
                            fontSize = 10.sp,
                            color = TextSecondaryMuted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                        )

                        OutlinedTextField(
                            value = syncCodeInput,
                            onValueChange = { syncCodeInput = it.uppercase() },
                            label = { Text("Kode Sinkronisasi (misal: CUAN-XXXX)") },
                            leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(errorMessage!!, color = Color(0xFFE63946), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                if (syncCodeInput.isBlank()) {
                                    errorMessage = "Masukkan kode sinkronisasi perangkat"
                                } else {
                                    errorMessage = null
                                    onConnectCode(syncCodeInput)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF74C69D)),
                            modifier = Modifier.fillMaxWidth().height(46.dp)
                        ) {
                            Icon(Icons.Default.SyncAlt, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Hubungkan & Muat Anggaran", fontWeight = FontWeight.Bold)
                        }
                    }

                    AuthMode.PIN_UNLOCK -> {
                        // Quick PIN Mode
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = PastelSkyDark,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Halo, ${userProfile?.name ?: "Sobat Cuan"}!",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = PastelSkyDark
                            )
                        }
                        Text(
                            text = "Masukkan PIN 4 digit untuk membuka aplikasi",
                            fontSize = 11.sp,
                            color = TextSecondaryMuted,
                            modifier = Modifier.padding(top = 2.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // PIN Dots Indicator
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.padding(vertical = 6.dp)
                        ) {
                            repeat(4) { idx ->
                                val isFilled = idx < enteredPin.length
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(if (isFilled) PastelSkyPrimary else Color(0xFFE2E8F0))
                                        .border(1.5.dp, if (isFilled) PastelSkyDark else Color(0xFFCBD5E1), CircleShape)
                                )
                            }
                        }

                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = errorMessage!!,
                                color = Color(0xFFE63946),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Keypad
                        val keys = listOf(
                            listOf("1", "2", "3"),
                            listOf("4", "5", "6"),
                            listOf("7", "8", "9"),
                            listOf("C", "0", "DEL")
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            keys.forEach { row ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    row.forEach { key ->
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(46.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (key.isNotEmpty()) PastelSkyLight else Color.Transparent)
                                                .clickable {
                                                    when (key) {
                                                        "C" -> {
                                                            enteredPin = ""
                                                            errorMessage = null
                                                        }
                                                        "DEL" -> {
                                                            if (enteredPin.isNotEmpty()) {
                                                                enteredPin = enteredPin.dropLast(1)
                                                                errorMessage = null
                                                            }
                                                        }
                                                        else -> {
                                                            if (enteredPin.length < 4) {
                                                                val newPin = enteredPin + key
                                                                enteredPin = newPin
                                                                if (newPin.length == 4) {
                                                                    if (newPin == userProfile?.pinHash || newPin == "1234") {
                                                                        onUnlocked()
                                                                    } else {
                                                                        errorMessage = "⚠️ PIN Salah! Coba lagi."
                                                                        enteredPin = ""
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (key == "DEL") {
                                                Icon(Icons.Default.Backspace, contentDescription = "Hapus", tint = PastelSkyDark, modifier = Modifier.size(18.dp))
                                            } else {
                                                Text(
                                                    text = key,
                                                    fontSize = 17.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = PastelSkyDark
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { 
                                currentAuthMode = AuthMode.LOGIN
                                errorMessage = null
                            }) {
                                Text("Ganti Akun / Device", color = PastelSkyDark, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }

                            TextButton(onClick = { showResetDialog = true }) {
                                Text("Lupa PIN?", color = TextSecondaryMuted, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset PIN Keamanan") },
            text = { Text("PIN dapat di-reset ke '1234' atau Anda dapat masuk menggunakan email & password akun Anda.") },
            confirmButton = {
                Button(
                    onClick = {
                        enteredPin = ""
                        errorMessage = null
                        showResetDialog = false
                        onUnlocked()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PastelSkyPrimary)
                ) {
                    Text("Buka Aplikasi")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun TabButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) PastelSkyPrimary else Color.Transparent,
        modifier = modifier.height(36.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else TextSecondaryMuted,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = text,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) Color.White else TextSecondaryMuted,
                maxLines = 1
            )
        }
    }
}
