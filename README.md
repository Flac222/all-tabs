# AllTabs

Native Android mobile app for guitarists — search, create, edit, and play guitar tabs with AI assistance.

**Package:** `com.uade.alltabs`  
**Stack:** Kotlin · Jetpack Compose · Room · Firebase · Retrofit · Hilt · Gemini

## Setup

1. Clone the repo and open in **Android Studio** (Ladybug or newer recommended).
2. Copy `gradle.properties.example` → `gradle.properties` and set `GEMINI_API_KEY`.
3. Add `app/google-services.json` from Firebase Console (not in git).
4. **File → Sync Project with Gradle Files**.

### Gradle wrapper (CLI builds)

If `.\gradlew.bat` fails with *Unable to access jarfile … gradle-wrapper.jar*:

- Sync the project in Android Studio once (regenerates the wrapper JAR locally), **or**
- Run `gradle wrapper --gradle-version 8.4` if Gradle is installed globally.

The wrapper JAR should be committed (`!gradle/wrapper/gradle-wrapper.jar` in `.gitignore`). AGP 8.3.1 requires Gradle **8.4+** (not 9.x).

## Run tests

**Android Studio:** right-click `app/src/test/java` → Run tests, or Gradle → `app` → `verification` → `test`.