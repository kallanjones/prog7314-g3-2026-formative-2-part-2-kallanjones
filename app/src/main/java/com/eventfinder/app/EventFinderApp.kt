package com.eventfinder.app

import android.app.Application
import android.content.Context
import android.os.Build
import android.util.Log
import com.eventfinder.app.di.AppContainer
import com.eventfinder.app.utils.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory

/**
 * Application entry point. Owns the dependency container and performs the
 * one-time global setup (logging, osmdroid configuration) described below.
 */
class EventFinderApp : Application() {

    /** Late-initialised dependency graph. */
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        enableStartupLogging()
        container = AppContainer(this)
        startNetworkMonitoring()
        initialiseMapSdk()
        seedLocaleMirror()
    }

    /**
     * Brings the synchronous locale mirror in line with DataStore on a
     * background thread, so a language chosen on a previous version is still
     * applied at startup. Off the main thread, so it never delays launch.
     */
    private fun seedLocaleMirror() {
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { container.preferences.syncLocaleMirror() }
        }
    }

    /** Platform logging util used before the app logger is available. */
    private fun enableStartupLogging() {
        Log.i("EventFinder", "Application starting (SDK ${Build.VERSION.SDK_INT})")
    }

    /** Track connectivity so screens can switch to offline mode instantly. */
    private fun startNetworkMonitoring() {
        container.networkMonitor.start()
        AppLogger.d("EventFinderApp", "Network monitoring started")
    }

    /**
     * osmdroid (OpenStreetMap SDK) needs its user-agent before any MapView is
     * created. It is configured here exactly once, following the official docs:
     *  - osmdroid wiki: https://github.com/osmdroid/osmdroid/wiki/How-to-use-the-osmdroid-library
     */
    private fun initialiseMapSdk() {
        Configuration.getInstance().load(
            this,
            getSharedPreferences("${packageName}_preferences", Context.MODE_PRIVATE)
        )
        Configuration.getInstance().userAgentValue =
            "EventFinder/1.0 (https://github.com/Zulfique/eventfinder-south-africa)"
        AppLogger.d("EventFinderApp", "osmdroid configured with ${TileSourceFactory.MAPNIK.name()}")
    }
}