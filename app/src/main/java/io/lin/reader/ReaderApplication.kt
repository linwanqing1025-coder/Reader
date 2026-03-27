package io.lin.reader

import android.app.Application
import android.util.Log
import io.lin.reader.data.appcontainer.AppContainer
import io.lin.reader.data.appcontainer.AppDataContainer

class ReaderApplication : Application() {
    /**
     * AppContainer instance used by the rest of classes to obtain dependencies
     */
    lateinit var container: AppContainer
    override fun onCreate() {
        super.onCreate()
        Log.d("linTest", "class ReaderApplication: override onCreate().")
        container = AppDataContainer(this)
    }
}