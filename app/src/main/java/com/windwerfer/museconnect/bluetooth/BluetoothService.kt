package com.windwerfer.museconnect.bluetooth

import android.bluetooth.BluetoothDevice
import android.content.Context
import com.windwerfer.museconnect.appendData

class BluetoothService(private val context: Context) {
    fun connect(device: BluetoothDevice) {
        appendData("Connecting to ${device.address}")
        // TODO: Add GATT logic
    }

    fun disconnect() {
        appendData("Disconnected")
    }
}