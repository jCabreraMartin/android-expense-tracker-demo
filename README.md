# Android Expense Tracker Demo

Android portfolio application built with Kotlin, Jetpack Compose, Material 3 and Room. The interface is in Spanish and amounts are formatted in euros.

## Features

- Create, edit and delete income and expense movements.
- Numeric amount input, calendar date selection and optional notes.
- Block/category selection and category management (create, edit, archive).
- Recent movements, monthly filtering and expandable month groups.
- Monthly and annual summaries, annual balances for Casa, Piso and Otros, and expense breakdowns with transaction details.
- Export and restore JSON backups using Android's file picker.
- Optional automatic Dropbox backups using OAuth with PKCE.

## Architecture

Single-module Android application with lifecycle-aware Compose state, a ViewModel, repository, Room DAOs and coroutine flows. Cloud operations sit behind `CloudSyncProvider`; `SyncManager` coordinates backups independently of the UI.

## Privacy and fictional data

All bundled accounts and movements are synthetic demonstration data. No personal financial records, account credentials, private repository history or developer-specific Dropbox key are included. Application ID `com.josem.expensetrackerdemo` allows a separate installation with its own local database.

Sample transactions use fixed dates in August 2026. The dashboard shows the current calendar month, so it may initially show zero; select August to explore the sample movements. Annual summaries show the current calendar year.

## Build

Use Android Studio with JDK 21, Android SDK 35 and Android 8.0/API 26 or newer. Java/Kotlin bytecode targets Java 17.

```powershell
.\gradlew.bat assembleDebug lintDebug
```

```bash
./gradlew assembleDebug lintDebug
```

## Optional Dropbox configuration

The app works locally without Dropbox. Create your own Dropbox API app with Scoped access and App folder access; enable `files.content.read` and `files.content.write`. Set its public App key as the `DROPBOX_APP_KEY` Gradle property in your user-level `~/.gradle/gradle.properties`, or build with `-PDROPBOX_APP_KEY=YOUR_APP_KEY`. Never commit an App secret or access/refresh token.

Connect from Settings and authorize your own Dropbox account. Pending local changes are uploaded on manual synchronization or when the app goes to the background. Opening or synchronizing without pending local changes restores the remote snapshot. The initial connection uploads the local snapshot.

This is a single-device snapshot backup strategy. A remote restore replaces the local dataset; pending local changes take precedence on upload. Android can stop background work, so use manual synchronization for confirmation. JSON backups contain unencrypted data: choose a trusted destination. Manual export/import works without Dropbox configuration.

## Validation

Build checks: `assembleDebug` and `lintDebug`. No automated unit or device test suite is bundled. OAuth requires your own Dropbox app and account; the default build disables Connect until configured.
