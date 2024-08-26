package de.dpd.vanassist.combox

import android.bluetooth.*
import android.util.Log
import de.dpd.vanassist.activity.MapActivity
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
    private val _connectionStatus: MutableStateFlow<ConnectionStatus>,
    private val bleService: BluetoothLeServiceImpl
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
                    bleService.setIsConnected(true)
                    gatt.discoverServices()
                    Log.i("BLEService", "Connection status changed (connected)!")
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    _connectionStatus.value = ConnectionStatus.NotConnected()
                    gatt.close();
                    this.servicesDiscovered = false
                    bleService.setIsConnected(false)
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
            BluetoothGatt.GATT_SUCCESS -> {
                services = gatt.services.map { it.uuid to it }.toMap()
                bleService.emptyTmpQueue()
                //bleService.nextQueueElement("", null)
            }
            else -> Log.w(TAG, "onServicesDiscovered received: $status")
        }
    }

    override fun onCharacteristicRead(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        status: Int
    ) {
        Log.i("BLEService", "read ${characteristic.uuid}  ${characteristic.value} short:${ByteBuffer.wrap(characteristic.value).short} string::${characteristic.value?.toString(
          Charset.defaultCharset()) ?: ""}")
        channel.offer(
            BluetoothResult(
                characteristic.uuid,
                characteristic.value,
                status
            )
        )
        bleService.nextQueueElement("onRead", characteristic.uuid)
    }

    override fun onCharacteristicWrite(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        status: Int
    ) {
        Log.i("BLEService", "write ${characteristic.uuid}  ${characteristic.value} short:${ByteBuffer.wrap(characteristic.value).short} string::${characteristic.value?.toString(Charset.defaultCharset()) ?: ""}")
        channel.offer(
            BluetoothResult(
                characteristic.uuid,
                characteristic.value,
                status
            )
        )
        bleService.nextQueueElement("onWrite", characteristic.uuid)
    }

    override fun onCharacteristicChanged(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic
    ) {
        Log.i("BLEService", "changed ${characteristic.uuid} ${characteristic.value}")
        if(MapActivity.waitingForBluetoothResult && characteristic.uuid.toString() == MapActivity.characteristicUUID) {
            MapActivity.waitingForBluetoothResult = false
            MapActivity.characteristicUUID = ""
        }
        channel.offer(
            BluetoothResult(
                characteristic.uuid,
                characteristic.value,
                0
            )
        )
        bleService.nextQueueElement("onChange", characteristic.uuid)
    }

    fun onConnnectionFailure(s: String) {
        _connectionStatus.value = ConnectionStatus.Failure(s)
    }


}