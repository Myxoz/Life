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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.myxoz.life.R
import com.myxoz.life.Theme
import com.myxoz.life.android.integration.GitHub
import com.myxoz.life.screens.feed.dayoverview.edgeToEdgeGradient
import com.myxoz.life.ui.BOTTOMSEARCHBARHEIGHT
import com.myxoz.life.ui.BottomSearchBar
import com.myxoz.life.ui.rememberAsymmetricalVerticalCornerRadius
import com.myxoz.life.ui.setMaxTabletWidth
import com.myxoz.life.ui.theme.FontFamily
import com.myxoz.life.ui.theme.FontSize
import com.myxoz.life.ui.theme.OldColors
import com.myxoz.life.ui.theme.TypoStyle
import com.myxoz.life.utils.AndroidUtils
import com.myxoz.life.utils.filteredWith
import com.myxoz.life.utils.formatTimeStamp
import com.myxoz.life.utils.plural
import com.myxoz.life.utils.plus
import com.myxoz.life.utils.rippleClick
import com.myxoz.life.utils.windowPadding
import com.myxoz.life.viewmodels.CommitsViewModel

@Composable
fun FullScreenRepos(commitsViewModel: CommitsViewModel){
    val nav = LocalNavController.current
    val context = LocalContext.current
    val repos by commitsViewModel.getAllRepos.collectAsState(listOf())
    var displayedRepos by remember(repos) { mutableStateOf(repos) }
    val innerPadding = windowPadding
    Box(
        Modifier
            .fillMaxSize()
        ,
        contentAlignment = Alignment.BottomCenter
    ) {
        LazyColumn(
            Modifier
                .fillMaxWidth()
                .edgeToEdgeGradient(Theme.background, innerPadding)
            ,
            reverseLayout = true,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(Modifier.height(innerPadding.calculateBottomPadding() + BOTTOMSEARCHBARHEIGHT))
            }
            itemsIndexed(displayedRepos, {i, it -> it.commitSha}) {i, it ->
                val calendar = remember { Calendar.getInstance() }
                Item(
                    it.repoOwner+"/",
                    it.repoName,
                    "Last commit: ${it.commitDate?.formatTimeStamp(calendar)}",
                    null,
                    false,
                    i == 0,
                    i == displayedRepos.size-1,
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
                .setMaxTabletWidth()
        ) {
            BottomSearchBar(
                Theme.background,
                innerPadding.calculateBottomPadding(),
                { t -> displayedRepos = repos.filteredWith(t, { it.repoOwner }) { it.repoName } }
            )
        }
    }
}
@Composable
fun FullScreenRepo(name: String, commitsViewModel: CommitsViewModel){
    val nav = LocalNavController.current
    val context = LocalContext.current
    val allCommits by commitsViewModel.getAllCommitsFor(name).collectAsState(listOf())
    var displayedCommits by remember(allCommits) {
        mutableStateOf(allCommits)
    }
    val innerPadding = windowPadding
    Box(
        Modifier
            .fillMaxSize()
        ,
        contentAlignment = Alignment.BottomCenter
    ) {
        LazyColumn(
            Modifier
                .fillMaxWidth()
                .edgeToEdgeGradient(Theme.background, innerPadding)
            ,
            reverseLayout = true,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Spacer(Modifier.height(innerPadding.calculateBottomPadding() + BOTTOMSEARCHBARHEIGHT))
            }
            itemsIndexed(displayedCommits, {i, it -> it.commitSha}) {i, it ->
                val calendar = remember { Calendar.getInstance() }
                Item(
                    "By ${it.commitAuthor} <${it.commitEmail}>",
                    it.commitMessage?:"",
                    "${it.additions} Addition${it.additions.plural("s")} · " +
                            "${it.deletions} Deletion${it.deletions.plural("s")} · " +
                            "${it.filesChanged} File${it.filesChanged.plural("s")} changed",
                    it.commitDate?.formatTimeStamp(calendar),
                    true,
                    i == 0,
                    i == displayedCommits.size-1,
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
                .setMaxTabletWidth()
        ) {
            BottomSearchBar(
                Theme.background,
                innerPadding.calculateBottomPadding(),
                {t -> displayedCommits = allCommits.filteredWith(t, {"\n"}) {it.commitMessage?:""}}
            )
        }
    }
}
@Composable
private fun Item(subTitle: String, title: String, subsubTitle: String, date: String?, smaller: Boolean, isFirst: Boolean, isLast: Boolean, onClick: (()->Unit)? = null, onClickAction: ()->Unit){
    Row(
        Modifier
            .setMaxTabletWidth()
            .height(IntrinsicSize.Min)
            .padding(vertical = 3.dp)
            .clip(rememberAsymmetricalVerticalCornerRadius(isFirst, isLast, reverse = true))
            .background(Theme.surfaceContainerHigh)
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
                Text(subTitle, Modifier.weight(1f), style = TypoStyle(Theme.secondary, if(smaller) FontSize.SMALLM else FontSize.MEDIUM), maxLines = 1, overflow = TextOverflow.Ellipsis)
                if(date != null) Text(date, style = TypoStyle(Theme.secondary, if(smaller) FontSize.SMALLM else FontSize.MEDIUM))
            }
            Spacer(Modifier.height(5.dp))
            Text(title, style = TypoStyle(Theme.primary, if(smaller) FontSize.LARGE else FontSize.XLARGE, FontFamily.Display), maxLines = 1 + smaller, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(10.dp))
            Text(subsubTitle, style = TypoStyle(Theme.secondary, FontSize.SMALLM))
        }
        Spacer(Modifier.width(10.dp))
        VerticalDivider(color = Theme.outline)
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
            tint = OldColors.GITHUB
        )
    }
}