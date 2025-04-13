package com.windwerfer.museconnect.bluetooth

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.os.Build
import com.windwerfer.museconnect.appendData
import java.util.UUID

class MuseCommandManager(private val onDataReceived: (ByteArray, String) -> Unit) {
    private var lastCommand: String? = null
    private var currentSubscriptionIndex = 0
    private lateinit var eegChannels: List<UUID>
    private var gatt: BluetoothGatt? = null
    private var service: BluetoothGattService? = null

    fun startEegStream(gatt: BluetoothGatt) {
        this.gatt = gatt
        service = gatt.getService(Constants.MUSE_SERVICE_UUID) ?: run {
            appendData("Muse service not found")
            return
        }
        appendData("Muse service found: ${Constants.MUSE_SERVICE_UUID}")

        val controlChar = service?.getCharacteristic(Constants.MUSE_GATT_ATTR_STREAM_TOGGLE)
        controlChar?.let {
            gatt.setCharacteristicNotification(it, true)
            appendData("Enabled notification for control ${it.uuid}")
            val descriptor = it.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
            descriptor?.let { desc ->
                desc.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                gatt.writeDescriptor(desc)
                appendData("Initiated subscription for control ${it.uuid}")
            }
        }
    }

    // Move your current gattCallback logic here
}