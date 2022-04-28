package com.bitpunchlab.android.blechat_android.deviceList

import android.bluetooth.BluetoothDevice
import android.view.View
import androidx.databinding.ViewDataBinding
import com.bitpunchlab.android.blechat_android.R
import com.bitpunchlab.android.blechat_android.base.GenericListener
import com.bitpunchlab.android.blechat_android.base.GenericRecyclerBindingInterface
import com.bitpunchlab.android.blechat_android.base.GenericRecyclerViewAdapter
import com.bitpunchlab.android.blechat_android.databinding.ItemDeviceBinding
import com.bitpunchlab.android.blechat_android.models.DeviceModel

class DeviceListAdapter(clickListener: GenericListener<BluetoothDevice>?
) :
    GenericRecyclerViewAdapter<BluetoothDevice>(
    layoutID = R.layout.item_device,
    compareItems = { old, new -> old.address == new.address },
    compareContents = { old, new -> old == new },
    onClickListener = clickListener,
    bindingInter = object : GenericRecyclerBindingInterface<BluetoothDevice> {
        override fun bindData(
            item: BluetoothDevice,
            binding: ViewDataBinding,
            onClickListener: GenericListener<BluetoothDevice>?
        ) {
            (binding as ItemDeviceBinding).bluetoothDevice = item
        }
    }
    ) {
}

class DeviceListener(clickListener: (BluetoothDevice) -> Unit) : GenericListener<BluetoothDevice>(clickListener) {
    fun onClick(device: BluetoothDevice) = clickListener(device)
}