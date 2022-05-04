package com.bitpunchlab.android.blechat_android.deviceList

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import com.bitpunchlab.android.blechat_android.database.BLEDatabase
import com.bitpunchlab.android.blechat_android.models.DeviceModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "DeviceRepository"

class DeviceRepository(val database: BLEDatabase) {

    fun getAllDevices() : LiveData<List<DeviceModel>> {
        return database.deviceDAO.getAllDevices()
    }

    fun saveDevice(device: DeviceModel) {
        val coroutineScope = CoroutineScope(Dispatchers.IO)
        coroutineScope.launch {
            database.deviceDAO.insertDevice(device)
            Log.i(TAG, "inserted device")
        }
    }

}