package com.bitpunchlab.android.blechat_android.messages

import android.util.Log
import androidx.lifecycle.LiveData
import com.bitpunchlab.android.blechat_android.database.BLEDatabase
import com.bitpunchlab.android.blechat_android.models.MessageModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "MessageRepository"

class MessageRepository(val database: BLEDatabase) {

    fun getDeviceMessages(deviceAddress: String) : LiveData<List<MessageModel>> {
        return database.messageDAO.getDeviceMessages(deviceAddress)
    }

    fun saveMessage(message: MessageModel) {
        val coroutineScope = CoroutineScope(Dispatchers.IO)
        coroutineScope.launch {
            database.messageDAO.insertMessage(message)
            Log.i(TAG, "saved message")
        }
    }
}