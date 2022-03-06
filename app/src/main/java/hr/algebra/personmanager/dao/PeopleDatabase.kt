package hr.algebra.personmanager.dao

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Person::class], version = 2)
@TypeConverters(DateConverter::class)
abstract class PeopleDatabase : RoomDatabase() {
    abstract fun personDao() : PersonDao

    companion object {
        @Volatile private var INSTANCE: PeopleDatabase? = null

        var MIGRATION_1_2:Migration = object : Migration(1,2){
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE people ADD COLUMN bio TEXT");
            }
        }

        fun getInstance(context: Context) =
            INSTANCE ?: synchronized(PeopleDatabase::class.java) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }


        private fun buildDatabase(context: Context) = Room.databaseBuilder(
            context.applicationContext,
            PeopleDatabase::class.java,
            "people.db"
        )
            .addMigrations(MIGRATION_1_2)
            .build()
    }
}

//https://developer.android.com/training/data-storage/room/migrating-db-versions