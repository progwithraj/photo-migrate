# 📸 PhotoMigrate

### Direct, AI-Powered Google Photos Migration Engine (100% Free)

**PhotoMigrate** is a high-performance Android application that gives you total control over your
cloud media. Move, sync, and organize thousands of photos between Google accounts directly from your
device. **No servers, no subscriptions, no middleman.**

---

## 🚀 The "Migration Terminal" Experience

Experience the most technical and transparent migration interface ever built for mobile.

- **Speed Monitoring**: A live, high-density area chart showing real-time network throughput.
- **Retro CLI Aesthetics**: A minimalist terminal-style dashboard with monospaced logs and
  `STOP_EXECUTION` control logic.
- **Live Remaining Estimates**: Intelligent calculations of time remaining based on live speed and
  data volume.
- **Deep-Dive Logs**: Real-time console output for every single file operation (Download ->
  Analyze -> Compress -> Upload).

---

## 🧠 Advanced Intelligence Features

### 1. Smart AI Organization (Experimental)

Leverage **Google ML Kit** for on-device content analysis.

- **Consensus Batch Grouping**: The app analyzes a sample of your selected photos to determine a
  dominant category (e.g., "Nature", "Pets", "City Life").
- **Auto-Albums**: Automatically creates and sorts photos into destination albums based on content
  or Date (e.g., "August 2026").
- **Privacy First**: All AI analysis happens **locally on your device**. No image data ever leaves
  your phone for analysis.

### 2. "Storage Saver" Optimization

Maximize your free 15GB Google storage tier.

- **On-the-fly Compression**: Converts images to **WebP Lossy (80% Quality)** during transfer.
- **Smart Resizing**: Automatically scales 4K+ images down to optimized resolutions.
- **Savings**: Reduce destination storage usage by up to **80%** with negligible quality loss.

### 3. Smart Duplicate Detection

Save time and bandwidth by skipping what's already there.

- **Background Indexing**: The app indexes your destination library in the background while you pick
  photos.
- **Multi-Layer Matching**: Checks for duplicates using both metadata (Filename, Size, Time) and
  SHA-256 binary fingerprints.

---

## 📊 Analytics & Persistent History

The app remembers your hard work even after a restart.

- **Lifetime Stats**: A global dashboard showing the total volume of data (GB) you've successfully
  migrated.
- **Job History**: A persistent database of every past migration session.
- **Post-Job Troubleshooting**: Tap any past job to view its full technical logs to see exactly why
  specific files might have failed.

---

## 🎨 Professional Personalization

PhotoMigrate comes with **9 Premium Themes** designed to match your style:

- 🌑 **AMOLED Black**: Pure black background for OLED efficiency.
- 🍏 **Apple Glass**: Clean, translucent iOS-inspired aesthetic.
- 🧛 **Dracula**: The classic developer's dark palette.
- ⚡ **Synthwave**: Vibrant 80s neon-retro theme.
- 🌿 **Emerald**, 💎 **Azure**, 🍯 **Amber**, 🌸 **Sakura**, 🍇 **Violet**.

---

## 🛠️ How it Works (Architecture)

```mermaid
graph TD
    A[Source Account] -->|Download (Original)| B(Mobile Device)
    B --> C{AI & Processing}
    C -->|AI Analysis| D[Consensus Category]
    C -->|Storage Saver| E[WebP Compression]
    D --> F[Destination Account]
    E --> F
    F -->|Verify| G[(History Database)]
    G -->|Update| H[Lifetime Analytics]
```

---

## ⚙️ Technical Stack

- **Language**: Kotlin 1.9 (Coroutines + Flow)
- **UI**: Jetpack Compose (Material 3 + Glassmorphic components)
- **Database**: Room (v7 Schema) for History, Analytics, and Indexing.
- **AI**: Google ML Kit (Image Labeling).
- **Background**: WorkManager with Foreground Service support.
- **Network**: OkHttp 4.12 (Custom throttled request bodies).

---

## ⚡ Quick Setup

1. Create a free project at [Google Cloud Console](https://console.cloud.google.com).
2. Enable `Google Photos Library API` and `Google Drive API`.
3. Create an **iOS OAuth Client ID** (Bundle ID: `com.photomigrate.app`).
4. Enter the Client ID in the app's **Advanced Settings**.
5. **Important**: Ensure you check the "Full Drive Access" permission box during sign-in to enable *
   *MOVE** mode.

---
*Created with ❤️ by **Priyanath Mukherjee***
