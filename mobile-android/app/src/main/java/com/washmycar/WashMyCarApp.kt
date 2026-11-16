package com.washmycar

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import org.osmdroid.config.Configuration

/** Point d'entrée de l'application : initialise Hilt et la configuration de la carte OpenStreetMap. */
@HiltAndroidApp
class WashMyCarApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Les serveurs de tuiles OpenStreetMap exigent un User-Agent identifiant l'application.
        Configuration.getInstance().apply {
            userAgentValue = packageName
            osmdroidBasePath = cacheDir.resolve("osmdroid")
            osmdroidTileCache = cacheDir.resolve("osmdroid/tiles")
        }
    }
}
