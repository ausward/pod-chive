package com.pod_chive.android


import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.pod_chive.android.api.Podcast
import com.pod_chive.android.api.RetrofitClient
import com.pod_chive.android.ui.theme.PodchiveTheme



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PodSearchBar(onSearch: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.weight(1f),
            singleLine = true,
            shape = RoundedCornerShape(50),
            placeholder = { Text("Search podcasts") }
        )
        Spacer(modifier = Modifier.width(8.dp))
        Button(
            onClick = { onSearch(text) },
            shape = RoundedCornerShape(50)
        ) {
            Icon(Icons.Default.Search, contentDescription = "Search")
        }
    }
}

@Composable
fun findPod(SearchString: String) {
    var podcasts by rememberSaveable { mutableStateOf<List<Podcast>>(emptyList()) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var isLoading by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(SearchString) {
        if (SearchString.isBlank()) return@LaunchedEffect
        isLoading = true
        try {
            val response = RetrofitClient.instance.searchPodcasts(term = SearchString)
            podcasts = response.results
            errorMessage = null
        } catch (e: Exception) {
            errorMessage = e.message
            Log.e("PodchiveAPI", "Error fetching podcasts", e)
        } finally {
            isLoading = false
        }
    }

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        if (isLoading) {
            Text(
                text = "Loading...",
                modifier = Modifier.padding(vertical = 8.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        } else if (errorMessage != null) {
            Text(
                text = "Error: $errorMessage",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            LazyColumn {
                items(podcasts) { podcast ->
                    PodcastItem(podcast)
                }
            }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun PodcastItem(podcast: Podcast) {
    Row(modifier = Modifier.padding(vertical = 8.dp)) {
        GlideImage(
            model = podcast.imageUrl,
            contentDescription = "Podcast artwork",
            modifier = Modifier
                .width(80.dp)
                .height(80.dp),
            loading = placeholder(R.mipmap.ic_launcher)
        )
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(
                text = podcast.title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 2
            )
            podcast.itunesAuthor?.let {
                Text(
                    text = "By $it",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
        }
    }
}
