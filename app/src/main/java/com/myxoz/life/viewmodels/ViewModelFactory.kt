package com.myxoz.life.viewmodels

import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras

class ViewModelFactory<MODEL : ViewModel>(private val create: (CreationExtras) -> MODEL) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        return create(extras) as T
    }

    companion object {
        fun <MODEL : ViewModel> factoryProducer(create: (CreationExtras) -> MODEL): () -> ViewModelProvider.Factory {
            return { ViewModelFactory(create) }
        }
    }
}

@MainThread
inline fun <reified VM : ViewModel> ComponentActivity.viewModel(
    noinline create: (CreationExtras) -> VM
): Lazy<VM> {
    return viewModels(
        factoryProducer = ViewModelFactory.factoryProducer(create)
    )
}
