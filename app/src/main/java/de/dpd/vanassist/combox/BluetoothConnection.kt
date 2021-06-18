package de.dpd.vanassist.combox

import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Handler
import android.util.Log
import de.dpd.vanassist.activity.MapActivity
import java.util.*
import java.util.concurrent.ArrayBlockingQueue

data class GattCall(val operation: String, val characteristic: BluetoothGattCharacteristic, val value: ByteArray?)

class BluetoothConnection(val context: Context, val gattCallback: ComboxGattCallback) {

    private val SCAN_PERIOD: Long = 6000

    private val bluetoothAdapter: BluetoothAdapter by lazy(LazyThreadSafetyMode.NONE) {
        val bluetoothManager =
            context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private val handler = Handler()

    private var gattBusy = false
    private var notificationUUID: UUID? = null

    private var queue = ArrayBlockingQueue<GattCall>(20)

    private var isConnected = false

    var bluetoothGatt: BluetoothGatt? = null

    private val notifications = mutableListOf<BluetoothGattCharacteristic>()


    fun scanForDevice(address: String, enable: Boolean) {
        when (enable) {
            true -> {
                val enabled = bluetoothAdapter.isEnabled()
                if (enabled) {
                    // Stops scanning after a pre-defined scan period.
                    handler.postDelayed({
                        bluetoothAdapter.bluetoothLeScanner?.stopScan(leScanCallback)
                    }, SCAN_PERIOD)

                    val filter =
                        listOf(ScanFilter.Builder().apply { setDeviceName(address) }.build())
                    val settings =
                        ScanSettings.Builder()
                            .apply { setScanMode(ScanSettings.SCAN_MODE_BALANCED) }.build();

                    bluetoothAdapter.bluetoothLeScanner?.startScan(
                        filter,
                        settings,
                        leScanCallback
                    )
                } else {
                    gattCallback.onConnnectionFailure("Bluetooth not available")
                }
            }
            else -> {
                bluetoothAdapter.bluetoothLeScanner?.stopScan(leScanCallback)
            }
        }
    }

    fun writeCharacteristic(characteristic: BluetoothGattCharacteristic, value: ByteArray?) {
        queue.add(GattCall("write", characteristic, value))
        if(!gattBusy && queue.size == 1) {
            queryCharacteristic("", null)
        }
        //characteristic.value = value
        //bluetoothGatt?.writeCharacteristic(characteristic)
    }

    fun readCharacteristic(characteristic: BluetoothGattCharacteristic) {
        queue.add(GattCall("read", characteristic, null))
        if(!gattBusy && queue.size == 1) {
            queryCharacteristic("", null)
        }
        //bluetoothGatt?.readCharacteristic(characteristic)
    }

    fun queryCharacteristic(type: String, uuid: UUID?) {

        if((type == "onChanged" && notificationUUID != uuid) || !isConnected) {
            return
        }
        Log.i("BLEService", "Queue size: " + queue.size)
        gattBusy = false
        if(queue.size > 0) {
            val call = queue.poll()
            if (call.operation == "read") {
                gattBusy = true
                bluetoothGatt?.readCharacteristic(call.characteristic)
            } else if (call.operation == "write") {
                call.characteristic.value = call.value
                gattBusy = true
                bluetoothGatt?.writeCharacteristic(call.characteristic)
            } else if (call.operation == "notification") {
                if (bluetoothGatt == null) {
                    Log.w("BluetoothLeService", "BluetoothAdapter not initialized")
                }
                gattBusy = true
                bluetoothGatt?.let {
                    val uuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
                    val descriptor = call.characteristic.getDescriptor(uuid)
                    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    it.writeDescriptor(descriptor)

                    val successful = it.setCharacteristicNotification(call.characteristic, true)
                    notificationUUID = call.characteristic.uuid
                    if (successful) {
                        notifications.add(call.characteristic)
                    }
                }
            }
        }
    }

    private val leScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            if (result?.device?.name != null)
                result.device?.let { connect(it) }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.d("BluetoothLeService", "scan failed" + errorCode)
        }
    }

    private fun connect(device: BluetoothDevice) {
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
    }



    fun setCharacteristicNotification(
        characteristic: BluetoothGattCharacteristic
    ) {
        queue.add(GattCall("notification", characteristic, null))
        Log.i("BLEService", "Add notification to queue " + queue.size)
        if(!gattBusy && queue.size == 1) {
            queryCharacteristic("", null)
        }
        /*gattBusy = true
        if (bluetoothGatt == null) {
            Log.w("BluetoothLeService", "BluetoothAdapter not initialized")
        }
        bluetoothGatt?.let {
            val uuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
            val descriptor = characteristic.getDescriptor(uuid)
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            it.writeDescriptor(descriptor)

            val successful = it.setCharacteristicNotification(characteristic, true)
            if (successful) {
                notifications.add(characteristic)
            }
            gattBusy = false
        }
        gattBusy = false*/
    }

    fun close() {
        val iterator = notifications.iterator()
        while (iterator.hasNext()) {
            val notification = iterator.next()
            val result = disableNotification(notification)
            if (result) {
                iterator.remove()
            }
        }

        bluetoothGatt?.apply {
            this.close()
        }
        bluetoothGatt = null
    }

    fun disableNotification(characteristic: BluetoothGattCharacteristic): Boolean {
        val uuid = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        val descriptor = characteristic.getDescriptor(uuid)
        if (descriptor != null) {
            descriptor.value = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
            bluetoothGatt?.writeDescriptor(descriptor)
            val successful = bluetoothGatt?.setCharacteristicNotification(characteristic, false)
            return successful ?: false
        }

        return false
    }

    fun setIsConnected(isConnected: Boolean) {
        this.isConnected = isConnected
    }

    fun addQueueElement(element: GattCall) {
        queue.add(element)
    }

    fun emptyQueue() {
        queue.clear()
    }


}