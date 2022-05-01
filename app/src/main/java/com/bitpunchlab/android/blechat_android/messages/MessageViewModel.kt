package com.bitpunchlab.android.blechat_android.messages

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.bitpunchlab.android.blechat_android.models.MessageModel

class MessageViewModel : ViewModel() {

    private var _messageList = MutableLiveData<List<MessageModel>>(emptyList())
    val messageList get() = _messageList

    fun addMessage(msgModel: MessageModel) {
        var list = messageList.value //
        if (list!!.isNotEmpty()) {
            (list as MutableList<MessageModel>).add(msgModel)
        } else {
            list = mutableListOf(msgModel)
        }
        _messageList.value = list!!
    }

    init {
        //_messageList.value = emptyList()
    }
}