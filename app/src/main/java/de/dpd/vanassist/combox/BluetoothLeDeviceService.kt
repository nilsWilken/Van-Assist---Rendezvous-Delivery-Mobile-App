package de.dpd.vanassist.combox

import kotlinx.coroutines.flow.Flow
import java.util.*



class BluetoothLeDeviceService(val bluetoothLeService: BluetoothLeService) {

    companion object {
        @Volatile
        private var instance: BluetoothLeDeviceService? = null
        val UUID_DEVICE_SERVICE= UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb")


        val UUID_MANUFACTURE_NAME_STRING= UUID.fromString("00002a29-0000-1000-8000-00805f9b34fb")
        val UUID_MODEL_NUMBER_STRING = UUID.fromString("00002a24-0000-1000-8000-00805f9b34fb")
        val UUID_SERIAL_NUMBER_STRING = UUID.fromString("00002a25-0000-1000-8000-00805f9b34fb")
        val UUID_HARDWARE_REVISION_STRING = UUID.fromString("00002a27-0000-1000-8000-00805f9b34fb")
        val UUID_FIRMWARE_REVISION_STRING = UUID.fromString("00002a26-0000-1000-8000-00805f9b34fb")
        val UUID_BATTERY_SERVICE = UUID.fromString("000180f-0000-1000-8000-00805f9b34fb")
        val UUID_BATTERY_SERVICE_BATTERY_LEVEL = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")


        fun getInstance(bluetoothLeService: BluetoothLeService) =
            instance ?: synchronized(this) {
                instance
                    ?: BluetoothLeDeviceService(bluetoothLeService)
                        .also { instance = it }
            }
    }

    suspend fun getManufactureNameString(): String {
        return bluetoothLeService.readString(UUID_DEVICE_SERVICE, UUID_MANUFACTURE_NAME_STRING)
    }

    suspend fun getModelNumberString(): String {
        return bluetoothLeService.readString(UUID_DEVICE_SERVICE, UUID_MODEL_NUMBER_STRING)
    }

    suspend fun getSerialNumberString(): String {
        return bluetoothLeService.readString(UUID_DEVICE_SERVICE, UUID_SERIAL_NUMBER_STRING)
    }

    suspend fun getHardwareRevisionString(): String {
        return bluetoothLeService.readString(UUID_DEVICE_SERVICE, UUID_HARDWARE_REVISION_STRING)
    }

    suspend fun getFirmwareRevisionString(): String {
        return bluetoothLeService.readString(UUID_DEVICE_SERVICE, UUID_FIRMWARE_REVISION_STRING)
    }

    suspend fun getBatteryLevel(): Byte? {
        return bluetoothLeService.readByte(UUID_BATTERY_SERVICE, UUID_BATTERY_SERVICE_BATTERY_LEVEL)
    }

    fun getBatteryLevelNotification(enabled:Boolean): Flow<Byte?> {
        return bluetoothLeService.getByteNotification(
            UUID_BATTERY_SERVICE,
            UUID_BATTERY_SERVICE_BATTERY_LEVEL
        )
    }

    fun disableBatteryLevelNotification() {
        bluetoothLeService.disableNotification(UUID_BATTERY_SERVICE, UUID_BATTERY_SERVICE_BATTERY_LEVEL)
    }



}