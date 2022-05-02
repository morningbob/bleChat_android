package com.bitpunchlab.android.blechat_android.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.bitpunchlab.android.blechat_android.models.MessageModel

@Dao
interface MessageDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMessage(message: MessageModel)

    @Query("SELECT * FROM message_table WHERE :key == deviceAddress")
    fun getDeviceMessages(key: String) : LiveData<List<MessageModel>>

    @Delete
    fun delete(message: MessageModel)
}