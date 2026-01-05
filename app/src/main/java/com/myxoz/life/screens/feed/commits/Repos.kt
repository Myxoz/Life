package com.myxoz.life.screens.feed.commits

import android.icu.util.Calendar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.myxoz.life.LocalNavController
import com.myxoz.life.LocalStorage
import com.myxoz.life.R
import com.myxoz.life.api.syncables.CommitSyncable
import com.myxoz.life.screens.feed.fullscreenevent.InputField
import com.myxoz.life.android.integration.GitHub
import com.myxoz.life.screens.feed.dayoverview.edgeToEdgeGradient
import com.myxoz.life.ui.theme.Colors
import com.myxoz.life.ui.theme.FontColor
import com.myxoz.life.ui.theme.FontFamily
import com.myxoz.life.ui.theme.FontSize
import com.myxoz.life.ui.theme.TypoStyle
import com.myxoz.life.utils.AndroidUtils
import com.myxoz.life.utils.filteredWith
import com.myxoz.life.utils.formatTimeStamp
import com.myxoz.life.utils.plural
import com.myxoz.life.utils.plus
import com.myxoz.life.utils.rippleClick
import kotlinx.coroutines.runBlocking

@Composable
fun FullScreenRepos(){
    val nav = LocalNavController.current
    val context = LocalContext.current
    val db = LocalStorage.current
    val repos = remember {
        runBlocking {
            db.commits.getAllRepos()
                .groupBy { it.repoOwner+"/"+it.repoName }.map { it.value[0] }
                    // if the last two commits have matching dates
                .map { CommitSyncable.from(it) }
                .sortedByDescending { it.commitDate }
        }
    }
    var displayedRepos by remember { mutableStateOf(repos) }
    Scaffold(
        containerColor = Colors.BACKGROUND
    ) {  innerPadding ->
        Column(
            Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LazyColumn(
                Modifier
                    .fillMaxWidth(.95f)
                    .weight(1f)
                    .edgeToEdgeGradient(Colors.BACKGROUND, innerPadding)
                ,
                reverseLayout = true
            ) {
                items(displayedRepos, {it.commitSha}) {
                    val calendar = remember { Calendar.getInstance() }
                    Item(
                        it.repoOwner+"/",
                        it.repoName,
                        "Last commit: ${it.commitDate?.formatTimeStamp(calendar)}",
                        null,
                        false,
                        {
                            nav.navigate("commits/repo/${it.repoName}")
                        }
                    ) {
                        AndroidUtils.openLink(context, GitHub.generateLinkForRepo(it))
                    }
                }
                // Due to reverse layout
                item {
                    Spacer(Modifier.height(innerPadding.calculateTopPadding()))
                }
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Colors.BACKGROUND)
                    .padding(10.dp)
                    .padding(bottom = innerPadding.calculateBottomPadding())
            )  {
                InputField(
                    null,
                    "Suchen",
                ) { t ->
                    displayedRepos = repos.filteredWith(t, {it.repoOwner}) {it.repoName}
                }
            }
        }
    }
}
@Composable
fun FullScreenRepo(name: String){
    val nav = LocalNavController.current
    val context = LocalContext.current
    val db = LocalStorage.current
    val repos = remember {
        runBlocking {
            db.commits.getAllForRepo(name).sortedByDescending { it.commitDate }
        }
    }
    var displayedCommits by remember { mutableStateOf(repos) }
    Scaffold(
        containerColor = Colors.BACKGROUND
    ) {  innerPadding ->
        Column(
            Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LazyColumn(
                Modifier
                    .fillMaxWidth(.95f)
                    .weight(1f)
                    .edgeToEdgeGradient(Colors.BACKGROUND, innerPadding)
                ,
                reverseLayout = true
            ) {
                items(displayedCommits, {it.commitSha}) {
                    val calendar = remember { Calendar.getInstance() }
                    Item(
                        "By ${it.commitAuthor} <${it.commitEmail}>",
                        it.commitMessage?:"",
                        "${it.additions} Addition${it.additions.plural("s")} · " +
                                "${it.deletions} Deletion${it.deletions.plural("s")} · " +
                                "${it.filesChanged} File${it.filesChanged.plural("s")} changed",
                        it.commitDate?.formatTimeStamp(calendar),
                        true,
                        {
                            nav.navigate("commits/commit/${it.commitSha}")
                        }
                    ) {
                        AndroidUtils.openLink(context, it.commitUrl?:"")
                    }
                }
                // Due to reverse layout
                item {
                    Spacer(Modifier.height(innerPadding.calculateTopPadding()))
                }
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Colors.BACKGROUND)
                    .padding(10.dp)
                    .padding(bottom = innerPadding.calculateBottomPadding())
            )  {
                InputField(
                    null,
                    "Suchen",
                ) { t ->
                    displayedCommits = repos.filteredWith(t, {it.repoOwner}) {it.repoName}
                }
            }
        }
    }
}
@Composable
private fun Item(subTitle: String, title: String, subsubTitle: String, date: String?, smaller: Boolean, onClick: (()->Unit)? = null, onClickAction: ()->Unit){
    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(vertical = 10.dp)
            .background(Colors.SECONDARY, RoundedCornerShape(25.dp))
            .rippleClick(onClick!=null) {
                onClick?.invoke()
            }
            .padding(horizontal = 15.dp, vertical = 10.dp)
        ,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            Modifier
                .weight(1f)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(subTitle, Modifier.weight(1f), style = TypoStyle(FontColor.SECONDARY, if(smaller) FontSize.SMALLM else FontSize.MEDIUM), maxLines = 1, overflow = TextOverflow.Ellipsis)
                if(date != null) Text(date, style = TypoStyle(FontColor.SECONDARY, if(smaller) FontSize.SMALLM else FontSize.MEDIUM))
            }
            Spacer(Modifier.height(5.dp))
            Text(title, style = TypoStyle(FontColor.PRIMARY, if(smaller) FontSize.LARGE else FontSize.XLARGE, FontFamily.Display), maxLines = 1 + smaller, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(10.dp))
            Text(subsubTitle, style = TypoStyle(FontColor.SECONDARY, FontSize.SMALLM))
        }
        Spacer(Modifier.width(10.dp))
        VerticalDivider(color = Colors.DIVIDERS)
        Icon(
            painterResource(R.drawable.github),
            "Github File",
            Modifier
                .clip(CircleShape)
                .rippleClick{
                    onClickAction()
                }
                .padding(20.dp)
                .size(30.dp)
            ,
            tint = Colors.GITHUB
        )
    }
}