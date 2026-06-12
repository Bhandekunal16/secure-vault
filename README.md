# 🛡️ SecureCrypt Vault: Zero-Knowledge Applet

SecureCrypt Vault is a security-first Android application designed to secure photos, videos, audio tracks, documents, notes, and credentials on-device using a high-density local database and memory-bound on-the-fly decryption. Powered by **Kotlin**, **Jetpack Compose**, and **Material Design 3**, it ensures that zero trace data is leaked to external physical storage.

---

## ✨ Features Breakdown

### 🔐 1. In-Memory Secure Preview Engine
Allows direct in-app visualization of raw hardware-decrypted AES containers:
- **Sandbox Photo Viewer**: Fully decrypts binary streams directly to bitmap textures and renders them safely in memory. Includes a clean developer preview option for raw mock sandbox items.
- **Embedded Audio Player**: Instantiates private cache streams to play encrypted tracks with scrubbing, real-time seek sliders, and automatic cache sweeping upon dismissal.
- **Cinema Video Controller**: Features simulated infinite ambient lighting glows (synced with animated radial canvas gradients) and a companion 5-column responsive equalizer that mirrors state-based actions (playing/paused). Supports variable playback speeds (`1.0x`, `1.5x`, `2.0x`) and sound muting.
- **PlainText Code Inspector**: Automatic UTF-8 string encoding inspection with a monospace diagnostic viewer, character counts, and size metrics.

### 📝 2. Cryptographic Notes & Board
Secure workspace notes with user-customized backgrounds:
- Automatically maps background containers to Material 3 pastels in Light Mode (e.g., Soft Lavender, Mint, Rose) and rich neon saturations in Dark Mode.
- Dynamic contrasting title and subtitle font mapping to ensure optimum legibility metrics across all color modes.

### 🔑 3. Decentralized Password Manager
A secure storage layout for managing highly sensitive accounts:
- Add, search, categorization, and secure viewing.
- Simple, high-fidelity edit pathways for changing credentials.

### 🚫 4. Zero-Knowledge Safeguard & Backup
- **Hardware-Decryption Protection Warning**: Alert system reminding users of zero-knowledge recovery limits (permanent deletion of local keys upon uninstallation).
- **Safe Export Portability**: Allows bulk backups through structured streams down to the public downloads space with a real-time progress layout.

### 🎨 5. Material Custom themes
Supports customized dark layouts, pure light templates, or system-synchronized auto-adjustment.

---

## 🏛️ Architecture Overview

SecureCrypt Vault uses modern Android development practices:
- **UI Framework**: Modern Jetpack Compose using Material 3 dynamically styled states.
- **State Management**: Reactive Architecture utilizing `ViewModel` and `MutableStateFlow` structures paired with lifecycle-aware flow collection.
- **Persistence Layer**: Custom secure SQLite mapping with Room for notes and metadata parameters.
- **Memory-Bound Security**: Cryptographic pipelines are processed strictly in RAM, automatically wiping decrypted buffer spaces on cleanup.

---

## ⚙️ Development & Configuration

To run and customize this project under a standard Android environment:

### Minimum Target SDK Requirements
- **Minimum SDK**: 26 (Android 8.0 Oreo)
- **Target SDK**: 34 / 35 (Android 14 / 15)

### Running the Applet
1. Import into **Android Studio** (Koala or newer recommended).
2. Configure your build chain with JDK 17.
3. Tap **Run** to execute on physical devices or standard emulators.