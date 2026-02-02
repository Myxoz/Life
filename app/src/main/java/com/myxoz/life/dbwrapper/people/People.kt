package com.myxoz.life.dbwrapper.people

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "people")
data class PersonEntity(
    @PrimaryKey(autoGenerate = false) // we’ll provide our own id
    @ColumnInfo(name = "id") val id: Long,

    @ColumnInfo(name = "name") val name: String,

    @ColumnInfo(name = "fullname") val fullname: String?,

    @ColumnInfo(name = "phone_number") val phoneNumber: String?,

    @ColumnInfo(name = "iban") val iban: String?,

    @ColumnInfo(name = "home") val home: Long?,

    @ColumnInfo(name = "birthday") val birthday: Long?,
)