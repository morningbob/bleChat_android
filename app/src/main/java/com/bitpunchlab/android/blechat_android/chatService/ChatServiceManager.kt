package com.bitpunchlab.android.blechat_android.chatService

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
import androidx.lifecycle.MutableLiveData
import com.bitpunchlab.android.blechat_android.ConnectionState
import com.bitpunchlab.android.blechat_android.DESCRIPTOR_MESSAGE_UUID
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
    var connectedDevice: BluetoothDevice? = null
    var disconnectedDevice: BluetoothDevice? = null
    var isServerRunning = MutableLiveData<Boolean>(false)
    private var _message = MutableLiveData<MessageModel>()
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
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                // we save the disconnected device info here
                // that we just disconnected with this device
                disconnectedDevice = device
                // here we call the method to clear the resource
                disconnectDevice(device!!)
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
            Log.i(TAG, "onChar Write, getting message")
            // check if it is the target characteristic
            characteristic?.let { char ->
                if (char.uuid == MESSAGE_UUID) {
                    val msg = value?.toString(Charsets.UTF_8)
                    Log.i(TAG, "message received: $msg")
                    val msgModel =
                        MessageModel(content = msg.toString(),
                        deviceAddress = device!!.address,
                        deviceName = device?.name)
                    _message.postValue(msgModel)
                }
            }
        }

        // when client set notification, this method is triggered
        // the success response the client receive comes from here.
        // we set the success response here and send to the client.
        @SuppressLint("MissingPermission")
        override fun onDescriptorWriteRequest(
            device: BluetoothDevice?,
            requestId: Int,
            descriptor: BluetoothGattDescriptor?,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {
            super.onDescriptorWriteRequest(
                device,
                requestId,
                descriptor,
                preparedWrite,
                responseNeeded,
                offset,
                value
            )
            Log.i(TAG, "server: onDescrWrite request")
            descriptor?.let { descr ->
                if (descr.uuid == DESCRIPTOR_MESSAGE_UUID) {
                    gattServer!!.sendResponse(device, requestId,
                        BluetoothGatt.GATT_SUCCESS, 0, null)
                    Log.i(TAG, "sent success response to client")
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

        val messageDescriptor = BluetoothGattDescriptor(DESCRIPTOR_MESSAGE_UUID,
            BluetoothGattDescriptor.PERMISSION_WRITE or
                    BluetoothGattDescriptor.PERMISSION_READ)

        messageCharacteristic.addDescriptor(messageDescriptor)
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
        Log.i(TAG, "starting server")
        isServerRunning.value = true
    }

    @SuppressLint("MissingPermission")
    fun stopChatServer() {
        Log.i(TAG, "stopping server")
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
        Log.i(TAG, "setting up server")
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

    // this method is also used to clear gatt and services
    @SuppressLint("MissingPermission")
    fun disconnectDevice(device: BluetoothDevice) {
        Log.i(TAG, "server disconnect the client's device")
        //if (connectionState.value == ConnectionState.STATE_CONNECTED &&
        //        gattServer != null) {
            gattServer!!.apply {
                // we'll look into how to better disconnect the device.
                clearServices()
                close()
            }
            connectionState.value = ConnectionState.STATE_DISCONNECTED
            connectedDevice = null
        //}
    }

    // server send message to client by writing on characteristic, then
    // notify client there is changes.
    @SuppressLint("MissingPermission")
    fun sendMessage(msg: String) : Boolean {
        Log.i(TAG, "server will send message here")
        if (gattServer != null) {
            val service = gattServer!!.getService(SERVICE_UUID)
            val characteristic = service.getCharacteristic(MESSAGE_UUID)
            val messageBytes = msg.toByteArray(Charsets.UTF_8)
            characteristic.value = messageBytes

            // here, we notify client
            val success = gattServer!!.notifyCharacteristicChanged(connectedDevice, characteristic, false)
            Log.i(TAG, "server wrote to character and notify client, success? $success")

            // we need to display the message user said to the message list.
            val msgModel = MessageModel(content = msg, deviceName = "You",
                deviceAddress = connectedDevice!!.address)
            _message.postValue(msgModel)
        } else {
            Log.i(TAG, "can't send message.  gatt is null")
            return false
        }
        return false
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