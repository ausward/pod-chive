# Podchive AI Documentation

## Project Overview
Podchive is an Android podcast player application built with Jetpack Compose and Media3. It allows users to browse, search, play podcasts, and save their favorites.

## Architecture

### Technology Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Navigation**: Jetpack Navigation Compose
- **Audio Playback**: Media3 (ExoPlayer)
- **Database**: Room
- **Networking**: Retrofit + Gson
- **Image Loading**: Glide
- **RSS Parsing**: rssparser library

### Project Structure
```
app/src/main/java/com/pod_chive/android/
├── MainActivity.kt              # Main activity with navigation setup
├── home.kt                      # Home page and podcast details screens
├── search.kt                    # Search functionality
├── playPod.kt                   # Player screen with mini controls
├── FavoritesScreen.kt           # Favorites management screen
├── PlaybackService.kt           # Background audio service
└── database/
    ├── FavoritePodcast.kt       # Room entity for favorites
    ├── FavoritePodcastDao.kt    # Database access object
    ├── PodchiveDatabase.kt      # Room database instance
    └── FavoritePodcastRepository.kt  # Data access layer
└── api/
    ├── PodchiveApi.kt           # API models and Retrofit clients
    └── RssDataSource.kt         # RSS feed parsing
└── ui/theme/
    └── Theme.kt, Color.kt, Type.kt  # Material Design 3 theming
```

## Core Features

### 1. Navigation Structure
- **Home**: Browse all available podcasts
- **Search**: Search for podcasts by name
- **Play**: Audio player (PlayPod) - full-screen player with controls
- **Favorites**: View and manage saved podcasts
- **Details**: Podcast detail pages with episode lists

Navigation is managed via Jetpack Navigation Compose with typed route support.

### 2. Audio Playback
**File**: `playPod.kt`, `PlaybackService.kt`

#### PlayPod Composable
- Displays current podcast artwork, title, and creator
- Integrates with Media3 MediaController
- Connects to persistent MediaSession service
- Shows full-screen player interface

#### MiniPlayerControls
- Compact player controls for all pages except PlayPod
- Features:
  - Play/pause button
  - Rewind 10 seconds
  - Forward 30 seconds
  - Progress slider with seek
  - Current time and duration display
- Only renders when audio is playing
- Hidden on PlayPod page (already showing full player)

#### Controls
- Play/Pause toggle
- Seek forward 30 seconds
- Rewind 10 seconds
- Speed control (0.5x - 3.0x)
- Progress slider with real-time seeking

### 3. Favorites System
**Files**: `FavoritesScreen.kt`, `database/`

#### Database Schema
```kotlin
@Entity(tableName = "favorite_podcasts")
data class FavoritePodcast(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val feedLink: String,
    val imageLocation: String,
    val description: String,
    val title: String,
    val addedAt: Long = System.currentTimeMillis()
)
```

#### Stored Data
- **Feed Link**: Unique identifier/URL of the podcast
- **Image Location**: URL to podcast artwork
- **Description**: Podcast description text
- **Title**: Podcast name
- **Timestamp**: When the podcast was added to favorites

#### Features
- Add/remove favorites from podcast detail pages
- Visual feedback with heart icon (filled = favorited)
- Persistent storage using SharedPreferences + Kotlinx Serialization
- FavoritesScreen shows all saved podcasts sorted by newest first
- Delete individual favorites from the list
- Navigate to podcast details from favorites list

#### Data Access Pattern (SharedPreferences)
- **Model**: `FavoritePodcast` (Serializable data class)
- **Repository**: `FavoritePodcastRepository` (manages SharedPreferences access)
- Uses JSON serialization for storing/retrieving favorites list

### 4. Podcast Discovery

#### Home Page (`home.kt`)
- Grid or list view toggle
- Browse all available podcasts
- Two layout options:
  - **Grid View**: 3-column card layout
  - **List View**: Horizontal items with artwork, title, and description

#### Search (`search.kt`)
- Full-text search across podcasts
- Displays search results with podcast information
- Navigate to details pages

#### Podcast Details (`home.kt`)
- **showPodDetsFromMainServer**: API-based podcasts
- **showPodDetsFromRSS**: RSS feed-based podcasts
- Features:
  - Scrollable header with shrinking artwork
  - Episode list with descriptions
  - Play episodes directly
  - Add/remove from favorites (heart button)
  - Sticky header with podcast title

### 5. Episode Management
**File**: `home.kt` - `EpisodeRow` composable

- Click to view full description in dialog
- Play button to start playback
- Automatically navigates to player when tapped
- Passes episode metadata to player
- Displays episode title and description

## Navigation Routes

```
home              → HomePage
search            → PodSearchBar + search results
playpod           → PlayPod (full-screen player)
  args: audioUrl, title, photoUrl, creator
details/{title}   → showPodDetsFromMainServer
homeItem          → showPodDetsFromRSS
favorites         → FavoritesScreen
```

## Key Composables

### MainActivity
- Sets up Scaffold with NavigationBar
- Configures NavHost with all routes
- Renders MiniPlayerControls conditionally
- Manages bottom navigation state

### HomePage
- Grid/list view toggle
- LazyColumn/LazyVerticalGrid for efficient rendering
- Loads podcasts on launch

### PlayPod
- Back handler for navigation
- Connects to MediaController via SessionToken
- AudioPlayer displays player controls
- MiniPlayerControls commented out (not used here)

### FavoritesScreen
- Shows list of favorite podcasts
- Delete button for each favorite
- Loading and empty states
- Click to navigate to podcast details
- Real-time list updates

