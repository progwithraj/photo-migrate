# Next Steps: PhotoMigrate Feature Roadmap

This document outlines potential high-value features to build on top of the existing PhotoMigrate
foundation.

## 1. Smart Duplicate Detection [COMPLETED]

Prevent destination library clutter by identifying existing media.

- **Implementation**: Indexing destination metadata (Filename, Size, CreationTime) and comparing
  before download.
- **Benefit**: Saves significant bandwidth and storage by skipping existing items.

## 2. Cross-Platform Migration

Expand beyond Google Photos to support a wider ecosystem.

- **Implementation**: Integrate APIs for Dropbox, OneDrive, or iCloud (via Apple's Data Transfer
  Project).
- **Benefit**: Becomes a universal tool for cloud media management.

## 3. "Storage Saver" Optimization

Help users maximize their free cloud storage tiers.

- **Implementation**: Optional on-device compression or downscaling before upload.
- **Benefit**: Fits more memories into free accounts (e.g., Google's 15GB).

## 4. Automatic Organization (AI Metadata)

Turn a pile of photos into an organized library.

- **Implementation**:
    - Auto-create albums based on date/source structure.
    - On-device ML (ML Kit) for face/object tagging suggestions.
- **Benefit**: Higher quality of organization in the destination account.

## 5. "Incognito" Secure Vault

Leverage `androidx.security:security-crypto` for privacy.

- **Implementation**: Biometric-locked local vault for encrypted local storage of sensitive photos.
- **Benefit**: Privacy-focused alternative to cloud storage for sensitive media.

## 6. Transfer History & Analytics

Provide users with transparency and control over their data.

- **Implementation**: Dashboard showing GBs moved, success/failure logs, and "One-Tap Retry" for
  failed items.
- **Benefit**: Improved user confidence and troubleshooting capabilities.

## 7. Scheduled "Mirroring"

Keep accounts in sync automatically.

- **Implementation**: Use `WorkManager` for periodic background sync tasks.
- **Benefit**: Zero-touch maintenance of secondary backups.

## 8.