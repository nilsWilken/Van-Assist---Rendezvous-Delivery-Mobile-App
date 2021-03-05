package de.dpd.vanassist.combox

import kotlinx.coroutines.flow.Flow
import java.util.*


class BluetoothLeDeliveryService(val bluetoothLeService: BluetoothLeService) {

    companion object {
        @Volatile
        private var instance: BluetoothLeDeliveryService? = null
        val UUID_DELIVERY_SERVICE = UUID.fromString("000021A1-0000-1000-8000-00805F9B34FB")


        val UUID_START_DELIVERY = UUID.fromString("00003201-0000-1000-8000-00805F9B34FB")
        val UUID_END_DELIVERY = UUID.fromString("00003202-0000-1000-8000-00805F9B34FB")
        val UUID_DELIVERY_MODE = UUID.fromString("00003203-0000-1000-8000-00805F9B34FB")
        val UUID_NEXT_STOP = UUID.fromString("00003204-0000-1000-8000-00805F9B34FB")
        val UUID_DELIVERY_COUNT = UUID.fromString("00003205-0000-1000-8000-00805F9B34FB")
        val UUID_DELIVERY_LIST_ITEM = UUID.fromString("00003206-0000-1000-8000-00805F9B34FB")
        val UUID_DELIVERY_LIST_ITEM_READ = UUID.fromString("00003207-0000-1000-8000-00805F9B34FB")

        val UUID_CONFIGURATION_SERVICE = UUID.fromString("000021A0-0000-1000-8000-00805F9B34FB")
        val UUID_CONFIGURATION_SERVICE_SERVICE_ENDPOINT =
            UUID.fromString("00003103-0000-1000-8000-00805F9B34FB")
        val UUID_CONFIGURATION_SERVICE_CREDENTIALS_USERNAME =
            UUID.fromString("00003101-0000-1000-8000-00805F9B34FB")
        val UUID_CONFIGURATION_SERVICE_CREDENTIALS_PASSWORD =
            UUID.fromString("00003102-0000-1000-8000-00805F9B34FB")
        val UUID_CONFIGURATION_SERVICE_CONNECT =
            UUID.fromString("00003104-0000-1000-8000-00805F9B34FB")


        val UUID_VANASSIST_VEHICLE_SERVICE = UUID.fromString("000021A2-0000-1000-8000-00805F9B34FB")
        val UUID_VANASSIST_VEHICLE_SERVICE_VEHICLE_ID =
            UUID.fromString("000003301-0000-1000-8000-00805F9B34FB")
        val UUID_VANASSIST_VEHICLE_SERVICE_ACCESS_CONTROL =
            UUID.fromString("000003302-0000-1000-8000-00805F9B34FB")
        val UUID_VANASSIST_VEHICLE_SERVICE_VEHICLE_POSITION =
            UUID.fromString("000003303-0000-1000-8000-00805F9B34FB")
        val UUID_VANASSIST_VEHICLE_SERVICE_TARGET_POSITION =
            UUID.fromString("000003306-0000-1000-8000-00805F9B34FB")

        val UUID_VANASSIST_STATUS_SERVICE = UUID.fromString("000021A3-0000-1000-8000-00805F9B34FB")
        val UUID_VANASSIST_STATUS_SERVICE_PRIMARY_CONNECTION_STATUS =
            UUID.fromString("000003401-0000-1000-8000-00805F9B34FB")
        val UUID_VANASSIST_STATUS_SERVICE_SECONDARY_CONNECTION_STATUS =
            UUID.fromString("000003402-0000-1000-8000-00805F9B34FB")
        val UUID_VANASSIST_STATUS_SERVICE_SERVICE_CONNECTION_STATUS =
            UUID.fromString("000003403-0000-1000-8000-00805F9B34FB")

        val UUID_VANASSIST_ERROR_AND_STATUS_SERVICE = UUID.fromString("000021A4-0000-1000-8000-00805F9B34FB")
        val UUID_VANASSIST__ERROR_AND_STATUS_SERVICE_SET =
            UUID.fromString("000003501-0000-1000-8000-00805F9B34FB")
        val UUID_VANASSIST__ERROR_AND_STATUS_SERVICE_ACKNOWLEDGE =
            UUID.fromString("000003502-0000-1000-8000-00805F9B34FB")
        val UUID_VANASSIST__ERROR_AND_STATUS_SERVICE_DISPLAY_MESSAGE =
            UUID.fromString("000003503-0000-1000-8000-00805F9B34FB")


        val UUID_VANASSIST_LOGISTIC_SERVICE = UUID.fromString("000021A5-0000-1000-8000-00805F9B34FB")

        fun getInstance(bluetoothLeService: BluetoothLeService) =
            instance ?: synchronized(this) {
                instance
                    ?: BluetoothLeDeliveryService(bluetoothLeService)
                        .also { instance = it }
            }
    }

    suspend fun getNextStop(): String {
        return bluetoothLeService.readPosition(
            UUID_DELIVERY_SERVICE, UUID_NEXT_STOP
        ).contentToString()

    }

    suspend fun startDelivery() {
        bluetoothLeService.writeCharacteristic(
            UUID_DELIVERY_SERVICE,
            UUID_START_DELIVERY,
            byteArrayOf(1)
        )
    }

