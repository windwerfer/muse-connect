package com.windwerfer.museconnect

import java.util.UUID


object Constants {
    const val PERMISSION_REQUEST_CODE = 1
    const val REQUEST_ENABLE_BT = 2
    const val SCAN_PERIOD: Long = 10000 // Scan for 10 seconds



    // UUIDs for Muse Headband BLE Connection
    val MUSE_PRESET_ID = "p51" // default: 21, but 51 listens to ppg as well file:///C:/Users/watdo/python/0_muse%20sdk/libmuse_android_7.2.2/doc/enumcom_1_1choosemuse_1_1libmuse_1_1_muse_preset.html#a6dd009f41b53e1ab0406420aaba5bb11

    //  00001800-0000-1000-8000-00805f9b34fb Generic Access 0x05-0x0b
    //  00001801-0000-1000-8000-00805f9b34fb Generic Attribute 0x01-0x04
    val MUSE_GATT_ATTR_SERVICECHANGED = UUID.fromString("0000fe8d-0000-1000-8000-00805f9b34fb") // ble std 0x02-0x04
    //  0000fe8d-0000-1000-8000-00805f9b34fb Interaxon Inc. 0x0c-0x42
    val MUSE_GATT_ATTR_STREAM_TOGGLE =  UUID.fromString("273e0001-4c4d-454d-96be-f03bac821358") // serial 0x0d-0x0f
    val MUSE_GATT_ATTR_LEFTAUX =        UUID.fromString("273e0002-4c4d-454d-96be-f03bac821358") // not implemented yet 0x1c-0x1e
    val MUSE_GATT_ATTR_TP9 =            UUID.fromString("273e0003-4c4d-454d-96be-f03bac821358") // 0x1f-0x21
    val MUSE_GATT_ATTR_AF7 =            UUID.fromString("273e0004-4c4d-454d-96be-f03bac821358") // fp1 0x22-0x24
    val MUSE_GATT_ATTR_AF8 =            UUID.fromString("273e0005-4c4d-454d-96be-f03bac821358") // fp2 0x25-0x27
    val MUSE_GATT_ATTR_TP10 =           UUID.fromString("273e0006-4c4d-454d-96be-f03bac821358") // 0x28-0x2a
    val MUSE_GATT_ATTR_RIGHTAUX =       UUID.fromString("273e0007-4c4d-454d-96be-f03bac821358") // 0x2b-0x2d
    val MUSE_GATT_ATTR_REFDRL =         UUID.fromString("273e0008-4c4d-454d-96be-f03bac821358") // not implemented yet 0x10-0x12
    val MUSE_GATT_ATTR_GYRO =           UUID.fromString("273e0009-4c4d-454d-96be-f03bac821358") // 0x13-0x15
    val MUSE_GATT_ATTR_ACCELEROMETER =  UUID.fromString("273e000a-4c4d-454d-96be-f03bac821358") // 0x16-0x18
    val MUSE_GATT_ATTR_TELEMETRY =      UUID.fromString("273e000b-4c4d-454d-96be-f03bac821358") // 0x19-0x1b

    val MUSE_GATT_ATTR_MAGNETOMETER =   UUID.fromString("273e000c-4c4d-454d-96be-f03bac821358") // 0x2e-0x30
    val MUSE_GATT_ATTR_PRESSURE =       UUID.fromString("273e000d-4c4d-454d-96be-f03bac821358") // 0x31-0x33
    val MUSE_GATT_ATTR_ULTRAVIOLET =    UUID.fromString("273e000e-4c4d-454d-96be-f03bac821358") // 0x34-0x36
    val MUSE_GATT_ATTR_PPG1 =           UUID.fromString("273e000f-4c4d-454d-96be-f03bac821358") // ambient 0x37-0x39
    val MUSE_GATT_ATTR_PPG2 =           UUID.fromString("273e0010-4c4d-454d-96be-f03bac821358") // infrared 0x3a-0x3c
    val MUSE_GATT_ATTR_PPG3 =           UUID.fromString("273e0011-4c4d-454d-96be-f03bac821358") // red 0x3d-0x3f
    val MUSE_GATT_ATTR_THERMISTOR =     UUID.fromString("273e0012-4c4d-454d-96be-f03bac821358") // muse S only, not implemented yet 0x40-0x42




    //        const val REQUEST_ENABLE_BT = 1
    const val REQUEST_PERMISSION_BLUETOOTH = 2 // Add this line
}