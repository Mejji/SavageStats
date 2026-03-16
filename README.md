# SavageStats 🛡️



[![Latest Release](https://img.shields.io/github/v/releaseMejji/SavageStats?label=Download%20APK&style=for-the-badge&color=red)](https://github.com/your-username/SavageStats/releases/latest)



**SavageStats** is an offline-first fitness tracker that uses on-device Large Language Models (LLMs) to provide witty, unfiltered, and data-driven coaching. No cloud, no subscriptions—just local AI that roasts your progress (or lack thereof).



![SavageStats Demo](https://github.com/user-attachments/assets/b681016c-087a-4533-b124-ab4f42739754)



### 🛠️ Tech Stack

* **Language:** Kotlin (Modern Native Android)

* **UI:** Jetpack Compose with Material 3

* **Architecture:** MVVM (Model-View-ViewModel)

* **Local Database:** Room (SQLite abstraction for offline persistence)

* **AI Engine:** Google MediaPipe LLM Inference API

* **Model:** Gemma 2B (INT4 Quantized for mobile performance)

* **Storage:** Preferences DataStore for user biometrics

* **Integrations:** Android Health Connect (Samsung Health, Strava, etc.)



---



### ✨ Key Features

* **Privacy First:** 100% of AI inference happens on your device. Your fitness and health data never leave your phone.

* **The Savage Coach:** A personalized AI assistant that reviews your protein intake, workout duration, and sleep quality to deliver blunt, 3-sentence performance reviews.

* **Context-Aware Intelligence:** The AI understands your Age, Weight, and specific Fitness Goals (Bulking, Cutting, Conditioning) to provide tailored advice.

* **Daily Log & Weekly View:** A sleek horizontal calendar to track your "grind" across the week and see your logs stack up.

* **Device Sync:** One-tap integration with Health Connect to automatically pull data from your favorite wearables and apps.



---



### 📦 Installation (Latest Release)

If you just want to try the app without building from source:

1. Navigate to the [Releases](https://github.com/Mejji/SavageStats/releases) page.

2. Download the latest `SavageStats_v1.apk`.

3. Install it on your Android device (Note: You may need to enable "Install from Unknown Sources").



---



### 🚀 Developer Setup (Build from Source)

Because the AI "brain" is a **1.5GB Gemma 2B** model, it is not included in this repository to keep the build lightweight.



#### 1. Download the Model

Get the **`gemma-2b-it-cpu-int4`** model from [Hugging Face](https://huggingface.co/google/gemma-2b-it-cpu-int4).



#### 2. Initialize the App

* **Automatic:** The app features a dedicated **Setup Screen**. Provide your hosted model URL in `SetupViewModel.kt`, and the app will stream the 1.5GB file directly to internal storage.

* **Manual (ADB):** If you've already downloaded the file, push it directly to the app's internal directory:

    ```bash

    adb push gemma-2b.bin /data/data/com.example.savagestats/files/gemma-2b.bin

    ```



#### 3. Build & Run

Open the project in **Android Studio (Ladybug 2024.2.1+)** and hit **Run**. Ensure your device or emulator has at least **4GB of RAM** available for the LLM.



---



### 📂 Project Structure

* `data/`: Room Database, DataStore, and Health Connect Managers.

* `ui/`: Composable screens and ViewModels for the Dashboard, Coach, and Profile.

* `ai/`: MediaPipe LLM Inference initialization and prompt engineering logic.



---



**Developed with ☕ and Sarcasm by Mj Alvear**
