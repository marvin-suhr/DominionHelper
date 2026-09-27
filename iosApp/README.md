# iOS app shell

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

Requires **Apple Silicon** to run on the simulator: Compose Multiplatform 1.11+
dropped the x86_64 iOS targets, so Intel Macs cannot run the Compose iOS app
in their simulator (cross-compiling the framework for arm64 from Intel works,
so development and CI builds are still possible - the app just needs an
arm64 simulator or a physical device to actually run).

## What is implemented

- `shared/` - Kotlin Multiplatform module containing the **entire domain
  layer** (models, card data parsing, generation rules, KingdomGenerator,
  CardDependencyResolver) compiled for Android and iOS.
- `shared/src/commonMain/.../shared/` - the **full app UI ported from the
  Android app** (theme, navigation, Library/Kingdoms/Settings screens, card
  tiles, kingdom cards, card detail, settings with range rule picker, and the
  Library/Kingdom/Settings ViewModels). Lives in the
  `dev.msuhr.dominionkingdoms.shared.*` packages so it never collides with
  the Android app's own UI classes.
- `shared/src/iosMain/.../data/BundledCards.kt` + `BundledSets.kt` - the card
  and expansion databases embedded as strings (no Xcode resource setup
  needed). Regenerate when cards.json / sets.json change.
- `shared/src/iosMain/.../data/InMemoryAppDataStore.kt` - in-memory
  implementations of the extended data sources (all expansions owned,
  default settings). Everything (kingdoms, favourites, bans, ownership,
  settings) works but resets on app restart until persistence is ported.
- `shared/src/iosMain/composeResources/drawable/` - the 954 card /
  expansion / category images copied from the Android drawables, packaged
  into the framework by the Compose resources system (iOS-only, they are
  not added to the Android APK).
- This Xcode project (from JetBrains' official CMP template) that hosts the
  Compose UI via `MainViewController()`.

## Differences from the Android app

- Community kingdoms tab, uploading/sharing kingdoms, and the card database
  auto-updater are Android-only (they need the share web service / network
  stack). The Community tab shows its empty state.
- Dynamic color ("Material You") does not exist on iOS - the custom
  "Official" Dominion palette is always used (light + dark).
- No persistence yet (next step: Room KMP + DataStore KMP).

## Not ported yet (roadmap)

- Persistence on iOS: Room/DataStore are still Android-only; the iOS app
  uses in-memory sources. Next step is moving the Room schema to commonMain
  (Room 2.8 supports KMP) with a native SQLite driver, plus DataStore KMP.
- App Store targets, CI signing (TEAM_ID in `Configuration/Config.xcconfig`).

## Files

- `iosApp.xcodeproj` - Xcode project (bundle id configured in
  `Configuration/Config.xcconfig`)
- `iosApp/ContentView.swift` - hosts the Compose view controller
- `iosApp/iOSApp.swift` - app entry
