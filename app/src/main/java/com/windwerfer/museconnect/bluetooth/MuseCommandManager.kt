package com.windwerfer.museconnect.bluetooth

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.content.Context
import android.os.Build
import com.windwerfer.museconnect.Constants
import com.windwerfer.museconnect.utils.Logging
import com.windwerfer.museconnect.utils.checkBluetoothPermissions
import java.util.UUID

class MuseCommandManager(
    private val context: Context,
    private val onDataReceived: (ByteArray, String) -> Unit
) {
    private var lastCommand: String? = null
    private var currentSubscriptionIndex = 0
    private lateinit var eegChannels: List<UUID>
    private var gatt: BluetoothGatt? = null
    private var service: BluetoothGattService? = null

    private val gattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            Logging.appendData("Write ${characteristic.uuid}: status=$status")
            if (status == BluetoothGatt.GATT_SUCCESS && characteristic.uuid == Constants.MUSE_GATT_ATTR_STREAM_TOGGLE) {
                Logging.appendData("Last command sent: $lastCommand")
                when (lastCommand) {
                    "p21" -> {
                        if (checkBluetoothPermissions(context)) {
                            try {
                                characteristic.value = byteArrayOf(0x02, 0x64, 0x0a) // "d\n"
                                lastCommand = "d"
                                val writeResult = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    gatt.writeCharacteristic(
                                        characteristic,
                                        characteristic.value,
                                        BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                                    )
                                } else {
                                    @Suppress("DEPRECATION")
                                    if (gatt.writeCharacteristic(characteristic)) BluetoothGatt.GATT_SUCCESS else BluetoothGatt.GATT_FAILURE
                                }
                                Logging.appendData("Initiated write for 'd': result=$writeResult")
                            } catch (e: SecurityException) {
                                Logging.appendData("SecurityException: Failed to write 'd': ${e.message}")
                            }
                        } else {
                            Logging.appendData("Missing permissions to write 'd'")
                        }
                    }
                    "d" -> {
                        if (checkBluetoothPermissions(context)) {
                            try {
                                Logging.appendData("Attempting to subscribe to EEG channels")
                                val service = gatt.getService(Constants.MUSE_SERVICE_UUID)
                                if (service == null) {
                                    Logging.appendData("Service not found for EEG subscriptions")
                                } else {
                                    Logging.appendData("Service found for EEG subscriptions: ${service.uuid}")
                                    subscribeToEegChannels(gatt, service)
                                }
                            } catch (e: SecurityException) {
                                Logging.appendData("SecurityException: Failed to access service for subscriptions: ${e.message}")
                            }
                        } else {
                            Logging.appendData("Missing permissions to access service for subscriptions")
                        }
                    }
                }
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            Logging.appendData("Descriptor write ${descriptor.characteristic.uuid}: status=$status")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (descriptor.characteristic.uuid == Constants.MUSE_GATT_ATTR_STREAM_TOGGLE) {
                    if (checkBluetoothPermissions(context)) {
                        try {
                            descriptor.characteristic.value =
                                byteArrayOf(0x04, 0x70, 0x32, 0x31, 0x0a) // "p21\n"
                            lastCommand = "p21"
                            val writeResult = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                gatt.writeCharacteristic(
                                    descriptor.characteristic,
                                    descriptor.characteristic.value,
                                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                                )
                            } else {
                                @Suppress("DEPRECATION")
                                if (gatt.writeCharacteristic(descriptor.characteristic)) BluetoothGatt.GATT_SUCCESS else BluetoothGatt.GATT_FAILURE
                            }
                            Logging.appendData("Initiated write for 'p21': result=$writeResult")
                        } catch (e: SecurityException) {
                            Logging.appendData("SecurityException: Failed to write 'p21': ${e.message}")
                        }
                    } else {
                        Logging.appendData("Missing permissions to write 'p21'")
                    }
                } else {
                    currentSubscriptionIndex++
                    subscribeNextChannel()
                }
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            Logging.appendData("onCharacteristicChanged triggered for ${characteristic.uuid}")
            val data = characteristic.value
            val channel = when (characteristic.uuid) {
                Constants.MUSE_GATT_ATTR_STREAM_TOGGLE -> "Control"
                Constants.MUSE_GATT_ATTR_TP9 -> "TP9"
                Constants.MUSE_GATT_ATTR_AF7 -> "AF7"
                Constants.MUSE_GATT_ATTR_AF8 -> "AF8"
                Constants.MUSE_GATT_ATTR_TP10 -> "TP10"
                else -> "Unknown"
            }
            onDataReceived(data, channel)
        }
    }

    fun startEegStream(gatt: BluetoothGatt) {
        this.gatt = gatt
        service = gatt.getService(Constants.MUSE_SERVICE_UUID) ?: run {
            Logging.appendData("Muse service not found")
            return
        }
        Logging.appendData("Muse service found: ${Constants.MUSE_SERVICE_UUID}")

        val controlChar = service?.getCharacteristic(Constants.MUSE_GATT_ATTR_STREAM_TOGGLE)
        controlChar?.let {
            if (checkBluetoothPermissions(context)) {
                try {
                    if (gatt.setCharacteristicNotification(it, true)) {
                        Logging.appendData("Enabled notification for control ${it.uuid}")
                        val descriptor =
                            it.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                        descriptor?.let { desc ->
                            desc.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            if (gatt.writeDescriptor(desc)) {
                                Logging.appendData("Initiated subscription for control ${it.uuid}")
                            } else {
                                Logging.appendData("Failed to initiate subscription for control ${it.uuid}")
                            }
                        } ?: Logging.appendData("Descriptor not found for control ${it.uuid}")
                    } else {
                        Logging.appendData("Failed to enable notification for control ${it.uuid}")
                    }
                } catch (e: SecurityException) {
                    Logging.appendData("SecurityException: Failed to subscribe to control: ${e.message}")
                }
            } else {
                Logging.appendData("Missing permissions to subscribe to control")
            }
        } ?: Logging.appendData("Control characteristic not found")
    }

    private fun subscribeToEegChannels(gatt: BluetoothGatt, service: BluetoothGattService) {
        if (!checkBluetoothPermissions(context)) {
            Logging.appendData("Missing Bluetooth permissions for EEG subscriptions")
            return
        }
        eegChannels = listOf(
            Constants.MUSE_GATT_ATTR_TP9,
            Constants.MUSE_GATT_ATTR_AF7,
            Constants.MUSE_GATT_ATTR_AF8,
            Constants.MUSE_GATT_ATTR_TP10
        )
        currentSubscriptionIndex = 0
        this.gatt = gatt
        this.service = service
        subscribeNextChannel()
    }

    private fun subscribeNextChannel() {
        if (currentSubscriptionIndex >= eegChannels.size) return
        val gatt = this.gatt ?: return
        val service = this.service ?: return
        val uuid = eegChannels[currentSubscriptionIndex]
        val char = service.getCharacteristic(uuid)
        char?.let {
            if (checkBluetoothPermissions(context)) {
                try {
                    if (gatt.setCharacteristicNotification(it, true)) {
                        Logging.appendData("Enabled notification for $uuid")
                        val descriptor =
                            it.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                        descriptor?.let { desc ->
                            desc.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            if (gatt.writeDescriptor(desc)) {
                                Logging.appendData("Initiated subscription for $uuid")
                            } else {
                                Logging.appendData("Failed to initiate subscription for $uuid")
                            }
                        } ?: Logging.appendData("Descriptor not found for $uuid")
                    } else {
                        Logging.appendData("Failed to enable notification for $uuid")
                    }
                } catch (e: SecurityException) {
                    Logging.appendData("SecurityException: Failed to subscribe to $uuid: ${e.message}")
                }
            } else {
                Logging.appendData("Missing permissions to subscribe to $uuid")
            }
        } ?: Logging.appendData("EEG characteristic $uuid not found")
    }
}