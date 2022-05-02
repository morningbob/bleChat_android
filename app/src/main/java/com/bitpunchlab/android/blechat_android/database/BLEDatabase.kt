package com.bitpunchlab.android.blechat_android.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.bitpunchlab.android.blechat_android.models.DeviceModel
import com.bitpunchlab.android.blechat_android.models.MessageModel
import kotlinx.coroutines.InternalCoroutinesApi

@Database(entities = [DeviceModel::class, MessageModel::class], version = 1, exportSchema = false)

abstract class BLEDatabase : RoomDatabase() {

    abstract val deviceDAO: DeviceDAO
    abstract val messageDAO: MessageDAO

    companion object {
        @Volatile
        private var INSTANCE: BLEDatabase? = null

        @InternalCoroutinesApi
        fun getInstance(context: Context?): BLEDatabase {
            synchronized(this) {
                var instance = INSTANCE
                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context!!.applicationContext,
                        BLEDatabase::class.java,
                        "ble_database"
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