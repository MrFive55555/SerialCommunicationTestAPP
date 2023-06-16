package com.mrfive.alarm.mqtt

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Message
import com.mrfive.alarm.data.MqttMessageData
import com.mrfive.alarm.tool.Tool.showToast
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttMessage

object MQTT {
    @SuppressLint("StaticFieldLeak")
    //mqttClient客户端
    var mqttClient: MqttAndroidClient? = null

    //确保全局只创建一次对象
    val messageList = ArrayList<MqttMessageData>() //messageList集合

    //mqttCallBack回调函数代码
    const val updateSendData = 1
    const val updateReceiveData = 2
    const val updateSendError = 3
    const val startMQ2Service = 4
    const val stopMQ2Service = 5
    const val subText = 6
    const val unSubText = 7

    //文本参数
    const val TAG = "MQTT"
    const val sendDirection = "发送:"
    const val receiveDirection = "接收:"
    const val sendError = "连接已断开，发送失败咯！"
    const val receiveError = "json数据格式错误..."
    var messageCount = 0

    //标志
    var subFlag = false //是否订阅

    //回调函数配置
    lateinit var handler:Handler //保存了Message的handlerMessage对象
    //配置MQtt的Callback 全局只存在唯一callback对象
    val callback = object : MqttCallback {
        override fun connectionLost(cause: Throwable?) {
            "MQTT连接已断开".showToast()
        }

        //接收消息
        override fun messageArrived(topic: String?, mqttMessage: MqttMessage?) {
            val message = Message()
            message.what = updateReceiveData
            message.obj = mqttMessage?.payload?.let { String(it) } //obj保存对象
            handler.sendMessage(message)
        }

        override fun deliveryComplete(token: IMqttDeliveryToken?) {

        }
    }

}