package com.example.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String, // Encrypted base64
    val category: String, // "Personal", "Work", "Private", etc.
    val createdAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val isPinned: Boolean = false,
    val color: Int = 0 // Custom background color index for Material 3 UI cards
)

@Entity(tableName = "passwords")
data class PasswordEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val website: String,
    val username: String,
    val password: String, // Encrypted base64
    val notes: String = "",
    val strengthScore: Int = 0, // 1 to 5 (Weak to Strong)
    val createdAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)

enum class VaultFileType {
    PHOTO, VIDEO, DOCUMENT, AUDIO, PRIVATE_FILE
}

@Entity(tableName = "vault_files")
data class VaultFileEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val fileType: String, // converted from VaultFileType
    val size: Long,
    val filePath: String, // Path to stored encrypted file content
    val createdAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val durationMs: Long = 0, // for video/audio files
    val extraMetadata: String = "" // serialised JSON of other attributes
)

@Entity(tableName = "intruder_logs")
data class IntruderLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val enteredCode: String,
    val photoUrl: String = "", // Placeholders for snap representation
    val isSuccessful: Boolean = false
)

@Entity(tableName = "app_settings")
data class SettingEntity(
    @PrimaryKey val key: String,
    val value: String
)
