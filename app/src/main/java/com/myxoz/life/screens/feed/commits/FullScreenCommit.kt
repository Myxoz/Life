package com.myxoz.life.screens.feed.commits

import android.icu.util.Calendar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.myxoz.life.LocalNavController
import com.myxoz.life.LocalScreens
import com.myxoz.life.R
import com.myxoz.life.Theme
import com.myxoz.life.android.integration.GitHub
import com.myxoz.life.screens.NavPath
import com.myxoz.life.screens.feed.dayoverview.edgeToEdgeGradient
import com.myxoz.life.ui.setMaxTabletWidth
import com.myxoz.life.ui.theme.FontFamily
import com.myxoz.life.ui.theme.FontSize
import com.myxoz.life.ui.theme.OldColors
import com.myxoz.life.ui.theme.TypoStyle
import com.myxoz.life.utils.AndroidUtils
import com.myxoz.life.utils.formatTimeStamp
import com.myxoz.life.utils.rippleClick
import com.myxoz.life.utils.windowPadding
import com.myxoz.life.viewmodels.CommitsViewModel
import java.time.Instant
import java.time.ZoneId

@Composable
fun FullScreenCommit(sha: String, commitsViewModel: CommitsViewModel){
    val context = LocalContext.current
    val commit by commitsViewModel.getCommit(sha).collectAsState()
    val displayCommit = commit ?: return
    val ordered = remember(displayCommit) { displayCommit.modifications.sortedByDescending { it.changes } }
    val innerPadding = windowPadding
    Box(
        Modifier
            .edgeToEdgeGradient(Theme.background, innerPadding)
            .fillMaxHeight()
        ,
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            Modifier
                .setMaxTabletWidth()
        ) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Spacer(Modifier.height(innerPadding.calculateTopPadding()))
                }
                item {
                    Row(
                        Modifier
                            .fillMaxWidth()
                        ,
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val calendar = remember { Calendar.getInstance() }
                        val nav = LocalNavController.current
                        Text(
                            "${displayCommit.repoOwner}/${displayCommit.repoName}",
                            Modifier
                                .weight(1f)
                                .rippleClick{
                                    nav.navigate(NavPath.Menu.Repos.REPO.with(displayCommit.commitSha))
                                }
                            ,
                            style = TypoStyle(Theme.secondary, FontSize.MEDIUM),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val screens = LocalScreens.current
                        Text(
                            displayCommit.commitDate?.formatTimeStamp(calendar) ?: "",
                            Modifier
                                .rippleClick{
                                    displayCommit.commitDate?.let {
                                        screens.openCalendarAt(
                                            Instant
                                                .ofEpochMilli(it)
                                                .atZone(ZoneId.systemDefault())
                                                .toLocalDate()
                                        )
                                    }
                                }
                            ,
                            style = TypoStyle(Theme.secondary, FontSize.MEDIUM),
                            maxLines = 1
                        )
                    }
                }
                item {
                    Spacer(Modifier.height(10.dp))
                }
                item {
                    Text(
                        displayCommit.commitMessage?:"Keine",
                        style = TypoStyle(Theme.primary, FontSize.XLARGE, FontFamily.Display)
                            .copy(fontStyle = if(displayCommit.commitMessage == null) FontStyle.Italic else FontStyle.Normal)
                    )
                }
                item {
                    Text(displayCommit.commitSha, style = TypoStyle(Theme.tertiary, FontSize.MEDIUM))
                }
                item {
                    Spacer(Modifier.height(1.dp))
                }
                item {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                            .background(Theme.surfaceContainer, RoundedCornerShape(25.dp))
                            .clip(RoundedCornerShape(25.dp))
                            .rippleClick{
                                AndroidUtils.openLink(context, displayCommit.commitUrl?:return@rippleClick)
                            }
                            .padding(vertical = 10.dp)
                    ) {
                        @Composable
                        fun RowScope.AddDelItem(text: String, value: Int, color: Color){
                            Column(
                                Modifier
                                    .weight(1f)
                                ,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text, style = TypoStyle(Theme.secondary, FontSize.MEDIUM))
                                Text(value.toString(), color = color, fontSize = FontSize.XXLARGE.size, fontFamily = FontFamily.Display.family)
                            }
                        }
                        AddDelItem("Additions", displayCommit.additions?:0, OldColors.Commits.ADDITION)
                        VerticalDivider(color = Theme.outlineVariant)
                        AddDelItem("Deletion", displayCommit.deletions?:0, OldColors.Commits.DELETION)
                    }
                }
                item {
                    Spacer(Modifier.height(1.dp))
                }
                item {
                    HorizontalDivider(color = Theme.outline)
                }
                item {
                    Spacer(Modifier.height(1.dp))
                }
                items(ordered,{it.filename}) { modification ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                            .background(Theme.surfaceContainer, RoundedCornerShape(25.dp))
                        ,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            Modifier
                                .weight(1f)
                                .padding(vertical = 10.dp)
                        ) {
                            Text(
                                modification.filename,
                                Modifier
                                    .padding(start = 10.dp, end = 5.dp)
                                ,
                                style = TypoStyle(Theme.primary, FontSize.MEDIUM)
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Min)
                                    .weight(1f)
                                ,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                @Composable
                                fun RowScope.AddDelItem(text: String, value: Int, color: Color){
                                    Column(
                                        Modifier
                                            .weight(1f)
                                        ,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(text, style = TypoStyle(Theme.secondary, FontSize.SMALLM))
                                        Text(value.toString(), color = color, fontSize = FontSize.XLARGE.size, fontFamily = FontFamily.Display.family)
                                    }
                                }
                                AddDelItem("Additions", modification.additions, OldColors.Commits.ADDITION)
                                VerticalDivider(Modifier.padding(vertical = 5.dp), color = Theme.outlineVariant)
                                AddDelItem("Deletions", modification.deletions, OldColors.Commits.DELETION)
                                VerticalDivider(Modifier.padding(vertical = 5.dp), color = Theme.outlineVariant)
                                AddDelItem("Changes", modification.changes, OldColors.Commits.CHANGE)
                            }
                        }
                        VerticalDivider(Modifier.padding(vertical = 5.dp), color = Theme.outline)
                        Icon(
                            painterResource(R.drawable.github),
                            "Github File",
                            Modifier
                                .clip(CircleShape)
                                .rippleClick{
                                    AndroidUtils.openLink(context, GitHub.generateLinkForFileOfCommit(displayCommit, modification))
                                }
                                .padding(20.dp)
                                .size(30.dp)
                            ,
                            tint = OldColors.GITHUB
                        )
                    }
                }
                item {
                    Spacer(Modifier.height(innerPadding.calculateBottomPadding()))
                }
            }
        }
    }
}