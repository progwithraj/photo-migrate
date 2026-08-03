# PhotoMigrate - Multi-Account Google Photos Transfer & Sync (100% Free)

**PhotoMigrate** is a native Android application designed to move, sync, and migrate photos and videos between multiple free Google accounts (e.g., migrating photos from a full 15GB account to a fresh 15GB account to avoid paying for Google One storage).

## Features
- **100% Free Architecture**: Connects directly from your Android device to Google REST APIs ($0.00 cost, no paid middleman servers or subscriptions).
- **Multi-Account OAuth**: Connect 2 or more Google Accounts with storage usage meters (Used GB / 15 GB total).
- **Move vs. Copy Modes**:
  - **Move Mode**: Transfers photo to destination account, verifies upload integrity, then trashes original in source account to **free up storage**.
  - **Copy Mode**: Copies photos to destination account while preserving original files in source account.
- **Deduplication Engine**: Uses SHA-256 file checksum fingerprints to prevent uploading duplicate photos.
- **Background Worker**: Powered by Android `WorkManager` & Foreground Notification service so transfers continue reliably even when your phone screen locks.
- **Live Transfer Dashboard**: Progress bar, MB/s speed gauge, item counter, pause/resume, and live activity log console.
- **Built-in Setup Guide**: Step-by-step wizard to create your free Google Cloud OAuth Client ID in under 2 minutes.

---

## Technical Stack & Libraries
- **Language**: Kotlin 1.9
- **UI Framework**: Jetpack Compose with Material 3 (Dark & Light theme, Glassmorphic design)
- **Networking**: OkHttp 4.12 & Gson
- **Image Loading**: Coil Compose 2.5
- **Background Engine**: Android WorkManager (DataSync Foreground Service)
- **Security**: Android Security Crypto (EncryptedSharedPreferences)

---

## How to Build & Run
1. Open Android Studio and choose **Open an Existing Project**.
2. Select `/Users/IPRIYANATH/.gemini/antigravity-ide/scratch/photo-sync-app`.
3. Select your connected Android device or emulator.
4. Click **Run** (or execute `./gradlew assembleDebug` in terminal).

---

## Quick Setup (Free Google Cloud OAuth ID)
1. Open [console.cloud.google.com](https://console.cloud.google.com).
2. Create a free project named **PhotoMigrate**.
3. Go to **APIs & Services** -> **Library**, search and enable:
   - `Google Photos Library API`
   - `Google Drive API`
4. Go to **Credentials** -> **Create Credentials** -> **OAuth Client ID**.
5. Select **iOS** as the Application Type (Allows custom URIs for Android).
6. Set **Bundle ID** to `com.photomigrate.app`.
7. Copy your Client ID and enter it into the app's **Setup Guide** screen.
