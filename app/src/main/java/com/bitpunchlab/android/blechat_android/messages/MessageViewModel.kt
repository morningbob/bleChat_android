package com.bitpunchlab.android.blechat_android.messages

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.bitpunchlab.android.blechat_android.models.MessageModel

class MessageViewModel : ViewModel() {

    private var _messageList = MutableLiveData<List<MessageModel>>()
    val messageList get() = _messageList

    fun addMessage(msgModel: MessageModel) {
        val list = messageList.value as MutableList<MessageModel>
        //(list as ArrayList<MessageModel>).add(msgModel)
        list.add(msgModel)
        _messageList.value = list!!
    }

    init {
        _messageList.value = emptyList()
    }
}