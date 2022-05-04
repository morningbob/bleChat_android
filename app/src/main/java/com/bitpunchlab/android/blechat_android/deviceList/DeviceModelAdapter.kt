package com.bitpunchlab.android.blechat_android.deviceList

import android.bluetooth.BluetoothDevice
import androidx.databinding.ViewDataBinding
import com.bitpunchlab.android.blechat_android.R
import com.bitpunchlab.android.blechat_android.base.GenericListener
import com.bitpunchlab.android.blechat_android.base.GenericRecyclerBindingInterface
import com.bitpunchlab.android.blechat_android.base.GenericRecyclerViewAdapter
import com.bitpunchlab.android.blechat_android.databinding.ItemDeviceBinding
import com.bitpunchlab.android.blechat_android.databinding.ItemDeviceModelBinding
import com.bitpunchlab.android.blechat_android.models.DeviceModel

class DeviceModelAdapter(clickListener: DeviceModelListener?
) :
    GenericRecyclerViewAdapter<DeviceModel>(
        layoutID = R.layout.item_device_model,
        compareItems = { old, new -> old.address == new.address },
        compareContents = { old, new -> old == new },
        onClickListener = clickListener,
        bindingInter = object : GenericRecyclerBindingInterface<DeviceModel> {
            override fun bindData(
                item: DeviceModel,
                binding: ViewDataBinding,
                onClickListener: GenericListener<DeviceModel>?
            ) {
                (binding as ItemDeviceModelBinding).deviceModel = item
                binding.onClickListener = clickListener
            }
        }
    ) {
}

class DeviceModelListener(clickListener: (DeviceModel) -> Unit) : GenericListener<DeviceModel>(clickListener) {
    fun onClick(device: DeviceModel) = clickListener(device)
}