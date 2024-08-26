package de.dpd.vanassist.comboxExampleUI

import android.content.Context
import de.dpd.vanassist.combox.BluetoothLeDeliveryService
import de.dpd.vanassist.combox.BluetoothLeDeviceService
import de.dpd.vanassist.combox.BluetoothLeServiceImpl

object InjectorUtils {


    fun provideBleViewModelFactory(
        context: Context
    ): BleViewModelFactory {
        val comboxService = getBluetoothLeService(context)
        val bluetoothLeDeliveryService = getBluetoothLeDeliveryService(context)
        val bluetoothLeDeviceService = getBluetoothLeDeviceService(context)

        return BleViewModelFactory(
            comboxService,
            bluetoothLeDeliveryService,
            bluetoothLeDeviceService
        )
    }

    private fun getBluetoothLeDeliveryService(context: Context): BluetoothLeDeliveryService {
        val bluetoothLeService = getBluetoothLeService(context)
        return BluetoothLeDeliveryService.getInstance(bluetoothLeService)
    }

    private fun getBluetoothLeService(context: Context): BluetoothLeServiceImpl {
        return BluetoothLeServiceImpl.getInstance(context)
    }

    private fun getBluetoothLeDeviceService(context: Context): BluetoothLeDeviceService {
        val bluetoothLeService = getBluetoothLeService(context)
        return BluetoothLeDeviceService.getInstance(bluetoothLeService)
    }


}