### EpisodeRow
- Shows episode title and description
- Click to view full description
- Play button to start playback
- Navigates to PlayPod with episode metadata

## Data Flow

### Playback Flow
1. User clicks play on episode
2. `EpisodeRow` creates MediaItem with metadata
3. Sets item on MediaController
4. Navigates to PlayPod screen
5. PlayPod connects to same MediaController
6. Displays currently playing content

### Favorites Flow
1. User clicks heart icon on podcast details
2. Favorite button checks current status
3. If favorited: DELETE from database
4. If not favorited: INSERT into database
5. Heart icon updates visually
6. List in FavoritesScreen updates

## Important Notes

### GlobalScope Usage
The app uses `GlobalScope.launch()` for database operations. While functional, this is considered a delicate API and best practices recommend:
- Using ViewModelScope for lifecycle awareness
- Using lifecycleScope in Activities
- Proper coroutine management

Future refactoring should implement ViewModel pattern for better lifecycle handling.

### Back Navigation
- PlayPod implements BackHandler to navigate up
- Enabled only when previous back stack entry exists
- Works with system back button

### Mini Player
- Only shows on: Home, Search, Details, Favorites
- Hidden on: PlayPod (full player shown instead)
- Automatically hides when no audio playing
- Shows: rewind, play/pause, forward, seek slider

## API Integration

### Retrofit Clients
- **RetrofitClient**: Connects to main Podchive API
- **RetrofitClientFront**: Connects to front-end API for details
- Both use OkHttp client with caching

### Data Models
```kotlin
data class homeItem {
    val podcast_title: String
    val description: String
    val rss_url: String
    val html_summary_location: String
    val output_directory: String
    val cover_image_url: String
}

data class Episode {
    val title: String
    val description: String?
    val audioFilePath: String
}
```

## UI/UX Considerations

### Theme
- Material Design 3 with dynamic color support
- Light/dark theme based on system settings
- Custom color scheme in `ui/theme/`

### Accessibility
- Semantic descriptions on all icons
- Proper contrast ratios
- Touch-friendly button sizes

### Performance
- LazyColumn/LazyVerticalGrid for efficient scrolling
- Image loading with Glide
- Coroutines for background operations
- Room database for efficient local storage

## Known Limitations & Future Improvements

1. **GlobalScope Usage**: Should migrate to ViewModelScope
2. **Navigation**: Could implement deep linking
3. **Search**: Currently client-side filtering
4. **Caching**: Could implement better episode caching
5. **Sync**: No cloud sync for favorites
6. **Notifications**: No download or notification support
7. **Offline Mode**: Limited offline functionality

## Troubleshooting

### Room Database Issues

**Error**: `Cannot find implementation for com.pod_chive.android.database.PodchiveDatabase. PodchiveDatabase_Impl does not exist`

**Solutions**:
1. **Clean Build**: Run `./gradlew clean :app:assembleDebug`
2. **Check build.gradle.kts**: Ensure `annotationProcessor` is configured (not `kapt`)
3. **Rebuild Project**: Sometimes IDE cache needs clearing via `Build > Clean Project`
4. **Invalidate Cache**: In Android Studio, `File > Invalidate Caches > Invalidate and Restart`

**Why This Happens**:
- Room's annotation processor generates `PodchiveDatabase_Impl` at compile time
- Without a clean build, the processor may not run properly
- Using `kapt` with built-in Kotlin support can cause conflicts

### Navigation Issues

**Error**: Route not found or arguments not passed

**Solutions**:
1. Check route name matches in both NavHost definition and navigation calls
2. Verify argument types are properly encoded with `Uri.encode()`
3. Use `navController.navigate()` with correct route syntax

### Mini Player Not Showing

**Check**:
1. Verify audio is actually playing: `mediaController?.isPlaying`
2. Ensure the route is not "playpod" (it's hidden on PlayPod screen)
3. Check `if (isPlaying)` condition in MiniPlayerControls

### Favorites Not Persisting

**Check**:
1. Verify FavoritePodcast entity is properly saved: check logcat for database errors
2. Ensure database context is from `LocalContext.current`
3. Check file permissions: `build.gradle.kts` has `allowMainThreadQueries()` for testing

## Common Git Commits

```bash
# After adding favorites feature
git add -A
git commit -m "feat: Add favorites system with Room database"

# After fixing Room compilation
git commit -m "fix: Configure annotation processor for Room code generation"

# Update documentation
git commit -m "docs: Update AI.md with Room configuration and troubleshooting"
```

## Building & Testing

```bash
# Build debug APK
./gradlew :app:assembleDebug

# Run unit tests
./gradlew :app:testDebugUnitTest

# Run instrumented tests
./gradlew :app:connectedAndroidTest

# Clean build (if annotation processor issues arise)
./gradlew clean :app:assembleDebug
```

### Favorites Storage Configuration
The app uses **SharedPreferences with Kotlinx Serialization** for storing favorites (instead of Room ORM).

**Why SharedPreferences?**
- No annotation processor complexity
- Built-in Android API
- Perfect for small datasets like favorites
- Easy JSON serialization with kotlinx-serialization
- No database file management needed

## Dependencies
- androidx.compose:* (UI)
- androidx.media3:* (Media playback)
- androidx.navigation:* (Navigation)
- androidx.room:* (Database)
- retrofit2:* (Networking)
- com.squareup.okhttp3:* (HTTP client)
- com.bumptech.glide:* (Image loading)
- com.prof18:rssparser:* (RSS parsing)

## Last Updated
February 2026

## Development Notes
- Minimum SDK: 33
- Target SDK: 36
- Language: Kotlin 2.0.21
- Compose: Latest BOM 2024.09.00





