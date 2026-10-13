# Akira - Developer & AI Agent Guide

## Overview
**Akira** is an Android application for anime discovery and streaming built with Jetpack Compose and AndroidX Media3 (ExoPlayer).

## Tech Stack
- **Language**: Kotlin (Target Java 17, SDK 37, Min SDK 24)
- **UI Framework**: Jetpack Compose + Material 3
- **Media Player**: AndroidX Media3 ExoPlayer + HLS
- **Networking**: OkHttpClient + BouncyCastle / Android Security Crypto
- **Architecture**: Feature-based package hierarchy (`features/auth`, `features/home`, `features/player`, `features/settings`, `features/updater`)

## Project Structure
```
app/src/main/java/com/ghostreborn/akira/
├── core/                  # Low-level config, endpoints, crypto
├── features/
│   ├── auth/              # Authentication repository, tokens, cloudflare verification
│   ├── home/              # Home feeds, anime list, details, favorites
│   ├── player/            # ExoPlayer video player, stream decryptor, parser
│   ├── settings/          # Application preferences & settings screen
│   └── updater/           # In-app APK updates, downloader & installer
├── ui/                    # Shared Compose components, theme, color palettes
└── utils/                 # Utility extensions
```

## AI Code Generation Guidelines
1. **Unidirectional Data Flow**: Prefer ViewModel + `StateFlow<UiState>` over holding mutable state in Composable functions.
2. **Modular Composables**: Keep Composable functions small and focused (<150 lines). Extract sub-views into separate files under `widgets/` or `components/`.
3. **Sealed UI States**: Model screen states using sealed interfaces (`Loading`, `Success`, `Error`).
4. **Previews**: Provide `@Preview` functions for reusable UI components.
5. **No Global Singletons for State**: Avoid static state objects (e.g. `NavHolder`). Pass arguments via Navigation routes or ViewModel handles.
6. **Pure Logic Separation**: Keep parsers (`Decryptor`, `StreamParser`, `MkissaBundleParser`) free of `android.content.Context` dependencies so unit tests can run cleanly on JVM.

## Verification Commands
- **Assemble Debug APK**: `./gradlew :app:assembleDebug`
- **Run JVM Unit Tests**: `./gradlew :app:testDebugUnitTest`
