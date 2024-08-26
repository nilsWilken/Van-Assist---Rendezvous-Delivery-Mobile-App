package de.dpd.vanassist.comboxExampleUI

import androidx.lifecycle.*
import de.dpd.vanassist.cloud.VanAssistAPIController
import de.dpd.vanassist.combox.BluetoothLeDeliveryService
import de.dpd.vanassist.combox.BluetoothLeDeviceService
import de.dpd.vanassist.combox.BluetoothLeServiceImpl
import de.dpd.vanassist.util.FragmentRepo
import de.dpd.vanassist.util.json.VehicleJSONParser


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

    val vehicleTargetPosition = MutableLiveData<String>(null)

    val vehicleTargetPositionNotification = MutableLiveData<String>(null)

    val vehicleStatus = MutableLiveData<String>(null)

    val vehicleStatusNotification = MutableLiveData<String>(null)

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

    val errorStatusNotification = MutableLiveData<String>(null)

    val errorMessageNotification = MutableLiveData<String>(null)

    val connectionStatus = bluetoothLeService.connectionStatus.asLiveData()

    var notifyVehiclePositionLabel = MutableLiveData("Subscribe Notification Vehicle Position")

    var notifyVehicleTargetPositionLabel = MutableLiveData("Subscribe Notification Vehicle Target Position")

    var notifyVehicleStatusLabel = MutableLiveData("Subscribe Notification Vehicle Status")

    var notifyErrorStatusLabel = MutableLiveData("Subscribe Notification Error Status")

    var notifyErrorMessageLabel = MutableLiveData("Subscribe Notification Error Message")

    var exampleDriveToPositionLabel = MutableLiveData("[0.0,0.0,0.0,0.0]")

    var exampleSendVehicleStatusLabel = MutableLiveData("3")

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

    fun notifyVehicleTargetPosition() {

        viewModelScope.launch(Dispatchers.IO) {
            if(notifyVehicleTargetPositionLabel.value == "Subscribe Notification Vehicle Target Position") {
                notifyVehicleTargetPositionLabel.postValue("Unsubscribe Notification Vehicle Target Position")
                bluetoothLeDeliveryService.getTargetPositionNotification().map { it.contentToString() }
                    .collect {
                        vehicleTargetPositionNotification.postValue(it)
                    }
            } else {
                notifyVehicleTargetPositionLabel.postValue("Subscribe Notification Vehicle Target Position")
                bluetoothLeDeliveryService.disableTargetPositionNotification()
            }
        }
    }

    fun notifyVehicleStatus() {
        viewModelScope.launch(Dispatchers.IO) {
            if(notifyVehicleStatusLabel.value == "Subscribe Notification Vehicle Status") {
                notifyVehicleStatusLabel.postValue("Unsubscribe Notification Vehicle Status")
                bluetoothLeDeliveryService.getVehicleStatusNotification().map { it }
                    .collect {
                        vehicleStatusNotification.postValue(it[0].toString())
                    }
            } else {
                notifyVehicleStatusLabel.postValue("Subscribe Notification Vehicle Status")
                bluetoothLeDeliveryService.disableVehicleStatusNotification()
            }
        }
    }

    fun notifyErrorStatus() {
        viewModelScope.launch(Dispatchers.IO) {
            if(notifyErrorStatusLabel.value == "Subscribe Notification Error Status") {
                notifyErrorStatusLabel.postValue("Unsubscribe Notification Error Status")

            }
            else {
                notifyErrorStatusLabel.postValue("Subscribe Notification Error Status")

            }
        }
    }

    fun notifyErrorMessage() {
        viewModelScope.launch(Dispatchers.IO) {
            if(notifyErrorMessageLabel.value == "Subscribe Notification Error Message") {
                notifyErrorMessageLabel.postValue("Unsubscribe Notification Error Message")
                bluetoothLeDeliveryService.getErrorMessageNotification().map { it.toString() }
                    .collect {
                        errorMessageNotification.postValue(it)
                    }
            }
            else {
                notifyErrorMessageLabel.postValue("Subscribe Notification Error Message")
                bluetoothLeDeliveryService.disableErrorMessageNotification()
            }
        }
    }


    fun refreshAll() {


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
            vehicleTargetPosition.value = bluetoothLeDeliveryService.getTargetPosition().contentToString()

            _primaryConnectionStatus.value =
                bluetoothLeDeliveryService.getPrimaryConnectionStatus()?.contentToString()
//           _secondaryConnectionStatus.value  = bluetoothLeDeliveryService.getSecondaryConnectionStatus().contentToString()
//            _serviceConnectionStatus.value =
//                bluetoothLeDeliveryService.getServiceConnectionStatus().toString()
        }
    }

    fun refreshBatteryLevel() {
        viewModelScope.launch {
            _batteryLevel.value = bluetoothLeDeviceService.getBatteryLevel().toString()
        }
    }

    fun refreshManufacturerName() {
        viewModelScope.launch {
            _manufactureNameString.value = bluetoothLeDeviceService.getManufactureNameString()
        }
    }

    fun refreshModelNumber() {
        viewModelScope.launch {
            _modelNumberString.value = bluetoothLeDeviceService.getModelNumberString()
        }
    }

    fun refreshSerialNumber() {
        viewModelScope.launch {
            _serialNumberString.value = bluetoothLeDeviceService.getSerialNumberString()
        }
    }

    fun refreshHardwareRevision() {
        viewModelScope.launch {
            _hardwareRevisionString.value = bluetoothLeDeviceService.getHardwareRevisionString()
        }
    }

    fun refreshFirmwareRevision() {
        viewModelScope.launch {
            _firmwareRevisionString.value = bluetoothLeDeviceService.getHardwareRevisionString()
        }
    }

    fun refreshDeliveryMode() {
        viewModelScope.launch {
            bluetoothLeDeliveryService.getDeliveryMode()?.apply {
                deliveryMode.value = this == 1.toByte()
            }
        }
    }

    fun writeChangedDeliveryMode() {
        viewModelScope.launch {
            if(deliveryMode.value!!) {
                bluetoothLeDeliveryService.setDeliveryMode(1.toByte())
            }
            else {
                bluetoothLeDeliveryService.setDeliveryMode((0.toByte()))
            }
        }
    }

    fun refreshServiceEndpoint() {
        viewModelScope.launch {
            serviceEndpoint.value = bluetoothLeDeliveryService.getServiceEndpoint()
        }
    }

    fun refreshNextStop() {
        viewModelScope.launch {
            _nextStop.value = bluetoothLeDeliveryService.getNextStop()
        }
    }

    fun refreshDeliveryCount() {
        viewModelScope.launch {
            _deliveryCount.value = bluetoothLeDeliveryService.getDeliveryCount().toString()
        }
    }

    fun refreshVehicleID() {
        viewModelScope.launch {
            vehicleId.value = bluetoothLeDeliveryService.getVehicleId()
        }
    }

    fun refreshAccessControl() {
        viewModelScope.launch {
            accessControl.value = bluetoothLeDeliveryService.getAccessControl().contentToString()
        }
    }

    fun refreshVehiclePosition() {
        viewModelScope.launch {
            vehiclePosition.value = bluetoothLeDeliveryService.getVehiclePosition().contentToString()
        }
    }

    fun refreshVehicleTargetPosition() {
        viewModelScope.launch {
            vehicleTargetPosition.value = bluetoothLeDeliveryService.getTargetPosition().contentToString()
        }
    }

    fun refreshVehicleStatus() {
        viewModelScope.launch {
            val status = bluetoothLeDeliveryService.getVehicleStatus()
            if (status.size > 0) {
                vehicleStatus.value =
                    VehicleJSONParser.parseVehicleStatusFromShort(bluetoothLeDeliveryService.getVehicleStatus()[0])
            }
        }
    }

    fun refreshPrimaryConnectionStatus() {
        viewModelScope.launch {
            _primaryConnectionStatus.value = bluetoothLeDeliveryService.getPrimaryConnectionStatus().contentToString()
        }
    }

    fun writeExampleVehicleStatus() {
        viewModelScope.launch {
            bluetoothLeDeliveryService.setVehicleStatus(exampleSendVehicleStatusLabel.value!!.toShort())
        }
    }

    fun writeExampleDriveToPosition() {
        viewModelScope.launch {
            val position = DoubleArray(2)
            position[0] = 0.0
            position[1] = 0.0
            bluetoothLeDeliveryService.driveToPosition(position, 0, 0)
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
            refreshAll()
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

    fun startDataCollection() {
        val api = VanAssistAPIController(FragmentRepo.mapActivity!!, FragmentRepo.mapActivity!!.applicationContext)
        api.checkConnectionStatus()

        //api.startCollectionOfVehiclePosition()
        //Thread.sleep(1000)
        //api.startCollectionOfVehicleStatus()
    }

    override fun onCleared() {
        //bluetoothLeService.disconnect()
        super.onCleared()

    }
}