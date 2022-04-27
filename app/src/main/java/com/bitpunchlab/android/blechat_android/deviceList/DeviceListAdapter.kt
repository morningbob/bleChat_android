package com.bitpunchlab.android.blechat_android.deviceList

import com.bitpunchlab.android.blechat_android.R
import com.bitpunchlab.android.blechat_android.base.GenericListener
import com.bitpunchlab.android.blechat_android.base.GenericRecyclerViewAdapter
import com.bitpunchlab.android.blechat_android.models.DeviceModel

class DeviceListAdapter(clickListener: GenericListener<DeviceModel>?
) :
    GenericRecyclerViewAdapter<DeviceModel>(
    layoutID = R.layout.item_device,
    onClickListener = clickListener,
    ) {
}