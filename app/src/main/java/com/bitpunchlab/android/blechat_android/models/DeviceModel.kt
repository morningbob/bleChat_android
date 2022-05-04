package com.bitpunchlab.android.blechat_android.models

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.android.parcel.Parcelize

@Entity(tableName = "device_table")
@Parcelize
data class DeviceModel (
    @PrimaryKey var address: String,
    var name: String?
    ) : Parcelable