    suspend fun endDelivery() {
        bluetoothLeService.writeCharacteristic(
            UUID_DELIVERY_SERVICE,
            UUID_END_DELIVERY,
            byteArrayOf(1)
        )
    }

    suspend fun getDeliveryCount(): Int? {
        val result =
            bluetoothLeService.readShort(UUID_DELIVERY_SERVICE, UUID_DELIVERY_COUNT).toInt()
        return result
    }

    suspend fun getDeliveryMode(): Byte? {
        return bluetoothLeService.readByte(UUID_DELIVERY_SERVICE, UUID_DELIVERY_MODE)
    }

    suspend fun setDeliveryMode(byte: Byte) {
        return bluetoothLeService.writeByte(UUID_DELIVERY_SERVICE, UUID_DELIVERY_MODE, byte)
    }


    suspend fun getDeliveryListItem(index: Short) : String {
        bluetoothLeService.writeShort(
            UUID_DELIVERY_SERVICE,
            UUID_DELIVERY_LIST_ITEM, index
        )
        return bluetoothLeService.readString(UUID_DELIVERY_SERVICE, UUID_DELIVERY_LIST_ITEM_READ)
    }

    suspend fun writeDeliveryListItem(index : Short) {
        return bluetoothLeService.writeShort(UUID_DELIVERY_SERVICE, UUID_DELIVERY_MODE, index)
    }

    suspend fun getVehicleId(): String? {
        return bluetoothLeService.readString(
            UUID_VANASSIST_VEHICLE_SERVICE,
            UUID_VANASSIST_VEHICLE_SERVICE_VEHICLE_ID
        )
    }

    suspend fun setVehicleId(value: String) {
        bluetoothLeService.writeString(
            UUID_VANASSIST_VEHICLE_SERVICE,
            UUID_VANASSIST_VEHICLE_SERVICE_VEHICLE_ID, value
        )
    }

    suspend fun getAccessControl(): ByteArray {
        return bluetoothLeService.readByteArray(
            UUID_VANASSIST_VEHICLE_SERVICE,
            UUID_VANASSIST_VEHICLE_SERVICE_ACCESS_CONTROL,
            2
        )
    }

    suspend fun getVehiclePosition(): DoubleArray {
        return bluetoothLeService.readPosition(
            UUID_VANASSIST_VEHICLE_SERVICE,
            UUID_VANASSIST_VEHICLE_SERVICE_VEHICLE_POSITION
        )
    }

    fun getVehiclePositionNotification(): Flow<DoubleArray> {
        return bluetoothLeService.getDoubleArrayNotification(
            UUID_VANASSIST_VEHICLE_SERVICE,
            UUID_VANASSIST_VEHICLE_SERVICE_VEHICLE_POSITION
        )
    }

    fun disableVehiclePositionNotification() {
        bluetoothLeService.disableNotification(
            UUID_VANASSIST_VEHICLE_SERVICE,
            UUID_VANASSIST_VEHICLE_SERVICE_VEHICLE_POSITION,
        )
    }

    suspend fun setConnect(value : Byte) {
        return bluetoothLeService.writeByte(
            UUID_CONFIGURATION_SERVICE,
            UUID_CONFIGURATION_SERVICE_CONNECT,
            value
        )
    }

    suspend fun getServiceEndpoint(): String? {
        return bluetoothLeService.readString(
            UUID_CONFIGURATION_SERVICE,
            UUID_CONFIGURATION_SERVICE_SERVICE_ENDPOINT
        )
    }

    suspend fun setServiceEndpoint(value: String) {
        bluetoothLeService.writeString(
            UUID_CONFIGURATION_SERVICE,
            UUID_CONFIGURATION_SERVICE_SERVICE_ENDPOINT, value
        )
    }

    suspend fun getPrimaryConnectionStatus(): ByteArray? {
        return bluetoothLeService.readByteArray(
            UUID_VANASSIST_STATUS_SERVICE,
            UUID_VANASSIST_STATUS_SERVICE_PRIMARY_CONNECTION_STATUS
        )
    }

    suspend fun getSecondaryConnectionStatus(): ByteArray {
        return bluetoothLeService.readByteArray(
            UUID_VANASSIST_STATUS_SERVICE,
            UUID_VANASSIST_STATUS_SERVICE_SECONDARY_CONNECTION_STATUS, 8
        )
    }

    suspend fun getServiceConnectionStatus(): Byte? {
        return bluetoothLeService.readByte(
            UUID_VANASSIST_STATUS_SERVICE,
            UUID_VANASSIST_STATUS_SERVICE_SERVICE_CONNECTION_STATUS
        )
    }

    suspend fun writeCredentialsUsername(username : String) {
        bluetoothLeService.writeString(
            UUID_CONFIGURATION_SERVICE,
            UUID_CONFIGURATION_SERVICE_CREDENTIALS_USERNAME,
            username
        )
    }

    suspend fun writeCredentialsPassword(password : String) {
        bluetoothLeService.writeString(
            UUID_CONFIGURATION_SERVICE,
            UUID_CONFIGURATION_SERVICE_CREDENTIALS_PASSWORD,
            password
        )
    }

}