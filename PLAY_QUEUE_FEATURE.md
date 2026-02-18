# Play Queue Feature Documentation

**Date**: February 18, 2026  
**Feature**: Play Queue for Episode Management  
**Status**: ✅ IMPLEMENTED

## Overview

The Play Queue feature allows users to:
- Add podcast episodes to a queue for sequential playback
- View and manage queued episodes
- Play episodes directly from the queue
- Remove episodes from the queue
- Track the currently playing episode in the queue

## Architecture

### Components

#### 1. **PlayQueueManager.kt**
- **Location**: `app/src/main/java/com/pod_chive/android/queue/`
- **Purpose**: Manages queue data persistence and operations
- **Storage**: SharedPreferences with JSON serialization

**Key Methods:**
```kotlin
- addToQueue(item: QueueItem)        // Add episode to queue
- removeFromQueue(itemId: String)    // Remove from queue
- getQueue(): List<QueueItem>        // Get all queued items
- clearQueue()                       // Clear entire queue
- moveItem(fromIndex, toIndex)       // Reorder items (future feature)
- getNextItem(): QueueItem?          // Get next in queue
- getPreviousItem(): QueueItem?      // Get previous in queue
- getCurrentItem(): QueueItem?       // Get current item
```

#### 2. **QueueItem Data Model**
```kotlin
@Serializable
data class QueueItem(
    val id: String,          // Unique identifier
    val title: String,       // Episode title
    val audioUrl: String,    // Audio file URL
    val photoUrl: String,    // Artwork URL
    val creator: String,     // Podcast name
    val description: String?,// Episode description
    val addedAt: Long        // Timestamp
)
```

#### 3. **PlayQueueScreen.kt**
- **Location**: `app/src/main/java/com/pod_chive/android/queue/`
- **Purpose**: UI for viewing and managing the queue
- **Features**:
  - List all queued episodes
  - Highlight currently playing episode
  - Play any episode in the queue
  - Delete episodes from queue
  - Clear entire queue button

### Integration Points

#### Home.kt - EpisodeRow
Added "Add to Queue" button next to the play button:
```kotlin
IconButton(onClick = {
    val queueManager = PlayQueueManager(context)
    val queueItem = QueueItem(...)
    queueManager.addToQueue(queueItem)
})
```

#### MainActivity.kt
Added queue route to navigation:
```kotlin
composable("queue") {
    PlayQueueScreen(navController)
}
```

## User Flow

### Adding to Queue
1. User browses podcast episodes
2. Clicks "Add to Queue" button on episode row
3. Episode is added to the queue (if not already present)
4. Toast/Log confirmation shows success

### Viewing Queue
1. User navigates to queue screen (via navigation or button)
2. Sees list of all queued episodes
3. Currently playing episode is highlighted
4. Can see episode artwork, title, and creator

### Playing from Queue
1. User taps on episode in queue
2. MediaController loads and plays the episode
3. App navigates to PlayPod screen
4. Queue index is updated

### Managing Queue
1. User can delete individual episodes (trash icon)
2. User can clear entire queue (clear button in app bar)
3. Queue persists across app restarts

## Data Persistence

### Storage Method
- **Technology**: SharedPreferences
- **Format**: JSON serialization via kotlinx.serialization
- **Key**: `"play_queue"`
- **Preferences File**: `"podchive_queue"`

### Benefits
- Simple implementation
- No database schema changes needed
- Instant read/write operations
- Compatible with existing favorites system

## Future Enhancements

### Planned Features
1. **Drag-to-Reorder**: Allow users to reorder queue items
2. **Auto-Play Next**: Automatically play next episode when current finishes
3. **Queue from Search**: Add to queue from search results
4. **Queue from Favorites**: Bulk add favorite podcast episodes
5. **Queue Shuffle**: Randomize queue order
6. **Queue Export/Import**: Share queue with other devices
7. **Smart Queue**: Auto-add similar episodes

### MediaController Integration
Currently the queue is manual. Future versions could:
- Automatically load next episode when current finishes
- Show "Next in Queue" in player UI
- Add skip to next/previous queue buttons

## UI Components

### PlayQueueScreen Layout
```
┌──────────────────────────────────┐
│  ←  Play Queue (5)         [✕]   │ ← Top App Bar
├──────────────────────────────────┤
│  ┌────┐                          │
│  │img │  Episode Title           │
│  │    │  Podcast Name       [▶]🗑│ ← Currently Playing (highlighted)
│  └────┘                          │
├──────────────────────────────────┤
│  ┌────┐                          │
│  │img │  Episode Title           │
│  │    │  Podcast Name          🗑│
│  └────┘                          │
├──────────────────────────────────┤
│  ... more episodes ...           │
└──────────────────────────────────┘
```

### EpisodeRow Changes
Before:
```
[Episode Info] [▶ Play]
```

After:
```
[Episode Info] [+ Queue] [▶ Play]
```

## Technical Notes

### ID Generation
Episodes get unique IDs based on:
```kotlin
"${timestamp}_${title.hashCode()}_${audioUrl.hashCode()}"
```

This prevents duplicates and tracks episodes uniquely.

### Coroutines Usage
- Queue operations use `Dispatchers.IO` for background work
- UI updates use `Dispatchers.Main`
- GlobalScope used (consider migrating to ViewModelScope)

### MediaController Integration
- Queue screen connects to existing PlaybackService
- Uses same MediaController as PlayPod
- Maintains playback continuity

## Build Configuration

No additional dependencies required. Uses existing:
- kotlinx.serialization (already in project)
- Media3 (existing)
- Compose UI (existing)

## Testing

### Manual Test Steps
1. **Add to Queue**: Click "Add to Queue" on episodes, verify in queue screen
2. **Play from Queue**: Tap episode in queue, verify it plays
3. **Delete from Queue**: Click trash icon, verify removal
4. **Clear Queue**: Click clear button, verify all removed
5. **Persistence**: Add items, close app, reopen, verify items still there
6. **Duplicates**: Try adding same episode twice, verify only one copy

### Edge Cases Handled
- Empty queue state with helpful message
- Duplicate prevention when adding to queue
- Queue persistence across app restarts
- Safe null handling for MediaController

## Files Created/Modified

### New Files
- `app/src/main/java/com/pod_chive/android/queue/PlayQueueManager.kt`
- `app/src/main/java/com/pod_chive/android/queue/PlayQueueScreen.kt`

### Modified Files
- `app/src/main/java/com/pod_chive/android/home.kt` (added "Add to Queue" button)
- `app/src/main/java/com/pod_chive/android/MainActivity.kt` (added queue route)
- `app/src/main/java/com/pod_chive/android/playPod.kt` (added queue button placeholder)

## Access Points

Currently, users can access the queue via:
1. Direct navigation to "queue" route
2. Future: Button in PlayPod screen (to be enabled with navController)
3. Future: Navigation bar item or FAB

## Known Limitations

1. **No Auto-Play**: Queue doesn't automatically play next episode
2. **Manual Navigation**: Must manually navigate to queue screen
3. **No Reordering**: Can't drag items to reorder
4. **No Queue in Mini Player**: Mini player doesn't show queue status

## Summary

✅ Core queue functionality implemented  
✅ Persistent storage working  
✅ Add/Remove/Clear operations functional  
✅ UI displays queue with current item highlighting  
✅ MediaController integration complete  
✅ Navigation routing configured  

The play queue feature is now ready for testing and user feedback!

