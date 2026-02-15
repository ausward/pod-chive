package com.pod_chive.android

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.pod_chive.android.api.RetrofitClient
import com.pod_chive.android.api.homeItem


@Composable
fun HomePage() {
    var isloading by rememberSaveable { mutableStateOf(false) }
    var podcasts by rememberSaveable { mutableStateOf<List<homeItem>>(emptyList()) }
    var error: String = ""


    LaunchedEffect(key1 = true) {
        isloading = true
        try {
            val res = RetrofitClient.instance.listPodcasts()

            podcasts = res.podcasts
        } catch (e: Exception) {
            error = e.message.toString()
        } finally {
            isloading = false
        }
    }
    Column() {
        if (isloading) {
            Text(
                text = "Loading...",
                style = MaterialTheme.typography.titleLarge,
                color = Color.Red
            )
        } else if (error != "") {
            Text(
                text = error,
                style = MaterialTheme.typography.titleLarge,
                color = Color.Red
            )

        } else {
            LazyColumn() {
                items(podcasts) { podcast ->
                    MainPodListExpander(podcast)
                    HorizontalDivider(Modifier, thickness = 2.dp, color = Color.LightGray)


                }
            }


        }
    }


}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun MainPodListExpander(podcast: homeItem){
    val photoURL = "https://pod-chive.com/" + podcast.output_directory + "/cover.webp"
    Row(modifier = Modifier.padding(vertical = 2.dp).background(Color.hsl(251f, .96f, .06f)).clip(MaterialTheme.shapes.medium)) {
        GlideImage(
            model = photoURL,
            contentDescription = "Podcast Album Cover",
            modifier = Modifier.width(80.dp).clip(MaterialTheme.shapes.medium)
                .height(80.dp).align(Alignment.CenterVertically),
            loading = placeholder(R.mipmap.ic_launcher),
            failure = placeholder(R.mipmap.ic_launcher),

            )
        Column(modifier = Modifier.padding(horizontal = 8.dp)) {
            Text(
                text = podcast.podcast_title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 1
            )
            HorizontalDivider(color = Color.LightGray, thickness = 4.dp)
            Text(
                text = podcast.description,
                fontSize = 12.sp,
                maxLines = 4,
                lineHeight = 14.sp
            )
        }
    }
}