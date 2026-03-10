package com.pod_chive.android

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.toRoute
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.pod_chive.android.model.*
import com.pod_chive.android.model.EpisodeNavType
import com.pod_chive.android.model.PodcastShow
import com.pod_chive.android.queue.PlayQueueManager
import com.pod_chive.android.ui.components.Details
import com.pod_chive.android.ui.components.Information
import com.pod_chive.android.ui.theme.PodchiveTheme
import kotlin.reflect.typeOf


class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalGlideComposeApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PodchiveTheme {
                // 1. Initialize the NavController here
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                var selectedItem by rememberSaveable { mutableStateOf(0) }
                val items = listOf("Home", "Search", "Play", "Favorites")
                val routes = listOf("home", "search", "playpod", "favorites") // Match these to your NavHost routes
                val icons = listOf(Icons.Filled.Home, Icons.Filled.Search, Icons.Filled.PlayArrow, Icons.Filled.Favorite)

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        Column {
                            if (currentRoute?.startsWith("playpod") != true) {
                                Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 4.dp) {
                                    MiniPlayerControls()
                                }
                            }

                            NavigationBar (
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                tonalElevation = 0.dp
                            ){
                                items.forEachIndexed { index, item ->
                                    NavigationBarItem(
                                        icon = { Icon(icons[index], contentDescription = item) },
                                        label = { Text(item) },
                                        alwaysShowLabel = false,
                                        selected = selectedItem == index,
                                        colors = NavigationBarItemDefaults.colors(
                                            // The pill/indicator color when an item is selected
                                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,

                                            // The appearance of items when they are NOT selected
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,

                                            // Optional: The ripple effect color
                                            disabledIconColor = MaterialTheme.colorScheme.outline,
                                            disabledTextColor = MaterialTheme.colorScheme.outline
                                        ),

                                        onClick = {
                                            selectedItem = index
                                            // 2. Navigate to the route when clicked
                                            navController.navigate(routes[index]) {
                                                // Pop up to the start destination to avoid building up a huge stack
                                                popUpTo(navController.graph.startDestinationId)
                                                launchSingleTop = true
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    // 3. Use the NavHost to handle screen swapping instead of if/else
                    Column(modifier = Modifier.padding(innerPadding)) {
                        NavHost(navController = navController, startDestination = "home") {
                            composable("home") {
                                FavoriteEpisodesScreen(navController)
                            }
                            composable("search") {
                                var searchQuery by rememberSaveable { mutableStateOf("") }
                                Column {
                                    PodSearchBar(onSearch = { searchQuery = it })
                                    if (searchQuery == "") {
                                        HomePage(navController)
                                    } else {
                                        FindPod(searchQuery, navController)

                                    }
                                }
                            }
                            composable("details/{podcastTitle}") { backStackEntry ->
                                val title = backStackEntry.arguments?.getString("podcastTitle") ?: ""
                                ShowPodDetsFromMainServer(title, navController)
                            }
                            composable<PodcastShow>{ backStackEntry ->
                                val Dets: PodcastShow = backStackEntry.toRoute()
                                ShowPodDetsFromRSS(Dets, navController  )
                            }
                            composable("favorite_episodes"){
                                FavoriteEpisodesScreen(navController)
                            }
                            composable("favorites") {
                                FavoritesScreen(navController)
                            }
                            composable("queue") {
                                com.pod_chive.android.queue.PlayQueueScreen(navController)
                            }

                            composable<Information>(typeMap = mapOf(typeOf<Episode?>() to EpisodeNavType)
                            ) { backStackEntry ->
                                val info: Information = backStackEntry.toRoute()
                                Details(info, navController)
                            }
                            composable("playpod"){
                               var pqm = PlayQueueManager(context = this@MainActivity)
                                var playingObj = pqm.getCurrentItem()
                                if (playingObj != null) {
                                    PlayPod(navController, playingObj)
                                }


                            }
                            composable<playEpisode>(typeMap = mapOf(typeOf<Episode?>() to EpisodeNavType)
                            ) {

                                val temp = it.toRoute<playEpisode>()
//                                val final = Episode(temp.Title, temp.description, temp.audioFilePath, temp.pubdate?:"", temp.transcript, temp.Creator, temp.PhotoUrl)
                                PlayPod(navController, temp.EpisodeObj!!)
                            }
//                            composable("debug_playback") {
//                                PlaybackDebugScreen(navController)
//                            }

                        }
                    }
                }
            }
        }
    }
}