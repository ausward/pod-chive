package com.pod_chive.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.pod_chive.android.HtmlText
import kotlinx.serialization.Serializable


@Serializable
data class Information(
    var Desc:String? = null,
    var Transcript:String? = null,
    var PubDate:String? = null,
    var Author:String? = null,
    var Title:String? = null
)


@Composable
fun Details(info: Information, nav: NavController){


    Box(contentAlignment = Alignment.TopStart){

        Column(){
            Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,

                ) {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
//                    Text(text = "Details", style = MaterialTheme.typography.titleLarge)
//                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()){
                Text(
                    text = info.Title ?: "",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleLarge,


                )
                }
//        }
            Column(){
                Box(contentAlignment = Alignment.Center) {

                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    Text(
                        text = info.Author ?: "",
                        color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.titleMedium,

                    )


                    Text(
                        text = info.PubDate ?: "",
                        color = MaterialTheme.colorScheme.tertiary,
                        style = MaterialTheme.typography.titleSmall,
                    )
                }

            }
            HorizontalDivider(thickness = 4.dp, color = MaterialTheme.colorScheme.tertiary)
            Text(
                    text= "Description",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.titleLarge,
            )
            val hasTranscript = !info.Transcript.isNullOrBlank()

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
                    // 2. Conditionally apply the height modifier
                    .then(if (hasTranscript) Modifier.fillMaxHeight(0.5f) else Modifier)
            ) {
                HtmlText(info.Desc ?: "")
            }
            if (info.Transcript != null && info.Transcript != "") {
                HorizontalDivider(thickness = 4.dp, color = MaterialTheme.colorScheme.tertiary)
                Text(
                    text = "Transcript",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleLarge,
                )
                Box(contentAlignment = Alignment.Center,
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant)
                        .padding(horizontal = 10.dp, vertical = 5.dp).fillMaxWidth(.5f)
                ) {

                    Text(text = info.Transcript ?: "", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

    }
}


