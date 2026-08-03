# Batch-Based Incremental Transfer Plan

Implement a feature to allow users to select batches of photos for transfer and track successfully processed files to prevent redundant work.

## Proposed Changes

### [Core] Database & Tracking
We need a local database to track which files have been successfully transferred to which destination account.

#### [NEW] [TransferDatabase.kt](file:///Users/IPRIYANATH/.gemini/antigravity-ide/scratch/photo-sync-app/app/src/main/java/com/photomigrate/app/data/db/TransferDatabase.kt)
- Define a Room database with a `TransferredFile` entity.
- `TransferredFile` will store `mediaId`, `destinationAccountId`, `sha256Hash`, and `timestamp`.

#### [MODIFY] [TransferRepository.kt](file:///Users/IPRIYANATH/.gemini/antigravity-ide/scratch/photo-sync-app/app/src/main/java/com/photomigrate/app/data/repository/TransferRepository.kt)
- Integrate `TransferDatabase`.
- Update `loadSourceMedia` to filter out files already present in the database for the selected destination account.
- Update `processNextMediaItem` to record successful transfers in the database.

### [UI] Media Picker Enhancements
Add batch selection controls to the picker screen.

#### [MODIFY] [MediaPickerScreen.kt](file:///Users/IPRIYANATH/.gemini/antigravity-ide/scratch/photo-sync-app/app/src/main/java/com/photomigrate/app/ui/screens/MediaPickerScreen.kt)
- Add a "Batch Size" selector (100, 200, 500, 1000).
- Add a "Select Next Batch" button that automatically selects the first N available items.
- Ensure the UI reflects the filtered list (only untransferred items).

### [Build] Dependencies
#### [MODIFY] [app/build.gradle.kts](file:///Users/IPRIYANATH/.gemini/antigravity-ide/scratch/photo-sync-app/app/build.gradle.kts)
- Add Room dependencies and KSP plugin.

## Verification Plan

### Automated Tests
- Build the project to ensure dependencies are correctly resolved.
- Manual verification of filtering logic by performing a small transfer and checking if those items disappear from the list on reload.

### Manual Verification
1. Load a large library.
2. Select a batch size of 100.
3. Click "Select Next Batch".
4. Start transfer.
5. After completion, return to the picker and verify the 100 transferred items are no longer shown.
