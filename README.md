# pod-chive

Podchive is an Android podcast player built with Kotlin, Jetpack Compose, and Media3.

## What it does

- Browse podcasts from the pod-chive catalog
- Search podcasts by keyword
- Open and play podcasts from RSS URLs
- Play episodes with a persistent Media3 playback service
- Save favorite podcasts locally
- Enable notifications for new episodes from favorites
- Resume playback progress for previously played episodes

## Tech stack

- Kotlin + Jetpack Compose
- Navigation Compose
- Media3 / ExoPlayer / MediaSession
- Retrofit + Gson
- RSSParser
- SharedPreferences + Kotlinx Serialization
- Glide Compose

## Project metadata

- Application ID: `com.pod_chive.android`
- App name: `pod-chive`
- Version name: `1.0`
- Version code: `1`
- Min SDK: `33`
- Target SDK: `36`
- Compile SDK: `36`

(Values are sourced from `app/build.gradle.kts`, `app/src/main/res/values/strings.xml`, and `app/release/output-metadata.json`.)

## Build and run

From the project root:

```bash
./gradlew :app:assembleDebug
```

Build a release APK:

```bash
./gradlew :app:assembleRelease
```

Run unit tests:

```bash
./gradlew :app:testDebugUnitTest
```

Run instrumented tests (requires connected device/emulator):

```bash
./gradlew :app:connectedAndroidTest
```

## Project layout

- `app/src/main/java/com/pod_chive/android/MainActivity.kt` - app entry point and navigation
- `app/src/main/java/com/pod_chive/android/home.kt` - home feed and podcast detail flows
- `app/src/main/java/com/pod_chive/android/search.kt` - keyword and RSS search
- `app/src/main/java/com/pod_chive/android/playPod.kt` - player UI
- `app/src/main/java/com/pod_chive/android/PlaybackService.kt` - background playback service
- `app/src/main/java/com/pod_chive/android/FavoritesScreen.kt` - favorites UI
- `app/src/main/java/com/pod_chive/android/database/FavoritePodcastRepository.kt` - favorites persistence
- `app/src/main/java/com/pod_chive/android/work/` - periodic background sync jobs
- `app/src/main/java/com/pod_chive/android/notif/notificationManager.kt` - notification setup and posting

## Permissions used

From `app/src/main/AndroidManifest.xml`:

- `android.permission.INTERNET` - fetch podcast metadata and RSS content
- `android.permission.FOREGROUND_SERVICE` - keep audio playback active in foreground
- `android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK` - media playback foreground service type
- `android.permission.POST_NOTIFICATIONS` - alert users about new favorite episodes
- `android.permission.RECEIVE_BOOT_COMPLETED` - restore scheduled sync job after reboot

## Background behavior

- A periodic job (`FavoriteEpisodesSyncJobService`) checks favorites for new episodes.
- Current scheduler interval in code is 15 minutes (`FavoriteEpisodesSyncScheduler`).
- Playback runs through a `MediaSessionService` for ongoing media controls.

## Privacy and data handling (current implementation)

- Favorites and playback state are stored locally in app storage (`SharedPreferences`).
- The app requests network data from pod-chive APIs and user-provided RSS feeds.
- No analytics SDK is declared in Gradle dependencies at this time.

## Release artifact

The attached release metadata indicates:

- Variant: `release`
- Output: `app/release/app-release.apk`
- Baseline profile artifacts are generated for API 28+

See `app/release/output-metadata.json` for details.

## Additional docs

- `AI.md` - architecture and implementation notes
- `TEST_README.md` - test suite details
- `docs/fdroid-listing.md` - F-Droid listing draft content

