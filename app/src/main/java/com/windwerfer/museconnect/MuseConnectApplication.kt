package com.windwerfer.museconnect

import android.app.Application
import com.windwerfer.museconnect.bluetooth.BluetoothService
import com.windwerfer.museconnect.data.ConfigManager
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

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
}