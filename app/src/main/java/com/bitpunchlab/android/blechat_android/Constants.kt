package com.bitpunchlab.android.blechat_android

import java.util.*

val SERVICE_UUID: UUID = UUID.fromString("00001811-0000-1000-8000-00805f9b34fb")
val MESSAGE_UUID: UUID = UUID.fromString("7db3e235-3608-41f3-a03c-955fcbd2ea4b")
val CONFIRM_UUID: UUID = UUID.fromString("36d4dc5c-814b-4097-a5a6-b93b39085928")
val DESCRIPTOR_MESSAGE_UUID: UUID = UUID.fromString("42a210d6-b6c5-4f82-a9cc-67d0e1d76a1e")

enum class ConnectionState {
    STATE_NONE,
    STATE_LISTEN,
    STATE_CONNECTING,
    STATE_CONNECTED,
    STATE_DISCONNECTED
}

enum class AppStatus {

}

val START_MESSAGE_NOTIFICATION = "StartMessageNotification"
val STOP_MESSAGE_NOTIFICATION = "StopMessageNotification"

val DISCONNECTION_KEY = "53796disconnect"