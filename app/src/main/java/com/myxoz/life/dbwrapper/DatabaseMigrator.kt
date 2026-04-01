package com.myxoz.life.dbwrapper

import android.util.Log
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class DatabaseMigrator(var version: Int, val builder: RoomDatabase.Builder<AppDatabase>) {
    fun add(migrate: (db: SupportSQLiteDatabase) -> Unit): DatabaseMigrator{
        val from = version++
        Log.d("DatabaseMigrator", "Added migration from $from to $version")
        builder.addMigrations(
            object: Migration(from, version) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    migrate(db)
                }
            }
        )
        return this
    }
    companion object {
        // TODO Increment for each migration.
        // Yes I tried but VERSION needs to be a compile-time constant
        const val VERSION = 40
        fun RoomDatabase.Builder<AppDatabase>.applyAllMigrations(): RoomDatabase.Builder<AppDatabase> {
            val mig = DatabaseMigrator(37, this)
            return mig
                .add { db ->
                    db.execSQL("""
                    CREATE TABLE IF NOT EXISTS todos (
                        id INTEGER NOT NULL PRIMARY KEY,
                        short TEXT NOT NULL,
                        details TEXT,
                        done INTEGER NOT NULL
                    )
                    """.trimIndent())
                }
                .add { db ->
                    db.execSQL("ALTER TABLE todos ADD COLUMN timestamp INTEGER NOT NULL")
                }
                .add { db ->
                    db.execSQL("""
                    CREATE TABLE IF NOT EXISTS transaction_split (
                        id INTEGER NOT NULL PRIMARY KEY,
                        syncable_id INTEGER,
                        remote_id TEXT
                    )
                    """.trimIndent())

                    db.execSQL("""
                    CREATE TABLE IF NOT EXISTS transaction_split_part (
                        id INTEGER NOT NULL,
                        person INTEGER NOT NULL,
                        amount INTEGER NOT NULL,
                        PRIMARY KEY (id, person)
                    )""".trimIndent())
                }
                .builder
        }
    }
}