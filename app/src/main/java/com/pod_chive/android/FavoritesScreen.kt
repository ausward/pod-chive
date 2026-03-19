package com.pod_chive.android

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.navigation.NavController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.pod_chive.android.api.EpisodeDC
import com.pod_chive.android.database.FavoritePodcast
import com.pod_chive.android.database.FavoritePodcastRepository
import com.pod_chive.android.model.PodcastShow
import com.pod_chive.android.notif.PodchiveNotificationManager
import com.pod_chive.android.ui.components.LoadingIndicator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

@OptIn(ExperimentalGlideComposeApi::class, ExperimentalPermissionsApi::class)
@Composable
fun FavoritesScreen(navController: NavController) {
    val context = LocalContext.current
    val isWideDisplay = LocalWindowInfo.current.containerDpSize.width > 500.dp

    var favorites by remember { mutableStateOf<List<FavoritePodcast>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var gridViewOverride by rememberSaveable { mutableStateOf<Boolean?>(null) }

    val isGridView = gridViewOverride ?: isWideDisplay

    LaunchedEffect(Unit) {
        val repository = FavoritePodcastRepository(context)
        favorites = withContext(Dispatchers.IO) {
            repository.getAllFavorites()
        }
        isLoading = false
    }

    val permissionState = rememberPermissionState(
        permission = Manifest.permission.POST_NOTIFICATIONS)





    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "Favorite Podcasts",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { gridViewOverride = !(gridViewOverride ?: isWideDisplay) }) {
                Icon(
                    imageVector = if (isGridView) Icons.Filled.ViewAgenda else Icons.Filled.GridView,
                    contentDescription = if (isGridView) "Switch to list view" else "Switch to grid view",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = { navController.navigate("favorite_episodes") }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.List,
                    contentDescription = "View all episodes",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            thickness = 1.dp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        if (ActivityCompat.checkSelfPermission(
                LocalContext.current,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            NotificationPermissionHandler()
            return
        } else{
            var notificationManager = PodchiveNotificationManager(context)
            notificationManager.notifyNewEpisode(FavoritePodcast(0, "","","","TEST"), EpisodeDC("","", "https://pod-chive.com/Darknet_Diaries/100_NSO.mp3","", "","Darknet_Diaries","https://pod-chive.com/Darknet_Diaries/cover.webp"))

        }
        when {
            isLoading -> {
                LoadingIndicator()
            }
            favorites.isEmpty() -> {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "No favorite podcasts yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }



            isGridView -> {








                var gridsize = max(((LocalWindowInfo.current.containerDpSize.width / 150.dp).toInt()),3)

                LazyVerticalGrid(
                    columns = GridCells.Fixed(gridsize),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(favorites) { favorite ->

                        FavoritePodcastGridItem(
                            favorite = favorite,
                            navController = navController,
                            onDelete = { deletedFavorite ->
                                favorites = favorites.filter { it.id != deletedFavorite.id }
                            }
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(favorites) { favorite ->
                        FavoritePodcastItem(
                            favorite = favorite,
                            navController = navController,
                            onDelete = { deletedFavorite ->
                                favorites = favorites.filter { it.id != deletedFavorite.id }
                            }
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            thickness = 1.dp
                        )
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun NotificationPermissionHandler() {
    // Notification permission is only required for Android 13 (Tiramisu) and above
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionState = rememberPermissionState(
            permission = Manifest.permission.POST_NOTIFICATIONS
        )

        if (!permissionState.status.isGranted) {
            if (permissionState.status.shouldShowRationale) {
                // Show a custom UI explaining WHY you need notifications
                // before calling launchPermissionRequest()
                RationaleDialog(
                    onConfirm = { permissionState.launchPermissionRequest() },
                    onDismiss = { }
                )
            } else {
                // Request the permission directly
                SideEffect {
                    permissionState.launchPermissionRequest()
                }
            }
        }
    }
}

@Composable
fun RationaleDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Stay in the loop")
        },
        text = {
            Text(
                "We use notifications to keep you updated on your task progress " +
                        "and send important alerts. Would you like to turn them on?"
            )
        },
        icon = {
            Icon(Icons.Default.Notifications, contentDescription = null)
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Allow")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Not Now")
            }
        }
    )
}
private fun navigateToFavorite(navController: NavController, favorite: FavoritePodcast) {
    if (!favorite.feedLink.contains("pod-chive.com")) {
        try {
            Log.e("FavoritesScreen", "Navigating to RSS feed: ${favorite.feedLink}")
            navController.navigate(
                PodcastShow(
                    favorite.title,
                    favorite.showDescription ?: "",
                    favorite.feedLink,
                    favorite.feedLink.substringAfterLast('/'),
                    favorite.imageLocation
                )
            )
        } catch (e: Exception) {
            Log.e("FavoritesScreen", "Navigation error: ${e.message}")
        }
    } else {
        Log.e("FavoritesScreen", "Navigating to local podcast: ${favorite.feedLink}")
        navController.navigate("details/${favorite.feedLink.slice(22..<favorite.feedLink.length - 9)}")
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun FavoritePodcastItem(
    favorite: FavoritePodcast,
    navController: NavController,
    onDelete: (FavoritePodcast) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { navigateToFavorite(navController, favorite) }
            .padding(vertical = 8.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlideImage(
            model = favorite.imageLocation,
            contentDescription = "Podcast artwork",
            modifier = Modifier
                .width(80.dp)
                .height(80.dp)
                .clip(MaterialTheme.shapes.medium),
            loading = placeholder(R.drawable.confused_chive),
            failure = placeholder(R.drawable.sad_chive),
            contentScale = ContentScale.Inside
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        ) {
            Text(
                text = favorite.title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = favorite.showDescription ?: "",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun FavoritePodcastGridItem(
    favorite: FavoritePodcast,
    navController: NavController,
    onDelete: (FavoritePodcast) -> Unit
) {
    var photoSize by remember { mutableStateOf(125.dp) }
    var cardSize by remember { mutableStateOf(225.dp) }

    Card(shape = MaterialTheme.shapes.medium,
        modifier = Modifier.width(photoSize).height(cardSize),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(360.dp).height(360.dp)
                .clip(MaterialTheme.shapes.medium)
                .clickable { navigateToFavorite(navController, favorite) }
                .padding(8.dp)
        ) {
            GlideImage(
                model = favorite.imageLocation,
                contentDescription = "Podcast artwork",
                modifier = Modifier
                    .width(150.dp)
                    .height(150.dp)
                    .clip(MaterialTheme.shapes.large).align(Alignment.CenterHorizontally),
                loading = placeholder(R.drawable.confused_chive),
                failure = placeholder(R.drawable.sad_chive),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                autoSize = TextAutoSize.StepBased(minFontSize = 12.sp, 18.sp),
                textAlign = TextAlign.Center,
                text = favorite.title,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,

            )
        }
    }
}
