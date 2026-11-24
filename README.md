# Recipe Finder

An offline-first Android app for discovering recipes from [TheMealDB](https://www.themealdb.com/).
You can search for dishes, browse by category, read full ingredients and cooking steps, save
favorites, and go back to recently opened recipes. Anything you have loaded once stays available
without an internet connection.

## Features

- **Home**: debounced search by name, a horizontal category selector (Chicken is selected on first
  launch), recipe cards in an adaptive grid, pull-to-refresh, an offline banner, and separate
  loading, empty, offline and error states.
- **Recipe detail**: a large photo, the name, category and cuisine, the ingredients with their
  measurements ("2 cups – Flour"), numbered cooking steps and a favorite heart. The screen stays
  on while a recipe is open, which helps when you're cooking.
- **Favorites**: saved recipes that persist across restarts and work offline.
- **Recently Viewed**: the last 20 opened recipes, newest first, with no duplicates.
- **Light and dark theme**: a warm cream/terracotta palette.

## Technologies

| Concern | Library |
|---|---|
| Language | Kotlin 2.0.21 |
| UI | Jetpack Compose (BOM 2024.12.01), Material 3, material-icons-extended |
| Navigation | Navigation Compose 2.8.5 |
| Architecture | MVVM + Repository, `ViewModel`, `StateFlow`, `SavedStateHandle` |
| Async | Kotlin Coroutines and Flow 1.9.0 |
| Networking | Retrofit 2.11.0 + Gson converter, OkHttp 4.12.0 |
| Database | Room 2.6.1 (KSP) |
| Images | Coil 2.7.0 (memory + disk cache) |
| DI | Manual container (`di/AppContainer.kt`) |
| Tests | JUnit 4, kotlinx-coroutines-test, Turbine, Robolectric 4.14.1 |
| Build | Android Gradle Plugin 8.7.3, Gradle 8.10.2, compileSdk/targetSdk 35, minSdk 24 |

## Architecture

```
 Compose screen ──(events)──▶ ViewModel ──▶ Repository interface (domain)
       ▲                          │                 │
       └──── StateFlow<UiState> ◀─┘                 ▼
                                    OfflineFirstRecipeRepository (data)
                                       │                     │
                                 Room (source of truth)   Retrofit (TheMealDB)
```

- **Presentation** (`presentation/`): Composables only render state and send events. They never
  touch Retrofit or Room. Each screen has a stateless `…Content` composable and a thin wrapper
  that connects it to its ViewModel.
- **ViewModels** expose one `StateFlow` of a sealed UI state, such as `HomeContent.Loading`,
  `Recipes`, `NoResults`, `OfflineNoCache` or `Error`. They don't use loose boolean flags.
  ViewModels depend only on the repository interfaces.
- **Domain** (`domain/`): plain Kotlin models, repository interfaces, `ConnectivityObserver` and
  the instruction step splitter. It has no Android or library types.
- **Data** (`data/`): Retrofit service and DTOs, Room entities, DAOs and the database, mappers,
  and the repository implementations.

DTOs, entities and domain models are separate classes. The mapping functions are in
`data/mapper/`.

## Project structure

```
app/src/main/java/com/example/recipefinder
├── RecipeFinderApplication.kt   # creates AppContainer, configures Coil
├── MainActivity.kt
├── di/AppContainer.kt           # manual dependency injection
├── domain
│   ├── model/                   # Recipe, RecipeSummary, Category, SyncStatus, DataError…
│   ├── repository/              # RecipeRepository, FavoritesRepository, RecentlyViewedRepository
│   ├── connectivity/            # ConnectivityObserver interface
│   └── util/InstructionSteps.kt
├── data
│   ├── remote/api/              # MealApiService, safeApiCall + error classification
│   ├── remote/dto/              # TheMealDB JSON DTOs
│   ├── local/entity/            # RecipeEntity, FavoriteEntity, RecentlyViewedEntity, CategoryEntity
│   ├── local/dao/               # RecipeDao, FavoriteDao, RecentlyViewedDao, CategoryDao
│   ├── local/database/          # RecipeDatabase, Converters
│   ├── mapper/                  # DTO→Entity→Domain mappers, IngredientParser
│   ├── network/                 # AndroidConnectivityObserver
│   └── repository/              # OfflineFirstRecipeRepository, LocalFavorites…, LocalRecentlyViewed…
└── presentation
    ├── AppViewModelProvider.kt  # ViewModel factory wired to AppContainer
    ├── navigation/              # Routes, TopLevelDestination, RecipeFinderApp (NavHost + bottom bar)
    ├── components/              # RecipeCard, RecipeGrid, CategoryChip, RecipeSearchBar,
    │                            # OfflineBanner, LoadingIndicator, EmptyState, ErrorState,
    │                            # IngredientRow, BottomNavigationBar, RecipeImage, ScreenTitleBar
    ├── home/  detail/  favorites/  recentlyviewed/   # screen + ViewModel + UI state
    ├── common/                  # CollectionUiState, error message mapping
    └── theme/                   # colors, typography, shapes
```

## How offline-first works

**Room is the single source of truth.** The UI never shows an API response directly.

For every recipe list and detail screen, the repository:

1. starts observing Room right away, so cached data appears immediately, even offline;
2. sends the matching TheMealDB request in parallel;
3. maps the response to entities and writes them to Room;
4. lets Room emit the updated rows, so the UI refreshes on its own;
5. on failure, leaves the database untouched. The cached rows keep flowing, and a `SyncStatus.Failed(error)`
   tells the UI what happened.

Each emission is a `RecipeListing(recipes, status)` or `RecipeDetail(recipe, status)`. The
ViewModel turns this into screen states:

| Local data | Network | Screen shows |
|---|---|---|
| some rows | refreshing | the rows plus a pull-to-refresh spinner |
| some rows | failed (offline) | the rows plus "You're offline — showing saved recipes." |
| none | refreshing | loading |
| none | succeeded | "No recipes found" |
| none | failed (offline) | "You're offline" (not "no results") |
| none | failed (other) | an error with *Try again* |

The status only becomes "synced" after Room has re-run its query *after* the write. This stops
the screen from briefly showing "no results" before fresh results arrive. See
`offlineFirstListing` in `OfflineFirstRecipeRepository`.

### Search

`HomeViewModel` turns the search text into requests with Flow operators:

- `debounce(450 ms)`: no request is sent per keystroke. Clearing the field takes effect immediately.
- `distinctUntilChanged()`: the same query is not repeated.
- `flatMapLatest`: a new query cancels the previous Flow, including its in-flight HTTP call.

Local search uses `name LIKE %query%`, so previously loaded recipes stay searchable offline. The
IDs that the API returned for the current query are also included in the query. This covers cases
where SQLite's `LIKE` doesn't match but the API did, such as accents ("creme" vs "Crème").

### How API data is stored without losing anything

| Source | Contains | Stored with |
|---|---|---|
| `filter.php?c=` | id, name, thumbnail | `RecipeDao.upsertBasic`: inserts new rows; on existing rows it updates **only** name, thumbnail, category and fetch time. Ingredients, instructions, area and `detailsLoaded` are never touched. |
| `search.php?s=` / `lookup.php?i=` | full recipe | `RecipeDao.upsertFull`: replaces the row, but any field the new response leaves empty keeps its stored value |
| `categories.php` | category list | `CategoryDao.upsertAll` (kept in API order) |

Other rules:

- Recipes are never deleted because they are missing from a later response.
- **Favorites** and **recently viewed** are separate tables with no foreign key to `recipes`, so
  refreshing recipes can never remove them.
- History uses an upsert on `recipeId` and then trims to the newest 20 entries. Both steps run in
  one transaction.
- The detail screen calls `lookup.php` **only** if the stored row has `detailsLoaded = false`.

### Ingredients

TheMealDB sends `strIngredient1..20` and `strMeasure1..20`. `IngredientParser` reads all 20
pairs by position and skips null, empty or whitespace-only ingredients. It also trims and
collapses whitespace and keeps the original order. A missing measure stays `null`; the app never
makes one up. The list is stored as a JSON column through a Room `TypeConverter`.

### Instructions

Instructions are stored exactly as the API returns them, with line endings normalized.
`InstructionSteps` splits them on line breaks and removes the source's own labels like
"STEP 1" or "2." so the app's numbering doesn't clash. If the text has no line breaks, it is
shown as one paragraph rather than guessing where sentences end.

### Images offline

Coil uses a 150 MB disk cache in the app's cache directory, so no storage permission is needed,
plus a memory cache. `respectCacheHeaders(false)` tells Coil to serve a cached image even if the
server's HTTP headers say it has expired. This is what lets photos you've already seen load offline.

### Error handling

`safeApiCall` turns every failure into a `DataError`: `NO_CONNECTION`, `TIMEOUT`, `SERVER`,
`INVALID_RESPONSE` (bad JSON or an empty body), `NOT_FOUND` or `UNKNOWN`. The UI shows a friendly
message for each one and never shows a raw exception. Coroutine cancellation is always re-thrown.
When the connection comes back, the Home and Detail screens retry automatically if they were
showing an error.

## Requirements

- Android Studio (Ladybug or newer) with Android SDK 35 installed
- JDK 17–23 for Gradle 8.10.2. Android Studio's bundled JDK 21 works; newer bundled JDKs (25+)
  are **not** supported by this Gradle version.
- Internet access the first time the app loads data. No API key is needed; the app uses
  TheMealDB's free public test key `1`.

## Opening the project in Android Studio

1. **File → Open** and select this folder (the one containing `settings.gradle.kts`).
2. If Android Studio reports a Gradle JDK problem, go to **Settings → Build, Execution,
   Deployment → Build Tools → Gradle → Gradle JDK** and choose a JDK 17–23
   (for example `jbr-21`).
3. Let Gradle sync, choose a device or emulator (API 24+) and press **Run ▶**.

`local.properties` (SDK path) is machine-specific and git-ignored; Android Studio recreates it.

## Building from the command line

Windows (PowerShell):

```powershell
$env:JAVA_HOME = "$env:USERPROFILE\.jdks\jbr-21.0.11"   # any JDK 17–23
.\gradlew.bat assembleDebug
```

macOS / Linux:

```bash
./gradlew assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`. To install it on a connected
device, run `.\gradlew.bat installDebug`.

## Running the tests

```powershell
.\gradlew.bat testDebugUnitTest      # unit, Room (Robolectric), repository and ViewModel tests
.\gradlew.bat lintDebug              # Android lint, report in app/build/reports/
```

All tests run on the JVM; no emulator is needed. Robolectric downloads its Android runtime jar
the first time it runs.

| Suite | What it covers |
|---|---|
| `IngredientParserTest` | 20 pairs, blank skipping, missing measures, whitespace, order |
| `RecipeMappersTest` | full recipe JSON, `meals: null`, missing fields, filter/category responses |
| `InstructionStepsTest` | step splitting and label removal without altering text |
| `SafeApiCallTest` | timeout, offline, server, empty body and JSON errors |
| `RecipeDatabaseTest` | insert/read, Flow updates, partial updates, favorites, history upsert and 20-entry limit |
| `OfflineFirstRecipeRepositoryTest` | API→Room saving, cached fallback, detail-preserving refresh, lookup only when needed |
| `HomeViewModelTest` | default category, debounce, loading/results/empty/offline/error states, auto-retry |
| `RecipeDetailViewModelTest` | history recording, missing-details state, favorites toggle |
| `CollectionViewModelsTest` | Favorites and Recently Viewed state changes |

## Possible future additions

The layers are separated so these can be added without rewriting existing code: Hilt (replace
`AppContainer`), pagination, a shopping list (a new table and repository), and accounts or sync
(another data source behind the same repository interfaces).
