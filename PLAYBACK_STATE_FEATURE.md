# Playback State Management & Resume Feature

**Date**: February 18, 2026  
**Feature**: Save and Resume Playback Position  
**Status**: ✅ IMPLEMENTED

## Overview

The playback state management system allows users to:
- Automatically save the playback position of episodes when paused or stopped
- Resume episodes from their saved position when replayed
- Track playback history and recently played episodes
- Persist playback state across app restarts

## How It Works

### Saving Playback State

When a user pauses or stops an episode, or when the app is destroyed, the current playback state is automatically saved:

1. **Audio URL** - Used as unique identifier
2. **Current Position** - How far through the episode (in milliseconds)
3. **Duration** - Total length of episode
4. **Title, Creator, PhotoUrl** - Episode metadata
5. **Last Played At** - Timestamp for sorting recent episodes

### Resuming Playback

When an episode is played again:

1. App checks if there's a saved playback state
2. If saved position exists and is reasonable (not at the end), it seeks to that position
3. Playback continues from where the user left off
4. Only resumes if position is less than 95% through the episode (prevents re-playing already finished episodes)

### Queue vs. Non-Queue Episodes

**Queue Episodes:**
- Automatically play next episode when current finishes
- Removed from queue when finished
- Can be manually removed from queue

**Non-Queue Episodes:**
- Play standalone
- Playback position is saved
- Can be resumed later

## Architecture

### PlaybackStateManager.kt

**Location**: `app/src/main/java/com/pod_chive/android/playback/`

**Key Methods:**

```kotlin
// Save playback state
fun savePlaybackState(state: PlaybackState)

// Get specific episode's saved state
fun getPlaybackState(audioUrl: String): PlaybackState?

// Get all saved states
fun getAllPlaybackStates(): Map<String, PlaybackState>

// Get recently played episodes
fun getRecentlyPlayed(limit: Int = 10): List<PlaybackState>

// Remove specific playback state
fun removePlaybackState(audioUrl: String)

// Clear all saved states
fun clearAllPlaybackStates()
```

### PlaybackState Data Model

```kotlin
@Serializable
data class PlaybackState(
    val audioUrl: String,              // Unique identifier
    val title: String,                 // Episode title
    val creator: String,               // Podcast name
    val photoUrl: String,              // Artwork URL
    val currentPosition: Long,          // Current playback position (ms)
    val duration: Long,                // Total duration (ms)
    val lastPlayedAt: Long             // When it was last played
)
```

## Integration

### PlaybackService

The service automatically:
1. **Saves state when paused** - `onIsPlayingChanged(false)`
2. **Saves state when ready** - `onPlaybackStateChanged(STATE_READY)`
3. **Saves state on destroy** - Final state saved before service stops

```kotlin
override fun onIsPlayingChanged(isPlaying: Boolean) {
    if (!isPlaying) {
        savePlaybackState(...)
    }
}
```

### PlayPod Screen

When an episode loads:
1. Connects to MediaController
2. Retrieves saved playback state
3. Seeks to saved position if available and reasonable
4. Continues playback

```kotlin
val savedState = playbackStateManager.getPlaybackState(audioUrl)
if (savedState != null && savedState.currentPosition > 0) {
    if (savedState.currentPosition < savedState.duration * 0.95) {
        controller.seekTo(savedState.currentPosition)
    }
}
```

## Data Persistence

**Storage Method**: SharedPreferences with JSON serialization
**Preferences File**: `"podchive_playback"`
**Key**: `"playback_states"`
**Format**: Map of audioUrl -> PlaybackState

## Features

### ✅ Implemented

- Save playback position automatically
- Resume from saved position
- Save on pause/stop
- Save on app destroy
- Persist across app restarts
- Filter out episodes near the end (95%+)
- Track last played time
- Support for non-queue episodes

### 🎯 Future Enhancements

- Resume queue automatically from last episode
- Recently played episodes screen
- Play history view with timestamps
- Clear old history (older than 30 days)
- Backup playback history
- Sync playback state across devices

## Usage Flow

### Scenario 1: Pause and Resume Later

1. User plays episode "Episode 101"
2. Listens to 30 minutes
3. Pauses the episode
4. **PlaybackState saved**: position = 30min, duration = 45min
5. User closes app
6. Next day, user plays "Episode 101" again
7. **Automatic resume**: Playback starts at 30 minutes

### Scenario 2: Queue Episode Auto-Continues

1. User adds 3 episodes to queue
2. Plays first episode
3. Episode finishes
4. **First episode removed from queue**
5. **Second episode automatically plays**
6. Continues through queue

### Scenario 3: Skip Finished Episodes

1. User played episode weeks ago
2. User plays it again
3. App checks saved state: position = 41min, duration = 42min (97%)
4. **Episode not resumed** (too near the end)
5. Playback starts from beginning

## Log Output

The system logs important events:

```
D/PLAYBACK: Saved playback state: Episode Title at 1800000ms
D/PLAYBACK: Service destroyed, saved final state: Episode Title
D/PLAYBACK: Restored playback position: 1800000ms / 2700000ms
D/QUEUE: Playing next in queue: Next Episode Title
D/QUEUE: No more items in queue
```

## Testing

### Manual Test Cases

1. **Save on Pause**
   - Play an episode
   - Pause at 50% mark
   - Close app
   - Reopen, play same episode
   - Should resume at ~50%

2. **Save on Destroy**
   - Play an episode
   - Let it play for 1 minute
   - Force close app
   - Reopen, play same episode
   - Should resume at ~1 minute

3. **Queue Auto-Play**
   - Add 2 episodes to queue
   - Play first, let it finish
   - Second should auto-play

4. **Skip Finished**
   - Play episode to 98% completion
   - Close and reopen
   - Play same episode
   - Should start from beginning (not skip to end)

## Files

### Created
- `app/src/main/java/com/pod_chive/android/playback/PlaybackStateManager.kt`

### Modified
- `app/src/main/java/com/pod_chive/android/PlaybackService.kt`
- `app/src/main/java/com/pod_chive/android/playPod.kt`

## Performance Considerations

- JSON serialization happens on-demand (not constantly)
- SharedPreferences is fast for small data sets
- Serialization to Map for O(1) lookups by audioUrl
- No memory leaks: states stored in SharedPreferences, not in-memory

## Security & Privacy

- Playback states stored locally on device only
- No data sent to servers
- Clearing app data removes all playback states
- No personal information collected beyond audio URLs

## Compatibility

- Works with all episode types (local and RSS)
- Works with queue and non-queue episodes
- Compatible with existing MediaController
- No breaking changes to existing APIs

## Summary

✅ Automatic playback position saving  
✅ Resume from saved position  
✅ Queue auto-advance  
✅ Persist across app restarts  
✅ Smart filtering for finished episodes  
✅ Comprehensive logging  

The playback state management system is now fully integrated and ready for production use!

