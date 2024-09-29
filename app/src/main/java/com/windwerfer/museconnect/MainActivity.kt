package com.windwerfer.museconnect

import android.util.Log;
import android.Manifest
import android.app.Activity
import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.*
import android.os.*
import android.widget.*

import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity

import java.util.UUID

import com.windwerfer.museconnect.Constants
import com.windwerfer.museconnect.Constants.PERMISSION_REQUEST_CODE
import com.windwerfer.museconnect.Constants.REQUEST_ENABLE_BT
import com.windwerfer.museconnect.Constants.REQUEST_PERMISSION_BLUETOOTH
import com.windwerfer.museconnect.Constants.SCAN_PERIOD

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


    private val bluetoothPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN
        )
    } else {
        arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION // Required for BLE operations
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set the content view to the layout
        setContentView(R.layout.activity_main)

        // Initialize UI components
        textViewData = findViewById(R.id.textViewData)
        buttonScan = findViewById(R.id.buttonScan)
        buttonConnect = findViewById(R.id.buttonConnect)
        spinnerDevices = findViewById(R.id.spinnerDevices)

        // Initialize spinner adapter
        spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, mutableListOf())
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDevices.adapter = spinnerAdapter

        // Set up button listeners
        buttonScan.setOnClickListener { startBleScan() }
        buttonConnect.setOnClickListener { connectToSelectedDevice() }

        Log.d("xxx","onCreate")
        requestPermissions()
        Log.d("xxx","requestPermissions")

        initializeBluetooth()
        Log.d("xxx","initializeBluetooth")

        startBleScan()
        Log.d("xxx","fist scanBT started")
    }

    // Request runtime permissions
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
            ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray(), PERMISSION_REQUEST_CODE)
        }
    }




    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_PERMISSION_BLUETOOTH -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    Log.d("xxx initBT", "All Bluetooth permissions granted")
                    // You might want to call initializeBluetooth() again or proceed with enabling Bluetooth
                } else {
                    Log.d("xxx initBT", "Bluetooth permissions not granted")
                }
            }
        }
    }

    // Initialize Bluetooth
    private fun initializeBluetooth() {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter


        Log.d("xxx initBT", "init start vars: ${bluetoothAdapter.isEnabled} ")

        if (bluetoothAdapter != null && (bluetoothAdapter.isEnabled)) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)

            // Check for permissions based on SDK version
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val missingPermissions = bluetoothPermissions.filter {
                    ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
                }

                if (missingPermissions.isNotEmpty()) {
                    Log.d("xxx initBT", "Requesting missing permissions: $missingPermissions")
                    requestPermissions(bluetoothPermissions, REQUEST_PERMISSION_BLUETOOTH)
                    return
                }
            }

            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            Log.d("xxx initBT", "init good")
        } else if (bluetoothAdapter == null) {
            Log.d("xxx initBT", "init impossible, bluetoothAdapter not available")
        } else {
            Log.d("xxx initBT", "Bluetooth is already enabled")
        }
        Log.d("xxx initBT", "init end")
    }

    // Handle Bluetooth enabling result
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode != Activity.RESULT_OK) {
                Toast.makeText(this, "Bluetooth must be enabled to use this app", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    // Start BLE scan
    private fun startBleScan() {
        Log.d("xxx scanBT", "scan start")
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
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
                // Check for permissions based on SDK version

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val missingPermissions = bluetoothPermissions.filter {
                        ActivityCompat.checkSelfPermission(this@MainActivity, it) != PackageManager.PERMISSION_GRANTED
                    }

                    if (missingPermissions.isNotEmpty()) {
                        Log.d("xxx scanBT", "Requesting missing permissions: $missingPermissions")
                        requestPermissions(bluetoothPermissions, REQUEST_PERMISSION_BLUETOOTH)
                        return
                    }
                }

                val deviceName = device.name ?: "Unknown"
                val deviceAddress = device.address
                val displayName = "$deviceName - $deviceAddress"

                Log.d("xxx scanBT", "scanBT found device: ${deviceName}")

                if (!deviceName.startsWith("muse", ignoreCase = true)) {
                    return
                }

                if (!deviceMap.containsKey(deviceAddress)) {
                    deviceList.add(device)
                    deviceMap[deviceAddress] = device
                    runOnUiThread {
                        spinnerAdapter.add(displayName)
                    }
                }
            }

            override fun onScanFailed(errorCode: Int) {
                Toast.makeText(this@MainActivity, "Scan failed with error: $errorCode", Toast.LENGTH_SHORT).show()
            }
        }

        // Check for permissions based on SDK version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val missingPermissions = bluetoothPermissions.filter {
                ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }

            if (missingPermissions.isNotEmpty()) {
                Log.d("xxx initBT", "Requesting missing permissions: $missingPermissions")
                requestPermissions(bluetoothPermissions, REQUEST_PERMISSION_BLUETOOTH)
                return
            }
        }
        bluetoothLeScanner?.startScan(scanCallback)
        scanning = true
        Toast.makeText(this, "Scanning for devices...", Toast.LENGTH_SHORT).show()

        // Stop scan after a predefined period
        Handler(Looper.getMainLooper()).postDelayed({
            if (scanning) {
                bluetoothLeScanner?.stopScan(scanCallback)
                scanning = false
                Toast.makeText(this, "Scan complete", Toast.LENGTH_SHORT).show()
            }
        }, SCAN_PERIOD)
    }

    // Connect to the selected device
    private fun connectToSelectedDevice() {
        val selectedPosition = spinnerDevices.selectedItemPosition
        if (selectedPosition in deviceList.indices) {
            val device = deviceList[selectedPosition]
            Log.d("xxx connBT","connBT: ${device}")
            connectToDevice(device)
        } else {
            Toast.makeText(this, "No device selected", Toast.LENGTH_SHORT).show()
        }
    }

    // Connect to the device
    private fun connectToDevice(device: BluetoothDevice) {
        // Check for permissions based on SDK version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val missingPermissions = bluetoothPermissions.filter {
                ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }

            if (missingPermissions.isNotEmpty()) {
                Log.d("xxx initBT", "Requesting missing permissions: $missingPermissions")
                requestPermissions(bluetoothPermissions, REQUEST_PERMISSION_BLUETOOTH)
                return
            }
        }
        bluetoothGatt?.close()
        bluetoothGatt = null

        bluetoothGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            device.connectGatt(this, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        } else {
            device.connectGatt(this, false, gattCallback)
        }

        Toast.makeText(this, "Connecting to ${device.name}", Toast.LENGTH_SHORT).show()
    }

    // BluetoothGattCallback implementation
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Connected to device", Toast.LENGTH_SHORT).show()
                    }

                    // Check for permissions based on SDK version
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val missingPermissions = bluetoothPermissions.filter {
                            ActivityCompat.checkSelfPermission(this@MainActivity, it) != PackageManager.PERMISSION_GRANTED
                        }

                        if (missingPermissions.isNotEmpty()) {
                            Log.d("xxx callback", "Requesting missing permissions: $missingPermissions")
                            requestPermissions(bluetoothPermissions, REQUEST_PERMISSION_BLUETOOTH)
                            return
                        }
                    }
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Disconnected from device", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                findAndSetCharacteristic(gatt)
            } else {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Service discovery failed", Toast.LENGTH_SHORT).show()
                }
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val data = characteristic.getStringValue(0)
                runOnUiThread {
                    appendData("Read: ${data ?: "null"}")
                }
            }
        }


        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            val data = characteristic.getStringValue(0)
            runOnUiThread {
                appendData("Notification: $data")
            }
        }
    }

    private fun writeCommandToCharacteristic(characteristic: BluetoothGattCharacteristic, command: String) {
        characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        characteristic.value = command.toByteArray(Charsets.UTF_8)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val missingPermissions = bluetoothPermissions.filter {
                ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }

            if (missingPermissions.isNotEmpty()) {
                Log.d("xxx initBT", "Requesting missing permissions: $missingPermissions")
                requestPermissions(bluetoothPermissions, REQUEST_PERMISSION_BLUETOOTH)
                return
            }
        }
        bluetoothGatt?.writeCharacteristic(characteristic)
    }


