package com.rahul.photosearch.di

import android.content.Context
import dagger.Component
import dagger.Module
import dagger.Provides
import dagger.android.AndroidInjector
import dagger.android.support.AndroidSupportInjectionModule
import com.rahul.photosearch.PhotoSearchApplication
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        ApplicationModule::class,
        ActivityBindingModule::class,
        AndroidSupportInjectionModule::class,
        DataModule::class,
        ViewModelModule::class
    ]
)
interface ApplicationComponent : AndroidInjector<PhotoSearchApplication> {
    @Component.Builder
    abstract class Builder : AndroidInjector.Builder<PhotoSearchApplication>()
}

@Module
class ApplicationModule {

    @Provides
    fun provideContext(application: PhotoSearchApplication): Context {
        return application.applicationContext
    }
}