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
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.lifecycle.*
import kotlinx.coroutines.*
import java.util.logging.Handler

private const val TAG = "DeviceViewModel"

class DeviceViewModel(application: Application) : AndroidViewModel(application) {

    private var bluetoothAdapter: BluetoothAdapter
    private var bleScanner: BluetoothLeScanner
    private val SCAN_PERIOD: Long = 10000
    private var scanning = false
    private var coroutineScope: CoroutineScope

    var _deviceList = MutableLiveData<List<BluetoothDevice>>()
    val deviceList : LiveData<List<BluetoothDevice>> get() = _deviceList

    var _chosenDevice = MutableLiveData<BluetoothDevice?>()
    val chosenDevice : LiveData<BluetoothDevice?> get() = _chosenDevice

    private val scanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            // add the newly discovered device here

        }
    }

    init {
        val bluetoothManager = getApplication<Application>().getSystemService(Context.BLUETOOTH_SERVICE)
                as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        Log.i(TAG, "just got adapter")
        bleScanner = bluetoothAdapter.bluetoothLeScanner
        coroutineScope = CoroutineScope(Dispatchers.Default)
    }

    fun onDeviceClicked(device: BluetoothDevice) {
        _chosenDevice.value = device
    }

    fun finishNavigationOfDevice() {
        _chosenDevice.value = null
    }

    private fun addADevice() {
        var list = deviceList.value
        list
    }

    @SuppressLint("MissingPermission")
    private fun scanLeDevice() {
        if (!bluetoothAdapter.isMultipleAdvertisementSupported) {
            Log.i(TAG, "no advertising")
            return
        }
        if (!scanning) {
            coroutineScope.launch {
                delay(SCAN_PERIOD)
                scanning = false
                bleScanner.stopScan(scanCallback)
                Log.i(TAG, "stop scanning")
            }
            scanning = true
            bleScanner.startScan(scanCallback)
        } else {
            scanning = false
            bleScanner.stopScan(scanCallback)
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
/*
fun View.delayOnLifecycle(
        durationInMillis: Long,
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
        block: () -> Unit
    ) : Job? = findViewTreeLifecycleOwner()?.let { lifecycleOwner ->
        lifecycleOwner.lifecycle.coroutineScope.launch(dispatcher) {
            delay(durationInMillis)
            block()
        }
    }

 */