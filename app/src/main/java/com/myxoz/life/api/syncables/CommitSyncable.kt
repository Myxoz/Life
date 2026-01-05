package com.myxoz.life.api.syncables

import com.myxoz.life.api.ServerSyncable
import com.myxoz.life.api.ServerSyncableCompanion
import com.myxoz.life.api.Syncable
import com.myxoz.life.api.jsonObjArray
import com.myxoz.life.dbwrapper.CommitEntity
import com.myxoz.life.dbwrapper.StorageManager
import com.myxoz.life.utils.getIntOrNull
import com.myxoz.life.utils.getLongOrNull
import com.myxoz.life.utils.getStringOrNull
import org.json.JSONArray
import org.json.JSONObject

class CommitSyncable(
    val repoOwner: String,
    val repoName: String,
    val commitSha: String,
    val commitMessage: String?,
    val commitAuthor: String?,
    val commitEmail: String?,
    val commitDate: Long?,
    val additions: Int?,
    val deletions: Int?,
    val filesChanged: Int?,
    val modifications: List<CommitModification>,
    val commitUrl: String?,
    val updated: Long
) : ServerSyncable(Syncable.SpecialSyncablesIds.BANKING) {
    companion object : ServerSyncableCompanion {
        class CommitModification(
            val filename: String,
            val additions: Int,
            val deletions: Int,
            val changes: Int,
            val status: String,
        )
        override suspend fun overwriteByJson(db: StorageManager, it: JSONObject) {
            db.commits.insert(
                CommitEntity(
                    it.getString("repo_owner"),
                    it.getString("repo_name"),
                    it.getString("commit_sha"),
                    it.getStringOrNull("commit_message"),
                    it.getStringOrNull("commit_author"),
                    it.getStringOrNull("commit_email"),
                    it.getLongOrNull("commit_date"),
                    it.getIntOrNull("additions"),
                    it.getIntOrNull("deletions"),
                    it.getIntOrNull("files_changed"),
                    it.getStringOrNull("files_json"),
                    it.getStringOrNull("commit_url"),
                    it.getString("updated").toLong()
                )
            )
        }
        fun from(en: CommitEntity) = CommitSyncable(
                en.repoOwner,
                en.repoName,
                en.commitSha,
                en.commitMessage,
                en.commitAuthor,
                en.commitEmail,
                en.commitDate,
                en.additions,
                en.deletions,
                en.filesChanged,
                en.filesJson?.run {
                    JSONArray(this).jsonObjArray.map {
                        CommitModification(
                            it.getString("filename"),
                            it.getInt("additions"),
                            it.getInt("deletions"),
                            it.getInt("changes"),
                            it.getString("filename"),

                        )
                    }
                } ?: listOf() ,
                en.commitUrl,
                en.updated,
            )
    }
}