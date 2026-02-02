package com.myxoz.life.dbwrapper.people

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profilepicture")
data class ProfilePictureStored(
    @PrimaryKey(autoGenerate = false) // we’ll provide our own id
    @ColumnInfo(name = "person_id") val personId: Long,

    @ColumnInfo(name = "has_pp") val hasPP: Boolean,
)