package com.bitpunchlab.android.blechat_android.chat

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.*
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import com.bitpunchlab.android.blechat_android.SERVICE_UUID

private const val TAG = "ChatServiceManager"

object ChatServiceManager {

    private var app: Application? = null
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothManager: BluetoothManager
    private var gattServer: BluetoothGattServer? = null
    private var advertiser: BluetoothLeAdvertiser? = null
    private var advertiseCallback: AdvertiseCallback? = null
    private var advertiseSettings: AdvertiseSettings = buildAdvertiseSettings()
    private var advertiseData: AdvertiseData = buildAdvertiseData()

    private var gattServerCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
            super.onConnectionStateChange(device, status, newState)
        }
    }

    private fun gattService() : BluetoothGattService {
        val gattService = BluetoothGattService(SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)

        return gattService
    }

    fun startChatServer(application: Application) {
        app = application

        bluetoothManager = application.getSystemService(Context.BLUETOOTH_SERVICE)
                as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        setupGattServer()
        startAdvertising()
    }

    @SuppressLint("MissingPermission")
    private fun setupGattServer() {
        gattServer = bluetoothManager.openGattServer(app, gattServerCallback).apply {
            addService(gattService())
        }
    }

    @SuppressLint("MissingPermission")
    private fun startAdvertising() {
        advertiser = bluetoothAdapter.bluetoothLeAdvertiser
        if (advertiseCallback == null) {
            advertiseCallback = DeviceAdvertiseCallback()
            advertiser?.startAdvertising(
                advertiseSettings,
                advertiseData,
                advertiseCallback
            )
        }
    }

    private fun buildAdvertiseSettings() : AdvertiseSettings {
        return AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
            .setTimeout(0)
            .build()
    }

    // we add the service uuid here, and let other knows this service
    private fun buildAdvertiseData() : AdvertiseData {
        val dataBuilder =  AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid(SERVICE_UUID))
            .setIncludeDeviceName(true)
        return dataBuilder.build()
    }

    private class DeviceAdvertiseCallback : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            super.onStartSuccess(settingsInEffect)
            Log.i(TAG, "successfully started advertising")
        }

        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
            Log.i(TAG, "error starting advertising, code: $errorCode")
        }
    }
}