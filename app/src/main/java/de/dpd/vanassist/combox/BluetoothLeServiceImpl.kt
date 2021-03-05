package de.dpd.vanassist.combox

import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.util.Log
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withTimeoutOrNull
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.TimeUnit

private const val DEVICE_NAME = "VanAssist"

sealed class ConnectionStatus(val description : String) {
    class Failure(description: String) : ConnectionStatus(description)
    class Connected() : ConnectionStatus("Connected")
    class NotConnected() : ConnectionStatus("Not connected")

}

@ExperimentalCoroutinesApi
class BluetoothLeServiceImpl(private val context: Context) : BluetoothLeService {

    private val TIMEOUT: Long = 2

    private val charset = Charsets.UTF_8

    private var bluetoothConnection : BluetoothConnection? = null

    private var bluetoothGattCallback : ComboxGattCallback? = null

    private val channel = BroadcastChannel<BluetoothResult>(Channel.BUFFERED)

    override val connectionStatus : StateFlow<ConnectionStatus>
        get() =  _connectionStatus

    private val _connectionStatus : MutableStateFlow<ConnectionStatus> = MutableStateFlow(ConnectionStatus.NotConnected())

    companion object {
        @Volatile
        private var instance: BluetoothLeServiceImpl? = null

        fun getInstance(context: Context) =
            instance ?: synchronized(this) {
                instance
                    ?: BluetoothLeServiceImpl(context)
                        .also { instance = it }
            }
    }

    override fun connect(): StateFlow<ConnectionStatus> {
        bluetoothGattCallback = ComboxGattCallback(channel, _connectionStatus).apply {
            val c = BluetoothConnection(context, this)
            c.scanForDevice(DEVICE_NAME, true)
            bluetoothConnection = c
        }
        return connectionStatus
    }

    override fun disconnect(): StateFlow<ConnectionStatus> {
        bluetoothConnection?.close()
        bluetoothConnection = null
        _connectionStatus.value = ConnectionStatus.NotConnected()
        return _connectionStatus
    }

    override suspend fun readString(
        uuidService: UUID,
        uuidCharacteristicUUID: UUID
    ): String {
        return readCharacteristic(uuidService, uuidCharacteristicUUID).getString()
    }

    override suspend fun readShort(
        uuidService: UUID,
        uuidCharacteristicUUID: UUID
    ): Short {
        return readCharacteristic(uuidService, uuidCharacteristicUUID).getShort()
    }

    override suspend fun readByteArray(
        uuidService: UUID,
        uuidCharacteristicUUID: UUID
    ): ByteArray? {
        return readCharacteristic(uuidService, uuidCharacteristicUUID).value
    }

    override suspend fun readByteArray(
        uuidService: UUID,
        uuidCharacteristicUUID: UUID, bitCount: Int
    ): ByteArray {
        return readCharacteristic(uuidService, uuidCharacteristicUUID).getByteArray(bitCount)
    }

    override suspend fun readPosition(
        uuidService: UUID,
        uuidCharacteristicUUID: UUID
    ): DoubleArray {
        return readCharacteristic(uuidService, uuidCharacteristicUUID).getPosition()
    }

    override suspend fun readByte(
        uuidService: UUID,
        uuidCharacteristicUUID: UUID
    ): Byte? {
        return readCharacteristic(uuidService, uuidCharacteristicUUID).readByte()
    }

    override fun getByteNotification(
        uuidService: UUID,
        uuidCharacteristicUUID: UUID
    ): Flow<Byte?> {
        setCharacteristicNotification(uuidService, uuidCharacteristicUUID)
        return getSubscription(uuidCharacteristicUUID).map {
            it.readByte()
        }
    }

    override fun getDoubleArrayNotification(
        uuidService: UUID,
        uuidCharacteristicUUID: UUID
    ): Flow<DoubleArray> {
        setCharacteristicNotification(uuidService, uuidCharacteristicUUID)
        return getSubscription(uuidCharacteristicUUID).map { it.getPosition() }
    }

    override fun getStringNotification(
        uuidService: UUID,
        uuidCharacteristicUUID: UUID
    ): Flow<String> {
        setCharacteristicNotification(uuidService, uuidCharacteristicUUID)
        return getSubscription(uuidCharacteristicUUID).map { it.getString() }
    }

    override fun setCharacteristicNotification(
        uuidService: UUID,
        uuidCharacteristicUUID: UUID
    ) {
        getCharacterisic(
            uuidService,
            uuidCharacteristicUUID
        )?.apply { bluetoothConnection?.setCharacteristicNotification(this) }
    }

    override suspend fun writeCharacteristic(
        uuidService: UUID,
        uuidCharacteristicUUID: UUID,
        value: ByteArray?
    ): BluetoothResult? {
        getCharacterisic(uuidService, uuidCharacteristicUUID)?.apply {
            this.value = value
            bluetoothConnection?.writeCharacteristic(this)
            return waitForResult(this.uuid)
        }

        return BluetoothResult(uuidService, null, 0)
    }


    override suspend fun writeString(
        uuidService: UUID,
        uuidCharacteristicUUID: UUID,
        value: String
    ) {
        writeCharacteristic(uuidService, uuidCharacteristicUUID, value.toByteArray(charset))
    }

    override suspend fun writeByte(
        uuidService: UUID,
        uuidCharacteristicUUID: UUID,
        value: Byte
    ) {
        writeCharacteristic(uuidService, uuidCharacteristicUUID, ByteArray(1, { value }))
    }

    override suspend fun writeShort(
        uuidService: UUID,
        uuidCharacteristicUUID: UUID,
        value: Short
    ) {
        val buffer = ByteBuffer.allocate(2)
        buffer.putShort(value)
        writeCharacteristic(uuidService, uuidCharacteristicUUID, buffer.array())

    }

    override fun disableNotification(
        uuidService: UUID,
        uuidCharacteristicUUID: UUID
    ): Boolean {
        getCharacterisic(uuidService, uuidCharacteristicUUID)?.apply {
            return bluetoothConnection?.disableNotification(this) ?: false
        }
        return false
    }

    private suspend fun waitForResult(uuid: UUID): BluetoothResult {
        val subsciption = channel.openSubscription()
        val result = withTimeoutOrNull(TimeUnit.SECONDS.toMillis(TIMEOUT)) {
            var bluetoothResult: BluetoothResult? = subsciption.receive()
            while (bluetoothResult == null || bluetoothResult.uuid != uuid) {
                bluetoothResult = subsciption.receive()
            }
            bluetoothResult
        }
        subsciption.cancel()
        return result ?: BluetoothResult(uuid = uuid, value = null, status = 0)
    }

    private suspend fun readCharacteristic(
        uuidService: UUID,
        uuidCharacteristicUUID: UUID
    ): BluetoothResult {
        getCharacterisic(uuidService, uuidCharacteristicUUID)?.apply {
            bluetoothConnection?.readCharacteristic(this).run {  }
            return waitForResult(this.uuid)
        }
        return BluetoothResult(uuidService, null, 0)
    }



    private fun getCharacterisic(uuidService: UUID, uuidCharacteristicUUID: UUID): BluetoothGattCharacteristic? {
        return bluetoothGattCallback?.getCharacterisic(uuidService, uuidCharacteristicUUID)
    }


    fun getSubscription(
        uuidCharacteristicUUID: UUID
    ): Flow<BluetoothResult> {
        return channel.openSubscription().consumeAsFlow().filter {
            it.uuid == uuidCharacteristicUUID
        }
    }
}