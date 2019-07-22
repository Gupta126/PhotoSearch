package com.rahul.photosearch.di

import android.content.Context
import com.rahul.photosearch.R
import com.rahul.photosearch.R.*
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import com.rahul.photosearch.data.flickr.FlickrRepository
import com.rahul.photosearch.data.flickr.FlickrService
import javax.inject.Singleton

@Module
class DataModule {

    @Provides
    fun provideCallFactory(context: Context): Call.Factory {
        val logging = HttpLoggingInterceptor()
        logging.level = HttpLoggingInterceptor.Level.BODY


        return OkHttpClient.Builder()
            .addInterceptor {
                it.proceed(
                    it.request().run {
                        val url = url()
                            .newBuilder()
                            .addQueryParameter("api_key", context.getString(string.api_key))
                            .addQueryParameter("format", "json")
                            .addQueryParameter("nojsoncallback", "1")
                            .build()

                        return@run newBuilder()
                            .url(url)
                            .build()
                    }
                )
            }
            .addInterceptor(logging)
            .build()
    }

    @Provides
    fun provideMoshi(): Moshi {
        return Moshi.Builder().build()
    }

    @Provides
    fun provideRetrofit(callFactory: Call.Factory, moshi: Moshi): Retrofit {
        return Retrofit.Builder()
            .callFactory(callFactory)
            .baseUrl(" https://api.flickr.com/services/rest/")
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    @Provides
    fun provideFlickrService(retrofit: Retrofit): FlickrService {
        return retrofit.create(FlickrService::class.java)
    }

    /**
     * Mark repository as singleton for sharing data between activities.
     * In reality the data should be committed to a database instead.
     */
    @Provides
    @Singleton
    fun provideRepository(service: FlickrService): FlickrRepository {
        return FlickrRepository(service)
    }
}
