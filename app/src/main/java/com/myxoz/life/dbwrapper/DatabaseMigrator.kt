package com.myxoz.life.dbwrapper

import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class DatabaseMigrator(var version: Int, val builder: RoomDatabase.Builder<AppDatabase>) {
    fun add(migrate: (db: SupportSQLiteDatabase) -> Unit): DatabaseMigrator{
        builder.addMigrations(
            object: Migration(++version, version + 1) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    migrate(db)
                }
            }
        )
        return this
    }
    companion object {
        // TODO Increment for each migration
        const val VERSION = 39
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
                .builder
        }
    }
}