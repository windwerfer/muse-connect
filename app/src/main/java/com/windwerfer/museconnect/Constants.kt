package com.windwerfer.museconnect

import java.util.UUID

object Constants {
    const val PERMISSION_REQUEST_CODE = 1
    const val REQUEST_ENABLE_BT = 2
    const val REQUEST_PERMISSION_BLUETOOTH = 3 // Unique value
    const val SCAN_PERIOD: Long = 10000 // Scan for 10 seconds

    // Muse BLE Commands
    val MUSE_PRESET_P51 = byteArrayOf(0x03, 0x70, 0x35, 0x31, 0x0a) // "p51\n" (EEG + PPG)
    val MUSE_PRESET_P21 = byteArrayOf(0x03, 0x70, 0x32, 0x31, 0x0a) // "p21\n" (EEG + PPG)
    val MUSE_START_STREAM = byteArrayOf(0x02, 0x64, 0x0a) // "d\n" (start streaming)
    val MUSE_STATUS_REQUEST = byteArrayOf(0x02, 0x73, 0x0a) // "s\n"

    // Muse BLE UUIDs
    val MUSE_SERVICE_UUID = UUID.fromString("0000fe8d-0000-1000-8000-00805f9b34fb") // Primary service for Muse S
    val MUSE_SERVICE_UUID_LEGACY = UUID.fromString("273e0000-4c4d-454d-96be-f03bac821358") // Legacy service for older Muse models
    val MUSE_GATT_ATTR_STREAM_TOGGLE = UUID.fromString("273e0001-4c4d-454d-96be-f03bac821358")
    val MUSE_GATT_ATTR_LEFTAUX = UUID.fromString("273e0002-4c4d-454d-96be-f03bac821358")
    val MUSE_GATT_ATTR_TP9 = UUID.fromString("273e0003-4c4d-454d-96be-f03bac821358")
    val MUSE_GATT_ATTR_AF7 = UUID.fromString("273e0004-4c4d-454d-96be-f03bac821358")
    val MUSE_GATT_ATTR_AF8 = UUID.fromString("273e0005-4c4d-454d-96be-f03bac821358")
    val MUSE_GATT_ATTR_TP10 = UUID.fromString("273e0006-4c4d-454d-96be-f03bac821358")
    val MUSE_GATT_ATTR_RIGHTAUX = UUID.fromString("273e0007-4c4d-454d-96be-f03bac821358") // Right AUX
    val MUSE_GATT_ATTR_REFDRL = UUID.fromString("273e0008-4c4d-454d-96be-f03bac821358") // Reference DRL
    val MUSE_GATT_ATTR_GYRO = UUID.fromString("273e0009-4c4d-454d-96be-f03bac821358") // Gyroscope
    val MUSE_GATT_ATTR_ACCELEROMETER = UUID.fromString("273e000a-4c4d-454d-96be-f03bac821358") // Accelerometer
    val MUSE_GATT_ATTR_TELEMETRY = UUID.fromString("273e000b-4c4d-454d-96be-f03bac821358") // Telemetry
    val MUSE_GATT_ATTR_MAGNETOMETER = UUID.fromString("273e000c-4c4d-454d-96be-f03bac821358") // Magnetometer
    val MUSE_GATT_ATTR_PRESSURE = UUID.fromString("273e000d-4c4d-454d-96be-f03bac821358") // Pressure
    val MUSE_GATT_ATTR_ULTRAVIOLET = UUID.fromString("273e000e-4c4d-454d-96be-f03bac821358") // Ultraviolet
    val MUSE_GATT_ATTR_PPG1 = UUID.fromString("273e000f-4c4d-454d-96be-f03bac821358") // PPG Ambient
    val MUSE_GATT_ATTR_PPG2 = UUID.fromString("273e0010-4c4d-454d-96be-f03bac821358") // PPG Infrared
    val MUSE_GATT_ATTR_PPG3 = UUID.fromString("273e0011-4c4d-454d-96be-f03bac821358") // PPG Red
    val MUSE_GATT_ATTR_THERMISTOR = UUID.fromString("273e0012-4c4d-454d-96be-f03bac821358") // Thermistor (Muse S)
}