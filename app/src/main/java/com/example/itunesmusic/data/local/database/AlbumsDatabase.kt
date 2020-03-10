package com.example.itunesmusic.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.itunesmusic.data.local.dao.AlbumsDao
import com.example.itunesmusic.data.local.models.AlbumLocalModel
import kotlinx.coroutines.*

@Database(entities = [AlbumLocalModel::class], version = 5, exportSchema = false)
abstract class AlbumsDatabase : RoomDatabase() {

    abstract fun albumsDao(): AlbumsDao

    companion object {

        // Singleton prevents multiple instances of database opening at the
        // same time.
        @Volatile
        private var INSTANCE: AlbumsDatabase? = null

        fun getDatabase(context: Context): AlbumsDatabase {
            synchronized(this) {
                var instance = INSTANCE

                if(instance == null){
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        AlbumsDatabase::class.java,
                        "albums_database"
                    )
                        .fallbackToDestructiveMigration()
                        .build()
                    INSTANCE = instance
                }
                return instance
            }
        }
    }

}