# Android Expense Tracker Demo

A compact Android expense-tracking application built as a portfolio project. It presents recent movements, monthly and yearly summaries, category breakdowns, and simple forms for adding movements and categories.

The interface is currently in Spanish and formats amounts as euros.

## Highlights

- Dashboard with the current month's income, expenses, and balance
- Recent income and expense movements
- Monthly and yearly financial summaries
- Expense totals grouped by category
- Forms for adding movements and custom categories
- Local persistence with a preloaded demonstration dataset

## Technology and architecture

- Kotlin and Coroutines
- Jetpack Compose with Material 3
- Android Architecture Components (`ViewModel` and lifecycle-aware state collection)
- Room for local persistence, with KSP schema generation
- Repository-based data access and reactive `Flow` state
- Gradle Kotlin DSL

The app uses a single Android application module. `MainViewModel` combines repository flows into immutable UI state, while Room entities and DAOs provide the local data layer.

## Privacy and demo data

This public edition contains only a small, wholly fictional sample dataset created for demonstration purposes. Account names, categories, amounts, dates, notes, and import identifiers are synthetic and do not describe a real person or real transactions. Any screenshots published for this project should likewise use only fictional data.

The Room schema under `app/schemas` documents the database structure and contains no transaction records.

## Build and run

Requirements:

- Android Studio with JDK 17
- Android SDK 35
- An emulator or device running Android 8.0 (API 26) or newer

Clone the repository, open its root directory in Android Studio, allow Gradle sync to finish, and run the `app` configuration. From a terminal on Windows, a debug build can also be created with:

```powershell
.\gradlew.bat assembleDebug
```

On macOS or Linux:

```bash
./gradlew assembleDebug
```

## Screenshots

Screenshots will be added here. All screenshots for this public demo will contain fictional sample data only.

## Project status

This is a focused portfolio demo rather than a production financial product. Data is stored locally on the device, and the project does not provide cloud synchronization or financial-institution integrations.
