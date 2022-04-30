package com.bitpunchlab.android.blechat_android.messages

import androidx.databinding.ViewDataBinding
import com.bitpunchlab.android.blechat_android.R
import com.bitpunchlab.android.blechat_android.base.GenericListener
import com.bitpunchlab.android.blechat_android.base.GenericRecyclerBindingInterface
import com.bitpunchlab.android.blechat_android.base.GenericRecyclerViewAdapter
import com.bitpunchlab.android.blechat_android.databinding.ItemMessageBinding
import com.bitpunchlab.android.blechat_android.models.MessageModel

class MessageListAdapter : GenericRecyclerViewAdapter<MessageModel>(
    layoutID = R.layout.item_message,
    compareItems = { old, new ->  old.id == new.id },
    compareContents = { old, new ->  old == new },
    onClickListener = null,
    bindingInter = object : GenericRecyclerBindingInterface<MessageModel> {
        override fun bindData(
            item: MessageModel,
            binding: ViewDataBinding,
            onClickListener: GenericListener<MessageModel>?
        ) {
            (binding as ItemMessageBinding).messageModel = item
        }
    }
) {
}