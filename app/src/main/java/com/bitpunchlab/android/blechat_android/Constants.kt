package com.bitpunchlab.android.blechat_android

import java.util.*

val SERVICE_UUID: UUID = UUID.fromString("00001811-0000-1000-8000-00805f9b34fb")
val MESSAGE_UUID: UUID = UUID.fromString("7db3e235-3608-41f3-a03c-955fcbd2ea4b")
val CONFIRM_UUID: UUID = UUID.fromString("36d4dc5c-814b-4097-a5a6-b93b39085928")

enum class ConnectionState {
    STATE_NONE,
    STATE_LISTEN,
    STATE_CONNECTING,
    STATE_CONNECTED,
    STATE_DISCONNECTED
}