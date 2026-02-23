package com.pod_chive.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.toRoute
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.pod_chive.android.api.PodcastDetailResponse
import com.pod_chive.android.api.RssFeedResult
import com.pod_chive.android.api.homeItem
import com.pod_chive.android.ui.theme.PodchiveTheme


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

                            NavigationBar {
                                items.forEachIndexed { index, item ->
                                    NavigationBarItem(
                                        icon = { Icon(icons[index], contentDescription = item) },
                                        label = { Text(item) },
                                        selected = selectedItem == index,
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
                                HomePage(navController)
                            }
                            composable("search") {
                                var searchQuery by rememberSaveable { mutableStateOf("") }
                                Column {
                                    PodSearchBar(onSearch = { searchQuery = it })
                                    if (searchQuery == "") {
                                        HomePage(navController)
                                    } else {
                                        findPod(searchQuery, navController)

                                    }
                                }
                            }
                            composable(
                                route = "playpod?audioUrl={audioUrl}&title={title}&photoUrl={photoUrl}&creator={creator}",
                                arguments = listOf(
                                    navArgument("audioUrl") { type = NavType.StringType; nullable = true; defaultValue = null },
                                    navArgument("title") { type = NavType.StringType; nullable = true; defaultValue = null },
                                    navArgument("photoUrl") { type = NavType.StringType; nullable = true; defaultValue = null },
                                    navArgument("creator") { type = NavType.StringType; nullable = true; defaultValue = null }
                                )
                            ) { backStackEntry ->
                                val audioUrl = backStackEntry.arguments?.getString("audioUrl")
                                val title = backStackEntry.arguments?.getString("title")
                                val photoUrl = backStackEntry.arguments?.getString("photoUrl")
                                val creator = backStackEntry.arguments?.getString("creator")
                                PlayPod(
                                    navController = navController,
                                    audioUrl = audioUrl,
                                    title = title,
                                    photoUrl = photoUrl,
                                    creator = creator
                                )
                            }
                            composable("details/{podcastTitle}") { backStackEntry ->
                                val title = backStackEntry.arguments?.getString("podcastTitle") ?: ""
                                showPodDetsFromMainServer(title, navController)
                            }
                            composable<homeItem>{ backStackEntry ->

                                val Dets: homeItem = backStackEntry.toRoute()

                                showPodDetsFromRSS(Dets, navController  )



                            }
                            composable("favorites") {
                                FavoritesScreen(navController)
                            }
                            composable("queue") {
                                com.pod_chive.android.queue.PlayQueueScreen(navController)
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