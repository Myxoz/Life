package com.myxoz.life.dbwrapper

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

const val currVer = 34
val migration = object : Migration(currVer-1, currVer) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Create the new dayscreentime table
        //db.execSQL("ALTER TABLE people ADD COLUMN birthday INTEGER")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS work (
                id INTEGER NOT NULL PRIMARY KEY,
                title TEXT NOT NULL,
                details TEXT
            )
            """.trimIndent()
        )
        //db.execSQL("UPDATE people SET home = (SELECT location.id FROM location WHERE location.homeof = people.id) WHERE EXISTS (SELECT 1 FROM location WHERE location.homeof = people.id)")
        //db.execSQL("ALTER TABLE location DROP COLUMN homeof")
    }
}
@Database(
    entities = [
        PersonEntity::class,
        DaysEntity::class,
        BankingEntity::class,
        DayScreenTimeEntity::class,
        WaitingSyncEntity::class,
        ProposedStepsEntity::class,
        EventEntity::class,
        SpontEntiy::class,
        TagsEntity::class,
        HobbyEntiy::class,
        LearnEntity::class,
        SocialEntity::class,
        PeopleMappingEntity::class,
        TravelEntity::class,
        VehicleEntity::class,
        LocationEntity::class,
        BankingSidecarEntity::class,
        ProfilePictureStored::class,
        SocialsEntity::class,
        DigSocEntity::class,
        DigSocMappingEntity::class,
        CommitEntity::class,
        WorkEntity::class,
    ],
    version = currVer,
    exportSchema = true
) abstract class AppDatabase : RoomDatabase() {
    abstract fun workDao(): WorkDao
    abstract fun commitsDao(): CommitDao
    abstract fun digsocMappingDao(): DigSocMappingDao
    abstract fun digsocDao(): DigSocDao
    abstract fun peopleDao(): PeopleDao
    abstract fun daysDao(): DaysDao
    abstract fun bankingDao(): BankingDao
    abstract fun dayScreenTimeDao(): DayScreenTimeDao
    abstract fun waitingSyncDao(): WaitingSyncDao
    abstract fun proposedStepsDao(): ProposedStepsDao
    abstract fun eventsDao(): EventsDao
    abstract fun spontDao(): SpontDao
    abstract fun tagsDao(): TagsDao
    abstract fun hobbyDao(): HobbyDao
    abstract fun learnDao(): LearnDao
    abstract fun socialDao(): SocialDao
    abstract fun peopleMappingDao(): PeopleMappingDao
    abstract fun travelDao(): TravelDao
    abstract fun vehicleDao(): VehicleDao
    abstract fun locationDao(): LocationDao
    abstract fun bankingSideCarDao(): BankingSidecarDao
    abstract fun databaseCleanupDao(): DatabaseCleanupDao
    abstract fun profilePicture(): ProfilePictureDao
    abstract fun socialsDao(): SocialsDao
}
object DatabaseProvider {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "database.db"
            ).addMigrations(migration).build()
            INSTANCE = instance
            instance
        }
    }
}
/**
 * 1. Create new Dao and Entity (copy from [com.myxoz.life.dbwrapper])
 * 2. Add entity to [AppDatabase] and dao (above)
 * 3. Create [migration] (INTEGER/STRING (NOT NULL)) and increase [currVer]
 * 4. Relaunch on devices to apply changed
 * 5. Add dao to API e.x. after [StorageManager]
 * 6. Add to CleanupDao [DatabaseCleanupDao.clearAllExceptPersistent]
 *
 * Guide to create new calendar:
 * 6. Create the color scheme for the new EventType in [com.myxoz.life.ui.theme.OldColors.Calendar]
 * 7. Create new [com.myxoz.life.events.additionals.EventType]
 * 8. Create event renderer and EventClass by copying a file from [com.myxoz.life.events]
 * 9. Add to [com.myxoz.life.events.ProposedEvent.from]
 * 10. Add to when in [com.myxoz.life.events.ProposedEvent.getProposedEventByJson]
 * 11. Add to [com.myxoz.life.api.ServerSyncable.overwriteByJson] AEFL
 * 12. Add to modify/add screen [com.myxoz.life.screens.feed.fullscreenevent.ModifyEvent] CalendarChip and to the content renderer
 * 13. Add to display screen [com.myxoz.life.screens.feed.fullscreenevent.DisplayEvent]
 * 14. Add to [com.myxoz.life.screens.feed.main.SegmentedEvent.getSegmentedEvents] to be rendered at all
 * 15. Go to serverside ( sshvim myxoz:~/myxoz.de/life/_api.php )
 * 16. Add event to the receive event specifcs (RCEV)
 * 17. Add event to the remove event specifcs from db (RFDB)
 * 18. Add event to the fetch event section (FEEV) and also to the if statement checking fetching calendars (FETT)
 *
 * Guide to create new Syncable:
 * 7. Create a new Syncable: Syncable in [com.myxoz.life.api]
 * 8. Add to [com.myxoz.life.api.Syncable.SpecialSyncablesIds] and then [com.myxoz.life.api.ServerSyncable.overwriteByJson]
 * 9. Add to [com.myxoz.life.api.Syncable.from] (only when also syncable)
 * 10. Go to serverside ( sshvim myxoz:~/myxoz.de/life/_api.php )
 * 11. Add to delete from db (only when also syncable) (SRMDB)
 * 12. Add to insert to db (only when also syncable) (SSTDB)
 * 13. Add to read from db (SRFDB)
 **/