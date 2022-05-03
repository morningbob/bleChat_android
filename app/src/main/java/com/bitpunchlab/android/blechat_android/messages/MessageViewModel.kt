package com.bitpunchlab.android.blechat_android.messages

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bitpunchlab.android.blechat_android.database.BLEDatabase
import com.bitpunchlab.android.blechat_android.deviceList.DeviceViewModel
import com.bitpunchlab.android.blechat_android.models.MessageModel

class MessageViewModel(val database: BLEDatabase) : ViewModel() {

    private var _messageList = MutableLiveData<List<MessageModel>>(emptyList())
    val messageList get() = _messageList
    private var messageRepository: MessageRepository = MessageRepository(database)
    lateinit var messages : LiveData<List<MessageModel>>

    fun addMessage(msgModel: MessageModel) {
        var list = messageList.value
        if (list!!.isNotEmpty()) {
            (list as MutableList<MessageModel>).add(msgModel)
        } else {
            list = mutableListOf(msgModel)
        }
        _messageList.value = list!!
    }

    init {
        //val database = BLEDatabase.getInstance(application.baseContext)
    }

    fun getDeviceMessages(deviceAddress: String)  {
        messages = database.messageDAO.getDeviceMessages(deviceAddress)
    }
}

class MessageViewModelFactory(private val database: BLEDatabase)
    : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MessageViewModel::class.java)) {
            return MessageViewModel(database) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}