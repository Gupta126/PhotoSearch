package com.rahul.photosearch.di

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.module.AppGlideModule
import dagger.Module
import dagger.Subcomponent
import com.rahul.photosearch.data.flickr.FlickrPhoto
import com.rahul.photosearch.data.glide.FlickrModelLoader
import java.io.InputStream


@GlideModule
class PhotoSearchGlideModule : AppGlideModule() {
    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        registry.replace(GlideUrl::class.java, InputStream::class.java, OkHttpUrlLoader.Factory())
        registry.append(FlickrPhoto::class.java, InputStream::class.java, FlickrModelLoader.Factory())
    }
}

@Subcomponent
interface GlideComponent {
    fun inject(module: PhotoSearchGlideModule)
    @Subcomponent.Builder
    interface Builder {
        fun build(): GlideComponent
    }
}

@Module(subcomponents = [GlideComponent::class])
abstract class GlideDaggerModule
