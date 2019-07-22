package com.rahul.photosearch.di

import dagger.Module
import dagger.android.ContributesAndroidInjector
import com.rahul.photosearch.ui.main.MainActivity
import com.rahul.photosearch.ui.main.MainModule
import com.rahul.photosearch.ui.photo.PhotoActivity
import com.rahul.photosearch.ui.photo.PhotoModule


@Module
abstract class ActivityBindingModule {

    @ActivityScoped
    @ContributesAndroidInjector(
        modules = [
            MainModule::class
        ]
    )
    internal abstract fun mainActivity(): MainActivity

    @ActivityScoped
    @ContributesAndroidInjector(
        modules = [
            PhotoModule::class
        ]
    )
    internal abstract fun photoActivity(): PhotoActivity
}