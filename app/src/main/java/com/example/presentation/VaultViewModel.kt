package com.example.presentation

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.database.IntruderLogEntity
import com.example.database.NoteEntity
import com.example.database.PasswordEntity
import com.example.database.VaultFileEntity
import com.example.database.VaultFileType
import com.example.repository.VaultRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class AuthScreenState {
    object Setup : AuthScreenState()
    object Enter : AuthScreenState()
    object Unlocked : AuthScreenState()
}

class VaultViewModel(private val repository: VaultRepository) : ViewModel() {

    // --- LOCK & AUTH STATE ---
    private val _authState = MutableStateFlow<AuthScreenState>(AuthScreenState.Enter)
    val authState: StateFlow<AuthScreenState> = _authState.asStateFlow()

    private val _isDecoyMode = MutableStateFlow(false)
    val isDecoyMode: StateFlow<Boolean> = _isDecoyMode.asStateFlow()

    private val _pinAttempts = MutableStateFlow(0)
    val pinAttempts: StateFlow<Int> = _pinAttempts.asStateFlow()

    private val _savedPin = MutableStateFlow("")
    private val _savedDecoyPin = MutableStateFlow("")
    
    val isInitialized = MutableStateFlow(false)
    val isBiometricsEnabled = MutableStateFlow(false)
    val isDarkMode = MutableStateFlow(true)
    val themeMode = MutableStateFlow("system") // "system", "dark", "light"
    val accentColorIndex = MutableStateFlow(0) // Indigo, Cyan, Emerald, Burgundy
    val uninstallWarningDismissed = MutableStateFlow(false)

    // --- SEARCH & FILTERS ---
    val searchQuery = MutableStateFlow("")
    val selectedCategoryFilter = MutableStateFlow("All")
    val selectedFileTypeFilter = MutableStateFlow<VaultFileType?>(null)

    init {
        loadSettingsAndInitialize()
    }

    private fun loadSettingsAndInitialize() {
        viewModelScope.launch {
            val pin = repository.getSetting("master_pin", "")
            val decoy = repository.getSetting("decoy_pin", "")
            val bio = repository.getSetting("biometrics_enabled", "false")
            val dark = repository.getSetting("dark_mode", "true")
            val theme = repository.getSetting("theme_mode", "system")
            val accent = repository.getSetting("accent_color_index", "0")
            val dismissed = repository.getSetting("uninstall_warning_dismissed", "false")

            _savedPin.value = pin
            _savedDecoyPin.value = decoy
            isBiometricsEnabled.value = bio.toBoolean()
            isDarkMode.value = dark.toBoolean()
            themeMode.value = theme
            accentColorIndex.value = accent.toIntOrNull() ?: 0
            uninstallWarningDismissed.value = dismissed.toBoolean()

            if (pin.isEmpty()) {
                _authState.value = AuthScreenState.Setup
                isInitialized.value = false
            } else {
                _authState.value = AuthScreenState.Enter
                isInitialized.value = true
            }
        }
    }

