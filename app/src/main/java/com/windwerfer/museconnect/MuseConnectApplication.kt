package com.windwerfer.museconnect

import android.app.Application
import com.windwerfer.museconnect.bluetooth.BluetoothService
import com.windwerfer.museconnect.bluetooth.MuseCommandManager
import com.windwerfer.museconnect.data.ConfigManager
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module
import com.windwerfer.museconnect.utils.Logging

class MuseConnectApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@MuseConnectApplication)
            modules(appModule)
        }
    }
}

val appModule = module {
    single { BluetoothService(get()) }
    single { ConfigManager(get()) }
    single { MuseCommandManager(get(), onDataReceived = { data, channel ->
        // Temporary: forward to Logging.appendData
        Logging.appendData("Data for $channel: ${data.size} bytes - ${data.joinToString(", ") { it.toInt().toString() }}")
    }) }
}