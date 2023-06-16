package com.mrfive.alarm.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import java.util.*

//单例类充当静态类使用
object Bluetooth {
    lateinit var bluetoothAdapter: BluetoothAdapter
    var mmSocket: BluetoothSocket? = null //socket
    var device: BluetoothDevice? = null  //选中的设备

    //注意：蓝牙串口只有这唯一一个UUID！！！！
    val MY_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") //将字符串转成UUID
    const val TAG = "BluetoothTag"

    fun releaseBluetoothResource() {
        mmSocket?.close()
        mmSocket = null
        device = null
    }
}
