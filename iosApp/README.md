# iOS app shell (prototype)

Xcode project wrapping the shared Kotlin/Compose code. **Opening and building
this requires macOS with Xcode** (iOS builds cannot run on Windows).

## First run on a Mac

```bash
# 1. From the repo root - compile the Kotlin framework for the simulator:
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64   # needs macOS + Xcode

# 2. Open the Xcode project:
open iosApp/iosApp.xcodeproj

# 3. In Xcode: select the iosApp scheme + an iPhone simulator, press Run.
```

Xcode invokes `./gradlew :shared:embedAndSignAppleFrameworkForXcode` during
each build, so the Swift app always links against the current shared module.

## What is implemented

- `shared/` - Kotlin Multiplatform module containing the **entire domain
  layer** (models, card data parsing, generation rules, KingdomGenerator,
  CardDependencyResolver) compiled for Android and iOS.
- `shared/src/iosMain/.../BundledCards.kt` - the card database embedded as a
  string (no Xcode resource setup needed). Regenerate it when cards.json
  changes:
  `python -c "..."` (see root README "Sync points").
- `shared/src/commonMain/.../demo/DemoKingdomApp.kt` - a minimal Compose UI:
  **Generate Kingdom** button + card grid, wired to the real generator with
  in-memory data sources (everything owned/enabled, default settings).
- This Xcode project (from JetBrains' official CMP template) that hosts the
  Compose UI via `MainViewController()`.

## Not ported yet (roadmap)

- Full UI (Library/Kingdoms/Settings screens) - currently Android-only.
- Persistence on iOS: Room/DataStore are still Android-only; the demo uses
  in-memory sources. Next step is moving the Room schema to commonMain
  (Room 2.8 supports KMP) with a native SQLite driver, plus DataStore KMP.
- App icon, assets, App Store targets, CI signing.

## Files

- `iosApp.xcodeproj` - Xcode project (bundle id configured in
  `Configuration/Config.xcconfig`)
- `iosApp/ContentView.swift` - hosts the Compose view controller
- `iosApp/iOSApp.swift` - app entry
