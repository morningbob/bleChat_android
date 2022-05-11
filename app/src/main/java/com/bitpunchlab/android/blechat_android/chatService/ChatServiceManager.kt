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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

import kotlin.random.Random.Default.nextInt

private const val TAG = "ChatServiceManager"
private const val RESTART_WAIT_PERIOD : Long = 7000

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
    private lateinit var coroutineScope: CoroutineScope
    var isChatEnded = MutableLiveData(true)
    var confirmCodeList = MutableLiveData<List<String>>(emptyList())
    var confirmCode = MutableLiveData<String>()

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
                isChatEnded.postValue(false)
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                // we save the disconnected device info here
                // that we just disconnected with this device
                disconnectedDevice = device
                isChatEnded.postValue(true)
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
            var msgContent = ""
            // for this confirmation code, we don't keep it, we just send it back to the peer
            var confirmCodeForPeer = ""
            characteristic?.let { char ->
                if (char.uuid == MESSAGE_UUID) {
                    val msg = value?.toString(Charsets.UTF_8)
                    Log.i(TAG, "message received: $msg")
                    // we extract the confirmation code here
                    msg?.let {
                        // check if it is just the confirm code, not message
                        // if it is a confirm code, it starts with confirm898
                        if (!msg.startsWith("confirm898", false)) {
                            val resultList = decodeConfirmCode(msg)
                            Log.i("resultList: ", resultList.toString())
                            // we will send back the confirmation code to show that we
                            // received the message.
                            confirmCodeForPeer = resultList[0]
                            //Log.i("confirmCode: ", confirmCode.value!!)
                            sendMessage("confirm898${confirmCodeForPeer}")
                            msgContent = resultList[1]
                            Log.i("message", msgContent)
                            val msgModel =
                                MessageModel(content = msgContent,
                                    deviceAddress = device!!.address,
                                    deviceName = device?.name, confirmCode = confirmCodeForPeer)
                            _message.postValue(msgModel)
                        } else {
                            // msg starts with confirm898
                            // notice the app that the message the server sent is
                            // delivered.
                            // compare with the confirm code we generated earlier
                            Log.i("confirm message: ", msg)
                            val resultList = decodeConfirmCode(msg)
                            Log.i("received confirm code to verify: ", resultList[0])
                            // we get the confirm code we sent to the peer, extract it
                            // and find it in last 15 message
                            confirmCode.postValue(resultList[0])
                        }
                    }

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
                // we'll send a disconnect request to the client, which ask the client to do
                // the disconnection also.

                // we'll look into how to better disconnect the device.
                clearServices()
                close()
            }
            // when we close the gattServer, the connected device will
            // certainly be disconnected no matter what, so we change the
            // state here
            disconnectedDevice = device
            connectionState.postValue(ConnectionState.STATE_DISCONNECTED)
            connectedDevice = null
        isServerRunning.postValue(false)
        coroutineScope = CoroutineScope(Dispatchers.Default)
        // we need to start gatt again, so it listens to incoming connections
        coroutineScope.launch {

            delay(RESTART_WAIT_PERIOD)
            //app?.let {
                //startChatServer(it)
                //Log.i(TAG, "restarting chat server")
            //}
        }
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
            if (!msg.startsWith("confirm898", false)) {

                val confirmCodeToVerify = generateRandomConfirmCode()
                val modifiedMessage = msg + confirmCodeToVerify
                val messageBytes = modifiedMessage.toByteArray(Charsets.UTF_8)
                characteristic.value = messageBytes

                // here, we notify client
                val success = gattServer!!.notifyCharacteristicChanged(connectedDevice, characteristic, false)
                Log.i(TAG, "server wrote to character and notify client, success? $success")

                // we need to display the message user said to the message list.
                val msgModel = MessageModel(content = msg, deviceName = "You",
                    deviceAddress = connectedDevice!!.address, confirmCode = confirmCodeToVerify)
                _message.postValue(msgModel)
                // update confirmCodeList, let the ChatFragment to start verifying
                //confirmCodeList.value
                addConfirmCode(confirmCodeToVerify)
                return success
            } else if (msg.startsWith("confirm898", false) || (msg.startsWith("disconnect735946"))){
                // send the confirm898XXXXX code
                val messageBytes = msg.toByteArray(Charsets.UTF_8)
                characteristic.value = messageBytes
                return gattServer!!.notifyCharacteristicChanged(connectedDevice, characteristic, false)
            }
            return false
        } else {
            Log.i(TAG, "can't send message.  gatt is null")
            return false
        }
    }

    private fun generateRandomConfirmCode() : String {
        val confirmNum = Random.nextInt(10000,99999)
        return confirmNum.toString() + "kcvx"
    }

    private fun decodeConfirmCode(msg: String) : ArrayList<String> {
        var resultList = ArrayList<String>()
        // there will be 9 characters at the end, that is confirmation code
        if (msg.length >= 9) {
            resultList.add(msg.takeLast(9))
        } else {
            Log.i("error decoding", "there is less then 9 characters in the message, unusual")
        }
        resultList.add(msg.dropLast(9))
        return resultList
    }

    private fun addConfirmCode(code: String) {
        val list = confirmCodeList.value!!.toMutableList()
        list.add(code)
        confirmCodeList.postValue(list)
    }

    fun removeConfirmCode(code: String) {
        if (!confirmCodeList.value.isNullOrEmpty()) {
            val list = confirmCodeList.value!!.toMutableList()
            for (each in list) {
                if (each == code) {
                    list.remove(each)
                }
            }
            confirmCodeList.value = list
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