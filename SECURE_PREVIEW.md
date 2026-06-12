# 🔐 Secure Zero-Knowledge Preview Engine

Welcome to the Secure Vault File Preview & Advanced Theme Documentation. This system allows you to preview fully encrypted, cryptographically secured files in-memory without leaving trace files on external physical storage. 

This guide outlines how the **Secure Preview Panel**, **In-Memory Media Streamers**, and **Adaptive Light Theme Palette** function in unity.

---

## 📸 1. Adaptive Photo Reader & Decrypter
When a secure image container is tapped:
* **AES-256 Memory Extraction**: The application pulls the byte array directly into Jetpack Compose.
* **Bitmap Conversion**: It decodes the stream safely on-the-fly via a secure JVM `BitmapFactory` stack.
* **Sandbox Sandbox Mode**: If the image payload contains simulated strings/raw data (e.g., sandbox simulation files), it renders an elegant blue-gradient developer layout exposing the safe diagnostic headers.

---

## 🎵 2. Rich Interactive Audio Player
The Audio player brings immersive media playback directly into the secure zone:
* **Native Audio Interface**: Writes decrypted bytes to secure temporary session files in the application's private cache directory.
* **MediaPlayer Integration**: Initializes a private `MediaPlayer` instance that enables standard tracking, metadata sizing, and hardware-level audio pipelines.
* **Interactive Timelines**: Drag seek bars to fast-forward or rewind decrypted audio.
* **Zero-Trace Disposal**: The temporary cache is forcibly garbage-collected and physically deleted as soon as the preview container is dismissed (`DisposableEffect`).

---

## 🎬 3. Cinema Video Controller
An aesthetic, virtualized cinema module for video containers featuring:
* **Dynamic Pulsers**: A radial gradient glow pulsing dynamically using `InfiniteTransition` animation hooks to mimic ambient movie lighting.
* **Interactive Equalizers**: Standard 5-column fluid vertical bar visualizers that bounce rhythmically when playing and freeze when paused.
* **Advanced Controls**:
  * **Interactive Slider**: Draggable video progress timelines.
  * **Video Speed Multipliers**: Quick toggling between `1.0x`, `1.5x`, and `2.0x` speeds.
  * **Audio Controls**: Quick volume toggles (Muted vs. Loud).

---

## 📄 4. High-Contrast Text & Document Reader
Binary-safe UTF-8 inspector designed to display plaintext, logs, and notes:
* **Binary Detection**: Scans the leading bytes for null terminators to differentiate structured documents from binary files.
* **Thematic Console**: Renders inside a clean, high-contrast monospace code block.
* **Information Overviews**: Tracks safe character counts and text size.

---

## 🎨 5. Material 3 Light Mode Palette Remap
To ensure the vault remains stunning and highly readable under bright conditions, the card containers use custom light-mode variables:
* **Uninstallation Card**: High-contrast warm red alerts with clean dark-ruby text indicators (`#7F1D1D`) that replace dark saturated alerts.
* **Soft Pastels**: Categorized notes display with soft pastel backgrounds (e.g., `#F3E8FF` Lavender, `#EFF6FF` Cloud Blue, `#ECFDF5` Mint, `#FEF2F2` Rose) to ensure premium visual scanning.
* **Contrasting Copy**: Custom contrast metrics map light cards directly to matching deep, accessible font colors to make reading easy on the eyes.
