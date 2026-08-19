# 📸 PhotoMigrate

### Direct, AI-Powered Google Photos Migration Engine (100% Free)

**PhotoMigrate** is a high-performance Android application that gives you total control over your
cloud media. Move, sync, and organize thousands of photos between Google accounts directly from your
device. **No servers, no subscriptions, no middleman.**

---

## 🚀 The "Migration Terminal" Experience

Experience the most technical and transparent migration interface ever built for mobile.

- **Speed Monitoring**: A live, high-density area chart showing real-time network throughput (KB/s, MB/s, GB/s).
- **Retro CLI Aesthetics**: A minimalist terminal-style dashboard with monospaced logs and
  `STOP_EXECUTION` control logic.
- **Live Remaining Estimates**: Intelligent calculations of time remaining based on live speed and
  data volume.
- **Deep-Dive Logs**: Real-time console output for every single file operation (Download ->
  Analyze -> Compress -> Upload).

---

## 🧠 Advanced Intelligence Features

### 1. "Incognito" Secure Vault (New)
Protect your most sensitive media with military-grade privacy.
- **Biometric Security**: Entry to the vault is locked behind your device's Fingerprint, Face Unlock, or PIN.
- **On-Device Encryption**: Files are encrypted using **AES-256 GCM** and stored in the app's internal private storage, hidden from the system gallery.
- **One-Tap Move**: Select items in the picker and tap the "Vault" button to securely download, encrypt, and trash them from the cloud in one step.

### 2. Gallery Explorer
Browse your cloud accounts like a local gallery.
- **Full-Resolution Viewing**: Tap any cloud photo to view it in full quality.
- **Direct Video Streaming**: Integrated **Media3 (ExoPlayer)** for seamless cloud video playback.
- **Premium Loading**: Features a custom "Solving Cube" animation during data fetching for a high-tech feel.

### 3. Advanced Sorting & Filtering
Prioritize your migration with surgical precision.
- **Size-Based Sorting**: Sort by "Largest First" to identify and move high-impact files to free up space quickly.
- **Threshold Filtering**: Filter out files smaller or larger than a specific MB limit.
- **Alphabetical & Date Sorting**: Organize by filename (A-Z) or chronological order.

### 4. Smart AI Organization (Experimental)
Leverage **Google ML Kit** for on-device content analysis.
- **Consensus Batch Grouping**: The app analyzes a sample of your selected photos to determine a
  dominant category (e.g., "Nature", "Pets", "City Life").
- **Auto-Albums**: Automatically creates and sorts photos into destination albums based on content
  or Date (e.g., "August 2026").

### 5. "Storage Saver" Optimization
Maximize your free 15GB Google storage tier.
- **On-the-fly Compression**: Converts images to **WebP Lossy (80% Quality)** during transfer.
- **Smart Resizing**: Automatically scales 4K+ images down to optimized resolutions.

---

## 📊 Analytics & Persistent History

The app remembers your hard work even after a restart.

- **Lifetime Stats**: A global dashboard showing the total volume of data (GB) you've successfully
  migrated.
- **Job History**: A persistent database of every past migration session.
- **Post-Job Troubleshooting**: Tap any past job to view its full technical logs.

---

## 🎨 Professional Personalization

PhotoMigrate comes with **9 Premium Themes** designed to match your style:

- 🌑 **AMOLED Black**, 🍏 **Apple Glass**, 🧛 **Dracula**, ⚡ **Synthwave**, 🌿 **Emerald**, 💎 **Azure**, 🍯 **Amber**, 🌸 **Sakura**, 🍇 **Violet**.

---

## 🛠️ How it Works (Architecture)

```mermaid
graph TD
    A[Source Account] -->|Download (Original)| B(Mobile Device)
    B --> C{AI & Processing}
    C -->|AI Analysis| D[Consensus Category]
    C -->|Vault| E[AES-256 Encryption]
    C -->|Storage Saver| F[WebP Compression]
    D --> G[Destination Account]
    F --> G
    E --> H[(Local Secure Vault)]
    G -->|Verify| I[(History Database)]
    I -->|Update| J[Lifetime Analytics]
```

---

## ⚙️ Technical Stack

- **Language**: Kotlin 1.9 (Coroutines + Flow)
- **UI**: Jetpack Compose (Material 3 + Glassmorphic components)
- **Database**: Room (v8 Schema) for History, Analytics, Vault, and Indexing.
- **Security**: Biometric Auth + Security Crypto (AES-256).
- **Media**: Media3 ExoPlayer for high-performance streaming.
- **AI**: Google ML Kit (Image Labeling).

---

## ⚡ Quick Setup

1. Create a free project at [Google Cloud Console](https://console.cloud.google.com).
2. Enable `Google Photos Library API` and `Google Drive API`.
3. Create an **iOS OAuth Client ID** (Bundle ID: `com.photomigrate.app`).
4. Enter the Client ID in the app's **Advanced Settings**.
5. **Important**: Ensure you check the "Full Drive Access" permission box during sign-in to enable **MOVE** mode.

---
*Created with ❤️ by **Priyanath Mukherjee***
