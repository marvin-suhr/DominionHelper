# Multiplatform structure (Android + iOS prototype)

This repository now contains three top-level pieces:

| Path      | What it is |
|-----------|------------|
| `app/`    | The Android app (Compose UI, Room, Hilt, DataStore). Unchanged UX. |
| `shared/` | Kotlin Multiplatform module: the **entire domain layer** (models, cards.json parsing, rules, `KingdomGenerator`, `CardDependencyResolver`) compiled for Android + iOS. |
| `web/`    | Kingdom-sharing web service prototype (Node). |
| `iosApp/` | Xcode shell hosting a minimal Compose demo UI on iOS (see `iosApp/README.md`). |

## How the domain layer moved to shared

- Models live in `shared/src/commonMain/kotlin/dev/msuhr/dominionkingdoms/model/`
  with the **same package names** as before, so Android UI code barely changed.
- Platform bits are abstracted:
  - `platform/` - `nowMillis()`, `randomUuid()`, logging (expect/actual)
  - `Set.imageId: Int` (Android resource) became `Set.imageName: String`,
    resolved per-platform (`getDrawableId` on Android)
  - `Card.expansionImageId` became `Card.expansionImageName`
  - JSON parsing moved to `CardJson` (platform passes the string)
- Storage behind interfaces in `shared/.../data/Sources.kt`:
  `CardDataSource`, `ExpansionDataSource`, `UserPrefsSource`.
  The Android Room DAOs and `UserPrefsRepository` implement them; Hilt binds
  them in `di/AppModule.kt`. The iOS demo uses in-memory implementations.
- UI enums moved to shared: `RandomMode`, `VetoMode`, `DarkAgesMode`,
  `ProsperityMode`, `PromoMode` (was `ui/SettingsViewModel.kt`) and
  `KingdomSortType` (was nested in `KingdomViewModel`).

## Verification status

- `:app:assembleDebug` builds green on Windows (APK produced).
- `:shared` compiles for Android + common/iOS metadata (iOS main typechecks).
- **Actual iOS compilation (`linkDebugFramework...`) and the Xcode app can
  only be verified on macOS with Xcode** - see `iosApp/README.md`.

## Sync points (keep these in step)

1. `app/src/main/assets/cards.json` -> `web/data/cards.json` (cp) and
   `shared/src/iosMain/.../BundledCards.kt` (regenerate embedded string).
2. Web JS port of dependency resolution: `web/lib/cards.js` (stays as a
   fallback; the app-side resolver in shared is authoritative).
