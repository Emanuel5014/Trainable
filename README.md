<div align="center">
  <img src="ICON/icon.png" alt="Trainable Logo" width="120">

  # Trainable
  <sub>Logo by br1_production</sub>
</div>

[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com/)
[![Language](https://img.shields.io/badge/Language-Kotlin-blue.svg)](https://kotlinlang.org/)
[![UI](https://img.shields.io/badge/UI-Jetpack%20Compose-orange.svg)](https://developer.android.com/jetpack/compose)
[![Design](https://img.shields.io/badge/Design-Material%203%20Expressive-purple.svg)](https://m3.material.io/)
[![Latest Release](https://img.shields.io/github/v/release/Emanuel5014/Trainable?logo=github)](https://github.com/Emanuel5014/Trainable/releases/latest)
[![Stars](https://img.shields.io/github/stars/Emanuel5014/Trainable?style=flat&logo=github&color=gold)](https://github.com/Emanuel5014/Trainable/stargazers)
[![Support](https://img.shields.io/badge/Support-Ko--fi-F16061?logo=ko-fi&logoColor=white)](https://ko-fi.com/emanuel5014)
[![Website](https://img.shields.io/badge/Website-trainableapp.vercel.app-8B5CF6?logo=vercel&logoColor=white)](https://trainableapp.vercel.app)

<div align="center">
  <br>
  <a href="https://github.com/Emanuel5014/Trainable/releases/latest/download/Trainable.apk">
    <img src="https://img.shields.io/badge/Download-Latest%20APK-00C853?style=for-the-badge&logo=android&logoColor=white" alt="Download APK" height="40">
  </a>
</div>


**Trainable** is a premium, native Android workout tracker designed for lifters. It features a monolithic design aligned with Google's **Material 3 Expressive** guidelines, with both **Dark** and **Light Mode** support, offering a seamless experience optimized for Pixel devices.

Available in **6 languages**: English, Italian, Portuguese, German, French, Spanish.

---

## 📱 Screenshots

<div align="center">
  <img src="SCREENS/dashboard.png" width="130" alt="Dashboard" />
  <img src="SCREENS/routines.png" width="130" alt="Routines" />
  <img src="SCREENS/routine_detail.png" width="130" alt="Routine Detail" />
  <img src="SCREENS/workout.png" width="130" alt="Workout Execution" />
  <img src="SCREENS/history.png" width="130" alt="Session History" />
  <img src="SCREENS/analytics.png" width="130" alt="Analytics" />
</div>


## ✨ Features

### 📊 Intelligent Dashboard
*   **Active Plan Hub**: Quick access to your current routine and next scheduled session.
*   **Smart Recommendations**: Assign routines to specific weekdays for intelligent "Today's Workout" suggestions.
*   **Resume Workout**: Smart hub to quickly resume interrupted sessions without data loss.
*   **Unfinished Session Detection**: Smart alerts when starting a workout for a plan that already has an in-progress session, with one-tap resume.
*   **Quick Start**: Start an empty workout directly from the dashboard without a plan.

### 📋 Advanced Routine Builder
*   **Exercise Library**: 130+ pre-populated exercises with categorized filters.
*   **Drag & Drop Reordering**: Fluid, haptic-powered reordering of exercises within routines.
*   **Custom Exercises**: Add and manage your own exercises with persistent categorization.
*   **Custom Categories**: Create, rename, and delete your own exercise categories.
*   **Editable Preset Exercises**: Toggle editing of built-in exercise names and categories.
*   **Superset Support**: Group exercises into supersets within routines.
*   **Auto-Adjust Sets**: Enter rep schemes like `10-8-6` and sets auto-update to match the number of segments.
*   **Plan Scheduling**: Set Start and Expire dates when creating or editing routines.
*   **Routine Sharing**: Export and import routines via `.trainableplan` file format.
*   **Routine Photos**: Capture multiple photos in-app and include them in exports.

### 🤖 On-Device AI Routine Scanner
*   **100% On-Device & Private**: Powered locally by Google's LiteRT-LM (Gemma 4 E2B / E4B) — zero cloud dependency, no API keys, and complete data privacy.
*   **Intelligent Sheet Extraction**: Capture or pick a picture of a paper gym sheet and automatically extract exercise names, sets, rep schemes, rest times, cardio durations, and suggested categories.
*   **Smart Catalog Matching**: Fuzzy-matches extracted exercises with 130+ built-in movements or user-defined custom exercises.
*   **Interactive Review & Inspector**: Collapsible photo inspector with high-resolution pinch-to-zoom, pan, fullscreen mode, and per-exercise editing before importing into the plan.
*   **Background Model Downloader**: Dedicated Foreground Service with WakeLock and real-time progress notifications with cancel control, allowing models to download uninterrupted even when switching apps or locking the phone.
*   **Live Resource Analytics**: Real-time telemetry dashboard monitoring CPU usage (with active core count), JVM/Native/System RAM allocation, token generation speed (`tok/s`), battery level, temperature (°C), and thermal throttling state during scanning.
*   **Hardware Compatibility Check**: Automatic device capability verification calibrated for devices with 6GB+ RAM and storage requirements.

### 💪 High-Performance Workout Execution
*   **Expressive UI**: Massive, touch-friendly inputs designed for use during heavy sets.
*   **Wheel Pickers**: Haptic-boosted weight and rep selection for precise control.
*   **Smart Hub**: Dynamic navigation between exercises with "Finish" and "Next" logic.
*   **Superset Flow**: Execute grouped superset exercises seamlessly.
*   **Cardio Exercise Support**: Log cardio sessions (Run, Bike, Walk, or custom) with distance and duration tracking, including cardio timer state persistence.
*   **Rest Timer**: Integrated countdown cards with enhanced notifications showing next-set details and planned reps.
*   **Custom Vibration**: Configurable continuous vibration duration (0–30s) when the rest timer finishes while the device is locked.
*   **Warmup Timer**: Configurable warmup timer in settings.
*   **Session Notes**: Persistence of notes for every single set for granular tracking.
*   **Swipe Navigation**: Swipe between exercises during workout execution.
*   **Exercise Note Pre-fill**: Auto-fill exercise notes from session history.
*   **On-the-fly Edits**: Add custom exercises mid-workout; tap plan title to view/edit plan details.
*   **Inline Exercise Modifications**: Add or remove exercises mid-workout with inline controls.
*   **Workout Timer**: Configurable workout duration timer with enable/disable toggle in Settings.


### 📈 Deep Analytics
*   **Vico Charts**: Professional-grade visualizations for total volume and strength trends.
*   **1RM Tracking**: Algorithmic strength assessment using the Epley formula, with optional 1RM cards.
*   **Volume by Muscle Group**: Track volume distributed across muscle groups.
*   **Session Comparison**: Compare workouts across time periods or specific sessions.
*   **Workout Calendar**: Visual calendar widget for workout frequency.
*   **Consistency Tracking**: Visual heatmaps and progress cards.
*   **Strength Index**: Algorithmic assessment of your structural balance and performance.
*   **Body Weight**: Integrated weight logging and history tracking.
*   **Body Weight History**: Dedicated module with date picker, delete functionality, collapsible UI, and configurable time range (1w/1m/6m/All).
*   **Cardio Tracking**: Cardio counter in weekly goal overview.
*   **Plans Reports**: Generate detailed HTML reports for workout plans with per-exercise history, max weight/volume, estimated 1RM, and swap tracking. Preview, save, or share reports.

### 🌍 Localization
*   **6 Languages**: English, Italian, Portuguese, German, French, Spanish.
*   **Dynamic Switching**: Change language directly from Settings.

### 🏟️ Gym Membership
*   **Expiration Tracking**: Monitor your gym subscription end date.
*   **Smart Alerts**: Expiration notifications with "Renew" quick action buttons.

### 🌐 Local WebServer
*   **On-Device Dashboard**: Start a local HTTP server from Settings to view your training data (analytics, plans, session history) in any browser on the same Wi-Fi network.
*   **REST API**: Built-in Ktor-powered API serving real-time data from the local Room database.
*   **Foreground Service**: Persistent notification with server status, URL display, and one-tap stop.
*   **Responsive Web UI**: Full-featured web pages with dark/light theme, i18n, and mobile-friendly layout.

### 📸 Social Sharing
*   **Workout Share Cards**: Generate and share full-length workout summary images.
*   **Live Preview**: Preview your share card before sharing.
*   **Muscle Group Details**: Muscle group breakdown included in share cards.
*   **Session Notes**: Workout notes included in shared summaries.
*   **Planned Set/Rep Scheme**: Planned set and rep structure included in share cards.

### 📱 Widgets
*   **Quick Start/Resume Widget**: Home Screen and Lock Screen widget for instant workout start or resume.
*   **Weekly Goal Widget**: Track weekly workout goal progress directly from your home screen.

### 🔒 Physical Checks
*   **Hidden Entry Point**: Tap the Trainable logo 3 times from the Dashboard to access Physical Checks.
*   **Body Progression Tracking**: Log body measurements with weight, notes, and timestamp.
*   **Photo Gallery**: Capture or import multiple photos per check with fullscreen pinch-to-zoom viewer.
*   **Before/After Comparison**: Interactive slider overlay to compare any two check entries.
*   **Biometric Lock**: Fingerprint or device PIN gate for quick access.
*   **Password Encryption**: Optional AES-256-GCM password-based encryption for all check photos.
*   **Forgot Password Reset**: Secure nuclear reset that deletes all check data if password is lost.
*   **Auto-Lock**: Session auto-locks after inactivity or when app goes to background.
*   **Unit Conversion**: Automatic kg/lb conversion for weight entries.
*   **Swipe & Long-Press Actions**: Quick edit, delete, and manage checks via gestures.
*   **Photo Editor**: Built-in crop, rotate, zoom, and pan tools with aspect ratio presets (Free, 1:1, 4:3, 16:9) before saving photos.

### ⚙️ Premium UX & Tools
*   **Dynamic Color**: Full support for Android 12+ wallpaper-based color extraction.
*   **Light Mode & Custom Themes**: Switch between Dark/Light mode, customize themes, and choose from an extensive color palette in Settings.
*   **Google Sans Font**: Clean, modern typography throughout the app.
*   **Horizontal Pager Navigation**: Fluid swipe-based transitions between main tabs.
*   **Backup & Restore**: Secure ZIP-based local backup system with photo compression and expanded preference sync (Theme, Units, Language).
*   **Auto-Backup**: Reliable WorkManager-based automated background backups with configurable frequency, max count, and image inclusion.
*   **Tactile Feedback**: Enhanced haptic responses for all critical UI interactions.
*   **Interaction Choice**: Configure Swipe or Long-press interaction style in Settings.
*   **Auto-Updates**: Built-in automatic update support for future releases.
*   **Unit System**: Choose between Metric (kg) and Imperial (lbs).

---

## 🏗️ Technical Architecture

Trainable follows **Clean Architecture** principles combined with **MVVM** for a robust and testable codebase:

*   **UI Layer**: 100% Jetpack Compose using the **Monolithic Design System** (no XML).
*   **Presentation Layer**: Hilt-injected ViewModels managing `StateFlow` for reactive UI updates.
*   **On-Device AI Engine**: Google LiteRT-LM runtime executing local quantized Gemma models (E2B / E4B) directly on-device with GPU/CPU acceleration.
*   **Data Layer**: 
    *   **Room Database**: Single Source of Truth (SSoT).
    *   **DataStore**: For lightweight user preferences (Haptic settings, theme, AI configuration).
    *   **WorkManager & Foreground Services**: Automated background backups, persistent web server, and resilient background model downloading with WakeLock.
*   **Navigation**: Type-safe navigation with `@Serializable` routes.


---

## 📁 Project Structure

```
com/emanuel5014/trainable/
├── data/
│   ├── ai/                     # On-device AI (LocalLlmEngine, RoutineScanner, ModelFileManager, ModelDownloadService & Manager, AiResourceTracker, DeviceCapabilityChecker, RoutineScanParser, AiModels, ExerciseMatcher)
│   ├── ExerciseTranslations.kt
│   ├── local/
│   │   ├── ExerciseData.kt
│   │   ├── GymDatabase.kt
│   │   ├── dao/                # Room DAOs (WorkoutDao, ExerciseDao, AnalyticsDao, UserDao, PhysicalCheckDao, WeightLogDao)
│   │   ├── entity/             # Room entities (SetLogEntity, CardioLogEntity, CustomCategoryEntity, WorkoutPlanEntity, WorkoutSessionEntity, ExerciseEntity, PlanExerciseEntity, SessionExerciseSwapEntity, PhysicalCheckEntity, WeightLogEntity, UserEntity, WorkoutPlanImageEntity)
│   │   └── relation/           # Relation classes (PlanWithDetails, SessionWithSets, SessionWithDetails, SessionWithPlanName)
│   ├── remote/
│   │   ├── GitHubModels.kt
│   │   └── dto/                # DTOs for export/import (WorkoutPlanExportDto)
│   ├── report/                 # HTML plan reports (ReportGenerator, HtmlReportFormatter, ReportModels)
│   └── repository/             # Repositories (WorkoutRepository, AnalyticsRepository, ExerciseRepository, UserPreferencesRepository, UserRepository, PhysicalCheckRepository)
├── di/                         # Hilt modules (DatabaseModule, NetworkModule)
├── ui/
│   ├── components/
│   │   ├── analytics/          # Charts, stat cards, consistency, body comp, personal bests (AnalyticsCharts, StatCard, ConsistencyCard, BodyCompositionCard, PersonalBestsSection, StrengthIndexCard)
│   │   ├── base/               # Design system primitives (GymCard, GymButton, GymIconButton, GymInputField, GymLoadingIndicator)
│   │   ├── dialogs/            # Reusable dialogs (UpdateDialog, ImportConfirmationDialog)
│   │   ├── exercise/           # Exercise cards, cardio input, picker, rest timer, set log input (ExerciseEntryCard, ExerciseGroupHeader, CardioInputForm, ExercisePickerBottomSheet, RestTimerSection, WeightRepsInput, SetLogRow, ExerciseNavigation, SwapExerciseBottomSheet)
│   │   ├── navbar/             # Bottom navigation bar and items (BottomNavBar, BottomNavItems, BottomBarManager, BottomNavBar_flo)
│   │   └── shared/             # Shared utilities (EmptyState, ImageEditor, ScreenHeader, SwipeableCard, WorkoutShareCard, RoutineImagePicker)
│   ├── navigation/             # NavGraph, Routes
│   ├── screens/
│   │   ├── analytics/          # Analytics dashboard, drag-drop state, models (AnalyticsScreen, AnalyticsViewModel, AnalyticsModels, DragDropState)
│   │   ├── compare/            # Session comparison (CompareSessionsScreen, CompareSessionsViewModel)
│   │   ├── dashboard/          # Main dashboard (DashboardScreen, DashboardViewModel)
│   │   ├── history/            # Session history, edit, filter bottom sheet (HistoryScreen, HistoryViewModel, EditWorkoutScreen, EditWorkoutViewModel, EditWorkoutDragDropState, HistoryFilterBottomSheet)
│   │   ├── onboarding/         # Onboarding flow (OnboardingScreen, OnboardingViewModel)
│   │   ├── physicalcheck/      # Physical checks (body progression, before/after slider, settings) (PhysicalCheckScreen, PhysicalCheckViewModel, PhysicalCheckCompareScreen, PhysicalCheckSettingsScreen, BeforeAfterSlider)
│   │   ├── routines/           # Routine list, detail, builder, report, AI scan preview sheet (RoutineListScreen, RoutineDetailScreen, RoutineDetailViewModel, RoutinesViewModel, ReportScreen, ReportViewModel, AiScanPreviewSheet)
│   │   ├── settings/           # App settings, workout settings, exercise customization, AI model manager (SettingsScreen, SettingsViewModel, WorkoutSettingsScreen, ExerciseCustomizationScreen, DonorsScreen)
│   │   └── workout/            # Workout execution (WorkoutExecutionScreen, WorkoutViewModel)
│   │   └── MainPagerScreen.kt
│   ├── theme/                  # Color, Shape, Type, Spacing, Theme, ResponsiveSize, ThemeColorStore
│   └── util/                   # DateFormatter
├── util/
│   ├── AppLocaleManager.kt
│   ├── BiometricHelper.kt
│   ├── ImageStorageUtils.kt
│   ├── PhysicalCheckCryptoManager.kt
│   ├── ShareUtils.kt
│   ├── UpdateManager.kt
│   ├── UriMigrationHelper.kt
│   ├── WeightUnitConverter.kt
│   ├── backup/                 # AutoBackupWorker, BackupManager
│   └── notification/           # TimerNotificationHelper, TimerNotificationReceiver, GymMembershipWorker
├── webserver/                  # Local HTTP server (Ktor), foreground service, server manager (LocalWebServer, LocalWebServerService, WebServerManager)
├── widget/                     # Glance widgets (TrainableWidget, TrainableWidgetReceiver, WeeklyGoalWidget, WeeklyGoalWidgetReceiver)
├── MainActivity.kt
└── GymTrackingApp.kt
```


## 🚀 Getting Started

### Prerequisites
*   Android Studio Ladybug or newer.
*   Android SDK 28 or higher.
*   A device/emulator running Android 12+ (for Dynamic Color support).

### Setup
1.  Clone the repository:
    ```bash
    git clone https://github.com/Emanuel5014/Trainable.git
    ```
2.  Open the project in Android Studio.
3.  Build and Run on your device.

---

## 🛠️ Tech Stack
*   **Language**: Kotlin (2.3.x)
*   **UI**: Jetpack Compose (2026.x) with Material3 (1.5.x)
*   **On-Device AI**: Google LiteRT-LM (Gemma 4 E2B / E4B)
*   **Dependency Injection**: Dagger Hilt (2.59.x)
*   **Database**: Room (2.8.x) with KSP
*   **Charts**: Vico (2.1.x)
*   **Serialization**: Kotlinx Serialization (1.11.x)
*   **Navigation**: Navigation Compose (2.9.x)
*   **Concurrency**: Kotlin Coroutines & Flow
*   **Preferences**: DataStore Preferences (1.2.x)
*   **Background Tasks**: WorkManager (2.11.x) & Foreground Services
*   **Image Loading**: Coil (2.7.x)
*   **Web Server**: Ktor (3.x)
*   **Build**: Gradle (9.4.x) + AGP (9.2.x)

---

## ❤️ Support the project

If you find this project useful, please consider giving it a ⭐ on GitHub or supporting development via Ko-fi!

<div align="center">
  <a href="https://ko-fi.com/emanuel5014" target="_blank">
    <img src="https://ko-fi.com/img/githubbutton_sm.svg" alt="Buy Me a Coffee" height="40">
  </a>
</div>

---

*Developed with ❤️ by Emanuel5014.*
