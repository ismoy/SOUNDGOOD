package cl.ismoy_belizaire.soundgood.di

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideGlideInstance(
        @ApplicationContext context: Context
    )=Glide.with(context).setDefaultRequestOptions(
        RequestOptions()
            .placeholder(com.google.android.exoplayer2.ui.R.drawable.exo_ic_default_album_image)
            .error(com.google.android.exoplayer2.ui.R.drawable.exo_ic_default_album_image)
            .diskCacheStrategy(DiskCacheStrategy.DATA)
    )
}