    // --- COMBINED reactive streams (Filtered by standard/decoy mode + search query) ---
    val notes: StateFlow<List<NoteEntity>> = combine(
        repository.allNotes,
        searchQuery,
        _isDecoyMode
    ) { rawNotes, query, isDecoy ->
        val cleanNotes = if (isDecoy) {
            // Decoy vault displays a simulated tutorial note only
            listOf(
                NoteEntity(
                    id = 99912,
                    title = "Decoy System Activated",
                    content = "This is a decoy folder. Your primary vault remains completely hidden.",
                    category = "Personal",
                    color = 4
                )
            )
        } else {
            rawNotes
        }
        if (query.isEmpty()) {
            cleanNotes
        } else {
            cleanNotes.filter {
                it.title.contains(query, ignoreCase = true) || 
                it.category.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val passwords: StateFlow<List<PasswordEntity>> = combine(
        repository.allPasswords,
        searchQuery,
        _isDecoyMode
    ) { rawPasswords, query, isDecoy ->
        // In decoy mode we hide passwords or show fake tutorial accounts
        val cleanPasswords = if (isDecoy) {
            listOf(
                PasswordEntity(
                    id = 99933,
                    title = "Tutorial Card",
                    website = "decoy.example.com",
                    username = "guest@example.com",
                    password = "fake_password_mode",
                    notes = "Decoy vault account",
                    strengthScore = 3
                )
            )
        } else {
            rawPasswords
        }
        if (query.isEmpty()) {
            cleanPasswords
        } else {
            cleanPasswords.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.website.contains(query, ignoreCase = true) ||
                it.username.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val vaultFiles: StateFlow<List<VaultFileEntity>> = combine(
        repository.allFiles,
        searchQuery,
        _isDecoyMode
    ) { rawFiles, query, isDecoy ->
        // In decoy mode, hide all real files!
        val cleanFiles = if (isDecoy) emptyList() else rawFiles
        if (query.isEmpty()) {
            cleanFiles
        } else {
            cleanFiles.filter { it.name.contains(query, ignoreCase = true) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteFiles: StateFlow<List<VaultFileEntity>> = combine(
        repository.favoriteFiles,
        _isDecoyMode
    ) { rawFavorites, isDecoy ->
        if (isDecoy) emptyList() else rawFavorites
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val intruderLogs: StateFlow<List<IntruderLogEntity>> = repository.allIntruderLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun decryptNoteContent(note: NoteEntity, onResult: (String) -> Unit) {
        viewModelScope.launch {
            if (note.id >= 99912) {
                onResult(note.content) // Decoy not encrypted
            } else {
                onResult(repository.getDecryptedNoteContent(note))
            }
        }
    }

    fun decryptPassword(password: PasswordEntity, onResult: (String) -> Unit) {
        viewModelScope.launch {
            if (password.id >= 99933) {
                onResult("decoy_pin_example")
            } else {
                onResult(repository.decryptPassword(password))
            }
        }
    }

    // --- VAULT FILTERS ---
    fun selectCategoryFilter(cat: String) {
        selectedCategoryFilter.value = cat
    }

    // --- AUTH ACTIONS ---
    fun setupPin(pin: String, decoyPin: String) {
        viewModelScope.launch {
            repository.saveSetting("master_pin", pin)
            repository.saveSetting("decoy_pin", decoyPin)
            _savedPin.value = pin
            _savedDecoyPin.value = decoyPin
            _isDecoyMode.value = false
            isInitialized.value = true
            _authState.value = AuthScreenState.Unlocked
        }
    }

    fun verifyPin(enteredPin: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            if (enteredPin == _savedPin.value) {
                _isDecoyMode.value = false
                _authState.value = AuthScreenState.Unlocked
                _pinAttempts.value = 0
                onResult(true)
            } else if (enteredPin == _savedDecoyPin.value && _savedDecoyPin.value.isNotEmpty()) {
                _isDecoyMode.value = true
                _authState.value = AuthScreenState.Unlocked
                _pinAttempts.value = 0
                onResult(true)
            } else {
                val nextAttempts = _pinAttempts.value + 1
                _pinAttempts.value = nextAttempts
                
                // Intusion log trigger
                repository.logIntruderAttempt(
                    enteredCode = enteredPin,
                    isSuccessful = false,
                    simulatedPhotoBase64 = "snap_${System.currentTimeMillis()}"
                )
                
                onResult(false)
            }
        }
    }

    fun lockApp() {
        _authState.value = AuthScreenState.Enter
    }

    fun unlockWithBiometrics() {
        _isDecoyMode.value = false
        _authState.value = AuthScreenState.Unlocked
        _pinAttempts.value = 0
    }

    // --- NOTES CRUD ---
    fun saveNote(id: Int = 0, title: String, content: String, category: String, isFavorite: Boolean = false, isPinned: Boolean = false, color: Int = 0) {
        viewModelScope.launch {
            repository.saveNote(id, title, content, category, isFavorite, isPinned, color)
        }
    }

    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }

    // --- PASSWORDS CRUD ---
    fun savePassword(
        id: Int = 0,
        title: String,
        website: String,
        username: String,
        plaintextPassword: String,
        notes: String = "",
        strengthScore: Int = 1,
        isFavorite: Boolean = false
    ) {
        viewModelScope.launch {
            repository.savePassword(id, title, website, username, plaintextPassword, notes, strengthScore, isFavorite)
        }
    }

    fun deletePassword(password: PasswordEntity) {
        viewModelScope.launch {
            repository.deletePassword(password)
        }
    }

    // --- FILE ACTIONS ---
    fun importFile(name: String, fileType: VaultFileType, fileBytes: ByteArray, durationMs: Long = 0, isFavorite: Boolean = false) {
        viewModelScope.launch {
            repository.importFile(name, fileType, fileBytes, durationMs, isFavorite)
        }
    }

    fun decryptFile(vaultFile: VaultFileEntity, onResult: (ByteArray?) -> Unit) {
        viewModelScope.launch {
            try {
                val decrypted = repository.decryptFileContent(vaultFile)
                onResult(decrypted)
            } catch (e: Exception) {
                onResult(null)
            }
        }
    }

    fun deleteFile(vaultFile: VaultFileEntity) {
        viewModelScope.launch {
            repository.deleteFile(vaultFile)
        }
    }

    fun toggleFileFavorite(vaultFile: VaultFileEntity) {
        viewModelScope.launch {
            repository.toggleFavoriteFile(vaultFile)
        }
    }

    // --- LOG SERVICE ---
    fun clearFailedLoginLogs() {
        viewModelScope.launch {
            repository.clearIntruderLogs()
        }
    }

    // --- GENERAL SETTINGS ---
    fun updateAppSetting(key: String, value: String) {
        viewModelScope.launch {
            repository.saveSetting(key, value)
            when (key) {
                "biometrics_enabled" -> isBiometricsEnabled.value = value.toBoolean()
                "dark_mode" -> isDarkMode.value = value.toBoolean()
                "theme_mode" -> themeMode.value = value
                "accent_color_index" -> accentColorIndex.value = value.toIntOrNull() ?: 0
                "master_pin" -> _savedPin.value = value
                "decoy_pin" -> _savedDecoyPin.value = value
                "uninstall_warning_dismissed" -> uninstallWarningDismissed.value = value.toBoolean()
            }
        }
    }
}

class VaultViewModelFactory(private val repository: VaultRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VaultViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return VaultViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}