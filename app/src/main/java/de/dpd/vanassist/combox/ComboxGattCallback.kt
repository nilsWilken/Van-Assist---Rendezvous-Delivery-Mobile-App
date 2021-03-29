package de.dpd.vanassist.combox

import android.bluetooth.*
import android.util.Log
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.flow.MutableStateFlow
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.util.*

private val TAG = ComboxGattCallback::class.java.simpleName

const val GATT_INTERNAL_ERROR = 129

@ExperimentalCoroutinesApi
class ComboxGattCallback(
    val channel: BroadcastChannel<BluetoothResult>,
    private val _connectionStatus: MutableStateFlow<ConnectionStatus>
) : BluetoothGattCallback() {

    private var services: Map<UUID, BluetoothGattService> = emptyMap()

    var servicesDiscovered: Boolean = false

    fun getCharacteristic(
        uuidService: UUID,
        uuidCharacteristicUUID: UUID
    ): BluetoothGattCharacteristic? {
        val s= services[uuidService]
        return s?.getCharacteristic(uuidCharacteristicUUID)
    }


    override fun onConnectionStateChange(
        gatt: BluetoothGatt,
        status: Int,
        newState: Int
    ) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    _connectionStatus.value = ConnectionStatus.Connected()
                    gatt.discoverServices()
                    Log.i("BLEService", "Connection status changed (connected)!")
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    _connectionStatus.value = ConnectionStatus.NotConnected()
                    gatt.close();
                    this.servicesDiscovered = false
                    Log.i("BLEService", "Connection status changed (disconnected)!")
                }
            }
        } else {
            gatt.close();
        }
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
        if (status == GATT_INTERNAL_ERROR) {
            gatt.close()
            return;
        }
        if(status == BluetoothGatt.GATT_SUCCESS) {
            this.servicesDiscovered = true
            Log.i("BLEService", "Services discovered!")
        }
        when (status) {
            BluetoothGatt.GATT_SUCCESS ->
                services = gatt.services.map { it.uuid to it }.toMap()
            else -> Log.w(TAG, "onServicesDiscovered received: $status")
        }
    }

    override fun onCharacteristicRead(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        status: Int
    ) {
        Log.d("BLEService", "read ${characteristic.uuid}  ${characteristic.value} short:${ByteBuffer.wrap(characteristic.value).short} string::${characteristic.value?.toString(
          Charset.defaultCharset()) ?: ""}")
        channel.offer(
            BluetoothResult(
                characteristic.uuid,
                characteristic.value,
                status
            )
        )
    }

    override fun onCharacteristicWrite(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        status: Int
    ) {
        Log.d("BLEService", "write ${characteristic.uuid}  ${characteristic.value} short:${ByteBuffer.wrap(characteristic.value).short} string::${characteristic.value?.toString(Charset.defaultCharset()) ?: ""}")
        channel.offer(
            BluetoothResult(
                characteristic.uuid,
                characteristic.value,
                status
            )
        )
    }

    override fun onCharacteristicChanged(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic
    ) {
        channel.offer(
            BluetoothResult(
                characteristic.uuid,
                characteristic.value,
                0
            )
        )
    }

    fun onConnnectionFailure(s: String) {
        _connectionStatus.value = ConnectionStatus.Failure(s)
    }


}