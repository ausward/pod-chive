# F-Droid Listing Draft: pod-chive

Use this document as source text when preparing F-Droid metadata.

## Core app info

- Name: `pod-chive`
- Application ID: `com.pod_chive.android`
- Current version name: `1.0`
- Current version code: `1`
- Min SDK: `33`
- Source repo: `<add repository URL>`
- Issue tracker: `<add issue tracker URL>`
- License: `<add SPDX license id>`

## Suggested short description

Podcast player for streaming, searching, and following favorite shows.

## Full description

pod-chive is a podcast player focused on finding and listening to shows from the pod-chive catalog or directly from RSS feeds.

Features include:

- Discover podcasts from the built-in catalog
- Search by keyword
- Open podcasts by RSS URL
- Queue episodes and control playback speed/seek
- Save favorite podcasts locally
- Optional notifications for new favorite episodes
- Resume playback from your saved position

The app is built with Kotlin and Jetpack Compose, and uses Android Media3 for playback.

## Categories (suggested)

- Multimedia
- Audio

## Permissions and rationale

- `INTERNET`: Required to load podcast feeds, metadata, and episode audio URLs.
- `FOREGROUND_SERVICE`: Required for uninterrupted playback while app is backgrounded.
- `FOREGROUND_SERVICE_MEDIA_PLAYBACK`: Declares media playback foreground service type.
- `POST_NOTIFICATIONS`: Used for optional new-episode alerts from favorites.
- `RECEIVE_BOOT_COMPLETED`: Re-schedules periodic favorite sync after reboot.

## Data safety / privacy notes

- Favorites and playback state are stored locally in app storage.
- App connects to pod-chive endpoints and RSS feeds entered by the user.
- No analytics dependency is currently declared in `app/build.gradle.kts`.

## Build notes for maintainers

- Gradle module: `:app`
- Build command: `./gradlew :app:assembleRelease`
- Output metadata path: `app/release/output-metadata.json`
- F-Droid metadata file in this repo: `metadata/com.pod_chive.android.yml`

## Screenshots and assets checklist

- [ ] App icon (512x512)
- [ ] At least 2 phone screenshots
- [ ] Optional feature graphic
- [ ] Localized title/summary/description (if needed)

## F-Droid metadata starter (YAML-style fields)

```yaml
Categories:
  - Multimedia
License: <SPDX>
AuthorName: <author/org>
SourceCode: <repo url>
IssueTracker: <issues url>
Changelog: <releases url>
Summary: Podcast player for streaming, searching, and following favorite shows.
Description: |
  pod-chive is a podcast player focused on finding and listening to shows
  from the pod-chive catalog or directly from RSS feeds.

  Features:
  * Discover podcasts from the built-in catalog
  * Search by keyword
  * Open podcasts by RSS URL
  * Queue episodes and control playback speed/seek
  * Save favorite podcasts locally
  * Optional notifications for new favorite episodes
  * Resume playback from saved position

RepoType: git
Repo: <repo clone url>
Builds:
  - versionName: 1.0
    versionCode: 1
    commit: <tag-or-commit>
    subdir: app
    gradle:
      - yes
```

## Notes to finalize before submission

- Replace all `<...>` placeholders.
- Confirm the project license and add `LICENSE` file in repo root.
- Confirm repository URLs and changelog location.
- Validate metadata against the exact branch/tag submitted to F-Droid.
