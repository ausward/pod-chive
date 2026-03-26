package com.pod_chive.android

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.pod_chive.android.database.FavoritePodcast
import com.pod_chive.android.database.FavoritePodcastRepository
import com.pod_chive.android.model.PodcastShow
import com.pod_chive.android.ui.components.LoadingIndicator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import androidx.compose.ui.res.stringResource

@OptIn(ExperimentalGlideComposeApi::class, ExperimentalPermissionsApi::class)
@Composable
fun FavoritesScreen(navController: NavController) {
    val context = LocalContext.current
    val isWideDisplay = LocalWindowInfo.current.containerDpSize.width > 500.dp

    var favorites by remember { mutableStateOf<List<FavoritePodcast>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var gridViewOverride by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var repository: FavoritePodcastRepository
    val isGridView = gridViewOverride ?: isWideDisplay

    LaunchedEffect(Unit) {
        repository = FavoritePodcastRepository(context)
        favorites = withContext(Dispatchers.IO) {
            repository.getAllFavorites()

        }

        isLoading = false
    }


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
                    contentDescription =stringResource( R.string.back),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = stringResource(R.string.fav_podcasts),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { gridViewOverride = !(gridViewOverride ?: isWideDisplay) }) {
                Icon(
                    imageVector = if (isGridView) Icons.Filled.ViewAgenda else Icons.Filled.GridView,
                    contentDescription = stringResource( R.string.Toggle_layout),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = { navController.navigate("favorite_episodes") }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.List,
                    contentDescription = stringResource( R.string.view_all_eps),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            thickness = 1.dp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

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
                            text = stringResource(R.string.no_fav_pods),
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
                            },

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


private fun navigateToFavorite(navController: NavController, favorite: FavoritePodcast) {
    if (!favorite.feedLink.contains("pod-chive.com")) {
        try {
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
    onDelete: (FavoritePodcast) -> Unit,

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
            contentDescription = stringResource(R.string.Podcast_artwork),
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
    onDelete: (FavoritePodcast) -> Unit,

) {



    Card(modifier = Modifier.fillMaxWidth(), colors = androidx.compose.material3.CardColors(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, MaterialTheme.colorScheme.onTertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)) {
        Column(modifier = Modifier.padding(8.dp).clickable{navigateToFavorite(navController, favorite)} ){
            GlideImage(
                model = favorite.imageLocation,
                contentDescription = stringResource( R.string.Podcast_artwork),
                modifier = Modifier.aspectRatio(1f).clip(MaterialTheme.shapes.medium),
                loading = placeholder(R.mipmap.shrug)
            )
            Text(
                text = favorite.title ?: "",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
