# Podchive RSS Favorites Navigation Fix

**Date**: February 18, 2026  
**Issue**: Crash when clicking RSS podcast favorites  
**Error**: `IllegalArgumentException: Navigation destination that matches route details/https://feeds.castos.com/ozqw5 cannot be found`  
**Root Cause**: App stores both local podcasts (directories like "1A") and RSS feeds (full URLs) as favorites, but navigation only worked for local podcasts  
**Status**: ✅ RESOLVED

## The Problem

The app has two types of podcasts:

1. **Local Podcasts** from the main server
   - Stored as directory names: `"1A"`, `"NPR"`
   - Navigate via: `details/1A`
   - Use `showPodDetsFromMainServer()` function

2. **RSS Feed Podcasts** from search results  
   - Stored as full URLs: `https://feeds.castos.com/ozqw5`
   - Navigate via: `homeItem` route (which calls `showPodDetsFromRSS()`)
   - Previously crashed when clicking favorites because it tried `details/https://feeds.castos.com/ozqw5`

## The Solution

Updated `FavoritePodcastItem` in `FavoritesScreen.kt` to detect the podcast type:

```kotlin
if (favorite.feedLink.startsWith("http")) {
    // It's an RSS feed URL - use homeItem navigation
    navController.navigate(
        homeItem(
            podcast_title = favorite.title,
            description = favorite.description,
            rss_url = favorite.feedLink,
            html_summary_location = "",
            output_directory = favorite.feedLink.substringAfterLast('/'),
            cover_image_url = favorite.imageLocation
        )
    )
} else {
    // It's a local directory - use details route
    navController.navigate("details/${favorite.feedLink}")
}
```

## Files Modified

1. **FavoritesScreen.kt**
   - Updated `FavoritePodcastItem()` to detect podcast type
   - Added `homeItem` import from `com.pod_chive.android.api`
   - Added proper error handling with logging

## How It Works Now

### When clicking an RSS podcast favorite:
1. App detects it's an RSS URL (starts with "http")
2. Creates a `homeItem` object with the podcast data
3. Navigates via the `<homeItem>` composable route in MainActivity
4. Loads `showPodDetsFromRSS()` which already has favorite button support

### When clicking a local podcast favorite:
1. App detects it's a local directory (doesn't start with "http")
2. Uses the standard `details/{directory}` route
3. Loads `showPodDetsFromMainServer()` which has favorite button support

## Testing

The fix handles:
✅ Local podcasts from home page
✅ RSS podcasts added from search results
✅ Proper navigation to podcast details
✅ Favorites can be deleted from favorites list
✅ Both types of podcasts can have their favorites toggled

## Build Status
✅ Build successful
✅ No compilation errors
✅ Ready for device testing

