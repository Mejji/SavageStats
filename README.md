# SavageStats 🛡️

An offline-first, "savage" fitness coach powered by on-device AI. 

### Tech Stack
* **Language:** Kotlin
* **UI:** Jetpack Compose (Material 3)
* **Local Database:** Room
* **AI Engine:** Google MediaPipe LLM Inference API
* **Model:** Gemma 2B (INT4 Quantized)
* **Integrations:** Android Health Connect

🚀 How to Run
Because the AI model is ~1.5GB, it is not included in this repository. To get the app running:

Download the Model: Get the gemma-2b-it-cpu-int4 model from Hugging Face.

Setup:

Automatic: The app includes a setup screen that will attempt to download the model on the first launch if you provide a valid URL in SetupViewModel.kt.

Manual: Alternatively, push the .bin file to your device via ADB:

Bash
adb push your-model.bin /data/data/com.example.savagestats/files/gemma-2b.bin
Build: Open the project in Android Studio (Ladybug or newer) and hit Run.

### Features
* **Privacy First:** All AI inference happens locally. No data leaves your device.
* **Savage Coaching:** Dynamic roasts based on your protein, sleep, and activity.
* **Smart Tracking:** Horizontal weekly calendar and biometric-aware critiques.
