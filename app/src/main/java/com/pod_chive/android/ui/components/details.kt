package com.pod_chive.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.pod_chive.android.HtmlText
import com.pod_chive.android.R
import com.pod_chive.android.model.Episode
import kotlinx.serialization.Serializable
import java.net.URL


@Serializable
data class Information(
      var Description :String? = null,//= desc
      var TranscriptUrl :String? = null,//= transcript
      var PublishDate :String? = null,//= pubdate?:""
      var Creator :String? = null,//= creator
      var EpisodeName :String? = null,
      var episode: Episode? = null
)


@Composable
fun Details(info: Information, nav: NavController){

    var TranscriptData: String? = null

    if (info.TranscriptUrl != null && info.TranscriptUrl!!.contains("Http*", true)){
        TranscriptData = URL(info.TranscriptUrl!!).readText()
    } else {
        TranscriptData = info.TranscriptUrl
    }


    Box(contentAlignment = Alignment.TopStart){

        Column(){
            Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,

                ) {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
//                    Text(text = "Details", style = MaterialTheme.typography.titleLarge)
//                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()){
                Text(
                    text = info.EpisodeName ?: "",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleLarge,


                )
                }
//        }
            Column(){
                Box(contentAlignment = Alignment.Center) {

                }
                Column (modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = info.Creator ?: "",
                        color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.titleMedium,

                    )

                    Spacer(modifier = Modifier.width(10.dp))


                    Text(
                        text = "${stringResource(R.string.pubdate)}: ${info.PublishDate}" ?: "",
                        color = MaterialTheme.colorScheme.tertiary,
                        style = MaterialTheme.typography.titleSmall,
                    )
                }

            }
            HorizontalDivider(thickness = 4.dp, color = MaterialTheme.colorScheme.tertiary)
            Text(
                    text= stringResource(R.string.desc),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.titleLarge,
            )
            val hasTranscript = !TranscriptData.isNullOrBlank()

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
                    // 2. Conditionally apply the height modifier
                    .then(if (hasTranscript) Modifier.fillMaxHeight(0.5f) else Modifier)
            ) {
                HtmlText(info.Description ?: "")
            }
            if (TranscriptData != null && TranscriptData != "") {
                HorizontalDivider(thickness = 4.dp, color = MaterialTheme.colorScheme.tertiary)
                Text(
                    text = stringResource(R.string.transcript),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleLarge,
                )
                Box(contentAlignment = Alignment.Center,
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant)
                        .padding(horizontal = 10.dp, vertical = 5.dp).fillMaxWidth(.5f)
                ) {

                    Text(text = TranscriptData ?: "", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

    }
}


