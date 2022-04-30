package com.bitpunchlab.android.blechat_android.chat

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.*
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.content.pm.PackageManager
import android.os.ParcelUuid
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.lifecycle.MutableLiveData
import com.bitpunchlab.android.blechat_android.ConnectionState
import com.bitpunchlab.android.blechat_android.MESSAGE_UUID
import com.bitpunchlab.android.blechat_android.SERVICE_UUID
import com.bitpunchlab.android.blechat_android.models.MessageModel

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
    var connectionState = MutableLiveData<ConnectionState>(ConnectionState.STATE_NONE)
    private var connectedDevice: BluetoothDevice? = null
    var isServerRunning = MutableLiveData<Boolean>(false)
    private var _message = MutableLiveData<String>()
    val message get() = _message

    private var gattServerCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
            super.onConnectionStateChange(device, status, newState)
            Log.i(TAG, "server callback: State change detected")
            val statusSuccess = status == BluetoothGatt.GATT_SUCCESS
            val stateConnected = newState == BluetoothProfile.STATE_CONNECTED
            Log.i(TAG, "server callback: success? $statusSuccess; connected? $stateConnected")
            if (statusSuccess && stateConnected) {
                connectionState.postValue(ConnectionState.STATE_CONNECTED)
                connectedDevice = device
            }
        }

        // when the client write to a characteristic, this method is triggered.
        // as a server, we retrieve the message from the characteristic
        // main fragment observe the message live variable and display it.
        @SuppressLint("MissingPermission")
        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice?,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic?,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {
            super.onCharacteristicWriteRequest(
                device,
                requestId,
                characteristic,
                preparedWrite,
                responseNeeded,
                offset,
                value
            )

            // check if it is the target characteristic
            characteristic?.let { char ->
                if (char.uuid == MESSAGE_UUID) {
                    val msg = value?.toString(Charsets.UTF_8)
                    val msgModel =
                        MessageModel(content = msg.toString(),
                        deviceAddress = device!!.address,
                        deviceName = device?.name)
                    _message.postValue(msg.toString())
                }
            }


        }
    }

    private fun gattService() : BluetoothGattService {
        val gattService = BluetoothGattService(SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)

        val messageCharacteristic = BluetoothGattCharacteristic(MESSAGE_UUID,
            BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_WRITE or
                BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ or BluetoothGattCharacteristic.PERMISSION_WRITE)

        gattService.addCharacteristic(messageCharacteristic)

        return gattService
    }

    fun startChatServer(application: Application) {
        app = application

        bluetoothManager = application.getSystemService(Context.BLUETOOTH_SERVICE)
                as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        setupGattServer()
        startAdvertising()
        isServerRunning.value = true
    }

    @SuppressLint("MissingPermission")
    fun stopChatServer() {
        stopAdvertising()
        gattServer?.let { gatt ->
            gatt.clearServices()
            gatt.close()
        }
        gattServer = null
        isServerRunning.value = false
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

    @SuppressLint("MissingPermission")
    private fun stopAdvertising() {
        advertiser?.stopAdvertising(advertiseCallback)
        advertiseCallback = null
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