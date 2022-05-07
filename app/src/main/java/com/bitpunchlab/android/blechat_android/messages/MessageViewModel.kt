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
    lateinit var messageRecordList : LiveData<List<MessageModel>>

    fun addMessage(msgModel: MessageModel) {
        var list = messageList.value
        if (list!!.isNotEmpty()) {
            (list as MutableList<MessageModel>).add(msgModel)
        } else {
            list = mutableListOf(msgModel)
        }
        _messageList.value = list!!
    }

    fun getDeviceMessages(deviceAddress: String)  {
        messageRecordList = messageRepository.getDeviceMessages(deviceAddress)
    }

    fun verifyConfirmCode(code: String) {
        //messageList.value?.subList()
        for (i in ((messageList.value?.size?.minus(1))?.downTo(messageList.value?.size?.minus(6)!!) ?: 0) step 1) {
            
        }
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