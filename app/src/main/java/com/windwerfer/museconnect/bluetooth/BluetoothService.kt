package com.windwerfer.museconnect.bluetooth

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Build
import com.windwerfer.museconnect.utils.appendData
import com.windwerfer.museconnect.utils.checkBluetoothPermissions

class BluetoothService(private val context: Context) {
    private var gatt: BluetoothGatt? = null
    private var museCommandManager: MuseCommandManager? = null

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            appendData("Connection state: status=$status, newState=$newState")
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    if (checkBluetoothPermissions(context)) {
                        try {
                            appendData("Connected to ${gatt.device.name}")
                            if (gatt.discoverServices()) appendData("Service discovery initiated")
                        } catch (e: SecurityException) {
                            appendData("SecurityException: Cannot access device name or discover services")
                        }
                    }
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    if (checkBluetoothPermissions(context)) {
                        try {
                            appendData("Disconnected from ${gatt.device.name}")
                        } catch (e: SecurityException) {
                            appendData("SecurityException: Cannot access device name")
                        }
                    }
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                appendData("Services discovered.")
                if (checkBluetoothPermissions(context)) {
                    try {
                        gatt.services.forEach { appendData("Found service: ${it.uuid}") }
                        museCommandManager?.startEegStream(gatt)
                    } catch (e: SecurityException) {
                        appendData("SecurityException: Cannot access services")
                    }
                }
            }
        }
    }

    fun connect(device: BluetoothDevice) {
        if (checkBluetoothPermissions(context)) {
            try {
                gatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
                } else {
                    @Suppress("DEPRECATION")
                    device.connectGatt(context, false, gattCallback)
                }
                museCommandManager = MuseCommandManager(context) { data, channel ->
                    appendData("Data for $channel: ${data.size} bytes - ${data.joinToString(", ") { it.toInt().toString() }}")
                }
                appendData("Connecting to ${device.name} - ${device.address}")
            } catch (e: SecurityException) {
                appendData("SecurityException: Failed to connect to device")
            }
        } else {
            appendData("Missing Bluetooth permissions for connection")
        }
    }

    fun disconnect() {
        if (checkBluetoothPermissions(context)) {
            try {
                gatt?.disconnect()
                gatt?.close()
                gatt = null
                appendData("Disconnected")
            } catch (e: SecurityException) {
                appendData("SecurityException: Failed to disconnect")
            }
        } else {
            appendData("Missing permissions to disconnect")
        }
    }
}