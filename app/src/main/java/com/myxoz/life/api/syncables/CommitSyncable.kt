package com.myxoz.life.api.syncables

import com.myxoz.life.api.ServerSyncable
import com.myxoz.life.api.ServerSyncableCompanion
import com.myxoz.life.api.Syncable
import com.myxoz.life.dbwrapper.commits.CommitEntity
import com.myxoz.life.dbwrapper.commits.ReadCommitsDao
import com.myxoz.life.api.API
import com.myxoz.life.utils.getIntOrNull
import com.myxoz.life.utils.getLongOrNull
import com.myxoz.life.utils.getStringOrNull
import com.myxoz.life.utils.jsonObjArray
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
    override suspend fun saveToDB(db: API.WriteSyncableDaos) {
        db.commitsDao.insert(
            CommitEntity(
                repoOwner,
                repoName,
                commitSha,
                commitMessage,
                commitAuthor,
                commitEmail,
                commitDate,
                additions,
                deletions,
                filesChanged,
                JSONArray()
                    .apply {
                        modifications.forEach {
                            this.put(it.toJson())
                        }
                    }
                    .toString(),
                commitUrl,
                updated,
            )
        )
    }
    fun toRepoKey() = "$repoOwner/$repoName"

    companion object : ServerSyncableCompanion<CommitSyncable> {
        class CommitModification(
            val filename: String,
            val additions: Int,
            val deletions: Int,
            val changes: Int,
            val status: String,
        ) {
            fun toJson(): JSONObject = JSONObject()
                .put("filename", filename)
                .put("additions", additions)
                .put("deletions", deletions)
                .put("changes", changes)
                .put("status", status)
            companion object {
                fun fromJSON(json: String?) =
                    json?.run {
                        JSONArray(this).jsonObjArray.map {
                            CommitModification(
                                it.getString("filename"),
                                it.getInt("additions"),
                                it.getInt("deletions"),
                                it.getInt("changes"),
                                it.getString("filename"),
                            )
                        }
                    } ?: listOf()
            }
        }

        override fun fromJSON(json: JSONObject): CommitSyncable {
            val repoOwner = json.getString("repo_owner")
            val repoName = json.getString("repo_name")
            val commitSha = json.getString("commit_sha")
            val commitMessage = json.getStringOrNull("commit_message")
            val commitAuthor = json.getStringOrNull("commit_author")
            val commitEmail = json.getStringOrNull("commit_email")
            val commitDate = json.getLongOrNull("commit_date")
            val additions = json.getIntOrNull("additions")
            val deletions = json.getIntOrNull("deletions")
            val filesChanged = json.getIntOrNull("files_changed")
            val filesJson = json.getStringOrNull("files_json")
            val commitUrl = json.getStringOrNull("commit_url")
            val updated = json.getString("updated").toLong()
            return CommitSyncable(
                repoOwner,
                repoName,
                commitSha,
                commitMessage,
                commitAuthor,
                commitEmail,
                commitDate,
                additions,
                deletions,
                filesChanged,
                CommitModification.fromJSON(filesJson),
                commitUrl,
                updated,
            )
        }
        suspend fun fromDB(db: ReadCommitsDao, sha: String): CommitSyncable {
            val entity = db.getBySha(sha)?:throw Error("Tried to get unknown commit. This is usupported. Check where you have the sha from.")
            return from(entity)
        }
        fun from(entity: CommitEntity) = CommitSyncable(
                entity.repoOwner,
                entity.repoName,
                entity.commitSha,
                entity.commitMessage,
                entity.commitAuthor,
                entity.commitEmail,
                entity.commitDate,
                entity.additions,
                entity.deletions,
                entity.filesChanged,
                CommitModification.fromJSON(entity.filesJson),
                entity.commitUrl,
                entity.updated
            )
    }
}