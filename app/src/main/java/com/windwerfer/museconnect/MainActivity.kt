package com.windwerfer.museconnect

import android.Manifest
import android.app.Activity
import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.GestureDetector
import android.content.ClipboardManager
import android.view.MotionEvent
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var bluetoothGatt: BluetoothGatt? = null

    private lateinit var textViewData: TextView
    private lateinit var buttonScan: Button
    private lateinit var buttonConnect: Button
    private lateinit var spinnerDevices: Spinner

    private lateinit var spinnerAdapter: ArrayAdapter<String>
    private val deviceList = mutableListOf<BluetoothDevice>()
    private val deviceMap = mutableMapOf<String, BluetoothDevice>()

    private var scanning = false
    private var scanCallback: ScanCallback? = null
    private val handler = Handler(Looper.getMainLooper())

    private val bluetoothPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN
        )
    } else {
        arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textViewData = findViewById(R.id.textViewData)
        buttonScan = findViewById(R.id.buttonScan)
        buttonConnect = findViewById(R.id.buttonConnect)
        spinnerDevices = findViewById(R.id.spinnerDevices)

        // Make TextView selectable and set up double-tap listener

// Set up GestureDetector for double-tap
        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                Log.d("xxx", "Double-tap detected")
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Muse Data", textViewData.text)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this@MainActivity, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                return true
            }
        })

        // Set touch listener on TextView
        textViewData.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false // Allow ScrollView to handle scrolling
        }

        spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, mutableListOf())
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDevices.adapter = spinnerAdapter

        buttonScan.setOnClickListener { startBleScan() }
        buttonConnect.setOnClickListener { connectToSelectedDevice() }

        Log.d("xxx", "onCreate")
        requestPermissions()
        Log.d("xxx", "requestPermissions")
        initializeBluetooth()
        Log.d("xxx", "initializeBluetooth")
        startBleScan()
        Log.d("xxx", "first scanBT started")
    }

    private fun requestPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray(), Constants.PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            Constants.REQUEST_PERMISSION_BLUETOOTH -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    Log.d("xxx initBT", "All Bluetooth permissions granted")
                } else {
                    Log.d("xxx initBT", "Bluetooth permissions not granted")
                    appendData("Permissions denied. Cannot proceed.")
                }
            }
        }
    }

    private fun initializeBluetooth() {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        if (bluetoothAdapter == null) {
            Log.d("xxx initBT", "Bluetooth not supported")
            appendData("Bluetooth not supported.")
            finish()
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            if (checkBluetoothPermissions()) {
                try {
                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    startActivityForResult(enableBtIntent, Constants.REQUEST_ENABLE_BT)
                    Log.d("xxx initBT", "Requesting Bluetooth enable")
                } catch (e: SecurityException) {
                    appendData("SecurityException: Missing permissions to enable Bluetooth.")
                }
            }
        } else {
            Log.d("xxx initBT", "Bluetooth already enabled")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Constants.REQUEST_ENABLE_BT) {
            if (resultCode != Activity.RESULT_OK) {
                Toast.makeText(this, "Bluetooth must be enabled to use this app", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun startBleScan() {
        if (!checkBluetoothPermissions()) return

        Log.d("xxx scanBT", "scan start")
        if (!bluetoothAdapter.isEnabled) {
            Toast.makeText(this, "Bluetooth is not enabled", Toast.LENGTH_SHORT).show()
            return
        }

        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
        deviceList.clear()
        deviceMap.clear()
        spinnerAdapter.clear()

        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device
                try {
                    val deviceName = device.name ?: "Unknown"
                    val deviceAddress = device.address
                    val displayName = "$deviceName - $deviceAddress"

                    if (deviceName.startsWith("muse", ignoreCase = true) && !deviceMap.containsKey(deviceAddress)) {
                        deviceList.add(device)
                        deviceMap[deviceAddress] = device
                        runOnUiThread {
                            spinnerAdapter.add(displayName)
                            appendData("Found: $displayName")
                        }
                    }
                } catch (e: SecurityException) {
                    appendData("SecurityException: Cannot access device name.")
                }
            }

            override fun onScanFailed(errorCode: Int) {
                Toast.makeText(this@MainActivity, "Scan failed with error: $errorCode", Toast.LENGTH_SHORT).show()
            }
        }

        try {
            bluetoothLeScanner?.startScan(scanCallback)
            scanning = true
            Toast.makeText(this, "Scanning for devices...", Toast.LENGTH_SHORT).show()
            handler.postDelayed({
                if (scanning) {
                    if (checkBluetoothPermissions()) {
                        try {
                            bluetoothLeScanner?.stopScan(scanCallback)
                            scanning = false
                            Toast.makeText(this, "Scan complete", Toast.LENGTH_SHORT).show()
                        } catch (e: SecurityException) {
                            appendData("SecurityException: Missing permissions to stop scan.")
                        }
                    }
                }
            }, Constants.SCAN_PERIOD)
        } catch (e: SecurityException) {
            appendData("SecurityException: Missing permissions for scanning.")
        }
    }

    private fun connectToSelectedDevice() {
        if (!checkBluetoothPermissions()) return

        val selectedPosition = spinnerDevices.selectedItemPosition
        if (selectedPosition in deviceList.indices) {
            val device = deviceList[selectedPosition]
            Log.d("xxx connBT", "connBT: $device")
            connectToDevice(device)
        } else {
            Toast.makeText(this, "No device selected", Toast.LENGTH_SHORT).show()
        }
    }

    private fun connectToDevice(device: BluetoothDevice) {
        if (!checkBluetoothPermissions()) return

        try {
            bluetoothGatt?.close()
            bluetoothGatt = null
        } catch (e: SecurityException) {
            appendData("SecurityException: Missing permissions to close GATT.")
        }

        try {
            bluetoothGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                device.connectGatt(this, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
            } else {
                device.connectGatt(this, false, gattCallback)
            }
            Toast.makeText(this, "Connecting to ${device.name}", Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            appendData("SecurityException: Missing permissions for GATT connection.")
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Connected to device", Toast.LENGTH_SHORT).show()
                        try {
                            appendData("Connected to ${gatt.device.name}")
                        } catch (e: SecurityException) {
                            appendData("SecurityException: Cannot access device name.")
                        }
                    }
                    if (checkBluetoothPermissions()) {
                        try {
                            gatt.discoverServices()
                        } catch (e: SecurityException) {
                            appendData("SecurityException: Missing permissions for service discovery.")
                        }
                    }
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Disconnected from device", Toast.LENGTH_SHORT).show()
                        try {
                            appendData("Disconnected from ${gatt.device.name}")
                        } catch (e: SecurityException) {
                            appendData("SecurityException: Cannot access device name.")
                        }
                    }
                    bluetoothGatt = null
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                appendData("Services discovered.")
                // Log all available services for debugging
                val services = gatt.services
                services.forEach { service ->
                    val uuid = service.uuid.toString()
                    appendData("Found service: $uuid")
                    Log.d("xxx gatt", "Service UUID: $uuid")
                }
                if (checkBluetoothPermissions()) {
                    startEegStream(gatt)
                }
            } else {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Service discovery failed: $status", Toast.LENGTH_SHORT).show()
                    appendData("Service discovery failed: $status")
                }
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val data = characteristic.value?.toString(Charsets.UTF_8) ?: "null"
                runOnUiThread {
                    appendData("Read: $data")
                }
            } else {
                appendData("Read failed for ${characteristic.uuid}: status $status")
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            val data = characteristic.value
            val channel = when (characteristic.uuid) {
                Constants.MUSE_GATT_ATTR_TP9 -> "TP9"
                Constants.MUSE_GATT_ATTR_AF7 -> "AF7"
                Constants.MUSE_GATT_ATTR_AF8 -> "AF8"
                Constants.MUSE_GATT_ATTR_TP10 -> "TP10"
                else -> "Unknown"
            }
            // Log raw data for debugging
            Log.d("xxx eeg", "Raw data for $channel: ${data.joinToString(", ") { it.toInt().toString() }}")
            appendData("Received data for $channel: ${data.size} bytes") // Confirm data arrival

            if (data.size >= 26) { // 2 bytes timestamp + 12 * 2 bytes samples
                val timestamp = (data[0].toInt() shl 8) or data[1].toInt()
                val samples = (2 until data.size step 2).map { i ->
                    ((data[i].toInt() shl 8) or data[i + 1].toInt()).toDouble() / 1000.0 // Scale to uV
                }
                appendData("$channel (t=$timestamp): ${samples.joinToString(", ")}")
            } else {
                appendData("$channel: Unexpected data size (${data.size} bytes)")
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            appendData("Write ${characteristic.uuid}: status=$status")
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            appendData("Descriptor write ${descriptor.characteristic.uuid}: status=$status")
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

            // Log characteristic properties
            service.characteristics.forEach { char ->
                val properties = char.properties
                appendData("Characteristic ${char.uuid}: properties=$properties (Read=${properties and BluetoothGattCharacteristic.PROPERTY_READ != 0}, Notify=${properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0})")
            }

            val controlChar = service.getCharacteristic(Constants.MUSE_GATT_ATTR_STREAM_TOGGLE)
            controlChar?.let {
                it.value = byteArrayOf(0x03, 0x70, 0x32, 0x31, 0x0a) // "p21\n" (EEG-only)
                if (gatt.writeCharacteristic(it)) appendData("Wrote preset 'p21'")
                else appendData("Failed to write preset 'p21'")
                Thread.sleep(100)
                it.value = byteArrayOf(0x02, 0x64, 0x0a) // "d\n"
                if (gatt.writeCharacteristic(it)) appendData("Wrote start command 'd'")
                else appendData("Failed to write start command 'd'")
                Thread.sleep(100)
                it.value = byteArrayOf(0x02, 0x73, 0x0a) // "s\n"
                if (gatt.writeCharacteristic(it)) appendData("Wrote status request 's'")
                else appendData("Failed to write status request 's'")

                // Subscribe to control for status response
                if (gatt.setCharacteristicNotification(it, true)) {
                    val descriptor = it.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                    descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    if (gatt.writeDescriptor(descriptor)) appendData("Subscribed to control channel")
                    else appendData("Failed to subscribe to control channel")
                }
            } ?: appendData("Control characteristic not found.")

            listOf(
                Constants.MUSE_GATT_ATTR_TP9,
                Constants.MUSE_GATT_ATTR_AF7,
                Constants.MUSE_GATT_ATTR_AF8,
                Constants.MUSE_GATT_ATTR_TP10
            ).forEach { uuid ->
                val char = service.getCharacteristic(uuid)
                char?.let {
                    if (gatt.setCharacteristicNotification(it, true)) {
                        appendData("Enabled notification for $uuid")
                        val descriptor = it.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                        descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        if (gatt.writeDescriptor(descriptor)) appendData("Subscribed to EEG channel: $uuid")
                        else appendData("Failed to subscribe to $uuid")
                    } else {
                        appendData("Failed to enable notification for $uuid")
                    }
                } ?: appendData("EEG characteristic $uuid not found.")
            }
        } catch (e: SecurityException) {
            appendData("SecurityException: Missing permissions.")
        }
    }

    private fun checkBluetoothPermissions(): Boolean {
        val missingPermissions = bluetoothPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missingPermissions.isNotEmpty()) {
            appendData("Missing permissions: $missingPermissions")
            ActivityCompat.requestPermissions(
                this,
                missingPermissions.toTypedArray(),
                Constants.REQUEST_PERMISSION_BLUETOOTH
            )
            return false
        }
        return true
    }

    private fun appendData(data: String) {
        runOnUiThread {
            if (textViewData.text.toString() == "Data will appear here") {
                textViewData.text = "" // Clear default text on first log
            }
            textViewData.append("$data\n")
            val layout = textViewData.layout
            if (layout != null) {
                val scrollAmount = layout.getLineTop(textViewData.lineCount) - textViewData.height
                if (scrollAmount > 0) textViewData.scrollTo(0, scrollAmount)
                else textViewData.scrollTo(0, 0)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (checkBluetoothPermissions()) {
            try {
                bluetoothGatt?.close()
                bluetoothGatt = null
            } catch (e: SecurityException) {
                appendData("SecurityException: Missing permissions to close GATT.")
            }
            if (scanning) {
                try {
                    bluetoothLeScanner?.stopScan(scanCallback)
                    scanning = false
                } catch (e: SecurityException) {
                    appendData("SecurityException: Missing permissions to stop scan.")
                }
            }
        }
    }
}