//    private fun findAndSetCharacteristic(gatt: BluetoothGatt) {
//        val service = gatt.getService(Constants.MUSE_GATT_ATTR_SERVICECHANGED)
//        if (service != null) {
//            // Get the control characteristic
//            val controlCharacteristic = service.getCharacteristic(Constants.MUSE_GATT_ATTR_TP9)
//            if (controlCharacteristic != null) {
//                // Write commands to enable EEG data and start streaming
////                writeCommandToCharacteristic(controlCharacteristic, MUSE_PRESET_ID) // Enable EEG data
////                writeCommandToCharacteristic(controlCharacteristic, "s")   // Start streaming
//            } else {
//                runOnUiThread {
//                    Toast.makeText(this, "Control characteristic not found", Toast.LENGTH_SHORT).show()
//                }
//                return
//            }
//
////            // Get the data characteristic
////            val dataCharacteristic = service.getCharacteristic(TP9_CHARACTERISTIC_UUID)
////            if (dataCharacteristic != null) {
////                // Enable notifications on the data characteristic
////                setCharacteristicNotification(dataCharacteristic, true)
////            } else {
////                runOnUiThread {
////                    Toast.makeText(this, "Data characteristic not found", Toast.LENGTH_SHORT).show()
////                }
////            }
//        } else {
//            runOnUiThread {
//                Toast.makeText(this, "Service not found", Toast.LENGTH_SHORT).show()
//            }
//        }
//    }

    // Find and set the characteristic
    private fun findAndSetCharacteristic(gatt: BluetoothGatt) {
        val service = gatt.getService(Constants.MUSE_GATT_ATTR_SERVICECHANGED)
        if (service != null) {
            val characteristic = service.getCharacteristic(Constants.MUSE_GATT_ATTR_TP9)
            if (characteristic != null) {
                // Read initial value
                // Check for permissions based on SDK version
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val missingPermissions = bluetoothPermissions.filter {
                        ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
                    }

                    if (missingPermissions.isNotEmpty()) {
                        Log.d("xxx initBT", "Requesting missing permissions: $missingPermissions")
                        requestPermissions(bluetoothPermissions, REQUEST_PERMISSION_BLUETOOTH)
                        return
                    }
                }

                gatt.readCharacteristic(characteristic)
                // Enable notifications
                setCharacteristicNotification(characteristic, true)
            } else {
                runOnUiThread {
                    Toast.makeText(this, "Characteristic not found", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            runOnUiThread {
                Toast.makeText(this, "Service not found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Set characteristic notification
    private fun setCharacteristicNotification(characteristic: BluetoothGattCharacteristic, enabled: Boolean) {
        // Check for permissions based on SDK version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val missingPermissions = bluetoothPermissions.filter {
                ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }

            if (missingPermissions.isNotEmpty()) {
                Log.d("xxx initBT", "Requesting missing permissions: $missingPermissions")
                requestPermissions(bluetoothPermissions, REQUEST_PERMISSION_BLUETOOTH)
                return
            }
        }
        bluetoothGatt?.setCharacteristicNotification(characteristic, enabled)
//        val descriptor = characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
        val descriptor = characteristic.getDescriptor(Constants.MUSE_GATT_ATTR_STREAM_TOGGLE)
        descriptor?.let {
            it.value = if (enabled) BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE else BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
            bluetoothGatt?.writeDescriptor(it)
        }
    }

    // Append data to TextView
    private fun appendData(data: String) {
        textViewData.append("$data\n")
        val layout = textViewData.layout
        if (layout != null) {
            val scrollAmount = layout.getLineTop(textViewData.lineCount) - textViewData.height
            if (scrollAmount > 0)
                textViewData.scrollTo(0, scrollAmount)
            else
                textViewData.scrollTo(0, 0)
        }
    }


    // Clean up resources
    override fun onDestroy() {
        super.onDestroy()
        // Check for permissions based on SDK version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val missingPermissions = bluetoothPermissions.filter {
                ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }

            if (missingPermissions.isNotEmpty()) {
                Log.d("xxx initBT", "Requesting missing permissions: $missingPermissions")
                requestPermissions(bluetoothPermissions, REQUEST_PERMISSION_BLUETOOTH)
                return
            }
        }
        bluetoothGatt?.close()
        bluetoothGatt = null
    }
}
