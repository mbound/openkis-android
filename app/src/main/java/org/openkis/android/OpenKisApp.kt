package org.openkis.android

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.openkis.android.data.repository.SyncManager
import org.osmdroid.config.Configuration
import javax.inject.Inject

@HiltAndroidApp
class OpenKisApp : Application() {

    @Inject lateinit var syncManager: SyncManager

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // Configure osmdroid
        Configuration.getInstance().apply {
            userAgentValue = packageName
            osmdroidBasePath = cacheDir
            osmdroidTileCache = java.io.File(cacheDir, "osmdroid")
        }

        appScope.launch {
            syncManager.ensureDefaultServer()
        }
    }
}
