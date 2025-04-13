package com.windwerfer.museconnect

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.windwerfer.museconnect.bluetooth.BluetoothService
import com.windwerfer.museconnect.data.ConfigManager
import com.windwerfer.museconnect.ui.MainScreen
import kotlinx.coroutines.delay
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val bluetoothService: BluetoothService by inject()
    private val configManager: ConfigManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ),
                1
            )
        }

        setContent {
            val scannedDevices by bluetoothService.scannedDevices.collectAsState()
            val deviceList = scannedDevices.map { "${it.first ?: "Unknown"} - ${it.second}" }
            val deviceMacs = scannedDevices.map { it.second }
            val lastDeviceMac = configManager.getLastConnectedDevice()

            // Auto-connect logic
            LaunchedEffect(scannedDevices) {
                if (scannedDevices.isNotEmpty()) {
                    // Wait 1s to collect more devices
                    delay(1_000)
                    val deviceMac = if (lastDeviceMac != null && scannedDevices.any { it.second == lastDeviceMac }) {
                        lastDeviceMac
                    } else {
                        scannedDevices.first().second
                    }
                    bluetoothService.connect(deviceMac)
                    configManager.setLastConnectedDevice(deviceMac)
                }
            }

            // Repeat scan every 20s
            LaunchedEffect(Unit) {
                while (true) {
                    bluetoothService.startScanning()
                    delay(20_000)
                    bluetoothService.stopScanning()
                }
            }

            MainScreen(
                museDevices = deviceList,
                deviceMacs = deviceMacs,
                selectedDevice = if (deviceMacs.isEmpty()) null else {
                    lastDeviceMac?.takeIf { deviceMacs.contains(it) } ?: deviceMacs.firstOrNull()
                },
                onDeviceSelected = { mac ->
                    bluetoothService.connect(mac)
                    configManager.setLastConnectedDevice(mac)
                },
                configManager = configManager
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothService.stopScanning()
        bluetoothService.disconnect()
    }
}