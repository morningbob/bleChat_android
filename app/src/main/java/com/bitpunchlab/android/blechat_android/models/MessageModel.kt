package com.bitpunchlab.android.blechat_android.models

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize

@Entity(tableName = "message_table")
@Parcelize
data class MessageModel (
    @PrimaryKey(autoGenerate = true) var id: Int = 0,
    var content: String,
    var deviceAddress: String,
    var deviceName: String?
) : Parcelable