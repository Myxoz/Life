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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
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
import com.myxoz.life.LocalStorage
import com.myxoz.life.R
import com.myxoz.life.api.syncables.CommitSyncable
import com.myxoz.life.android.integration.GitHub
import com.myxoz.life.screens.feed.dayoverview.edgeToEdgeGradient
import com.myxoz.life.ui.theme.Colors
import com.myxoz.life.ui.theme.FontColor
import com.myxoz.life.ui.theme.FontFamily
import com.myxoz.life.ui.theme.FontSize
import com.myxoz.life.ui.theme.TypoStyle
import com.myxoz.life.utils.AndroidUtils
import com.myxoz.life.utils.formatTimeStamp
import com.myxoz.life.utils.rippleClick
import kotlinx.coroutines.runBlocking

@Composable
fun FullScreenCommit(sha: String){
    val context = LocalContext.current
    Scaffold(
        containerColor = Colors.BACKGROUND
    ) { innerPadding ->
        val db = LocalStorage.current
        val commit = remember {
            runBlocking {
                CommitSyncable.from(db.commits.getBySha(sha)!!)
            }
        }
        Box(
            Modifier
                .fillMaxSize()
                .edgeToEdgeGradient(Colors.BACKGROUND, innerPadding)
            ,
            contentAlignment = Alignment.Center
        ) {
            Column(
                Modifier
                    .fillMaxWidth(.95f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(Modifier.height(innerPadding.calculateTopPadding()))
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                    ,
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val calendar = remember { Calendar.getInstance() }
                    Text("${commit.repoOwner}/${commit.repoName}", Modifier.weight(1f), style = TypoStyle(FontColor.SECONDARY, FontSize.MEDIUM), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(commit.commitDate?.formatTimeStamp(calendar) ?: "", style = TypoStyle(FontColor.SECONDARY, FontSize.MEDIUM), maxLines = 1)
                }
                Spacer(Modifier.height(30.dp))
                Text(
                    commit.commitMessage?:"Keine",
                    style = TypoStyle(FontColor.PRIMARY, FontSize.XLARGE, FontFamily.Display)
                        .copy(fontStyle = if(commit.commitMessage == null) FontStyle.Italic else FontStyle.Normal)
                )
                Spacer(Modifier.height(10.dp))
                Text(commit.commitSha, style = TypoStyle(FontColor.TERTIARY, FontSize.MEDIUM))
                Spacer(Modifier.height(20.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                        .background(Colors.SECONDARY, RoundedCornerShape(25.dp))
                        .clip(RoundedCornerShape(25.dp))
                        .rippleClick{
                            AndroidUtils.openLink(context, commit.commitUrl?:return@rippleClick)
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
                            Text(text, style = TypoStyle(FontColor.SECONDARY, FontSize.MEDIUM))
                            Text(value.toString(), color = color, fontSize = FontSize.XXLARGE.size, fontFamily = FontFamily.Display.family)
                        }
                    }
                    AddDelItem("Additions", commit.additions?:0, Colors.Commits.ADDITION)
                    VerticalDivider(color = Colors.DIVIDERS)
                    AddDelItem("Deletion", commit.deletions?:0, Colors.Commits.DELETION)
                }
                Spacer(Modifier.height(20.dp))
                HorizontalDivider(color = Colors.DIVIDERS)
                Spacer(Modifier.height(20.dp))
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    for (modification in commit.modifications.sortedByDescending { it.changes }) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min)
                                .background(Colors.SECONDARY, RoundedCornerShape(25.dp))
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
                                    style = TypoStyle(FontColor.PRIMARY, FontSize.MEDIUM)
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
                                            Text(text, style = TypoStyle(FontColor.SECONDARY, FontSize.SMALLM))
                                            Text(value.toString(), color = color, fontSize = FontSize.XLARGE.size, fontFamily = FontFamily.Display.family)
                                        }
                                    }
                                    AddDelItem("Additions", modification.additions, Colors.Commits.ADDITION)
                                    VerticalDivider(Modifier.padding(vertical = 5.dp), color = Colors.DIVIDERS)
                                    AddDelItem("Deletions", modification.deletions, Colors.Commits.DELETION)
                                    VerticalDivider(Modifier.padding(vertical = 5.dp), color = Colors.DIVIDERS)
                                    AddDelItem("Changes", modification.changes, Colors.Commits.CHANGE)
                                }
                            }
                            VerticalDivider(Modifier.padding(vertical = 5.dp), color = Colors.DIVIDERS)
                            Icon(
                                painterResource(R.drawable.github),
                                "Github File",
                                Modifier
                                    .clip(CircleShape)
                                    .rippleClick{
                                        AndroidUtils.openLink(context, GitHub.generateLinkForFileOfCommit(commit, modification))
                                    }
                                    .padding(20.dp)
                                    .size(30.dp)
                                ,
                                tint = Colors.GITHUB
                            )
                        }
                    }
                }
                Spacer(Modifier.height(innerPadding.calculateBottomPadding()))
            }
        }
    }
}