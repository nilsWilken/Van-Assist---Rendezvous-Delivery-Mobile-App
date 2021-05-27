package de.dpd.vanassist.combox

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.util.*

interface BluetoothLeService {
    val connectionStatus: StateFlow<ConnectionStatus>
    fun connect(): StateFlow<ConnectionStatus>
    fun disconnect(): StateFlow<ConnectionStatus>
    suspend fun readString(
        uuidService: UUID,
        uuidCharacteristicUUID: UUID
    ): String

    suspend fun readShort(
        uuidService: UUID,
        uuidCharacteristicUUID: UUID
    ): Short

    suspend fun readByteArray(
        uuidService: UUID,
        uuidCharacteristicUUID: UUID
    ): ByteArray?

    suspend fun readByteArray(
        uuidService: UUID,
        uuidCharacteristicUUID: UUID, bitCount: Int
    ): ByteArray

    suspend fun readPosition(
        uuidService: UUID,
        uuidCharacteristicUUID: UUID
    ): DoubleArray

    suspend fun readNextStop(
        uuidService: UUID,
        uuidCharacteristicUUID: UUID
    ): DoubleArray

    suspend fun readVehicleStatus(
        uuidService: UUID,
        uuidCharacteristicUUID: UUID
    ): ShortArray

    suspend fun readByte(
        uuidService: UUID,
        uuidCharacteristicUUID: UUID
    ): Byte?

    fun getByteNotification(
        uuidService: UUID,
        uuidCharacteristicUUID: UUID
    ): Flow<Byte?>

    fun getShortNotification(
        uuidService: UUID,
        uuidCharacteristicUUID: UUID
    ): Flow<Short?>

    fun getDoubleArrayNotification(
        uuidService: UUID,
        uuidCharacteristicUUID: UUID
    ): Flow<DoubleArray>

    fun getVehicleStatusNotification(
        uuidService: UUID,
        uuidCharacteristicUUID: UUID
    ): Flow<ShortArray>

    fun getStringNotification(
        uuidService: UUID,
        uuidCharacteristicUUID: UUID
    ): Flow<String>

    fun setCharacteristicNotification(
        uuidService: UUID,
        uuidCharacteristicUUID: UUID
    )

    suspend fun writePosition(
        uuidService: UUID,
        uuidCharacteristicUUID: UUID,
        position: DoubleArray,
        z: Float,
        orientation: Float
    ): BluetoothResult?

    suspend fun writeDeliveryStatus(
        uuidService: UUID,
        uuidCharacteristicUUID: UUID,
        paketID: Int,
        deliveryStatus: Short
    ): BluetoothResult?

    suspend fun writeCharacteristic(
        uuidService: UUID,
        uuidCharacteristicUUID: UUID,
        value: ByteArray?
    ): BluetoothResult?

    suspend fun writeString(
        uuidService: UUID,
        uuidCharacteristicUUID: UUID,
        value: String
    )

    suspend fun writeByte(
        uuidService: UUID,
        uuidCharacteristicUUID: UUID,
        value: Byte
    )

    suspend fun writeShort(
        uuidService: UUID,
        uuidCharacteristicUUID: UUID,
        value: Short
    )

    suspend fun writeShortArray(
        uuidService: UUID,
        uuidCharacteristicUUID: UUID,
        array: ShortArray?
    )

    fun disableNotification(
        uuidService: UUID,
        uuidCharacteristicUUID: UUID
    ): Boolean

}