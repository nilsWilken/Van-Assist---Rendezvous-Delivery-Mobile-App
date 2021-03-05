package de.dpd.vanassist.combox

import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Handler
import android.util.Log
import java.util.*

class BluetoothConnection(val context: Context, val gattCallback: ComboxGattCallback) {

    private val SCAN_PERIOD: Long = 6000

    private val bluetoothAdapter: BluetoothAdapter by lazy(LazyThreadSafetyMode.NONE) {
        val bluetoothManager =
            context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private val handler = Handler()

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

    fun writeCharacteristic(characteristic: BluetoothGattCharacteristic) {
        bluetoothGatt?.writeCharacteristic(characteristic)
    }

    fun readCharacteristic(characteristic: BluetoothGattCharacteristic) {
        bluetoothGatt?.readCharacteristic(characteristic)
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
    ): Boolean {
        if (bluetoothGatt == null) {
            Log.w("BluetoothLeService", "BluetoothAdapter not initialized")
            return false
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
            return successful
        }

        return false
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


}