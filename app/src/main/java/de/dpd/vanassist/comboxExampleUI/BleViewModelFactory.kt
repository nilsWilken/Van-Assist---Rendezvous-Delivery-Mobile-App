package de.dpd.vanassist.comboxExampleUI

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import de.dpd.vanassist.combox.BluetoothLeDeliveryService
import de.dpd.vanassist.combox.BluetoothLeDeviceService
import de.dpd.vanassist.combox.BluetoothLeServiceImpl

class BleViewModelFactory(
    private val bluetoothLeService: BluetoothLeServiceImpl,
    private val bluetoothLeDeliveryService: BluetoothLeDeliveryService,
    private val bluetoothLeDeviceService: BluetoothLeDeviceService

) : ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return BleViewModel(bluetoothLeService, bluetoothLeDeliveryService, bluetoothLeDeviceService) as T
    }
}