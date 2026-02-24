# Swipe Gesture Feature for Episodes

## Overview
Added swipe gesture support to the `EpisodeRow` composable to provide quick actions for podcast episodes:
- **Swipe Right (→)**: Add episode to queue
- **Swipe Left (←)**: Play episode immediately

## Implementation Details

### File Modified
- `app/src/main/java/com/pod_chive/android/home.kt` - `EpisodeRow` function

### Architecture

#### 1. State Management
Three states control swipe behavior:

```kotlin
var swipeOffset by remember { mutableFloatStateOf(0f) }      // Visual feedback during swipe
var lastSwipeTime by remember { mutableLongStateOf(0L) }     // Debounce tracking
var swipeProcessed by remember { mutableStateOf(false) }     // Prevent multiple triggers
```

#### 2. Helper Functions
Two reusable lambda functions handle the actions:

**addToQueue**
```kotlin
val addToQueue: (String, String) -> Unit = { audioUrl, photoUrl ->
    val queueManager = com.pod_chive.android.queue.PlayQueueManager(context)
    val queueItem = com.pod_chive.android.queue.QueueItem(...)
    queueManager.addToQueue(queueItem)
    Toast.makeText(context, "Added to queue", Toast.LENGTH_SHORT).show()
}
```

**playEpisode**
```kotlin
val playEpisode: (String, String) -> Unit = { audioUrl, photoUrl ->
    if (controller != null) {
        // Create media item, play, add to queue, and navigate
        ...
    }
}
```

#### 3. Swipe Detection with Visual Feedback
Uses `pointerInput` with Press, Move, and Release events:

```kotlin
.pointerInput(Unit) {
    var swipeStartX = 0f
    var swipeStartTime = 0L
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent()
            when (event.type) {
                PointerEventType.Press -> {
                    swipeStartX = event.changes.first().position.x
                    swipeStartTime = System.currentTimeMillis()
                    swipeProcessed = false
                }
                PointerEventType.Move -> {
                    // Update visual feedback during swipe
                    val currentX = event.changes.first().position.x
                    swipeOffset = currentX - swipeStartX
                }
                PointerEventType.Release -> {
                    val endX = event.changes.first().position.x
                    val swipeDelta = endX - swipeStartX
                    val swipeDuration = System.currentTimeMillis() - swipeStartTime
                    
                    // Check thresholds and debounce
                    if (!swipeProcessed && timeSinceLastSwipe > debounceTime && swipeDuration >= minSwipeDuration) {
                        if (swipeDelta > 200f) {  // Swiped right
                            addToQueue(...)
                        } else if (swipeDelta < -200f) {  // Swiped left
                            playEpisode(...)
                        }
                    }
                }
            }
        }
    }
}
.offset(x = (swipeOffset / 10).dp)  // Visual feedback - subtle translation
```

### Key Improvements

1. **Higher Threshold**: 200dp minimum swipe distance (up from 100dp)
   - Prevents accidental triggers from short touches
   - Requires deliberate swipe gesture

2. **Debouncing**: 500ms cooldown between swipes
   - Prevents multiple actions from rapid successive swipes
   - Ensures only one action per intentional swipe

3. **Duration Check**: Minimum 100ms swipe duration
   - Distinguishes swipes from quick taps
   - Requires a genuine dragging motion

4. **Visual Feedback**: Real-time offset during swipe
   - Row translates 1/10 of swipe distance while user drags
   - Clear indication that swipe is being detected
   - Smooth animation gives user confidence

5. **Toast Notification**: "Added to queue" confirmation
   - Clear feedback when action is triggered

### Button Interactions
The existing buttons remain fully functional:
- **+ Icon** (right side): Add to queue (or swipe right)
- **Play Icon** (right side): Play episode (or swipe left)
- **Text Area**: Click to view full description

### Imports Added
```kotlin
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
```

## Swipe Detection Logic

### Valid Swipe Criteria
All conditions must be met for action to trigger:
- ✅ `swipeDelta > 200f` (right) or `swipeDelta < -200f` (left)
- ✅ `swipeDuration >= 100ms` (minimum drag time)
- ✅ `timeSinceLastSwipe > 500ms` (debounce cooldown)
- ✅ `!swipeProcessed` (action not already triggered)

### Swipe Flow
1. **Press**: Record start position, time, reset processed flag
2. **Move**: Update visual offset (1/10 of drag distance)
3. **Release**: Check all thresholds, trigger action if valid, reset offset

## Testing

### Test Cases
1. **Quick Tap**: Should NOT trigger (duration < 100ms)
2. **Short Swipe**: Should NOT trigger (delta < 200dp)
3. **Deliberate Right Swipe**: Should add to queue with visual feedback
4. **Deliberate Left Swipe**: Should play and navigate to PlayPod
5. **Rapid Successive Swipes**: Should only trigger once (debounce)
6. **Button Click**: Existing buttons should work as before
7. **Description Click**: Tapping episode info should show description dialog
8. **Visual Feedback**: Row should subtly move while swiping

### Expected Behavior
- Only genuine swipes (>200dp, >100ms) trigger actions
- Visual translation during swipe provides clear feedback
- 500ms delay between actions prevents accidental double-triggers
- Toast confirms "Added to queue"
- Existing button interactions remain unchanged

## Future Enhancements
- Haptic feedback (vibration) on successful swipe
- Animated visual indicator showing swipe direction
- Sound effect on swipe completion
- Configurable swipe threshold per user preference
- Additional swipe directions for more actions (up/down)


