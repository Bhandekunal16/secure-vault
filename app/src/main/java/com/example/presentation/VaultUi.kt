package com.example.presentation

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.database.IntruderLogEntity
import com.example.database.NoteEntity
import com.example.database.PasswordEntity
import com.example.database.VaultFileEntity
import com.example.database.VaultFileType
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.provider.OpenableColumns
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asImageBitmap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultMainScreen(
    viewModel: VaultViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val accentIndex by viewModel.accentColorIndex.collectAsStateWithLifecycle()

    val systemInDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        "system" -> systemInDark
        "dark" -> true
        "light" -> false
        else -> systemInDark
    }

    // Determine Theme Configuration
    MyApplicationTheme(darkTheme = isDark) {
        Surface(
            modifier = modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            AnimatedContent(
                targetState = authState,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                },
                label = "auth_screen_transition"
            ) { state ->
                when (state) {
                    AuthScreenState.Setup -> {
                        PinSetupScreen(
                            onConfirm = { pin, decoy ->
                                viewModel.setupPin(pin, decoy)
                                Toast.makeText(context, "Lock Credentials Generated Successfully", Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                    AuthScreenState.Enter -> {
                        PinEnterScreen(
                            viewModel = viewModel,
                            onUnlocked = {
                                Toast.makeText(context, "Access Granted", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                    AuthScreenState.Unlocked -> {
                        AppDashboardShell(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

// --- SETUP PIN SCREEN ---
@Composable
fun PinSetupScreen(
    onConfirm: (String, String) -> Unit
) {
    var step by remember { mutableStateOf(1) } // 1: Main PIN, 2: Decoy PIN
    var pinValue by remember { mutableStateOf("") }
    var decoyPinValue by remember { mutableStateOf("") }
    var confirmPinValue by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Vault Key Security Graphic
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = "Security Shield Logo",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Initialize Secure Vault",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (step == 1) {
                "Define your Primary Secret 4-digit PIN lock. This enters the private vault."
            } else {
                "Establish a Decoy PIN. Entering this PIN opens an empty fake directory to mask your actual data."
            },
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (step == 1) {
            // Setup Main PIN & Confirmation
            OutlinedTextField(
                value = pinValue,
                onValueChange = { if (it.length <= 4) pinValue = it },
                label = { Text("Set 4-Digit primary PIN") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("setup_pin_field"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = confirmPinValue,
                onValueChange = { if (it.length <= 4) confirmPinValue = it },
                label = { Text("Confirm Primary PIN") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("confirm_pin_field"),
                shape = RoundedCornerShape(12.dp)
            )
        } else {
            // Setup Decoy PIN
            OutlinedTextField(
                value = decoyPinValue,
                onValueChange = { if (it.length <= 4) decoyPinValue = it },
                label = { Text("Set 4-Digit Decoy PIN (Panic Mode)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("setup_decoy_field"),
                shape = RoundedCornerShape(12.dp)
            )
        }

        if (errorText != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = errorText!!,
                color = MaterialTheme.colorScheme.error,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                errorText = null
                if (step == 1) {
                    if (pinValue.length < 4) {
                        errorText = "PIN must be exactly 4 digits long"
                        return@Button
                    }
                    if (pinValue != confirmPinValue) {
                        errorText = "Primary PIN and Confirmation PIN do not match!"
                        return@Button
                    }
                    step = 2
                } else {
                    if (decoyPinValue.length < 4) {
                        errorText = "Decoy PIN must be exactly 4 digits long"
                        return@Button
                    }
                    if (decoyPinValue == pinValue) {
                        errorText = "Decoy PIN cannot match your primary master PIN!"
                        return@Button
                    }
                    onConfirm(pinValue, decoyPinValue)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("setup_pin_button"),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = if (step == 1) "Configure Decoy PIN" else "Establish Enrypted Vault",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// --- PIN UNLOCK SCREEN ---
@Composable
fun PinEnterScreen(
    viewModel: VaultViewModel,
    onUnlocked: () -> Unit
) {
    var enteredText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val isDecoyMode by viewModel.isDecoyMode.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val activity = remember(context) { context as? androidx.fragment.app.FragmentActivity }
    val isBiometricsSelected by viewModel.isBiometricsEnabled.collectAsStateWithLifecycle()

    // Automatically trigger biometric authentication on screen load if enabled!
    if (isBiometricsSelected && activity != null) {
        LaunchedEffect(Unit) {
            showBiometricPrompt(
                activity = activity,
                onSuccess = {
                    viewModel.unlockWithBiometrics()
                    onUnlocked()
                },
                onError = { error ->
                    errorMessage = "Device authentication available: $error"
                }
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Secured Vault Indicator",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(64.dp)
                .padding(bottom = 16.dp)
        )

        Text(
            text = "Vault Locked",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Enter secure credentials to decrypt vault containers.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Large PIN Display Dots
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            for (i in 1..4) {
                val filled = enteredText.length >= i
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(
                            color = if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f),
                            shape = CircleShape
                        )
                )
            }
        }

        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // Custom Numeric PIN Pad for premium secure look
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val buttons = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("CLR", "0", "DEL")
            )

            for (row in buttons) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth(0.85f)
                ) {
                    for (label in row) {
                        Button(
                            onClick = {
                                errorMessage = null
                                when (label) {
                                    "CLR" -> enteredText = ""
                                    "DEL" -> {
                                        if (enteredText.isNotEmpty()) {
                                            enteredText = enteredText.dropLast(1)
                                        }
                                    }
                                    else -> {
                                        if (enteredText.length < 4) {
                                            val newText = enteredText + label
                                            enteredText = newText
                                            if (newText.length == 4) {
                                                viewModel.verifyPin(newText) { success ->
                                                    if (success) {
                                                        enteredText = ""
                                                        onUnlocked()
                                                    } else {
                                                        enteredText = ""
                                                        errorMessage = "Incorrect security code sequence logged!"
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(60.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (label == "CLR" || label == "DEL") {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                                contentColor = MaterialTheme.colorScheme.onBackground
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = if (label.length > 1) 13.sp else 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            if (isBiometricsSelected && activity != null) {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        errorMessage = null
                        showBiometricPrompt(
                            activity = activity,
                            onSuccess = {
                                viewModel.unlockWithBiometrics()
                                onUnlocked()
                            },
                            onError = { error ->
                                errorMessage = error
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth(0.85f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "Trigger Device Biometrics Unlock",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Unlock with Biometrics", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// --- SHELL CONTAINER WITH BOTTOM NAVIGATION ---
@Composable
fun AppDashboardShell(
    viewModel: VaultViewModel
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Home, 1: Files, 2: Notes, 3: Passwords, 4: Settings/Logs
    val isDecoyMode by viewModel.isDecoyMode.collectAsStateWithLifecycle()

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                windowInsets = WindowInsets.navigationBars
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Dashboard, "Dashboard") },
                    label = { Text("Dashboard", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Folder, "Vault Files") },
                    label = { Text("Files", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Description, "Secure Notes") },
                    label = { Text("Notes", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.VpnKey, "Passwords") },
                    label = { Text("Passwords", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                )
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    icon = { 
                        Icon(
                            imageVector = if (isDecoyMode) Icons.Default.VerifiedUser else Icons.Default.Security, 
                            contentDescription = "Security Logging"
                        ) 
                    },
                    label = { Text("Security", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            when (selectedTab) {
                0 -> DashboardScreen(viewModel = viewModel, onNavigateToTab = { selectedTab = it })
                1 -> VaultMediaScreen(viewModel = viewModel)
                2 -> SecureNotesScreen(viewModel = viewModel)
                3 -> PasswordSafeScreen(viewModel = viewModel)
                4 -> SettingsAndLogsScreen(viewModel = viewModel)
            }
        }
    }
}

// --- PANEL 1: DASHBOARD ---
@Composable
fun DashboardScreen(
    viewModel: VaultViewModel,
    onNavigateToTab: (Int) -> Unit
) {
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val passwords by viewModel.passwords.collectAsStateWithLifecycle()
    val files by viewModel.vaultFiles.collectAsStateWithLifecycle()
    val isDecoyMode by viewModel.isDecoyMode.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val uninstallWarningDismissed by viewModel.uninstallWarningDismissed.collectAsStateWithLifecycle()
    var showWarningPopup by remember { mutableStateOf(!uninstallWarningDismissed) }
    
    // States for bulk export progress
    var isBulkExporting by remember { mutableStateOf(false) }
    var bulkExportProgress by remember { mutableStateOf(0 to 0) } // current to total

    if (showWarningPopup && !isDecoyMode) {
        AlertDialog(
            onDismissRequest = { showWarningPopup = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Security Alert",
                    tint = Color(0xFFEF4444), // Crimson alarm red
                    modifier = Modifier.size(40.dp)
                )
            },
            title = {
                Text(
                    text = "Uninstall Data Loss Warning",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Because this is a zero-knowledge hardware-encrypted local vault, your private files are stored securely inside the app's isolated sandbox.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "⚠️ WARNING: If you uninstall this app or clear its device data, Android will permanently destroy your private key material and database. You will lose all your data!",
                        fontSize = 13.sp,
                        color = Color(0xFFEF4444),
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "To preserve your safe files, click \"Export All to Device\" to decrypt and transfer them back to your public Downloads folder before deleting the app.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (isBulkExporting) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Exporting Backup...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("${bulkExportProgress.first}/${bulkExportProgress.second}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { if (bulkExportProgress.second > 0) bulkExportProgress.first.toFloat() / bulkExportProgress.second.toFloat() else 0f },
                                    modifier = Modifier.fillMaxWidth(),
                                    color = Color(0xFF10B981)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (files.isEmpty()) {
                            Toast.makeText(context, "Your secure vault is empty - nothing to export!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isBulkExporting = true
                        bulkExportProgress = 0 to files.size
                        bulkExportFiles(
                            context = context,
                            viewModel = viewModel,
                            files = files,
                            onProgress = { current, total ->
                                bulkExportProgress = current to total
                            },
                            onComplete = { success, total ->
                                isBulkExporting = false
                                Toast.makeText(context, "Successfully decrypted & exported $success of $total files to Downloads!", Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)), // Emerald green
                    shape = RoundedCornerShape(8.dp),
                    enabled = !isBulkExporting
                ) {
                    Icon(Icons.Default.CloudDownload, "Backup Files", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export All to Device", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            },
            dismissButton = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = {
                            viewModel.updateAppSetting("uninstall_warning_dismissed", "true")
                            showWarningPopup = false
                        },
                        enabled = !isBulkExporting
                    ) {
                        Text("Don\'t ask again", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    }
                    Button(
                        onClick = { showWarningPopup = false },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(8.dp),
                        enabled = !isBulkExporting
                    ) {
                        Text("Dismiss", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                }
            }
        )
    }

    val systemInDark = isSystemInDarkTheme()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val isDark = when (themeMode) {
        "system" -> systemInDark
        "dark" -> true
        "light" -> false
        else -> systemInDark
    }

    val totalFiles = files.size
    val photoCount = files.count { it.fileType == VaultFileType.PHOTO.name }
    val videoCount = files.count { it.fileType == VaultFileType.VIDEO.name }
    val documentCount = files.count { it.fileType == VaultFileType.DOCUMENT.name }
    val audioCount = files.count { it.fileType == VaultFileType.AUDIO.name }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Elegant Design Header: Security Status & Pulse Indicator
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SECURITY STATUS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B), // slate-500
                        letterSpacing = 1.5.sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            text = if (isDecoyMode) "Decoy Safe" else "Vault",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        // Radiant pulse green/cyan circle representing active hardware protection
                        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                        val pulseAlpha by infiniteTransition.animateFloat(
                            initialValue = 0.4f,
                            targetValue = 1.0f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1200, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "pulse_alpha"
                        )
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .background(Color(0xFF22D3EE), shape = CircleShape)
                                .alpha(pulseAlpha)
                        )
                    }
                }
                
                // Outer Badge container (glassmorphic border outline)
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(
                            if (isDark) Color(0xFF1E293B).copy(alpha = 0.4f) else Color(0xFFEDF2F7),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .border(
                            1.dp,
                            if (isDark) Color(0xFF334155).copy(alpha = 0.5f) else Color(0xFFCBD5E1),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Shield Guard Active",
                        tint = Color(0xFF6366F1), // Indigo accent
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // Premium security storage progress banner with actual device & vault statistics
        item {
            val context = LocalContext.current
            val totalSpaceBytes = context.filesDir.totalSpace
            val freeSpaceBytes = context.filesDir.freeSpace
            val usedSpaceBytes = totalSpaceBytes - freeSpaceBytes

            val totalGb = if (totalSpaceBytes > 0L) totalSpaceBytes.toDouble() / (1024.0 * 1024.0 * 1024.0) else 128.0
            val usedGb = if (totalSpaceBytes > 0L) usedSpaceBytes.toDouble() / (1024.0 * 1024.0 * 1024.0) else 45.4
            
            // Total size of encrypted files inside the vault database
            val vaultFilesBytes = files.sumOf { it.size }
            val formattedVaultSize = formatSize(vaultFilesBytes)

            val maxStorage = totalGb
            val realUsedGb = usedGb

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF4F46E5), // From Indigo 600
                                Color(0xFF312E81)  // To Indigo 900
                            )
                        ),
                        shape = RoundedCornerShape(28.dp) // rounded-[2rem] fallback style
                    )
                    .border(
                        1.dp,
                        Color(0xFF818CF8).copy(alpha = 0.25f), // indigo-400 opacity border
                        shape = RoundedCornerShape(28.dp)
                    )
                    .padding(24.dp)
            ) {
                // Blur accent corner decoration
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = 24.dp, y = (-24).dp)
                        .background(Color(0xFF22D3EE).copy(alpha = 0.12f), shape = CircleShape)
                        .blur(24.dp)
                )

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                text = "Device Storage Used",
                                color = Color(0xFFE2E8F0).copy(alpha = 0.7f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            // Calculate current weight based on actual device assets
                            val formattedGb = String.format(Locale.US, "%.1f", realUsedGb)
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = formattedGb,
                                    color = Color.White,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = " GB",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(bottom = 5.dp)
                                )
                            }
                        }

                        // Percentage storage chip in decimal matching device
                        val percentage = ((realUsedGb / maxStorage) * 100).toInt().coerceIn(1, 100)
                        Box(
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.15f), shape = CircleShape)
                                .border(1.dp, Color.White.copy(alpha = 0.2f), shape = CircleShape)
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "$percentage% Full",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Encrypted Vault size: $formattedVaultSize ($totalFiles files)",
                        color = Color(0xFF22D3EE),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Real partition container tracking progress bar
                    val percentageFloat = (realUsedGb / maxStorage).toFloat().coerceIn(0.05f, 1.0f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .background(Color.Black.copy(alpha = 0.25f), shape = CircleShape)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(percentageFloat)
                                .background(Color(0xFF22D3EE), shape = CircleShape) // Cyan highlight progress indicator
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "0 GB",
                            color = Color(0xFFE2E8F0).copy(alpha = 0.6f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        val formattedMax = String.format(Locale.US, "%.0f GB Max", maxStorage)
                        Text(
                            text = formattedMax,
                            color = Color(0xFFE2E8F0).copy(alpha = 0.6f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Persistent Uninstallation Warning Card & Backup Quick Action
        if (!isDecoyMode) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF7F1D1D).copy(alpha = 0.15f) else Color(0xFFFEF2F2)
                    ),
                    border = BorderStroke(
                        width = 1.2.dp,
                        color = if (isDark) Color(0xFFEF4444).copy(alpha = 0.3f) else Color(0xFFFCA5A5)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Uninstallation warning",
                                tint = if (isDark) Color(0xFFF87171) else Color(0xFFDC2626),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Uninstallation Warning Alert",
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFF87171) else Color(0xFFDC2626),
                                fontSize = 15.sp
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Due to zero-knowledge secure local device encryption, if you uninstall this application, all encrypted files, secure notes, and passwords inside the vault will be permanently deleted and cannot be recovered.",
                            color = if (isDark) Color(0xFFFCA5A5) else Color(0xFF7F1D1D).copy(alpha = 0.85f),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        if (isBulkExporting) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Exporting Backup...",
                                        fontSize = 11.sp,
                                        color = if (isDark) Color.White else Color(0xFF0F172A),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "${bulkExportProgress.first}/${bulkExportProgress.second}",
                                        fontSize = 11.sp,
                                        color = if (isDark) Color.White else Color(0xFF0F172A),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { if (bulkExportProgress.second > 0) bulkExportProgress.first.toFloat() / bulkExportProgress.second.toFloat() else 0f },
                                    modifier = Modifier.fillMaxWidth().height(6.dp),
                                    color = Color(0xFF10B981)
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (files.isEmpty()) {
                                            Toast.makeText(context, "Your secure vault is empty!", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        isBulkExporting = true
                                        bulkExportProgress = 0 to files.size
                                        bulkExportFiles(
                                            context = context,
                                            viewModel = viewModel,
                                            files = files,
                                            onProgress = { current, total ->
                                                bulkExportProgress = current to total
                                            },
                                            onComplete = { success, total ->
                                                isBulkExporting = false
                                                Toast.makeText(context, "Exported $success of $total files to Downloads!", Toast.LENGTH_LONG).show()
                                            }
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)), // Green
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.CloudDownload, "Backup Now", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Bulk Export Data", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }

                                Button(
                                    onClick = {
                                        showWarningPopup = true
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f),
                                        contentColor = if (isDark) Color.White else Color(0xFF0F172A)
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text("Details", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Global Search Bar
        item {
            var searchStr by remember { mutableStateOf("") }
            OutlinedTextField(
                value = searchStr,
                onValueChange = {
                    searchStr = it
                    viewModel.searchQuery.value = it
                },
                placeholder = { Text("Global secure search...") },
                leadingIcon = { Icon(Icons.Default.Search, "Search", tint = Color(0xFF64748B)) },
                trailingIcon = {
                    if (searchStr.isNotEmpty()) {
                        IconButton(onClick = {
                            searchStr = ""
                            viewModel.searchQuery.value = ""
                        }) {
                            Icon(Icons.Default.Clear, "Clear", tint = Color(0xFF64748B))
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF6366F1),
                    unfocusedBorderColor = Color(0xFF1E293B),
                    focusedContainerColor = Color(0xFF111114).copy(alpha = 0.5f),
                    unfocusedContainerColor = Color(0xFF111114).copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp)
            )
        }

        // Storage Counts Grid Title
        item {
            Text(
                text = "Secure Directories",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        // Primary Grid
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                DashboardCountCard(
                    title = "Photos",
                    count = photoCount,
                    subtitleSuffix = "secret photos",
                    icon = Icons.Default.Image,
                    modifier = Modifier.weight(1f),
                    color = Color(0xFF6366F1), // Indigo
                    onClick = { onNavigateToTab(1) }
                )
                DashboardCountCard(
                    title = "Passwords",
                    count = passwords.size,
                    subtitleSuffix = "safe entries",
                    icon = Icons.Default.VpnKey,
                    modifier = Modifier.weight(1f),
                    color = Color(0xFF22D3EE), // Cyan
                    onClick = { onNavigateToTab(3) }
                )
            }
        }

        // Secondary Grid
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                DashboardCountCard(
                    title = "Docs",
                    count = documentCount,
                    subtitleSuffix = "encrypted files",
                    icon = Icons.Default.Description,
                    modifier = Modifier.weight(1f),
                    color = Color(0xFF10B981), // Emerald
                    onClick = { onNavigateToTab(1) }
                )
                DashboardCountCard(
                    title = "Notes",
                    count = notes.size,
                    subtitleSuffix = "encrypted notes",
                    icon = Icons.Default.Edit,
                    modifier = Modifier.weight(1f),
                    color = Color(0xFFF59E0B), // Amber
                    onClick = { onNavigateToTab(2) }
                )
            }
        }

        // Recent Activity Dynamic Slate Feed
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Recent Activity",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B), // slate-500
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                if (files.isEmpty() && notes.isEmpty()) {
                    // Item 1: Passport_Scan.pdf
                    RecentActivityRow(
                        title = "Passport_Scan.pdf",
                        subtitle = "Encrypted • 2m ago",
                        icon = Icons.Default.Description,
                        iconTint = Color(0xFF10B981) // emerald
                    )

                    // Item 2: Family_Trip_HD.mp4
                    RecentActivityRow(
                        title = "Family_Trip_HD.mp4",
                        subtitle = "Hidden • 1h ago",
                        icon = Icons.Default.PlayCircle,
                        iconTint = Color(0xFFA855F7) // purple
                    )
                } else {
                    // Map active file system elements
                    val sortedFiles = files.take(3)
                    sortedFiles.forEach { file ->
                        RecentActivityRow(
                            title = file.name,
                            subtitle = "Encrypted • ${formatSize(file.size)}",
                            icon = when (file.fileType) {
                                VaultFileType.PHOTO.name -> Icons.Default.Image
                                VaultFileType.VIDEO.name -> Icons.Default.PlayCircle
                                VaultFileType.DOCUMENT.name -> Icons.Default.Description
                                else -> Icons.Default.MusicNote
                            },
                            iconTint = Color(0xFF6366F1)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardCountCard(
    title: String,
    count: Int,
    subtitleSuffix: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background == DarkBackground

    Card(
        modifier = modifier
            .clickable { onClick() }
            .border(
                1.dp,
                if (isDark) Color(0xFF1E293B).copy(alpha = 0.5f) else Color.Gray.copy(alpha = 0.15f), // softer border in light mode
                RoundedCornerShape(24.dp)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) {
                Color(0xFF111114).copy(alpha = 0.7f) // bg-slate-900/50
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) // clean, soft grey/white background
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.15f), shape = RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(2.dp))
                val rawLabel = if (count == 1) "1 item" else "$count items"
                Text(
                    text = if (count > 0) "$count $subtitleSuffix" else "0 items",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B) // text-slate-500
                )
            }
        }
    }
}

@Composable
fun RecentActivityRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF111114).copy(alpha = 0.4f), shape = RoundedCornerShape(18.dp)) // bg-slate-900/30
            .border(1.dp, Color(0xFF1E293B).copy(alpha = 0.3f), shape = RoundedCornerShape(18.dp)) // border-slate-800/50
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(Color(0xFF1E293B).copy(alpha = 0.5f), shape = RoundedCornerShape(10.dp)), // bg-slate-800
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = Color(0xFF64748B) // text-slate-500
            )
        }

        Icon(
            imageVector = Icons.Default.MoreVert,
            contentDescription = "Options",
            tint = Color(0xFF475569), // slate-600
            modifier = Modifier.size(16.dp)
        )
    }
}

fun getFileName(context: android.content.Context, uri: android.net.Uri): String? {
    var name: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    name = it.getString(index)
                }
            }
        }
    }
    if (name == null) {
        name = uri.path
        val cut = name?.lastIndexOf('/')
        if (cut != null && cut != -1) {
            name = name.substring(cut + 1)
        }
    }
    return name
}

fun determineVaultFileType(fileName: String): VaultFileType {
    val ext = fileName.substringAfterLast('.', "").lowercase()
    return when (ext) {
        "jpg", "jpeg", "png", "gif", "webp", "bmp" -> VaultFileType.PHOTO
        "mp4", "mkv", "avi", "mov", "webm", "3gp" -> VaultFileType.VIDEO
        "mp3", "wav", "m4a", "ogg", "flac" -> VaultFileType.AUDIO
        else -> VaultFileType.DOCUMENT
    }
}

// --- PANEL 2: VAULT MEDIA FILES ---
@Composable
fun VaultMediaScreen(
    viewModel: VaultViewModel
) {
    val files by viewModel.vaultFiles.collectAsStateWithLifecycle()
    val isDecoyMode by viewModel.isDecoyMode.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val contentResolver = context.contentResolver
                val inputStream = contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes()
                if (bytes != null) {
                    val realFileName = getFileName(context, uri) ?: "imported_file_${System.currentTimeMillis()}"
                    val fileType = determineVaultFileType(realFileName)
                    viewModel.importFile(
                        name = realFileName,
                        fileType = fileType,
                        fileBytes = bytes
                    )
                    Toast.makeText(context, "Encrypted and secured: $realFileName", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Error: Could not read file content", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Encryption error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    var selectedTypeFilter by remember { mutableStateOf<VaultFileType?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }
    var selectedMediaFileForPreview by remember { mutableStateOf<VaultFileEntity?>(null) }

    val filteredFiles = remember(files, selectedTypeFilter) {
        if (selectedTypeFilter == null) files else files.filter { it.fileType == selectedTypeFilter!!.name }
    }

    Scaffold(
        floatingActionButton = {
            if (!isDecoyMode) {
                FloatingActionButton(
                    onClick = { showImportDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, "Import File to Vault")
                }
            }
        }
    ) { paddingVals ->
        Column(
            modifier = Modifier
                .padding(paddingVals)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "File Vault",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "AES-256 byte encryption applied to all imported media files.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Categories horizontal slider
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedTypeFilter == null,
                    onClick = { selectedTypeFilter = null },
                    label = { Text("All") }
                )
                FilterChip(
                    selected = selectedTypeFilter == VaultFileType.PHOTO,
                    onClick = { selectedTypeFilter = VaultFileType.PHOTO },
                    label = { Text("Photos") }
                )
                FilterChip(
                    selected = selectedTypeFilter == VaultFileType.VIDEO,
                    onClick = { selectedTypeFilter = VaultFileType.VIDEO },
                    label = { Text("Videos") }
                )
                FilterChip(
                    selected = selectedTypeFilter == VaultFileType.DOCUMENT,
                    onClick = { selectedTypeFilter = VaultFileType.DOCUMENT },
                    label = { Text("Documents") }
                )
                FilterChip(
                    selected = selectedTypeFilter == VaultFileType.AUDIO,
                    onClick = { selectedTypeFilter = VaultFileType.AUDIO },
                    label = { Text("Audios") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (filteredFiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = "Empty Folder Illustration",
                            modifier = Modifier
                                .size(72.dp)
                                .alpha(0.4f),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (isDecoyMode) "Decoy Folder is Empty" else "No Encrypted Files",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isDecoyMode) "Decoy vaults mask your real media profiles safely." else "Click the + button to import and secure files secure-locally",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredFiles) { file ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedMediaFileForPreview = file },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(110.dp)
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                                                )
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when (file.fileType) {
                                            VaultFileType.PHOTO.name -> Icons.Default.Image
                                            VaultFileType.VIDEO.name -> Icons.Default.PlayCircle
                                            VaultFileType.DOCUMENT.name -> Icons.Default.Description
                                            else -> Icons.Default.Audiotrack
                                        },
                                        contentDescription = "File Type Secure Asset Icon",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }

                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = file.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = formatSize(file.size),
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            var isExporting by remember { mutableStateOf(false) }

                                            if (isExporting) {
                                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Color(0xFF10B981))
                                            } else {
                                                IconButton(
                                                    onClick = {
                                                        isExporting = true
                                                        viewModel.decryptFile(file) { bytes ->
                                                             isExporting = false
                                                             if (bytes != null) {
                                                                 val success = exportFileToDownloads(context, file.name, bytes)
                                                                 if (success) {
                                                                     Toast.makeText(context, "Exported successfully to Downloads!", Toast.LENGTH_LONG).show()
                                                                 } else {
                                                                     Toast.makeText(context, "Export failed.", Toast.LENGTH_SHORT).show()
                                                                 }
                                                             } else {
                                                                 Toast.makeText(context, "Decryption issue.", Toast.LENGTH_SHORT).show()
                                                             }
                                                         }
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.CloudDownload,
                                                        contentDescription = "Decrypt and Export back to Device Storage",
                                                        tint = Color(0xFF10B981).copy(alpha = 0.8f),
                                                        modifier = Modifier.size(16.dp)
                                                     )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(4.dp))

                                            IconButton(
                                                onClick = { viewModel.deleteFile(file) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete Local File Permanently from Vault",
                                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Interactive import dialog supporting both real and simulated imports
    if (showImportDialog) {
        Dialog(onDismissRequest = { showImportDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.border(
                    1.dp,
                    Color(0xFF1E293B).copy(alpha = 0.5f),
                    RoundedCornerShape(24.dp)
                )
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Encrypted Import",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // PRIMARY BUTTON: REAL DEVICE IMPORT
                    Button(
                        onClick = {
                            showImportDialog = false
                            // Launch Android GET_CONTENT system file picker
                            filePickerLauncher.launch("*/*")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.CloudUpload, "Cloud Upload", modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Import from Device",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFF1E293B))
                        Text(
                            text = " OR SIMULATE ",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFF1E293B))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            viewModel.importFile(
                                name = "photo_${Random.nextInt(100)}.jpg",
                                fileType = VaultFileType.PHOTO,
                                fileBytes = "Simulated high-resolution photo data placeholder encrypted successfully".toByteArray()
                            )
                            showImportDialog = false
                            Toast.makeText(context, "Encrypted Image Import complete", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1E293B).copy(alpha = 0.5f),
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Image, "Photo", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Simulate Photo File", fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            viewModel.importFile(
                                name = "video_clip_${Random.nextInt(100)}.mp4",
                                fileType = VaultFileType.VIDEO,
                                fileBytes = "Simulated high-framerate encrypted video clip payload data".toByteArray()
                            )
                            showImportDialog = false
                            Toast.makeText(context, "Encrypted Video Import complete", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1E293B).copy(alpha = 0.5f),
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PlayCircle, "Video", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Simulate Video File", fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            viewModel.importFile(
                                name = "confidential_tax_declaration.pdf",
                                fileType = VaultFileType.DOCUMENT,
                                fileBytes = "Secure private financial tax information cryptographed".toByteArray()
                            )
                            showImportDialog = false
                            Toast.makeText(context, "Encrypted Document Import complete", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1E293B).copy(alpha = 0.5f),
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Description, "Document", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Simulate Document File", fontSize = 13.sp)
                    }
                }
            }
        }
    }

    // Media Viewer Preview Dialog
    if (selectedMediaFileForPreview != null) {
        var decryptedBytes by remember { mutableStateOf<ByteArray?>(null) }
        var isDecrypting by remember { mutableStateOf(true) }

        val systemInDark = isSystemInDarkTheme()
        val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
        val isDark = when (themeMode) {
            "system" -> systemInDark
            "dark" -> true
            "light" -> false
            else -> systemInDark
        }

        LaunchedEffect(selectedMediaFileForPreview) {
            isDecrypting = true
            viewModel.decryptFile(selectedMediaFileForPreview!!) {
                decryptedBytes = it
                isDecrypting = false
            }
        }

        Dialog(
            onDismissRequest = { selectedMediaFileForPreview = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.85f)
                    .border(
                        BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        ),
                        RoundedCornerShape(24.dp)
                    )
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxSize()
                ) {
                    // Header Area
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = selectedMediaFileForPreview!!.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Secure File Preview Panel",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        IconButton(
                            onClick = { selectedMediaFileForPreview = null },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), CircleShape)
                                .size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Preview Dialog",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isDecrypting) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Decrypting secure file from database...",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    } else if (decryptedBytes != null) {
                        val fileType = selectedMediaFileForPreview!!.fileType
                        val fileName = selectedMediaFileForPreview!!.name

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // --- RENDER COMPONENT ACCORDING TO TYPE ---
                            when (fileType) {
                                VaultFileType.PHOTO.name -> {
                                    val bitmap = remember(decryptedBytes) {
                                        try {
                                            BitmapFactory.decodeByteArray(decryptedBytes, 0, decryptedBytes!!.size)
                                        } catch (e: Exception) {
                                            null
                                        }
                                    }

                                    if (bitmap != null) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(260.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(if (isDark) Color.Black else Color(0xFFF1F5F9))
                                                .border(
                                                    1.dp,
                                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                                    RoundedCornerShape(16.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            androidx.compose.foundation.Image(
                                                bitmap = bitmap.asImageBitmap(),
                                                contentDescription = "Decrypted secure preview",
                                                modifier = Modifier.fillMaxSize().padding(6.dp),
                                                contentScale = androidx.compose.ui.layout.ContentScale.Fit
                                            )
                                        }
                                    } else {
                                        // Sandbox Mock rendering for simulated text strings
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(220.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(
                                                    Brush.linearGradient(
                                                        colors = if (isDark) {
                                                            listOf(Color(0xFF1E1E24), Color(0xFF0F172A))
                                                        } else {
                                                            listOf(Color(0xFFEFF6FF), Color(0xFFDBEAFE))
                                                        }
                                                    )
                                                )
                                                .border(
                                                    1.dp,
                                                    if (isDark) Color(0xFF334155) else Color(0xFFBFDBFE),
                                                    RoundedCornerShape(16.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier.padding(16.dp),
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Image,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(56.dp)
                                                )
                                                Spacer(modifier = Modifier.height(10.dp))
                                                Text(
                                                    text = "Simulated Photo Sandbox Container",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                val stringContent = remember(decryptedBytes) {
                                                    try { String(decryptedBytes!!) } catch(e: Exception) { "" }
                                                }
                                                Text(
                                                    text = if (stringContent.length > 90) stringContent.take(90) + "..." else stringContent,
                                                    fontSize = 12.sp,
                                                    textAlign = TextAlign.Center,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                                    modifier = Modifier.padding(horizontal = 8.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                VaultFileType.AUDIO.name -> {
                                    val tempFile = remember(decryptedBytes) {
                                        try {
                                            val file = java.io.File(context.cacheDir, "temp_prev_" + System.currentTimeMillis() + "_" + fileName)
                                            file.writeBytes(decryptedBytes!!)
                                            file
                                        } catch (e: Exception) {
                                            null
                                        }
                                    }

                                    val mediaPlayer = remember { android.media.MediaPlayer() }
                                    var isPlaying by remember { mutableStateOf(false) }
                                    var currentPosition by remember { mutableStateOf(0) }
                                    var duration by remember { mutableStateOf(0) }

                                    LaunchedEffect(tempFile) {
                                        if (tempFile != null) {
                                            try {
                                                mediaPlayer.reset()
                                                mediaPlayer.setDataSource(tempFile.absolutePath)
                                                mediaPlayer.prepare()
                                                duration = mediaPlayer.duration
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        }
                                    }

                                    DisposableEffect(Unit) {
                                        onDispose {
                                            try {
                                                if (mediaPlayer.isPlaying) {
                                                    mediaPlayer.stop()
                                                }
                                                mediaPlayer.release()
                                            } catch (e: Exception) {
                                                // ignore
                                            } finally {
                                                tempFile?.delete()
                                            }
                                        }
                                    }

                                    LaunchedEffect(isPlaying) {
                                        if (isPlaying) {
                                            while (mediaPlayer.isPlaying) {
                                                currentPosition = mediaPlayer.currentPosition
                                                kotlinx.coroutines.delay(200)
                                            }
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(if (isDark) Color(0xFF1E293B).copy(alpha = 0.4f) else Color(0xFFF8FAFC))
                                            .border(1.dp, if (isDark) Color(0xFF334155).copy(alpha = 0.5f) else Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Audiotrack,
                                                contentDescription = "Audio track",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(52.dp)
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text(
                                                text = if (duration > 0) "Decrypted Secure Audio Track" else "Simulated Secure Audio File",
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                            Spacer(modifier = Modifier.height(14.dp))

                                            if (duration > 0) {
                                                Slider(
                                                    value = currentPosition.toFloat(),
                                                    onValueChange = { newValue ->
                                                        mediaPlayer.seekTo(newValue.toInt())
                                                        currentPosition = newValue.toInt()
                                                    },
                                                    valueRange = 0f..duration.toFloat(),
                                                    colors = SliderDefaults.colors(
                                                        thumbColor = MaterialTheme.colorScheme.primary,
                                                        activeTrackColor = MaterialTheme.colorScheme.primary
                                                    ),
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                                Row(
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    val curSec = currentPosition / 1000
                                                    val durSec = duration / 1000
                                                    Text(
                                                        text = String.format("%d:%02d", curSec / 60, curSec % 60),
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                    )
                                                    Text(
                                                        text = String.format("%d:%02d", durSec / 60, durSec % 60),
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(8.dp))
                                                IconButton(
                                                    onClick = {
                                                        if (mediaPlayer.isPlaying) {
                                                            mediaPlayer.pause()
                                                            isPlaying = false
                                                        } else {
                                                            mediaPlayer.start()
                                                            isPlaying = true
                                                        }
                                                    }
                                                ) {
                                                    Icon(
                                                        imageVector = if (isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                                                        contentDescription = if (isPlaying) "Pause" else "Play",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(52.dp)
                                                    )
                                                }
                                            } else {
                                                // Simulated Audio Player
                                                var simulatedPlay by remember { mutableStateOf(false) }
                                                var simulatedProgress by remember { mutableStateOf(0f) }

                                                LaunchedEffect(simulatedPlay) {
                                                    if (simulatedPlay) {
                                                        while (simulatedProgress < 1f && simulatedPlay) {
                                                            simulatedProgress += 0.02f
                                                            kotlinx.coroutines.delay(100)
                                                        }
                                                        if (simulatedProgress >= 1f) {
                                                            simulatedPlay = false
                                                            simulatedProgress = 0f
                                                        }
                                                    }
                                                }

                                                Slider(
                                                    value = simulatedProgress,
                                                    onValueChange = { simulatedProgress = it },
                                                    valueRange = 0f..1f,
                                                    colors = SliderDefaults.colors(
                                                        thumbColor = MaterialTheme.colorScheme.primary,
                                                        activeTrackColor = MaterialTheme.colorScheme.primary
                                                    ),
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                                Row(
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text(
                                                        text = String.format("0:%02d", (simulatedProgress * 58).toInt()),
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                    )
                                                    Text(
                                                        text = "0:58",
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(8.dp))
                                                IconButton(
                                                    onClick = { simulatedPlay = !simulatedPlay }
                                                ) {
                                                    Icon(
                                                        imageVector = if (simulatedPlay) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                                                        contentDescription = "Simulated Core Audio Action",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(52.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                VaultFileType.VIDEO.name -> {
                                    // Interactive cinema mock video streams controller
                                    var videoPlaying by remember { mutableStateOf(false) }
                                    var videoProgress by remember { mutableStateOf(0.12f) }
                                    var videoMuted by remember { mutableStateOf(false) }
                                    var videoSpeed by remember { mutableStateOf("1.0x") }

                                    LaunchedEffect(videoPlaying) {
                                        if (videoPlaying) {
                                            while (videoPlaying) {
                                                videoProgress = (videoProgress + 0.005f).coerceAtMost(1f)
                                                if (videoProgress >= 1f) {
                                                    videoProgress = 0f
                                                }
                                                kotlinx.coroutines.delay(100)
                                            }
                                        }
                                    }

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(Color.Black)
                                            .border(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1), RoundedCornerShape(16.dp))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(180.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            val infiniteTransition = rememberInfiniteTransition(label = "video_canvas")
                                            val pulse by infiniteTransition.animateFloat(
                                                initialValue = 0.82f,
                                                targetValue = 1.18f,
                                                animationSpec = infiniteRepeatable(
                                                    animation = tween(2400, easing = EaseInOut),
                                                    repeatMode = RepeatMode.Reverse
                                                ),
                                                label = "pulse"
                                            )

                                            Canvas(modifier = Modifier.fillMaxSize()) {
                                                drawCircle(
                                                    brush = Brush.radialGradient(
                                                        colors = listOf(
                                                            Color(0xFF0EA5E9).copy(alpha = 0.16f),
                                                            Color.Transparent
                                                        )
                                                    ),
                                                    radius = 220.dp.toPx() * pulse
                                                )
                                            }

                                            if (!videoPlaying) {
                                                IconButton(
                                                    onClick = { videoPlaying = true },
                                                    modifier = Modifier
                                                        .size(60.dp)
                                                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                                        .border(1.5.dp, Color.White.copy(alpha = 0.8f), CircleShape)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.PlayArrow,
                                                        contentDescription = "Play Video",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(32.dp)
                                                    )
                                                }
                                            } else {
                                                // Dynamic visual equalizer lines
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    repeat(5) { index ->
                                                        val barPulse by infiniteTransition.animateFloat(
                                                            initialValue = 15f,
                                                            targetValue = 65f + (index * 10f),
                                                            animationSpec = infiniteRepeatable(
                                                                animation = tween(350 + (index * 120), easing = EaseInOut),
                                                                repeatMode = RepeatMode.Reverse
                                                            ),
                                                            label = "video_bar_$index"
                                                        )
                                                        Box(
                                                            modifier = Modifier
                                                                .width(6.dp)
                                                                .height(barPulse.dp)
                                                                .background(Color(0xFF0EA5E9), RoundedCornerShape(3.dp))
                                                        )
                                                    }
                                                }
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(12.dp)
                                                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = "DEC-MP4 STREAM",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF0EA5E9)
                                                )
                                            }
                                        }

                                        // Player controls
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color(0xFF0F172A))
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            IconButton(
                                                onClick = { videoPlaying = !videoPlaying },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (videoPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                                    contentDescription = "Play/Pause",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }

                                            Slider(
                                                value = videoProgress,
                                                onValueChange = { videoProgress = it },
                                                modifier = Modifier.weight(1f),
                                                colors = SliderDefaults.colors(
                                                    thumbColor = Color(0xFF0EA5E9),
                                                    activeTrackColor = Color(0xFF0EA5E9),
                                                    inactiveTrackColor = Color.White.copy(alpha = 0.2f)
                                                )
                                            )

                                            IconButton(
                                                onClick = { videoMuted = !videoMuted },
                                                modifier = Modifier.size(30.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (videoMuted) Icons.Filled.VolumeMute else Icons.Filled.VolumeUp,
                                                    contentDescription = "Mute Toggle",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                                                    .clickable {
                                                        videoSpeed = when (videoSpeed) {
                                                            "1.0x" -> "1.5x"
                                                            "1.5x" -> "2.0x"
                                                            else -> "1.0x"
                                                        }
                                                    }
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(text = videoSpeed, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }

                                else -> {
                                    // Handle Document/Other Files
                                    val printableText = remember(decryptedBytes) {
                                        try {
                                            val s = String(decryptedBytes!!, Charsets.UTF_8)
                                            val isBinary = s.take(350).any { it == '\u0000' }
                                            if (!isBinary && s.trim().isNotEmpty()) s else null
                                        } catch (e: Exception) {
                                            null
                                        }
                                    }

                                    if (printableText != null) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "Decrypted Document Streams",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    letterSpacing = 0.5.sp
                                                )
                                                Text(
                                                    text = "${printableText.length} characters",
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .heightIn(max = 240.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(if (isDark) Color(0xFF111114) else Color(0xFFF1F5F9))
                                                    .border(
                                                        1.dp,
                                                        if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                                                        RoundedCornerShape(12.dp)
                                                    )
                                                    .verticalScroll(rememberScrollState())
                                                    .padding(12.dp)
                                            ) {
                                                Text(
                                                    text = printableText,
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    fontFamily = FontFamily.Monospace,
                                                    lineHeight = 16.sp
                                                )
                                            }
                                        }
                                    } else {
                                        // Binary File Fallback
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(180.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(if (isDark) Color(0xFF1E293B).copy(alpha = 0.4f) else Color(0xFFF8FAFC))
                                                .border(1.dp, if (isDark) Color(0xFF334155).copy(alpha = 0.4f) else Color(0xFFE2E8F0), RoundedCornerShape(16.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                                                Icon(
                                                    imageVector = Icons.Default.Description,
                                                    contentDescription = "Secure Document Asset",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(52.dp)
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = "Decrypted Secure Binary File",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "Binary data streams cannot be rendered as plain text. Run Export below to open this container using your system's specialized apps.",
                                                    fontSize = 11.sp,
                                                    textAlign = TextAlign.Center,
                                                    lineHeight = 15.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // --- METADATA AND CRYPTO CARD PANEL ---
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (isDark) Color(0xFF111114).copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (isDark) Color(0xFF1E293B).copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Secure Container Protection",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(1.dp))
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Size On Disk", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                                    Text(
                                        text = formatSize(selectedMediaFileForPreview!!.size),
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Encryption Key", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                                    Text(
                                        text = "Pass-through Keystore API",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Integrity Check", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                                    Text(
                                        text = "GCM Verification Passed",
                                        fontSize = 12.sp,
                                        color = if (isDark) Color(0xFF10B981) else Color(0xFF047857),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "Error decrypting container payload.",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (decryptedBytes != null) {
                        Button(
                            onClick = {
                                val success = exportFileToDownloads(context, selectedMediaFileForPreview!!.name, decryptedBytes!!)
                                if (success) {
                                    Toast.makeText(context, "Exported successfully to Downloads folder!", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Export failed. Check storage settings.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF10B981), // Beautiful emerald green
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.CloudDownload, "Download / Export", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Export File to Device", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Button(
                        onClick = { selectedMediaFileForPreview = null },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Close Secure Viewer", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Format Size String
fun formatSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = listOf("B", "KB", "MB", "GB")
    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format("%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

// Export a decrypted file from our secure vault back to the public downloads folder
fun exportFileToDownloads(context: Context, fileName: String, fileBytes: ByteArray): Boolean {
    return try {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                val ext = fileName.substringAfterLast('.', "").lowercase()
                val mimeType = when (ext) {
                    "jpg", "jpeg" -> "image/jpeg"
                    "png" -> "image/png"
                    "gif" -> "image/gif"
                    "webp" -> "image/webp"
                    "mp4" -> "video/mp4"
                    "mp3" -> "audio/mpeg"
                    "pdf" -> "application/pdf"
                    else -> "application/octet-stream"
                }
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(fileBytes)
                    outputStream.flush()
                }
                true
            } else {
                false
            }
        } else {
            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            val file = java.io.File(downloadsDir, fileName)
            java.io.FileOutputStream(file).use { out ->
                out.write(fileBytes)
                out.flush()
            }
            true
        }
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

// Helper function to bulk export all files in the vault to public Downloads
fun bulkExportFiles(
    context: Context,
    viewModel: VaultViewModel,
    files: List<VaultFileEntity>,
    onProgress: (Int, Int) -> Unit, // current, total
    onComplete: (Int, Int) -> Unit  // successes, total
) {
    if (files.isEmpty()) {
        onComplete(0, 0)
        return
    }
    
    var completedCount = 0
    var successCount = 0
    val totalCount = files.size

    for (file in files) {
        viewModel.decryptFile(file) { bytes ->
            completedCount++
            if (bytes != null) {
                val success = exportFileToDownloads(context, file.name, bytes)
                if (success) {
                    successCount++
                }
            }
            onProgress(completedCount, totalCount)
            if (completedCount == totalCount) {
                onComplete(successCount, totalCount)
            }
        }
    }
}

// Launch Android system biometric authentication or device credentials fallback
fun showBiometricPrompt(
    activity: androidx.fragment.app.FragmentActivity,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val executor = androidx.core.content.ContextCompat.getMainExecutor(activity)
    val biometricPrompt = androidx.biometric.BiometricPrompt(
        activity,
        executor,
        object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: androidx.biometric.BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                activity.runOnUiThread { onSuccess() }
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                activity.runOnUiThread { onError(errString.toString()) }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                activity.runOnUiThread { onError("Biometric mismatch. Try again.") }
            }
        }
    )

    val promptInfo = androidx.biometric.BiometricPrompt.PromptInfo.Builder()
        .setTitle("Secure Vault Unlock")
        .setSubtitle("Authenticate using biometrics or secure device credentials")
        // Devices without biometric hardware automatically fall back to Device PIN/Pattern/Password!
        .setAllowedAuthenticators(
            androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG or 
            androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        .build()

    try {
        biometricPrompt.authenticate(promptInfo)
    } catch (e: Exception) {
        onError(e.localizedMessage ?: "Biometric prompt error")
    }
}

// --- PANEL 3: SECURE NOTES ---
@Composable
fun SecureNotesScreen(
    viewModel: VaultViewModel
) {
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val isDecoyMode by viewModel.isDecoyMode.collectAsStateWithLifecycle()

    var showEditorDialog by remember { mutableStateOf(false) }
    var selectedNoteForEdit by remember { mutableStateOf<NoteEntity?>(null) }

    val systemInDark = isSystemInDarkTheme()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val isDark = when (themeMode) {
        "system" -> systemInDark
        "dark" -> true
        "light" -> false
        else -> systemInDark
    }

    // Selection background colors matching Material 3 cards
    val cardColorsList = if (isDark) {
        listOf(
            MaterialTheme.colorScheme.surface,
            CardColorPurple,
            CardColorNavy,
            CardColorSlate,
            CardColorTeal,
            CardColorBurgundy,
            CardColorEmerald
        )
    } else {
        listOf(
            MaterialTheme.colorScheme.surface,
            Color(0xFFF3E8FF), // Soft light purple
            Color(0xFFEFF6FF), // Soft light blue
            Color(0xFFF1F5F9), // Soft light slate/gray
            Color(0xFFECFDF5), // Soft light teal
            Color(0xFFFEF2F2), // Soft light red
            Color(0xFFF0FDF4)  // Soft light emerald
        )
    }

    val cardContentColorsList = if (isDark) {
        listOf(
            MaterialTheme.colorScheme.onSurface,
            Color.White,
            Color.White,
            Color.White,
            Color.White,
            Color.White,
            Color.White
        )
    } else {
        listOf(
            MaterialTheme.colorScheme.onSurface,
            Color(0xFF581C87), // Dark purple
            Color(0xFF1E40AF), // Dark blue
            Color(0xFF334155), // Dark slate/gray
            Color(0xFF047857), // Dark teal
            Color(0xFFB91C1C), // Dark red
            Color(0xFF15803D)  // Dark emerald
        )
    }

    Scaffold(
        floatingActionButton = {
            if (!isDecoyMode) {
                FloatingActionButton(
                    onClick = {
                        selectedNoteForEdit = null
                        showEditorDialog = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, "Write Note")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Secure Notes",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "End-to-end encrypted notepad files. Hidden dynamically.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (notes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.EditNote,
                            contentDescription = "Empty Notes Illustration",
                            modifier = Modifier
                                .size(72.dp)
                                .alpha(0.4f),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Encrypted Notes",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Write a private thought. These are instantly secured with AES.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(notes) { note ->
                        val cardColor = if (note.color in cardColorsList.indices) {
                            cardColorsList[note.color]
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                        val contentColor = if (note.color in cardContentColorsList.indices) {
                            cardContentColorsList[note.color]
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedNoteForEdit = note
                                    showEditorDialog = true
                                },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                            colors = CardDefaults.cardColors(containerColor = cardColor)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = note.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = contentColor
                                    )
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = if (note.color > 0) {
                                                    if (isDark) Color.White.copy(alpha = 0.2f) else contentColor.copy(alpha = 0.12f)
                                                } else {
                                                    MaterialTheme.colorScheme.surfaceVariant
                                                },
                                                shape = RoundedCornerShape(20.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = note.category,
                                            fontSize = 11.sp,
                                            color = if (note.color > 0) contentColor else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Tap note to load decrypt
                                var decText by remember { mutableStateOf<String?>(null) }
                                LaunchedEffect(note) {
                                    viewModel.decryptNoteContent(note) { decText = it }
                                }

                                Text(
                                    text = decText ?: "Decrypting raw bytes content...",
                                    fontSize = 13.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    color = contentColor.copy(alpha = 0.82f)
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(note.createdAt)),
                                        fontSize = 10.sp,
                                        color = contentColor.copy(alpha = 0.5f)
                                    )

                                    if (!isDecoyMode) {
                                        IconButton(
                                            onClick = { viewModel.deleteNote(note) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete Note",
                                                tint = if (note.color > 0) contentColor.copy(alpha = 0.6f) else MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Comprehensive Dialog Editor
    if (showEditorDialog) {
        var editTitle by remember { mutableStateOf(selectedNoteForEdit?.title ?: "") }
        var editContent by remember { mutableStateOf("") }
        var editCategory by remember { mutableStateOf(selectedNoteForEdit?.category ?: "Personal") }
        var noteColorIdx by remember { mutableStateOf(selectedNoteForEdit?.color ?: 0) }

        LaunchedEffect(selectedNoteForEdit) {
            if (selectedNoteForEdit != null) {
                viewModel.decryptNoteContent(selectedNoteForEdit!!) { editContent = it }
            } else {
                editContent = ""
            }
        }

        Dialog(
            onDismissRequest = { showEditorDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.85f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxSize()
                ) {
                    Text(
                        text = if (selectedNoteForEdit == null) "New Encrypted Note" else "Edit Secured Note",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = editCategory,
                            onValueChange = { editCategory = it },
                            label = { Text("Category") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Color bubble selector
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        cardColorsList.forEachIndexed { idx, colorVal ->
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(colorVal, shape = CircleShape)
                                    .border(
                                        width = if (noteColorIdx == idx) 2.dp else 1.dp,
                                        color = if (noteColorIdx == idx) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f),
                                        shape = CircleShape
                                    )
                                    .clickable { noteColorIdx = idx }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Big content container
                    OutlinedTextField(
                        value = editContent,
                        onValueChange = { editContent = it },
                        placeholder = { Text("Start typing confidential notes...") },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        maxLines = 10
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(
                            onClick = { showEditorDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Discard", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                        }

                        Button(
                            onClick = {
                                if (editTitle.isEmpty() || editContent.isEmpty()) return@Button
                                viewModel.saveNote(
                                    id = selectedNoteForEdit?.id ?: 0,
                                    title = editTitle,
                                    content = editContent,
                                    category = editCategory,
                                    color = noteColorIdx
                                )
                                showEditorDialog = false
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Secure Page")
                        }
                    }
                }
            }
        }
    }
}

// --- PANEL 4: PASSWORD MANAGER ---
@Composable
fun PasswordSafeScreen(
    viewModel: VaultViewModel
) {
    val passwords by viewModel.passwords.collectAsStateWithLifecycle()
    val isDecoyMode by viewModel.isDecoyMode.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var showEditorDialog by remember { mutableStateOf(false) }
    var selectedPasswordForEdit by remember { mutableStateOf<PasswordEntity?>(null) }
    var revealedPasswordsMap by remember { mutableStateOf<Map<Int, Boolean>>(emptyMap()) }

    Scaffold(
        floatingActionButton = {
            if (!isDecoyMode) {
                FloatingActionButton(
                    onClick = {
                        selectedPasswordForEdit = null
                        showEditorDialog = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, "New Secret Account")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Password Safe",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Hardware decrypt website passphrases locally from safe storage.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (passwords.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.VpnKey,
                            contentDescription = "Empty Password Safe",
                            modifier = Modifier
                                .size(72.dp)
                                .alpha(0.4f),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Saved Passwords",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Secure credit profiles, bank credentials, and accounts privately.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(passwords) { credentials ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = credentials.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )

                                    // Strength indicator bubble
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = when (credentials.strengthScore) {
                                                    5 -> Color(0xFF10B981).copy(alpha = 0.15f)
                                                    4, 3 -> Color(0xFFFBBF24).copy(alpha = 0.15f)
                                                    else -> Color(0xFFEF4444).copy(alpha = 0.15f)
                                                },
                                                shape = RoundedCornerShape(20.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = when (credentials.strengthScore) {
                                                5 -> "Ultra Secure"
                                                4 -> "Strong"
                                                3 -> "Medium"
                                                else -> "Weak PIN"
                                            },
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when (credentials.strengthScore) {
                                                5 -> Color(0xFF10B981)
                                                4, 3 -> Color(0xFFFBBF24)
                                                else -> Color(0xFFEF4444)
                                            }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Username: ${credentials.username}",
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                        )
                                        
                                        var realPassDec by remember { mutableStateOf<String?>(null) }
                                        LaunchedEffect(credentials) {
                                            viewModel.decryptPassword(credentials) { realPassDec = it }
                                        }

                                        val revealed = revealedPasswordsMap[credentials.id] ?: false
                                        Text(
                                            text = "Password: " + if (revealed) (realPassDec ?: "🔓 Decrypting...") else "••••••••",
                                            fontSize = 13.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }

                                    // Actions: Copy, Reveal, Trash
                                    Row {
                                        IconButton(onClick = {
                                            viewModel.decryptPassword(credentials) { rawPass ->
                                                clipboardManager.setText(AnnotatedString(rawPass))
                                                Toast.makeText(context, "Password Copied Securely to Clipboard", Toast.LENGTH_SHORT).show()
                                            }
                                        }) {
                                            Icon(Icons.Default.ContentCopy, "Copy credentials")
                                        }

                                        IconButton(onClick = {
                                            val revealed = revealedPasswordsMap[credentials.id] ?: false
                                            revealedPasswordsMap = revealedPasswordsMap.toMutableMap().apply {
                                                put(credentials.id, !revealed)
                                            }
                                        }) {
                                            Icon(
                                                imageVector = if (revealedPasswordsMap[credentials.id] == true) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = "Toggle visual hide transformation"
                                            )
                                        }

                                        if (!isDecoyMode) {
                                            IconButton(onClick = { viewModel.deletePassword(credentials) }) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete Password Securely",
                                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // New/Edit password editor dialog with Generator
    if (showEditorDialog) {
        var editTitle by remember { mutableStateOf(selectedPasswordForEdit?.title ?: "") }
        var editWebsite by remember { mutableStateOf(selectedPasswordForEdit?.website ?: "") }
        var editUsername by remember { mutableStateOf(selectedPasswordForEdit?.username ?: "") }
        var editPasswordVal by remember { mutableStateOf("") }
        var editNotes by remember { mutableStateOf(selectedPasswordForEdit?.notes ?: "") }
        var strengthRating by remember { mutableStateOf(1) }

        LaunchedEffect(selectedPasswordForEdit) {
            if (selectedPasswordForEdit != null) {
                viewModel.decryptPassword(selectedPasswordForEdit!!) { editPasswordVal = it }
            } else {
                editPasswordVal = ""
            }
        }

        // Dynamically compute password complexity
        LaunchedEffect(editPasswordVal) {
            strengthRating = when {
                editPasswordVal.length >= 12 && editPasswordVal.any { it.isDigit() } && editPasswordVal.any { !it.isLetterOrDigit() } -> 5
                editPasswordVal.length >= 8 && editPasswordVal.any { it.isDigit() } -> 4
                editPasswordVal.length >= 6 -> 3
                else -> 1
            }
        }

        Dialog(onDismissRequest = { showEditorDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = if (selectedPasswordForEdit == null) "Add Password Card" else "Edit Credentials",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Title (e.g. GitHub, bank)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = editWebsite,
                        onValueChange = { editWebsite = it },
                        label = { Text("URL / App Website") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = editUsername,
                        onValueChange = { editUsername = it },
                        label = { Text("Account Login / Email") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = editPasswordVal,
                        onValueChange = { editPasswordVal = it },
                        label = { Text("Secured Passphrase") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Passphrase generator trigger
                    TextButton(
                        onClick = {
                            // Generate high strength secure password instantly
                            val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()_+-"
                            val generated = (1..14).map { chars[Random.nextInt(chars.length)] }.joinToString("")
                            editPasswordVal = generated
                            Toast.makeText(context, "Hyper Secure Credentials Generated", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(Icons.Default.Autorenew, "Generate Password")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Auto-Generate 14-char PIN", fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Dynamic strength bar
                    Column {
                        Text(
                            text = "Cryptographic Strength Rating:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            for (i in 1..5) {
                                val filled = strengthRating >= i
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(6.dp)
                                        .background(
                                            color = if (filled) {
                                                when (strengthRating) {
                                                    5 -> Color(0xFF10B981)
                                                    4, 3 -> Color(0xFFFBBF24)
                                                    else -> Color(0xFFEF4444)
                                                }
                                            } else {
                                                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)
                                            },
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = editNotes,
                        onValueChange = { editNotes = it },
                        label = { Text("Extra Metadata / Hints") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(
                            onClick = { showEditorDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Discard", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                        }

                        Button(
                            onClick = {
                                if (editTitle.isEmpty() || editPasswordVal.isEmpty()) return@Button
                                viewModel.savePassword(
                                    id = selectedPasswordForEdit?.id ?: 0,
                                    title = editTitle,
                                    website = editWebsite,
                                    username = editUsername,
                                    plaintextPassword = editPasswordVal,
                                    notes = editNotes,
                                    strengthScore = strengthRating
                                )
                                showEditorDialog = false
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Lock Wallet")
                        }
                    }
                }
            }
        }
    }
}

// --- PANEL 5: SETTINGS & INTRUDER LOGS ---
@Composable
fun SettingsAndLogsScreen(
    viewModel: VaultViewModel
) {
    val context = LocalContext.current
    val isDecoyMode by viewModel.isDecoyMode.collectAsStateWithLifecycle()
    val isDarkSelected by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val isBiometricsSelected by viewModel.isBiometricsEnabled.collectAsStateWithLifecycle()
    val logs by viewModel.intruderLogs.collectAsStateWithLifecycle()

    var showResetPinDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Security Console",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Modify cryptographic settings and check intrusion trackers.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }

        // Appearance Adjustments
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Device Interface", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("App Theme Mode", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeOptionCard(
                            title = "System Default",
                            icon = Icons.Default.Settings,
                            selected = themeMode == "system",
                            modifier = Modifier.weight(1f)
                        ) {
                            viewModel.updateAppSetting("theme_mode", "system")
                        }
                        ThemeOptionCard(
                            title = "Dark Mode",
                            icon = Icons.Default.DarkMode,
                            selected = themeMode == "dark",
                            modifier = Modifier.weight(1f)
                        ) {
                            viewModel.updateAppSetting("theme_mode", "dark")
                        }
                        ThemeOptionCard(
                            title = "Light Mode",
                            icon = Icons.Default.LightMode,
                            selected = themeMode == "light",
                            modifier = Modifier.weight(1f)
                        ) {
                            viewModel.updateAppSetting("theme_mode", "light")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Enable Biometric Shortcut", fontSize = 14.sp)
                        Switch(
                            checked = isBiometricsSelected,
                            onCheckedChange = { viewModel.updateAppSetting("biometrics_enabled", it.toString()) }
                        )
                    }
                }
            }
        }

        // Safety controls (Reset credentials)
        if (!isDecoyMode) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth()
                    ) {
                        Text("Security Adjustments", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { showResetPinDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Password, "Reset Code")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reset Lock PINs")
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                viewModel.lockApp()
                                Toast.makeText(context, "Hardware context flushed. Locked", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Lock, "Lock Now")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Lock App Now")
                        }
                    }
                }
            }
        }

        // Intruder attempts logger (MANDATORY Premium detail!)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Intruder Attempts Logger", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        if (logs.isNotEmpty()) {
                            TextButton(onClick = { viewModel.clearFailedLoginLogs() }) {
                                Text("Clear", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (logs.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No security breaches recorded yet.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            logs.take(10).forEach { log ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            MaterialTheme.colorScheme.background,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Security Breach snap avatar
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(Color.Red.copy(alpha = 0.15f), shape = CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Warning,
                                            "Breach alert",
                                            tint = Color.Red,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Failed Entry Attempt Code: ${log.enteredCode}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color.Red
                                        )
                                        Text(
                                            text = SimpleDateFormat("MMM dd, yyyy - HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp)),
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // PIN change setup modal
    if (showResetPinDialog) {
        var oldPinVal by remember { mutableStateOf("") }
        var newPinVal by remember { mutableStateOf("") }
        var newDecoyPinVal by remember { mutableStateOf("") }
        var validationErr by remember { mutableStateOf<String?>(null) }

        Dialog(onDismissRequest = { showResetPinDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Reset Encryption PINs",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = newPinVal,
                        onValueChange = { if (it.length <= 4) newPinVal = it },
                        label = { Text("New 4-Digit Primary PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = newDecoyPinVal,
                        onValueChange = { if (it.length <= 4) newDecoyPinVal = it },
                        label = { Text("New 4-Digit Decoy PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (validationErr != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = validationErr!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(
                            onClick = { showResetPinDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                        }

                        Button(
                            onClick = {
                                if (newPinVal.length < 4 || newDecoyPinVal.length < 4) {
                                    validationErr = "PIN matches must be exactly 4 digits long!"
                                    return@Button
                                }
                                if (newPinVal == newDecoyPinVal) {
                                    validationErr = "Primary PIN cannot match Decoy PIN!"
                                    return@Button
                                }
                                viewModel.updateAppSetting("master_pin", newPinVal)
                                viewModel.updateAppSetting("decoy_pin", newDecoyPinVal)
                                showResetPinDialog = false
                                Toast.makeText(context, "Credentials Correctly Synced", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Re-encrypt")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ThemeOptionCard(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(68.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            }
        ),
        border = BorderStroke(
            width = 1.2.dp,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
            }
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                },
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                }
            )
        }
    }
}
