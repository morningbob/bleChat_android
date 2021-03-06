package com.bitpunchlab.android.blechat_android.deviceList

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.util.Log
import androidx.lifecycle.*
import com.bitpunchlab.android.blechat_android.chatService.ChatServiceClient
import com.bitpunchlab.android.blechat_android.database.BLEDatabase
import com.bitpunchlab.android.blechat_android.messages.MessageRepository
import com.bitpunchlab.android.blechat_android.models.DeviceModel
import com.bitpunchlab.android.blechat_android.models.MessageModel
import kotlinx.coroutines.*

private const val TAG = "DeviceViewModel"

// we do the scanning in the device view model because the information got will
// be saved there.  I want the info to persist too.
@OptIn(InternalCoroutinesApi::class)
class DeviceViewModel(application: Application) : AndroidViewModel(application) {

    private var bluetoothAdapter: BluetoothAdapter
    private var bleScanner: BluetoothLeScanner
    private val SCAN_PERIOD: Long = 10000
    private var scanning = MutableLiveData<Boolean>(false)
    private var coroutineScope: CoroutineScope
    private var deviceScanCallback = DeviceScanCallback()
    //private lateinit var deviceRespository: DeviceRepository
    private var messageRepository: MessageRepository
    private var deviceRespository: DeviceRepository
    var connectingDevice: BluetoothDevice? = null
    lateinit var recordedDeviceList : LiveData<List<DeviceModel>>
    lateinit var database: BLEDatabase

    private var _deviceList = MutableLiveData<List<BluetoothDevice>>(emptyList())
    val deviceList : LiveData<List<BluetoothDevice>> get() = _deviceList

    private var _chosenDevice = MutableLiveData<BluetoothDevice?>()
    val chosenDevice : LiveData<BluetoothDevice?> get() = _chosenDevice

    private var _chosenModel = MutableLiveData<DeviceModel?>()
    val chosenModel get() = _chosenModel

    init {
        val bluetoothManager = application.getSystemService(Context.BLUETOOTH_SERVICE)
                as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        Log.i(TAG, "just got adapter")
        bleScanner = bluetoothAdapter.bluetoothLeScanner
        coroutineScope = CoroutineScope(Dispatchers.Default)
        //_deviceList.value = emptyList()

        database = BLEDatabase.getInstance(application.baseContext)
        deviceRespository = DeviceRepository(database)
        messageRepository = MessageRepository(database)
    }

    fun onDeviceClicked(device: BluetoothDevice) {
        _chosenDevice.value = device
    }

    fun finishNavigationOfDevice() {
        _chosenDevice.value = null
    }

    fun onModelClicked(device: DeviceModel) {
        _chosenModel.value = device
    }

    fun finishNavigationOfDeviceModel() {
        _chosenModel.value = null
    }

    fun getRecordedDevices() {
        recordedDeviceList = deviceRespository.getAllDevices()
        //Log.i(TAG, "device view model: recorded devices count ${recordedDeviceList.value?.size}")
    }

    // we need to add to the list in this way, we can't add to mutable live data list directly
    // we also check if the device already exists
    // consider the case when the device suddenly disappear, how to detect this change
    private fun addADevice(newDevice: BluetoothDevice) {
        Log.i(TAG, "adding a device")
        var list = deviceList.value

        //as ArrayList<BluetoothDevice>
        var isExisted = false
        var newList = list?.map { device ->
            if (device.address == newDevice.address) {
                isExisted = true
                Log.i(TAG, "update existing device")
                newDevice
            } else {
                Log.i(TAG, "old device")
                device
            }
        }
        if (!isExisted) {
            (newList as ArrayList<BluetoothDevice>).add(newDevice)
        }
        newList?.let {
            _deviceList.value = it
        }
    }

    @SuppressLint("MissingPermission")
    fun scanLeDevice() {
        if (!bluetoothAdapter.isMultipleAdvertisementSupported) {
            Log.i(TAG, "no advertising")
            return
        }
        if (!scanning.value!!) {
            coroutineScope.launch {
                delay(SCAN_PERIOD)
                scanning.postValue(false)
                bleScanner.stopScan(deviceScanCallback)
                Log.i(TAG, "stop scanning")
            }
            scanning.postValue(true)
            bleScanner.startScan(deviceScanCallback)
        } else {
            scanning.postValue(false)
            bleScanner.stopScan(deviceScanCallback)
        }
    }

    fun connectToDevice(device: BluetoothDevice) {
        connectingDevice = device
        ChatServiceClient.connectToDevice(device, getApplication<Application>())
    }

    private inner class DeviceScanCallback : ScanCallback() {
        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)
            results?.let {
                for (item in results) {
                    addADevice(item.device)
                }
            }
        }

        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            result?.device?.let { device ->
                Log.i(TAG, "added a device")
                addADevice(device)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.i(TAG, "scan error: $errorCode")
        }
    }
}

class DeviceViewModelFactory(private val application: Application)
    : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DeviceViewModel::class.java)) {
            return DeviceViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
