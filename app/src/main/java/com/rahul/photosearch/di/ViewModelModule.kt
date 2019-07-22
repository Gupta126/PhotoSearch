package com.rahul.photosearch.di

import androidx.lifecycle.ViewModelProvider
import com.rahul.photosearch.di.ViewModelFactory
import dagger.Binds
import dagger.Module

@Module
abstract class ViewModelModule {

    @Binds
    internal abstract fun bindViewModelFactory(factory: ViewModelFactory): ViewModelProvider.Factory
}