package com.rahul.photosearch.ui.common

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import com.rahul.photosearch.BR

abstract class DataBindingAdapter<Item, VDB : ViewDataBinding> :
    RecyclerView.Adapter<DataBindingViewHolder<Item, VDB>>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DataBindingViewHolder<Item, VDB> {
        val inflater = LayoutInflater.from(parent.context)
        val binding = DataBindingUtil.inflate<VDB>(inflater, viewType, parent, false)
        return DataBindingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DataBindingViewHolder<Item, VDB>, position: Int) {
        holder.bind(getVariableId(), getItem(position))
    }

    override fun onViewRecycled(holder: DataBindingViewHolder<Item, VDB>) {
        super.onViewRecycled(holder)
        holder.binding.unbind()
    }

    override fun getItemViewType(position: Int): Int {
        return getLayoutId(position)
    }

    open fun getVariableId(): Int {
        return BR.item
    }

    abstract fun getLayoutId(position: Int): Int
    abstract fun getItem(position: Int): Item
}