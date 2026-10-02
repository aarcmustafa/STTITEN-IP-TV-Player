package com.sttiten.iptv

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import com.sttiten.iptv.data.worker.ExtensionPluginBootstrapWorker
import com.sttiten.iptv.data.worker.PersistedUriPermissionCleanupWorker
import com.sttiten.iptv.data.worker.ProviderCredentialRecoveryWorker
import com.sttiten.iptv.data.worker.ProviderSessionCleanupWorker
import com.sttiten.iptv.data.worker.initializePersistedUriPermissionLeases
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class M3UApplication : Application(), Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        initializePersistedUriPermissionLeases(this)
        PersistedUriPermissionCleanupWorker.enqueueRecovery(
            WorkManager.getInstance(this)
        )
        ProviderCredentialRecoveryWorker.enqueue(WorkManager.getInstance(this))
        ProviderSessionCleanupWorker.enqueue(
            workManager = WorkManager.getInstance(this),
        )
        ExtensionPluginBootstrapWorker.enqueue(WorkManager.getInstance(this))
    }

    override val workManagerConfiguration: Configuration by lazy {
        Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    }
}
