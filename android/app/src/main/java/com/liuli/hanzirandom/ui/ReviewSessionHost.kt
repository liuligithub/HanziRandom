package com.liuli.hanzirandom.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore

class ReviewSessionHost : ViewModel() {
    private val stores = mutableMapOf<Long, ViewModelStore>()

    fun storeFor(id: Long): ViewModelStore = stores.getOrPut(id) { ViewModelStore() }

    fun release(id: Long) {
        stores.remove(id)?.clear()
    }

    override fun onCleared() {
        stores.values.forEach { it.clear() }
        stores.clear()
    }
}
