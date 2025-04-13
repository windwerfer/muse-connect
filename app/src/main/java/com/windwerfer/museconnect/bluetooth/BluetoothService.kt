package com.windwerfer.museconnect.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Build
import com.windwerfer.museconnect.utils.Logging
import com.windwerfer.museconnect.utils.checkBluetoothPermissions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class BluetoothService(private val context: Context) {
    private var gatt: BluetoothGatt? = null
    private var museCommandManager: MuseCommandManager? = null
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val _scannedDevices = MutableStateFlow<List<Pair<String?, String>>>(emptyList())
    val scannedDevices = _scannedDevices.asStateFlow()
    private var connectedDevice: Pair<String?, String>? = null

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            Logging.appendData("Connection state: status=$status, newState=$newState")
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    if (checkBluetoothPermissions(context)) {
                        try {
                            Logging.appendData("Connected to ${gatt.device.name}")
                            connectedDevice = Pair(gatt.device.name, gatt.device.address)
                            updateScannedDevicesWithConnected()
                            if (gatt.discoverServices()) Logging.appendData("Service discovery initiated")
                        } catch (e: SecurityException) {
                            Logging.appendData("SecurityException: Cannot access device name or discover services")
                        }
                    }
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    if (checkBluetoothPermissions(context)) {
                        try {
                            Logging.appendData("Disconnected from ${gatt.device.name}")
                            connectedDevice = null
                            updateScannedDevicesWithConnected()
                        } catch (e: SecurityException) {
                            Logging.appendData("SecurityException: Cannot access device name")
                        }
                    }
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Logging.appendData("Services discovered.")
                if (checkBluetoothPermissions(context)) {
                    try {
                        gatt.services.forEach { Logging.appendData("Found service: ${it.uuid}") }
                        museCommandManager?.startEegStream(gatt)
                    } catch (e: SecurityException) {
                        Logging.appendData("SecurityException: Cannot access services")
                    }
                }
            }
        }
    }

    private fun updateScannedDevicesWithConnected() {
        _scannedDevices.update { current ->
            val filtered = current.filter { it.second != connectedDevice?.second }
            if (connectedDevice != null) filtered + connectedDevice!! else filtered
        }
    }

    fun startScanning() {
        if (!checkBluetoothPermissions(context)) {
            Logging.appendData("Missing Bluetooth permissions for scanning")
            return
        }
        if (bluetoothAdapter?.isEnabled != true) {
            Logging.appendData("Bluetooth is disabled")
            return
        }
        try {
            val scanner = bluetoothAdapter.bluetoothLeScanner
            // Only clear non-connected devices
            _scannedDevices.update { current ->
                current.filter { it.second == connectedDevice?.second }
            }
            scanner?.startScan(object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: ScanResult?) {
                    result?.device?.let { device ->
                        if (device.name?.startsWith("Muse", ignoreCase = true) == true) {
                            Logging.appendData("Found device: ${device.name} - ${device.address}")
                            _scannedDevices.update { current ->
                                if (current.none { it.second == device.address }) {
                                    val newDevice = Pair(device.name, device.address)
                                    if (connectedDevice?.second == device.address) {
                                        current
                                    } else {
                                        current + newDevice
                                    }
                                } else {
                                    current
                                }
                            }
                        }
                    }
                }

                override fun onScanFailed(errorCode: Int) {
                    Logging.appendData("Scan failed with error: $errorCode")
                }
            })
            Logging.appendData("Started Bluetooth scan")
        } catch (e: SecurityException) {
            Logging.appendData("SecurityException: Failed to start scan: ${e.message}")
        }
    }

    fun stopScanning() {
        if (checkBluetoothPermissions(context)) {
            try {
                bluetoothAdapter?.bluetoothLeScanner?.stopScan(object : ScanCallback() {})
                Logging.appendData("Stopped Bluetooth scan")
            } catch (e: SecurityException) {
                Logging.appendData("SecurityException: Failed to stop scan: ${e.message}")
            }
        }
    }

    fun connect(deviceAddress: String) {
        if (checkBluetoothPermissions(context)) {
            try {
                val device = bluetoothAdapter?.getRemoteDevice(deviceAddress)
                if (device == null) {
                    Logging.appendData("Device not found: $deviceAddress")
                    return
                }
                gatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
                } else {
                    @Suppress("DEPRECATION")
                    device.connectGatt(context, false, gattCallback)
                }
                museCommandManager = MuseCommandManager(context) { data, channel ->
                    Logging.appendData("Data for $channel: ${data.size} bytes - ${data.joinToString(", ") { it.toInt().toString() }}")
                }
                Logging.appendData("Connecting to ${device.name} - ${device.address}")
            } catch (e: SecurityException) {
                Logging.appendData("SecurityException: Failed to connect to device")
            }
        } else {
            Logging.appendData("Missing Bluetooth permissions for connection")
        }
    }

    fun disconnect() {
        if (checkBluetoothPermissions(context)) {
            try {
                gatt?.disconnect()
                gatt?.close()
                gatt = null
                connectedDevice = null
                updateScannedDevicesWithConnected()
                Logging.appendData("Disconnected")
            } catch (e: SecurityException) {
                Logging.appendData("SecurityException: Failed to disconnect")
            }
        } else {
            Logging.appendData("Missing permissions to disconnect")
        }
    }
}