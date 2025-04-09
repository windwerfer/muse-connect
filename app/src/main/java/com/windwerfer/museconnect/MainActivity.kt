package com.windwerfer.museconnect

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothProfile
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.util.UUID

class MainActivity : AppCompatActivity() {
    private var bluetoothGatt: BluetoothGatt? = null
    private lateinit var dataTextView: TextView

    // Subscription chaining state
    private var currentSubscriptionIndex = 0
    private lateinit var eegChannels: List<UUID>
    private var subscriptionGatt: BluetoothGatt? = null
    private var subscriptionService: BluetoothGattService? = null

    private val gattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            appendData("Connection state: status=$status, newState=$newState")
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    if (checkBluetoothPermissions()) {
                        try {
                            appendData("Connected to ${gatt.device.name}")
                            if (gatt.discoverServices()) appendData("Service discovery initiated")
                        } catch (e: SecurityException) {
                            appendData("SecurityException: Cannot access device name or discover services")
                        }
                    }
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    if (checkBluetoothPermissions()) {
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
                if (checkBluetoothPermissions()) {
                    try {
                        gatt.services.forEach { service -> appendData("Found service: ${service.uuid}") }
                        startEegStream(gatt)
                    } catch (e: SecurityException) {
                        appendData("SecurityException: Cannot access services")
                    }
                }
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            appendData("Write ${characteristic.uuid}: status=$status")
            if (status == BluetoothGatt.GATT_SUCCESS && characteristic.uuid == Constants.MUSE_GATT_ATTR_STREAM_TOGGLE) {
                val currentValue = characteristic.value.joinToString(", ") { it.toInt().toString() }
                when (currentValue) {
                    "4, 112, 50, 49, 10" -> { // "p21\n"
                        if (checkBluetoothPermissions()) {
                            try {
                                characteristic.value = byteArrayOf(0x02, 0x64, 0x0a) // "d\n"
                                val writeResult = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    gatt.writeCharacteristic(characteristic, characteristic.value, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                                } else {
                                    @Suppress("DEPRECATION")
                                    if (gatt.writeCharacteristic(characteristic)) BluetoothGatt.GATT_SUCCESS else BluetoothGatt.GATT_FAILURE
                                }
                                appendData("Initiated write for 'd': result=$writeResult")
                            } catch (e: SecurityException) {
                                appendData("SecurityException: Failed to write 'd'")
                            }
                        }
                    }
                    "2, 100, 10" -> { // "d\n"
                        if (checkBluetoothPermissions()) {
                            try {
                                val service = gatt.getService(Constants.MUSE_SERVICE_UUID)
                                subscribeToEegChannels(gatt, service)
                                // Write status command after starting stream
                                characteristic.value = byteArrayOf(0x02, 0x73, 0x0a) // "s\n"
                                val writeResult = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    gatt.writeCharacteristic(characteristic, characteristic.value, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                                } else {
                                    @Suppress("DEPRECATION")
                                    if (gatt.writeCharacteristic(characteristic)) BluetoothGatt.GATT_SUCCESS else BluetoothGatt.GATT_FAILURE
                                }
                                appendData("Initiated write for 's': result=$writeResult")
                            } catch (e: SecurityException) {
                                appendData("SecurityException: Failed to access service or write 's'")
                            }
                        }
                    }
                    "2, 115, 10" -> { // "s\n"
                        appendData("Status command acknowledged")
                    }
                }
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            appendData("Descriptor write ${descriptor.characteristic.uuid}: status=$status")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (descriptor.characteristic.uuid == Constants.MUSE_GATT_ATTR_STREAM_TOGGLE) {
                    // Write p21 after control subscription
                    descriptor.characteristic.value = byteArrayOf(0x04, 0x70, 0x32, 0x31, 0x0a) // "p21\n"
                    val writeResult = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        gatt.writeCharacteristic(descriptor.characteristic, descriptor.characteristic.value, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                    } else {
                        @Suppress("DEPRECATION")
                        if (gatt.writeCharacteristic(descriptor.characteristic)) BluetoothGatt.GATT_SUCCESS else BluetoothGatt.GATT_FAILURE
                    }
                    appendData("Initiated write for 'p21': result=$writeResult")
                } else {
                    currentSubscriptionIndex++
                    subscribeNextChannel()
                }
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            appendData("onCharacteristicChanged triggered for ${characteristic.uuid}")
            val data = characteristic.value
            val channel = when (characteristic.uuid) {
                Constants.MUSE_GATT_ATTR_STREAM_TOGGLE -> "Control"
                Constants.MUSE_GATT_ATTR_TP9 -> "TP9"
                Constants.MUSE_GATT_ATTR_AF7 -> "AF7"
                Constants.MUSE_GATT_ATTR_AF8 -> "AF8"
                Constants.MUSE_GATT_ATTR_TP10 -> "TP10"
                else -> "Unknown"
            }
            appendData("Data for $channel: ${data.size} bytes - ${data.joinToString(", ") { it.toInt().toString() }}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        dataTextView = findViewById(R.id.dataTextView)

        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val device = bluetoothAdapter?.getRemoteDevice("00:55:DA:B9:59:09") // MuseS-5909 MAC
        device?.let {
            connectToDevice(it)
        } ?: appendData("No Bluetooth device found.")
    }

    private fun connectToDevice(device: BluetoothDevice) {
        if (checkBluetoothPermissions()) {
            try {
                bluetoothGatt = device.connectGatt(this, false, gattCallback)
                appendData("Connecting to ${device.name} - ${device.address}")
            } catch (e: SecurityException) {
                appendData("SecurityException: Failed to connect to device")
            }
        } else {
            appendData("Missing Bluetooth permissions for connection")
            // Request permissions here if needed
        }
    }

    private fun startEegStream(gatt: BluetoothGatt) {
        if (!checkBluetoothPermissions()) return

        try {
            val service = gatt.getService(Constants.MUSE_SERVICE_UUID)
            if (service == null) {
                appendData("Muse service not found.")
                return
            }
            appendData("Muse service found: ${Constants.MUSE_SERVICE_UUID}")

            service.characteristics.forEach { char ->
                val properties = char.properties
                appendData("Characteristic ${char.uuid}: properties=$properties (Read=${properties and BluetoothGattCharacteristic.PROPERTY_READ != 0}, Notify=${properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0})")
            }

            val controlChar = service.getCharacteristic(Constants.MUSE_GATT_ATTR_STREAM_TOGGLE)
            controlChar?.let {
                if (gatt.setCharacteristicNotification(it, true)) {
                    appendData("Enabled notification for control ${it.uuid}")
                    val descriptor = it.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                    descriptor?.let { desc ->
                        desc.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        if (gatt.writeDescriptor(desc)) {
                            appendData("Initiated subscription for control ${it.uuid}")
                        } else {
                            appendData("Failed to initiate subscription for control ${it.uuid}")
                        }
                    } ?: appendData("Descriptor not found for control ${it.uuid}")
                } else {
                    appendData("Failed to enable notification for control ${it.uuid}")
                }
                // Move p21 write to onDescriptorWrite
            } ?: appendData("Control characteristic not found.")
        } catch (e: SecurityException) {
            appendData("SecurityException: Missing permissions.")
        }
    }

    private fun subscribeNextChannel() {
        if (currentSubscriptionIndex >= eegChannels.size) return
        val gatt = subscriptionGatt ?: return
        val service = subscriptionService ?: return
        val uuid = eegChannels[currentSubscriptionIndex]
        val char = service.getCharacteristic(uuid)
        char?.let {
            try {
                if (gatt.setCharacteristicNotification(it, true)) {
                    appendData("Enabled notification for $uuid")
                    val descriptor = it.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                    descriptor?.let { desc ->
                        desc.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        if (gatt.writeDescriptor(desc)) {
                            appendData("Initiated subscription for $uuid")
                        } else {
                            appendData("Failed to initiate subscription for $uuid")
                        }
                    } ?: appendData("Descriptor not found for $uuid")
                } else {
                    appendData("Failed to enable notification for $uuid")
                }
            } catch (e: SecurityException) {
                appendData("SecurityException: Failed to subscribe to $uuid")
            }
        } ?: appendData("EEG characteristic $uuid not found.")
    }

    private fun subscribeToEegChannels(gatt: BluetoothGatt, service: BluetoothGattService) {
        if (!checkBluetoothPermissions()) {
            appendData("Missing Bluetooth permissions for EEG subscriptions")
            return
        }
        eegChannels = listOf(
            Constants.MUSE_GATT_ATTR_TP9,
            Constants.MUSE_GATT_ATTR_AF7,
            Constants.MUSE_GATT_ATTR_AF8,
            Constants.MUSE_GATT_ATTR_TP10
        )
        currentSubscriptionIndex = 0
        subscriptionGatt = gatt
        subscriptionService = service
        subscribeNextChannel()
    }

    private fun checkBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun appendData(text: String) {
        runOnUiThread {
            dataTextView.append("$text\n")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (checkBluetoothPermissions()) {
            try {
                bluetoothGatt?.close()
                bluetoothGatt = null
                appendData("Bluetooth GATT closed")
            } catch (e: SecurityException) {
                appendData("SecurityException: Failed to close Bluetooth GATT")
            }
        } else {
            appendData("Missing permissions to close Bluetooth GATT")
            bluetoothGatt = null
        }
    }
}