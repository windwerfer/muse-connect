package com.windwerfer.museconnect

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.windwerfer.museconnect.bluetooth.BluetoothService
import com.windwerfer.museconnect.data.ConfigManager
import com.windwerfer.museconnect.ui.MainScreen
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val bluetoothService: BluetoothService by inject()
    private val configManager: ConfigManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN), 1)
        }

        // Connect to last device or default
        val lastDeviceMac = configManager.getLastConnectedDevice() ?: "00:55:DA:B9:59:09"
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val device = bluetoothAdapter?.getRemoteDevice(lastDeviceMac)
        device?.let { bluetoothService.connect(it) }

        setContent {
            MainScreen(
                museDevices = listOf(lastDeviceMac), // TODO: Add device scanning
                selectedDevice = lastDeviceMac,
                onDeviceSelected = { mac -> bluetoothService.connect(bluetoothAdapter.getRemoteDevice(mac)) },
                configManager = configManager,
                debugMessages = emptyList() // TODO: Collect from BluetoothService
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothService.disconnect()
    }
}