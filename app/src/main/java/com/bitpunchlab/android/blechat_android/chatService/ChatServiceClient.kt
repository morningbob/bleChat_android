package com.bitpunchlab.android.blechat_android.chatService

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.*
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.lifecycle.MutableLiveData
import com.bitpunchlab.android.blechat_android.ConnectionState
import com.bitpunchlab.android.blechat_android.MESSAGE_UUID
import com.bitpunchlab.android.blechat_android.SERVICE_UUID
import com.bitpunchlab.android.blechat_android.base.ChatServiceBase
import com.bitpunchlab.android.blechat_android.models.MessageModel

private const val TAG = "ChatServiceClient"

object ChatServiceClient {

    private var app: Application? = null
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothManager: BluetoothManager
    private var gattClient: BluetoothGatt? = null
    var connectionState = MutableLiveData<ConnectionState>(ConnectionState.STATE_NONE)
    private var messageCharacteristic: BluetoothGattCharacteristic? = null
    private var _message = MutableLiveData<MessageModel>()
    val message get() = _message
    private var connectedDevice: BluetoothDevice? = null

    private var gattClientCallback = object : BluetoothGattCallback() {

        // most chat states are detected here, we'll do cleanings here too.
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            Log.i(TAG, "client callback: State change detected")
            val statusSuccess = status == BluetoothGatt.GATT_SUCCESS
            val stateConnected = newState == BluetoothProfile.STATE_CONNECTED
            Log.i(TAG, "client callback: success? $statusSuccess; connected? $stateConnected")

            gatt?.let {
                // keep the reference
                //gattClient = gatt
                if (statusSuccess && stateConnected) {
                    gatt.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.i(TAG, "client callback: disconnected")
                    gatt.close()
                } else {
                    Log.i(TAG, "client callback: else case")
                    connectedDevice = null
                }
            }
        }

        // we discover the server's service.  we retrieve and keep the message char reference here
        // we also keep a ref to the gatt
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)

            gatt?.let {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    gatt.services.map { each ->
                        Log.i(TAG, "service discovered: ${each.uuid.toString()}")
                    }
                    // retrieve the service and the message char
                    val service = gatt.getService(SERVICE_UUID)
                    messageCharacteristic = service.getCharacteristic(MESSAGE_UUID)
                    if (messageCharacteristic != null) {
                        Log.i(TAG, "got message char")
                    } else {
                        Log.i(TAG, "couldn't get message char")
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(device: BluetoothDevice, application: Application) {
        app = application
        Log.i(TAG, "connecting to device")
        // here, actually the device is not connected yet, if the state is disconnected,
        // or not succeed, we'll remove the connectedDevice
        connectedDevice = device
        gattClient = device.connectGatt(app, false, gattClientCallback)
    }

    @SuppressLint("MissingPermission")
    fun sendMessage(msg: String) : Boolean {
        Log.i(TAG, "client is sending message")
        // here we first check if the connected device exist
        if (connectedDevice != null) {
            // we kept a ref to message char, and write a message to it
            // then, we write the characteristic in the gatt
            if (messageCharacteristic != null) {
                messageCharacteristic!!.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                val messageBytes = msg.toByteArray(Charsets.UTF_8)
                messageCharacteristic!!.value = messageBytes
            } else {
                Log.i(TAG, "error of sending message: couldn't get message characteristic")
            }
            // now write the characteristic to the gatt
            gattClient?.let { gatt ->
                val writeSuccess = gatt.writeCharacteristic(messageCharacteristic)
                Log.i(TAG, "write success? $writeSuccess")
                if (writeSuccess) {
                    val messageModel = MessageModel(content = msg, deviceAddress = connectedDevice!!.address,
                    deviceName = connectedDevice!!.name)
                    _message.value = messageModel
                    return true
                } else {
                    Log.i(TAG, "there is error when writing to gatt")
                    return false
                }
            }
        }
        return false
    }


}