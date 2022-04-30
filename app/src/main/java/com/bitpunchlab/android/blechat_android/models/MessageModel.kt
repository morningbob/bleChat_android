package com.bitpunchlab.android.blechat_android.models

data class MessageModel (
    var id: Int = 0,
    var content: String,
    var deviceAddress: String,
    var deviceName: String?
)