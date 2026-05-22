package io.github.mobdev

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import io.github.mobdev.data.ChatRepository
import io.github.mobdev.data.CredentialStore
import io.github.mobdev.data.db.AppDatabase
import io.github.mobdev.util.NetworkMonitor

class ChatApp : Application(), ImageLoaderFactory {

    val credentialStore: CredentialStore by lazy { CredentialStore(this) }
    val db: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val networkMonitor: NetworkMonitor by lazy { NetworkMonitor(this) }
    val repository: ChatRepository by lazy {
        ChatRepository(credentialStore, db, networkMonitor, this)
    }

    override fun onCreate() {
        super.onCreate()
        networkMonitor // start listening for connectivity changes immediately
    }

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(256L * 1024 * 1024)
                    .build()
            }
            .build()
}
