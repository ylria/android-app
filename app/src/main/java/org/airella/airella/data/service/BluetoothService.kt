package org.airella.airella.data.service

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.airella.airella.exception.BluetoothDisabledException
import org.airella.airella.utils.Log

object BluetoothService {

    private val bluetoothAdapter: BluetoothAdapter by lazy { BluetoothAdapter.getDefaultAdapter() }

    private val scanning: MutableLiveData<Boolean> = MutableLiveData(false)

    val isScanning: LiveData<Boolean> = scanning

    @Throws(BluetoothDisabledException::class)
    fun scanBTDevices(callback: ScanCallback, enable: Boolean) {
        try {
            scanning.value = enable
            if (enable) {
                Log.i("Start BT scan")
                bluetoothAdapter.bluetoothLeScanner.startScan(callback)
            } else {
                Log.i("Stop BT scan")
                bluetoothAdapter.bluetoothLeScanner.stopScan(callback)
            }
        } catch (e: NullPointerException) {
            throw BluetoothDisabledException()
        }
    }

}