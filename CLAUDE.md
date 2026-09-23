# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

DominionHelper is an Android app for the Dominion card game that generates random kingdoms (10 card setups) from user-selected expansions. Built with Kotlin, Jetpack Compose, and follows MVVM architecture with clean architecture principles.

## Build & Development Commands

```bash
# Build
./gradlew assembleDebug      # Build debug APK
./gradlew assembleRelease    # Build release APK
./gradlew build              # Build all variants

# Testing
./gradlew test               # Run unit tests
./gradlew connectedAndroidTest  # Run instrumented tests

# Other
./gradlew clean              # Clean build artifacts
./gradlew dependencies       # Show dependency tree
```

**Note:** On Windows, use `gradlew.bat` instead of `./gradlew`.

## Architecture Overview

### MVVM + Clean Architecture

The app follows a single-activity architecture with Jetpack Navigation Compose:

```
MainActivity (Root)
    ├── Navigation Controller (CurrentScreen sealed class)
    │   ├── Library Screen (card browser)
    │   ├── Kingdoms Screen (saved/generated kingdoms)
    │   └── Settings Screen
    └── ViewModels (ScreenViewModel interface)
```

### Key Architectural Patterns

1. **Navigation**: Sealed class `CurrentScreen` in `Navigation.kt` handles routing. Bottom nav switches between three main destinations.

2. **State Management**: ViewModels expose `StateFlow<UiState>` for reactive UI updates. Each screen has its own UI state enum/class.

3. **Dependency Injection**: Hilt (`@AndroidEntryPoint`) is used throughout. All dependencies are configured in `di/AppModule.kt`.

4. **Data Layer**: Room database with Repository pattern. `KingdomRepository` abstracts data access from ViewModels.

5. **Business Logic**: Core game logic is in `KingdomGenerator.kt` (generates random kingdoms) and `CardDependencyResolver.kt` (resolves card dependencies like spoils, horses, etc.).

### Data Flow

```
User Action → UI (Compose) → ViewModel → Repository/DAO → Database
     ↓                                                           ↑
StateFlow ← UI State ← ViewModel ← Business Logic ← Data Layer
```

## Package Structure

```
com.marvinsuhr.dominionhelper/
├── di/                    # Hilt modules (database, DAOs, repos)
├── data/
│   ├── entities/         # Room entities (CardEntity, ExpansionEntity, KingdomEntity)
│   ├── mappers/          # Entity ↔ Domain model conversion
│   ├── repositories/     # Repository implementations
│   └── *Dao.kt           # Room DAOs
├── model/                # Domain models (Card, Kingdom, Expansion, enums)
├── ui/
│   ├── components/       # Reusable Compose components
│   └── *ViewModel.kt     # Screen ViewModels (implement ScreenViewModel)
├── DominionHelper.kt     # Application class with Hilt
├── MainActivity.kt       # Single activity with Scaffold + bottom nav
├── Navigation.kt         # Sealed class navigation + nav graph
├── KingdomGenerator.kt   # Core kingdom generation logic
└── CardDependencyResolver.kt # Card dependency resolution
```

## Key Business Logic

### Kingdom Generation
- `KingdomGenerator`: Takes selected expansions, generates 10 random kingdom cards
- Supports expansion weighting, min/max cards per expansion
- Handles landscape cards (Events, Ways, Landmarks, Allies)

### Card Dependencies
- `CardDependencyResolver`: Adds required non-supply cards based on kingdom composition
- Examples: Horses for "Oblivion", Spoils for "Bandit Camp", etc.

## Common Patterns

### Adding a New Screen
1. Add entry to `CurrentScreen` sealed class in `Navigation.kt`
2. Create ViewModel implementing `ScreenViewModel` interface
3. Add UI state sealed class to `ui/`
4. Create Composable screen in `ui/` or `ui/components/`
5. Update `MainActivity` navigation graph
6. Add bottom nav entry (if main destination)

### Adding a New Feature to Existing Screen
1. Update UI state class in `ui/`
2. Modify ViewModel to handle new logic
3. Update Composable UI to reflect new state
4. If database changes needed: update entity, DAO, mapper, repository

## UI Conventions

- **Compose**: All UI is Jetpack Compose with Material 3
- **Modifiers**: Use consistent padding values from existing components
- **Images**: Card images are in `assets/`, loaded with Coil
- **Theme**: Material 3 with dynamic color support
- **Navigation**: Use `navController.navigate()` with `CurrentScreen` entries

## Database Schema

- **CardEntity**: Individual cards (name, cost, types, expansion, image path)
- **ExpansionEntity**: Expansions (name, is selected by user)
- **KingdomEntity**: Saved kingdoms (cards, name, favorite, timestamp)

## Important Notes

- App uses **single-activity architecture** - do not add new activities
- **StateFlow** is used instead of LiveData for reactive streams
- **Coil** for async image loading (not Glide or Picasso)
- **Room** for persistence - raw SQL queries are discouraged
- **Hilt** handles all DI - manual constructor injection is anti-pattern
- All ViewModels must implement `ScreenViewModel` interface
