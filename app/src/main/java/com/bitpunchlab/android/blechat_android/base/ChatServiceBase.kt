package com.bitpunchlab.android.blechat_android.base

import kotlinx.coroutines.InternalCoroutinesApi

open class ChatServiceBase {
    fun sendMessage(message: String) {

    }
/*
    companion object {

        @Volatile
        private var INSTANCE: ChatServiceBase? = null

        @InternalCoroutinesApi
        fun getInstance(): ChatServiceBase {
            synchronized(this) {
                var instance = INSTANCE
                if (instance == null) {


                    INSTANCE = instance
                }

                return instance
            }
        }
    }

 */
}