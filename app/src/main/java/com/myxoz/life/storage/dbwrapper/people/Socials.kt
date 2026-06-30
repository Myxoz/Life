package com.myxoz.life.storage.dbwrapper.people

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(tableName = "socials", primaryKeys = ["person_id", "platform", "handle"])
data class SocialsEntity(
    @ColumnInfo(name = "person_id") val personId: Long,

    @ColumnInfo(name = "platform") val platform: Int,

    @ColumnInfo(name = "handle") val handle: String,
)