package com.bitpunchlab.android.blechat_android.messages

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bitpunchlab.android.blechat_android.database.BLEDatabase
import com.bitpunchlab.android.blechat_android.deviceList.DeviceViewModel
import com.bitpunchlab.android.blechat_android.models.MessageModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MessageViewModel(val database: BLEDatabase) : ViewModel() {

    private var _messageList = MutableLiveData<List<MessageModel>>(emptyList())
    val messageList get() = _messageList
    private var messageRepository: MessageRepository = MessageRepository(database)
    //lateinit var messageRecordList = messageRepository.getDeviceMessages(deviceAddress)
    lateinit var messageRecordList : LiveData<List<MessageModel>>
    private var coroutineScope: CoroutineScope

    init {
        coroutineScope = CoroutineScope(Dispatchers.IO)
    }

    fun saveMessage(msgModel: MessageModel) {
        val repo = MessageRepository(database)
        repo.saveMessage(msgModel)
    }

    fun getDeviceMessages(deviceAddress: String) : LiveData<List<MessageModel>> {
        return messageRepository.getDeviceMessages(deviceAddress)
    }

    fun verifyConfirmationCode(code: String) {
        coroutineScope.launch {
            var upperLimit = 0
            var lowerLimit = 0
            Log.i("verify: ", "verifying...")
            messageRecordList.value?.let { msgList ->
                if (msgList.size > 15) {
                    upperLimit = msgList.size - 16
                } else {
                    upperLimit = 0
                }
                lowerLimit = msgList.size - 1
            }
            var found = false
            Log.i("verifying: upper lower: $upperLimit $lowerLimit ", "begin for loop")
            if (!messageRecordList.value.isNullOrEmpty()) {
                for (i in lowerLimit downTo upperLimit step 1) {
                    //Log.i("verify: i: ", i.toString())
                    messageRecordList.value?.get(i)?.let { msg ->
                        Log.i("i: ", i.toString())
                        Log.i("verifying","message in verification: $msg.content")
                        Log.i("confirmCode got: ", code)
                        Log.i("message's code: ", msg.confirmCode)
                        if (!msg.sent && msg.confirmCode == code) {
                            //for (code in codeList) {
                            //if (msg.confirmCode == code) {
                            Log.i("verified $i", "message: ${msg.content}")
                            msg.sent = true
                            // record that in verified list, and delete it from list of confirm codes
                            //       verifiedCodes.add(code)
                            val messageRepo = MessageRepository(database)
                            //coroutineScope.launch {
                            messageRepo.saveMessage(msg)
                            Log.i("verified ", "updated message")
                            //}
                            found = true
                            //       break

                            //}
                            //}
                            //}
                        }
                    }
                    if (found) {
                        break
                    }
                }
            }
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

/*
fun addMessage(msgModel: MessageModel) {
        var list = messageList.value
        if (list!!.isNotEmpty()) {
            (list as MutableList<MessageModel>).add(msgModel)
        } else {
            list = mutableListOf(msgModel)
        }
        _messageList.value = list!!
    }
 */