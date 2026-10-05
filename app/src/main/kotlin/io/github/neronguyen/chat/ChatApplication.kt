package io.github.neronguyen.chat

import android.app.Application
import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil3.ImageLoader
import coil3.SingletonImageLoader
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ChatApplication : Application(), Configuration.Provider, SingletonImageLoader.Factory {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var imageLoader: ImageLoader

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun newImageLoader(context: Context): ImageLoader {
        return imageLoader
    }
}
