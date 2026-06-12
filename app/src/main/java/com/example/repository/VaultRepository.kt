package com.example.repository

import android.content.Context
import com.example.database.*
import com.example.security.CryptoUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class VaultRepository(
    private val context: Context,
    private val noteDao: NoteDao,
    private val passwordDao: PasswordDao,
    private val vaultFileDao: VaultFileDao,
    private val intruderLogDao: IntruderLogDao,
    private val settingDao: SettingDao
) {
    private val vaultDir = File(context.filesDir, "secured_vault_media").apply {
        if (!exists()) mkdirs()
    }

    // --- NOTES (Encrypted) ---
    val allNotes: Flow<List<NoteEntity>> = noteDao.getAllNotes()

    suspend fun getDecryptedNoteContent(note: NoteEntity): String = withContext(Dispatchers.Default) {
        CryptoUtils.decryptString(note.content)
    }

    suspend fun saveNote(id: Int = 0, title: String, content: String, category: String, isFavorite: Boolean = false, isPinned: Boolean = false, color: Int = 0): Long = withContext(Dispatchers.Default) {
        val encryptedContent = CryptoUtils.encryptString(content)
        val entity = NoteEntity(
            id = id,
            title = title,
            content = encryptedContent,
            category = category,
            isFavorite = isFavorite,
            isPinned = isPinned,
            color = color
        )
        noteDao.insertOrUpdateNote(entity)
    }

    suspend fun deleteNote(note: NoteEntity) {
        noteDao.deleteNote(note)
    }

    // --- PASSWORDS (Encrypted) ---
    val allPasswords: Flow<List<PasswordEntity>> = passwordDao.getAllPasswords()

    suspend fun decryptPassword(password: PasswordEntity): String = withContext(Dispatchers.Default) {
        CryptoUtils.decryptString(password.password)
    }

    suspend fun savePassword(
        id: Int = 0,
        title: String,
        website: String,
        username: String,
        plaintextPassword: String,
        notes: String = "",
        strengthScore: Int = 1,
        isFavorite: Boolean = false
    ): Long = withContext(Dispatchers.Default) {
        val encryptedPassword = CryptoUtils.encryptString(plaintextPassword)
        val entity = PasswordEntity(
            id = id,
            title = title,
            website = website,
            username = username,
            password = encryptedPassword,
            notes = notes,
            strengthScore = strengthScore,
            isFavorite = isFavorite
        )
        passwordDao.insertOrUpdatePassword(entity)
    }

    suspend fun deletePassword(password: PasswordEntity) {
        passwordDao.deletePassword(password)
    }

    // --- VAULT FILES (Encrypted) ---
    val allFiles: Flow<List<VaultFileEntity>> = vaultFileDao.getAllFiles()
    val favoriteFiles: Flow<List<VaultFileEntity>> = vaultFileDao.getFavoriteFiles()

    fun getFilesByType(type: VaultFileType): Flow<List<VaultFileEntity>> {
        return vaultFileDao.getFilesByType(type.name)
    }

    /**
     * Imports a raw file into the encrypted internal storage.
     */
    suspend fun importFile(name: String, type: VaultFileType, fileBytes: ByteArray, durationMs: Long = 0, isFavorite: Boolean = false): VaultFileEntity = withContext(Dispatchers.IO) {
        // Encrypt the binary payload using AES-256-GCM via Keystore
        val encryptedBytes = CryptoUtils.encrypt(fileBytes)
        
        // Generate a random unique file name to hide identity on disk
        val secureFileName = "enc_${UUID.randomUUID()}"
        val destinationFile = File(vaultDir, secureFileName)
        destinationFile.writeBytes(encryptedBytes)

        val entity = VaultFileEntity(
            name = name,
            fileType = type.name,
            size = fileBytes.size.toLong(),
            filePath = destinationFile.absolutePath,
            isFavorite = isFavorite,
            durationMs = durationMs
        )
        val insertedId = vaultFileDao.insertOrUpdateFile(entity)
        entity.copy(id = insertedId.toInt())
    }

    /**
     * Decrypts file content from the encrypted storage.
     */
    suspend fun decryptFileContent(vaultFile: VaultFileEntity): ByteArray = withContext(Dispatchers.IO) {
        val file = File(vaultFile.filePath)
        if (!file.exists()) {
            throw java.io.FileNotFoundException("Secured file has been deleted or cannot be found on disk")
        }
        val encryptedBytes = file.readBytes()
        CryptoUtils.decrypt(encryptedBytes)
    }

    suspend fun deleteFile(vaultFile: VaultFileEntity) = withContext(Dispatchers.IO) {
        // Remove from physical database
        vaultFileDao.deleteFile(vaultFile)
        // Cleanup the encrypted file from storage
        val file = File(vaultFile.filePath)
        if (file.exists()) {
            file.delete()
        }
    }

    suspend fun toggleFavoriteFile(vaultFile: VaultFileEntity) = withContext(Dispatchers.IO) {
        val updated = vaultFile.copy(isFavorite = !vaultFile.isFavorite)
        vaultFileDao.insertOrUpdateFile(updated)
    }

    // --- INTRUDER LOGS ---
    val allIntruderLogs: Flow<List<IntruderLogEntity>> = intruderLogDao.getAllLogs()

    suspend fun logIntruderAttempt(enteredCode: String, isSuccessful: Boolean, simulatedPhotoBase64: String = "") = withContext(Dispatchers.IO) {
        val log = IntruderLogEntity(
            enteredCode = enteredCode,
            photoUrl = simulatedPhotoBase64,
            isSuccessful = isSuccessful
        )
        intruderLogDao.insertLog(log)
    }

    suspend fun clearIntruderLogs() {
        intruderLogDao.clearAllLogs()
    }

    // --- SETTINGS (PIN, Decoy, Appearance, Storage etc) ---
    suspend fun getSetting(key: String, defaultValue: String): String {
        return settingDao.getSettingValue(key) ?: defaultValue
    }

    suspend fun saveSetting(key: String, value: String) {
        settingDao.saveSetting(SettingEntity(key, value))
    }

    // Prefill helper for initial startup
    suspend fun prefillInitialDataIfEmpty() = withContext(Dispatchers.IO) {
        val currentNotes = noteDao.getAllNotes().first()
        if (currentNotes.isEmpty()) {
            // Add a friendly greeting/help note
            saveNote(
                title = "Welcome to Secure Vault",
                content = "This note is fully encrypted using hardware-backed AES-256-GCM. Your files, passwords, and secrets are absolutely safe here on your device. To add a new password, note, or file, click the action buttons below!",
                category = "General",
                isFavorite = true,
                isPinned = true,
                color = 2
            )
            
            // Add a sample password card
            savePassword(
                title = "Google Secure Account",
                website = "accounts.google.com",
                username = "vault_user@gmail.com",
                plaintextPassword = "p_at_ss_word_example_123",
                notes = "Primary email address, secured behind bio-lock",
                strengthScore = 5,
                isFavorite = true
            )
        }
    }
}
