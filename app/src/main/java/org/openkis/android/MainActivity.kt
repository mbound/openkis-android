package org.openkis.android

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import org.openkis.android.ui.navigation.OpenKisNavHost
import org.openkis.android.ui.theme.OpenKISTheme
import java.util.Locale

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        val locale = LocaleHelper.getLocale(newBase)
        if (locale.isNotEmpty()) {
            val config = Configuration()
            config.setLocale(Locale(locale))
            super.attachBaseContext(newBase.createConfigurationContext(config))
        } else {
            super.attachBaseContext(newBase)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OpenKISTheme {
                OpenKisNavHost()
            }
        }
    }
}
