package com.bitpunchlab.android.blechat_android.chat

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.*
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.lifecycle.MutableLiveData
import com.bitpunchlab.android.blechat_android.ConnectionState

private const val TAG = "ChatServiceClient"

object ChatServiceClient {

    private var app: Application? = null
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothManager: BluetoothManager
    private var gattClient: BluetoothGatt? = null
    var connectionState = MutableLiveData<ConnectionState>(ConnectionState.STATE_NONE)

    private var gattClientCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            Log.i(TAG, "client callback: State change detected")
            val statusSuccess = status == BluetoothGatt.GATT_SUCCESS
            val stateConnected = newState == BluetoothProfile.STATE_CONNECTED
            Log.i(TAG, "client callback: success? $statusSuccess; connected? $stateConnected")

            gatt?.let {
                if (statusSuccess && stateConnected) {
                    gatt.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.i(TAG, "client callback: disconnected")
                    gatt.close()
                } else {
                    Log.i(TAG, "client callback: else case")
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(device: BluetoothDevice, application: Application) {
        app = application

        gattClient = device.connectGatt(app, false, gattClientCallback)
    }
}