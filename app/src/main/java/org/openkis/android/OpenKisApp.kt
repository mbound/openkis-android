package org.openkis.android

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import org.osmdroid.config.Configuration

@HiltAndroidApp
class OpenKisApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Configure osmdroid
        Configuration.getInstance().apply {
            userAgentValue = packageName
            osmdroidBasePath = cacheDir
            osmdroidTileCache = java.io.File(cacheDir, "osmdroid")
        }
    }
}
