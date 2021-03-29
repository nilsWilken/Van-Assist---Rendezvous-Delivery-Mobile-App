package de.dpd.vanassist.comboxExampleUI

import androidx.lifecycle.*
import de.dpd.vanassist.combox.BluetoothLeDeliveryService
import de.dpd.vanassist.combox.BluetoothLeDeviceService
import de.dpd.vanassist.combox.BluetoothLeServiceImpl


import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class BleViewModel internal constructor(
    val bluetoothLeService: BluetoothLeServiceImpl,
    val bluetoothLeDeliveryService: BluetoothLeDeliveryService,
    val bluetoothLeDeviceService: BluetoothLeDeviceService
) : ViewModel() {

    val nextStop: LiveData<String?>
        get() = _nextStop

    val _nextStop = MutableLiveData<String>(null)

    val deliveryCount: LiveData<String?>
        get() = _deliveryCount

    val _deliveryCount = MutableLiveData<String>(null)

    val vehicleId = MutableLiveData<String>(null)

    val batteryLevel: LiveData<String?>
        get() = _batteryLevel

    val _batteryLevel = MutableLiveData<String>(null)

    val batteryLevelNotification: LiveData<String?>
        get() = _batteryLevelNotification

    var _batteryLevelNotification = MutableLiveData<String>(null)


    val manufactureNameString: LiveData<String?>
        get() = _manufactureNameString

    val _manufactureNameString = MutableLiveData<String>(null)

    val firmwareRevisionString: LiveData<String?>
        get() = _firmwareRevisionString

    val _firmwareRevisionString = MutableLiveData<String>(null)


    val hardwareRevisionString: LiveData<String?>
        get() = _hardwareRevisionString

    val _hardwareRevisionString = MutableLiveData<String>(null)

    val modelNumberString: LiveData<String?>
        get() = _modelNumberString

    val _modelNumberString = MutableLiveData<String>(null)

    val serialNumberString: LiveData<String?>
        get() = _serialNumberString

    val _serialNumberString = MutableLiveData<String>(null)

    val serviceEndpoint = MutableLiveData<String>(null)

    val accessControl = MutableLiveData<String>(null)

    val vehiclePosition = MutableLiveData<String>(null)

    val vehiclePositionNotifcation = MutableLiveData<String>(null)

    val connect = MutableLiveData<Byte>(null)

    var deliveryMode =  MutableLiveData(false)


    val deliveryListItemIndex = MutableLiveData<String>(null)

    val deliveryListItemNotification: LiveData<String?>
        get() = _deliveryListItemNotification

    val _deliveryListItemNotification = MutableLiveData<String>(null)

    val primaryConnectionStatus: LiveData<String?>
        get() = _primaryConnectionStatus

    val _primaryConnectionStatus = MutableLiveData<String>(null)

    val secondaryConnectionStatus: LiveData<String?>
        get() = _secondaryConnectionStatus

    val _secondaryConnectionStatus = MutableLiveData<String>(null)

    val serviceConnectionStatus: LiveData<String?>
        get() = _serviceConnectionStatus

    val _serviceConnectionStatus = MutableLiveData<String>(null)

    val connectionStatus = bluetoothLeService.connectionStatus.asLiveData()

    var notifyVehiclePositionLabel = MutableLiveData("Subscribe Notification Vehicle Position")

    fun notifyVehiclePosition() {

        viewModelScope.launch(Dispatchers.IO) {
            if (notifyVehiclePositionLabel.value == "Subscribe Notification Vehicle Position") {
                notifyVehiclePositionLabel.postValue("Unubscribe Notification Vehicle Position")
                bluetoothLeDeliveryService.getVehiclePositionNotification().map { it.contentToString() }
                    .collect {
                        vehiclePositionNotifcation.postValue(it)
                    }
            } else {
                notifyVehiclePositionLabel.postValue("Subscribe Notification Vehicle Position")
                bluetoothLeDeliveryService.disableVehiclePositionNotification()
            }

        }


    }


    fun refresh() {


        viewModelScope.launch {

            _batteryLevel.value = bluetoothLeDeviceService.getBatteryLevel().toString()
            _manufactureNameString.value = bluetoothLeDeviceService.getManufactureNameString()
            _firmwareRevisionString.value = bluetoothLeDeviceService.getFirmwareRevisionString()
            _hardwareRevisionString.value = bluetoothLeDeviceService.getHardwareRevisionString()
            _modelNumberString.value = bluetoothLeDeviceService.getModelNumberString()
            _serialNumberString.value = bluetoothLeDeviceService.getSerialNumberString()

            bluetoothLeDeliveryService.getDeliveryMode()?.apply {
                deliveryMode.value = this == 1.toByte()
            }


            serviceEndpoint.value = bluetoothLeDeliveryService.getServiceEndpoint()

            _nextStop.value = bluetoothLeDeliveryService.getNextStop()

            _deliveryCount.value = bluetoothLeDeliveryService.getDeliveryCount()?.toString()

            vehicleId.value = bluetoothLeDeliveryService.getVehicleId()
            accessControl.value = bluetoothLeDeliveryService.getAccessControl().contentToString()
            vehiclePosition.value = bluetoothLeDeliveryService.getVehiclePosition().contentToString()

            _primaryConnectionStatus.value =
                bluetoothLeDeliveryService.getPrimaryConnectionStatus()?.contentToString()
//           _secondaryConnectionStatus.value  = bluetoothLeDeliveryService.getSecondaryConnectionStatus().contentToString()
//            _serviceConnectionStatus.value =
//                bluetoothLeDeliveryService.getServiceConnectionStatus().toString()
        }
    }

    var notifyBatteryLevelLabel = MutableLiveData("Subscribe Notification Battery Level")

    fun notifyBatteryLevel() {
        viewModelScope.launch(Dispatchers.IO) {
            if (notifyBatteryLevelLabel.value == "Subscribe Notification Battery Level") {
                notifyBatteryLevelLabel.postValue("Unubscribe Notification Battery Level")
                bluetoothLeDeviceService.getBatteryLevelNotification(true).map { it.toString() }
                    .collect {
                        _batteryLevelNotification.postValue(it)
                    }
            } else {
                notifyBatteryLevelLabel.postValue("Subscribe Notification Battery Level")
                bluetoothLeDeviceService.disableBatteryLevelNotification()
            }

        }
    }

    fun connect() {
        bluetoothLeService.connect()
    }

    fun disConnect() {
        bluetoothLeService.disconnect()
    }

    fun onSendClick() {
        viewModelScope.launch {
            refresh()
            //vehicleId.value?.let { bluetoothLeDeliveryService.setVehicleId(it) }
            //serviceEndpoint.value?.let { bluetoothLeDeliveryService.setServiceEndpoint(it) }
            //deliveryMode.value?.let { it -> bluetoothLeDeliveryService.setDeliveryMode(it.toByte()) }
//            try {
//                deliveryListItemIndex.value?.let {
//                    bluetoothLeDeliveryService.writeDeliveryListItem(
//                        it.toShort()
//                    )
//                }
//            } catch (e: NumberFormatException) {
//                Log.e("CarViewModel", e.message)
//            }
            //refresh()
//            _batteryLevelNotification =
//                bluetoothLeDeviceService.getBatteryLevelNotification(true).map { it.toString() }.asLiveData() as MutableLiveData<String>

        }

    }

    fun login(enable: Boolean) {
        viewModelScope.launch {
            bluetoothLeDeliveryService.writeCredentialsUsername("vanassist@hs-offenburg.de")
            bluetoothLeDeliveryService.writeCredentialsPassword("myPasssword2")
            bluetoothLeDeliveryService.setConnect(if (enable) 1 else 2)
        }
    }

    fun onStartDeliveryClicked() {
        viewModelScope.launch {
            bluetoothLeDeliveryService.startDelivery()
        }
    }

    fun onEndDeliveryClicked() {
        viewModelScope.launch {
            bluetoothLeDeliveryService.endDelivery()
        }
    }

    fun onNextDeliveryListItemClicked() {
        viewModelScope.launch {
            val deliveryListItem = bluetoothLeDeliveryService.getDeliveryListItem(1)
            _deliveryListItemNotification.postValue(deliveryListItem)
        }
    }

    override fun onCleared() {
        bluetoothLeService.disconnect()
        super.onCleared()

    }
}