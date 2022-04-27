package com.bitpunchlab.android.blechat_android.base

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

abstract class GenericRecyclerViewAdapter<T: Any> (
    private val onClickListener: GenericListener<T>?,
    compareItems: (old: T, new: T) -> Boolean,
    compareContents: (old: T, new: T) -> Boolean,
    private val bindingInter: GenericRecyclerBindingInterface<T>,
    @LayoutRes val layoutID: Int) :
    ListAdapter<T, GenericViewHolder>(GenericDiffCallback(compareItems, compareContents)) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GenericViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = DataBindingUtil.inflate<ViewDataBinding>(layoutInflater, layoutID
            , parent, false)
        return GenericViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GenericViewHolder, position: Int) {
        val item = getItem(position)
        if (onClickListener != null) {
            holder.bind(item, bindingInter, onClickListener)
        } else {
            holder.bind(item, bindingInter, null)
        }

    }
}

class GenericViewHolder(val binding: ViewDataBinding) : RecyclerView.ViewHolder(binding.root) {
    fun <T: Any> bind(
        item: T,
        bindingInterface: GenericRecyclerBindingInterface<T>, onClickListenr: GenericListener<T>?) {
        if (onClickListenr != null) {
            bindingInterface.bindData(item, binding, onClickListenr)
        } else {
            bindingInterface.bindData(item, binding, null)
        }
        binding.executePendingBindings()
    }
}

class GenericDiffCallback<T>(
    private val compareItems: (old: T, new: T) -> Boolean,
    private val compareContents: (old: T, new: T) -> Boolean
) : DiffUtil.ItemCallback<T>() {
    override fun areItemsTheSame(old: T, new: T): Boolean
            = compareItems(old, new)

    override fun areContentsTheSame(old: T, new: T): Boolean
            = compareContents(old, new)
}

interface GenericRecyclerBindingInterface<T: Any> {
    fun bindData(item: T, binding: ViewDataBinding, onClickListener: GenericListener<T>?)
}

open abstract class GenericListener<T>(val clickListener: (T) -> Unit) {
}
