package com.bitpunchlab.android.blechat_android.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.bitpunchlab.android.blechat_android.models.DeviceModel

@Dao
interface DeviceDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertDevice(device: DeviceModel)

    @Query("SELECT * FROM device_table WHERE :deviceAddress == address")
    fun getDevice(deviceAddress: String) : LiveData<List<DeviceModel>>

    @Query("SELECT * FROM device_table")
    fun getAllDevices() : LiveData<List<DeviceModel>>

    @Delete
    fun delete(device: DeviceModel)
}