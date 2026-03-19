# SavageStats 🛡️

[![Latest Release](https://img.shields.io/github/v/release/Mejji/SavageStats?label=Download%20APK&style=for-the-badge&color=red)](https://github.com/Mejji/SavageStats/releases/latest)
![Platform](https://img.shields.io/badge/Platform-Android-green?style=for-the-badge&logo=android)
![Language](https://img.shields.io/badge/Language-Kotlin-purple?style=for-the-badge&logo=kotlin)
![AI](https://img.shields.io/badge/AI-On--Device%20Gemma%203-blue?style=for-the-badge&logo=google)
![License](https://img.shields.io/badge/License-MIT-lightgrey?style=for-the-badge)

> **No cloud. No subscriptions. No mercy.**
> SavageStats is a fully offline AI fitness coach that lives entirely on your phone — tracks your macros, reads your camera, and roasts your lack of discipline with the unfiltered fury of a 528MB language model.

![SavageStats Demo](https://github.com/user-attachments/assets/b681016c-087a-4533-b124-ab4f42739754)

---

## Table of Contents

- [What Is This?](#-what-is-this)
- [System Architecture](#-system-architecture)
- [The Hybrid AI Engine](#-the-hybrid-ai-engine--gemma-3--gemini-nano)
- [The RAG Pipeline](#-the-rag-pipeline--offline-nutrition-intelligence)
- [Camera Food Scanner](#-camera-food-scanner)
- [Key Features](#-key-features)
- [Tech Stack](#️-tech-stack)
- [Project Structure](#-project-structure)
- [Installation](#-installation-latest-apk)
- [Developer Setup](#-developer-setup-build-from-source)
- [Requirements](#-requirements)

---

## 🔥 What Is This?

Most fitness apps are glorified spreadsheets with a cloud subscription stapled to them. SavageStats is different.

It is an **offline-first, AI-powered fitness tracker** built for Android that:
- Runs a **528MB quantized LLM** entirely on-device via Google MediaPipe
- Scans food through your **camera** using a custom TFLite model
- Looks up verified macros from a **411MB USDA nutrition database** using FTS4 full-text search — no internet required
- Delivers **savage, personalized coaching** that factors in your actual biometrics, goals, and logged activity
- Syncs seamlessly with **Android Health Connect** (Samsung Health, Strava, Fitbit, etc.)
- Earns you a **Savage Rank** (from "Uncooked Noodle" to "Savage God") based on real XP progress

Zero telemetry. Zero accounts. Zero cloud. Your data stays on your device — period.

---

## 🏗️ System Architecture

SavageStats is built around a clean **MVVM (Model-View-ViewModel)** architecture with three distinct subsystems that feed into each other:

```
┌─────────────────────────────────────────────────────────────┐
│                        UI Layer                             │
│   Jetpack Compose  ·  Material 3  ·  WindowSizeClass        │
│   Dashboard · Coach · Camera · Missions · Profile · Setup   │
└───────────────────┬─────────────────────────────────────────┘
                    │  StateFlow / collectAsStateWithLifecycle
┌───────────────────▼─────────────────────────────────────────┐
│                   ViewModel Layer                           │
│  DashboardViewModel · CoachViewModel · SetupViewModel        │
│  MissionsViewModel · ProfileViewModel                        │
│  (Mifflin-St Jeor TDEE · Macro Targets · Savage Rank)       │
└──────┬────────────────────┬───────────────┬─────────────────┘
       │                    │               │
┌──────▼──────┐  ┌──────────▼──────┐  ┌────▼────────────────┐
│  AI Engine  │  │  Data Layer     │  │  Health Connect     │
│  Subsystem  │  │  Subsystem      │  │  Subsystem          │
└─────────────┘  └─────────────────┘  └─────────────────────┘
```

### AI Engine Subsystem (`ai/`)
| Component | Role |
|---|---|
| `AIEngineRouter` | Hardware detection — routes to Gemini Nano (flagship) or local Gemma 3 |
| `LlmInferenceManager` | MediaPipe lifecycle management, streaming inference, model load states |
| `ModelDownloadService` | Foreground service — streams 528MB model from HuggingFace with progress broadcast |
| `FoodScanner` | Custom TFLite image labeler — classifies food from camera bitmap |
| `NutritionCalculator` | Mifflin-St Jeor BMR/TDEE engine + macro split calculator |

### Data Layer Subsystem (`data/`)
| Component | Role |
|---|---|
| `SavageDatabase` | Room DB — user logs, missions, custom foods (v7) |
| `NutritionDatabase` | Separate read-only Room DB — 411MB USDA food data loaded from asset |
| `FoodDao` | FTS4 full-text search queries against `food_items_fts` |
| `UserProfileManager` | Preferences DataStore — biometrics, goals, XP persistence |
| `LogRepository` | Repository pattern — single access point for logs + missions |
| `HealthConnectManager` | Android Health Connect — pulls steps, sleep, calories, activity |

### UI Layer (`ui/`)
| Screen | Purpose |
|---|---|
| `OnboardingScreen` | First-run profile setup (name, age, weight, height, goal) |
| `SetupScreen` | AI model download with live progress UI |
| `DashboardScreen` | Weekly calendar, macro rings, daily log entry |
| `CoachScreen` | Savage AI chat interface with streaming responses |
| `CameraScreen` | Live camera feed → food scan → macro lookup → log |
| `MissionsScreen` | Active/completed challenges with XP rewards |
| `ProfileScreen` | Biometrics editor, Savage Rank display, XP progress bar |

---

## 🤖 The Hybrid AI Engine — Gemma 3 + Gemini Nano

SavageStats uses a **tiered hybrid inference architecture** that automatically selects the best available AI engine for the device it's running on.

### How It Works

```
App Launch
    │
    ▼
AIEngineRouter.checkHardwareSupport()
    │
    ├─── Pixel 8/9 or Galaxy S24/S25? ──► [NATIVE_NANO path]
    │         │                            (Gemini Nano via Android AICore)
    │         │                            Currently: falls back to Gemma 3
    │         │                            Future: ML Kit GenAI Prompt API
    │
    └─── All other devices ──────────────► [LOCAL_GEMMA_FALLBACK path]
                                            Gemma 3 1B INT4 via MediaPipe
```

### Engine 1: Local Gemma 3 (Active — All Devices)

The primary engine. A **528MB Gemma 3 1B INT4-quantized** model runs entirely in-process via the **Google MediaPipe LLM Inference API**.

- Model file: `gemma3-1b-it-int4.task` (stored in app internal storage)
- Inference: fully on-device, no network calls during generation
- Token limit: 1024 tokens per response
- Thread safety: `LlmInferenceManager` uses `SupervisorJob + Dispatchers.Default` — model load and inference are fully coroutine-safe
- Load states: `Uninitialized → Loading → Ready | NeedsDownload | Error`

**Model Download Flow:**
```
SetupScreen (user taps "Download")
    │
    ▼
ModelDownloadService (Foreground Service)
    │   OkHttp streaming download — 64KB buffer
    │   Live progress via LocalBroadcastManager
    │   Atomic write: .part file → rename to final
    ▼
filesDir/gemma3-1b-it-int4.task (528MB)
    │
    ▼
LlmInferenceManager.initialize()
    │   Validates file size ≥ 300MB
    ▼
LlmInference (MediaPipe) — READY
```

### Engine 2: Gemini Nano (Future — Flagship Devices)

The `NATIVE_NANO` path is **architecture-ready** for when Google's ML Kit GenAI Prompt API reaches general availability. On supported devices (Pixel 8/9, Galaxy S24/S25), the router will bypass the downloaded model entirely and use the system-level **Gemini Nano** already installed on the device — zero storage, zero download, faster inference.

```kotlin
// Future activation (AIEngineRouter.kt):
// val generativeModel = Generation.getClient()
// if (generativeModel.checkStatus() == FeatureStatus.AVAILABLE) {
//     return EngineType.NATIVE_NANO
// }
```

### The Savage Prompt

The coaching prompt is context-assembled at runtime from live user data:

```
You are SavageStats — a brutally honest, no-excuses fitness coach.
User: [Name], [Age]yo, [Weight]kg, Goal: [Goal]
Today: [Protein]g protein | [Calories] kcal | [Steps] steps | [Sleep]h sleep
Target: [TargetProtein]g protein | [TargetCalories] kcal/day
Rules: 3 sentences max. Be savage. No fluff. No emojis.
```

---

## 🗄️ The RAG Pipeline — Offline Nutrition Intelligence

SavageStats implements a **Retrieval-Augmented Generation (RAG)** pipeline that grounds the AI's nutrition knowledge in verified USDA data — entirely offline.

### Architecture

```
Camera Scan / Manual Search
         │
         ▼
   FoodScanner (TFLite)
   Returns top-5 label cluster
   e.g. "Hamburger, Cheeseburger, Sandwich"
         │
         ▼
   NutritionDatabase (Room + FTS4)
   ┌─────────────────────────────────┐
   │  savage_nutrition.db (411MB)    │
   │  ┌──────────────┐  ┌─────────┐ │
   │  │  food_items  │  │ fts4    │ │
   │  │  (USDA data) │◄─│ index   │ │
   │  └──────────────┘  └─────────┘ │
   └─────────────────────────────────┘
         │
         ▼
   FoodDao.searchFoods(ftsQuery)
   FTS4 MATCH query — top 15 results
         │
         ▼
   Retrieved: FoodItem { calories, protein,
              carbs, fat, fiber, sodium }
         │
         ▼
   Injected into LLM prompt context
   OR logged directly to DailyLog
```

### The Database

The `savage_nutrition.db` asset is a **411MB pre-packaged SQLite database** built from USDA FoodData Central. It contains:

- **`food_items`** table: `id, name, calories, protein, carbs, fat, fiber, sodium_mg`
- **`food_items_fts`** table: FTS4 virtual table with content entity pointing to `food_items`

The FTS4 index enables tokenized prefix-match search:
```sql
-- Query built as: "fried* chicken*"
SELECT food_items.* FROM food_items
JOIN food_items_fts ON food_items.id = food_items_fts.rowid
WHERE food_items_fts MATCH 'fried* chicken*'
LIMIT 15
```

The database is loaded via Room's `createFromAsset()` — first open copies from APK assets to app storage, then FTS index is rebuilt automatically via `DatabaseCallback.onCreate()`.

### Why Two Databases?

SavageStats deliberately separates user data from nutrition data:

| | `SavageDatabase` | `NutritionDatabase` |
|---|---|---|
| Contents | DailyLogs, Missions, CustomFoods | USDA food items + FTS index |
| Mutability | Read/Write | Read-only (asset) |
| Migration | Destructive allowed (user can re-log) | Destructive allowed (re-copy from asset) |
| Version | v7 | v3 |

This isolation means a schema migration never wipes your workout logs.

---

## 📸 Camera Food Scanner

The camera pipeline combines a custom **TFLite computer vision model** with the RAG nutrition lookup to identify and log food macros from a photo.

### Pipeline

```
CameraScreen (CameraX preview)
       │
       ▼ (user taps capture)
Bitmap captured from ImageCapture UseCase
       │
       ▼
FoodScanner.scanImage(bitmap)           ← runs on background thread
  ML Kit ImageLabeling
  Custom model: ml/food-v1.tflite
  2,000 food classes
  Confidence threshold: 15%             ← tuned for 2k-class probability spread
  Max results: 5
       │
       ▼
Label cluster: "Hamburger, Cheeseburger, Sandwich, ..."
Top label → FTS4 query token
       │
       ▼
NutritionDatabase.searchFoods("hamburger*")
       │
       ▼
FoodItem { calories: 295, protein: 17g, carbs: 24g, fat: 14g, ... }
       │
       ▼
DashboardViewModel.logFood(foodItem)
       │
       ▼
DailyLog updated in SavageDatabase ✓
```

### The TFLite Model

- **File:** `app/src/main/assets/ml/food-v1.tflite`
- **Classes:** ~2,000 food categories
- **Confidence threshold:** 15% (deliberately low — probability mass is spread across 2,000 classes, so a 15% match is actually high signal)
- **Debug output:** "Neural X-Ray" Logcat dumps the model's raw confidence scores for every detection, allowing easy calibration

---

## ✨ Key Features

| Feature | Description |
|---|---|
| 🔒 **100% Private** | All inference, all data, entirely on-device. Nothing leaves your phone. |
| 🤖 **Savage Coach** | Gemma 3 LLM synthesizes your protein, sleep, steps, and calories into a brutally honest 3-sentence review |
| 📸 **Camera Scanner** | Point at food → TFLite classifies → USDA macros auto-populated → one tap to log |
| 🗄️ **Offline RAG** | 411MB USDA nutrition database with FTS4 search — zero internet needed for food lookup |
| 🧮 **TDEE Calculator** | Mifflin-St Jeor BMR engine computes personalized daily calorie and macro targets |
| 🏅 **Savage Rank** | XP-based progression system: Uncooked Noodle → Couch Predator → Iron Novice → Local Threat → Savage God |
| 📅 **Weekly Dashboard** | Horizontal 7-day calendar with macro rings and daily log tracking |
| ⚡ **Health Connect** | One-tap sync with Samsung Health, Strava, Fitbit, and any HC-compatible app |
| 🎯 **Missions** | Time-gated challenges with XP rewards and failure tracking |
| 🧬 **Hybrid AI Router** | Architecture-ready for Gemini Nano on flagship devices (Pixel 8/9, Galaxy S24/S25) |

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| **Language** | Kotlin |
| **UI** | Jetpack Compose + Material 3 + WindowSizeClass |
| **Architecture** | MVVM + Repository pattern |
| **User DB** | Room (SQLite) — DailyLog, Missions, CustomFood |
| **Nutrition DB** | Room + pre-packaged SQLite asset (411MB USDA data) |
| **Search** | FTS4 full-text search (SQLite virtual table) |
| **AI Inference** | Google MediaPipe LLM Inference API |
| **LLM Model** | Gemma 3 1B INT4 quantized (528MB, `.task` format) |
| **Vision Model** | Custom TFLite food classifier (2,000 classes) |
| **Vision SDK** | ML Kit Custom Image Labeling |
| **Camera** | CameraX |
| **Profile Storage** | Preferences DataStore |
| **Health Data** | Android Health Connect API |
| **Download** | OkHttp streaming (64KB buffer, foreground service) |
| **Concurrency** | Kotlin Coroutines + StateFlow |

---

## 📂 Project Structure

```
app/src/main/
├── assets/
│   ├── database/
│   │   └── savage_nutrition.db     # 411MB USDA nutrition database (Git LFS)
│   └── ml/
│       └── food-v1.tflite          # Custom food classification model (2k classes)
│
└── java/com/savagestats/app/
    ├── MainActivity.kt             # Entry point, dependency wiring, nav host
    │
    ├── ai/
    │   ├── AIEngineRouter.kt       # Hardware detection, Nano vs Gemma routing
    │   ├── LlmInferenceManager.kt  # MediaPipe lifecycle, load states, inference
    │   ├── ModelDownloadService.kt # Foreground download service (OkHttp streaming)
    │   ├── FoodScanner.kt          # TFLite food classification (ML Kit)
    │   └── NutritionCalculator.kt  # Mifflin-St Jeor BMR/TDEE + macro splits
    │
    ├── data/
    │   ├── SavageDatabase.kt       # Room DB: logs, missions, custom foods
    │   ├── DailyLog.kt / Dao       # Daily macro + activity log entity
    │   ├── Mission.kt / Dao        # Challenge system entity
    │   ├── CustomFoodItem.kt / Dao # User-defined food entries
    │   ├── LogRepository.kt        # Repository — single data access point
    │   ├── UserProfileManager.kt   # DataStore: biometrics, goals, XP, rank
    │   ├── HealthConnectManager.kt # Health Connect sync (steps, sleep, calories)
    │   ├── Converters.kt           # Room TypeConverters
    │   └── nutrition/
    │       ├── NutritionDatabase.kt # Read-only USDA Room DB (createFromAsset)
    │       ├── FoodItem.kt          # Food entity (calories, protein, carbs, fat…)
    │       ├── FoodItemFts.kt       # FTS4 virtual table entity
    │       └── FoodDao.kt           # FTS4 search queries
    │
    └── ui/
        ├── MainScreen.kt           # Bottom nav shell + screen routing
        ├── OnboardingScreen.kt     # First-run profile setup
        ├── SetupScreen.kt          # AI model download with progress UI
        ├── DashboardScreen.kt      # Weekly calendar, macro rings, daily log
        ├── DashboardViewModel.kt   # Log aggregation, TDEE targets, macro state
        ├── CoachScreen.kt          # Savage AI chat interface
        ├── CoachViewModel.kt       # Prompt assembly, LLM call, response streaming
        ├── CameraScreen.kt         # CameraX + food scan trigger
        ├── MissionsScreen.kt       # Active/completed challenges
        ├── MissionsViewModel.kt    # Mission lifecycle, XP award logic
        ├── ProfileScreen.kt        # Biometrics editor, rank display
        ├── ProfileViewModel.kt     # Profile state, XP progress bar
        └── theme/
            ├── Color.kt / Theme.kt / Type.kt
            └── WindowSize.kt       # Responsive layout breakpoints
```

---

## 📦 Installation (Latest APK)

No build required. Just sideload:

1. Go to the [**Releases**](https://github.com/Mejji/SavageStats/releases) page
2. Download the latest `SavageStats_vX.X.apk`
3. Enable **Install from Unknown Sources** on your device
4. Install and open — the Setup Screen will guide you through the AI model download

> **First Launch:** The app will prompt you to download the ~528MB Gemma 3 model over Wi-Fi. This is a one-time download that streams directly to internal storage.

---

## 🚀 Developer Setup (Build from Source)

### Prerequisites

- **Android Studio** Ladybug 2024.2.1 or newer
- **Android SDK** API 35+
- Device or emulator with **≥ 4GB RAM** (for LLM inference)
- **ADB** (for manual model push)

### 1. Clone & Open

```bash
git clone https://github.com/Mejji/SavageStats.git
cd SavageStats
```

Open in Android Studio. The `savage_nutrition.db` is tracked via **Git LFS** — ensure `git lfs pull` runs after clone.

### 2. Get the AI Model

The **528MB Gemma 3 1B INT4** model is not bundled in the APK. Two ways to get it:

**Option A — In-App Download (Recommended)**
Build and run the app. On first launch the Setup Screen will stream the model directly from HuggingFace into internal storage. Requires ~600MB free space.

**Option B — Manual ADB Push**
```bash
# Download from HuggingFace first, then:
adb push gemma3-1b-it-int4.task \
  /data/data/com.savagestats.app/files/gemma3-1b-it-int4.task
```

Model source: [Mejji16/SavageStats-Brain-gemma3b](https://huggingface.co/Mejji16/SavageStats-Brain-gemma3b/resolve/main/gemma3-1b-it-int4.task)

### 3. Build & Run

Hit **▶ Run** in Android Studio. The nutrition database (`savage_nutrition.db`) is bundled as an asset — it auto-copies and builds its FTS4 index on first launch.

---

## 📋 Requirements

| Requirement | Minimum |
|---|---|
| Android Version | API 26 (Android 8.0) |
| RAM | 4GB (for LLM inference) |
| Free Storage | ~1GB (model + nutrition DB) |
| Camera | Required for food scanner |
| Health Connect | Optional (for wearable sync) |
| Internet | Only for initial model download |

---

**Developed with ☕ and Sarcasm by Mj Alvear**