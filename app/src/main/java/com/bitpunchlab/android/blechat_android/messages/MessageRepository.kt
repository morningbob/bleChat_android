package com.bitpunchlab.android.blechat_android.messages

import androidx.lifecycle.LiveData
import com.bitpunchlab.android.blechat_android.database.BLEDatabase
import com.bitpunchlab.android.blechat_android.models.MessageModel

class MessageRepository(val database: BLEDatabase) {

    //var messagesForDevice = database.messageDAO.getDeviceMessages("xx")

    fun getDeviceMessages(deviceAddress: String) : LiveData<List<MessageModel>> {
        return database.messageDAO.getDeviceMessages(deviceAddress)
    }
}