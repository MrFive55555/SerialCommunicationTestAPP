package com.mrfive.alarm.mqtt

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.mrfive.alarm.R
import com.mrfive.alarm.mqtt.MQTT.callback
import com.mrfive.alarm.mqtt.MQTT.mqttClient
import com.mrfive.alarm.mqtt.MQTT.startMQ2Service
import com.mrfive.alarm.mqtt.MQTT.stopMQ2Service
import com.mrfive.alarm.service.AlarmService
import com.mrfive.alarm.service.ServiceData
import com.mrfive.alarm.tool.FontStyle
import com.mrfive.alarm.tool.MyApplication
import com.mrfive.alarm.tool.Tool.showToast
import com.mrfive.alarm.ui.InitUI
import kotlinx.android.synthetic.main.fragment_mqtt_home.*
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*

class MqttHomeFragment : Fragment(), View.OnClickListener, InitUI {
    private lateinit var pagerActivity: MqttPagerActivity
    private var connectFlag: Boolean = false

    //activity与service通信
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(
            name: ComponentName?,
            service: IBinder?
        ) {
            ServiceData.mQ2Binder = service as AlarmService.MQ2Binder
            ServiceData.mQ2Binder?.updateData(0,0.00f)
            //每次调用 ServiceData.MQ2Binder对象会重新创建
            Log.d(MQTT.TAG, ServiceData.mQ2Binder.toString())
        }

        override fun onServiceDisconnected(name: ComponentName?) {

        }
    }

    //异步消息处理
    private val handlerHome = @SuppressLint("HandlerLeak")
    object : Handler() {
        @SuppressLint("HandlerLeak")
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when (msg.what) {
                startMQ2Service -> {
                    startMQ2Service()
                }
                stopMQ2Service -> {
                    stopMQ2Service()
                }
            }
        }
    }

    //启动MQ2服务
    private fun startMQ2Service() {
        //启动MQ2服务
        val intent = Intent(MyApplication.context, AlarmService::class.java)
        //Service的onCreate方法只会调用一次，多次调用只会执行startCommand方法
        //且每个Service只存在一个对象，故调用stop一次就结束服务
        MyApplication.context.startService(intent)
        //BIND_AUTO_CREATE自动创建service(如果Service不存在的话),执行onCreate方法
        MyApplication.context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    //停止MQ2 service
    private fun stopMQ2Service() {
        //先解绑 再停止
        /**
         * 退出MqttPagerActivity后再进入activity再停止MQ2Service时会报错 service not registered
         * 这是因为PagerActivity被销毁时 connection会自动解绑
         * 1.不再使用binder service
         * 2.启动时再次绑定service 成功！
         */
        val intent = Intent(MyApplication.context, AlarmService::class.java)
        MyApplication.context.unbindService(connection)
        MyApplication.context.stopService(intent)
        //释放mqtt资源
        mqttClient?.close()
        mqttClient = null
        Log.d(MQTT.TAG, "MQTT已经关闭")
        Log.d(MQTT.TAG, mqttClient.toString())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        //将布局加载进来
        return inflater.inflate(R.layout.fragment_mqtt_home, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        recoverMqttConfigData()
        initListener()
        initData()
        initView()
        Log.d(MQTT.TAG, "Mqtt Home on ActivityCreated")
        //每次调用 handlerHome对象会重新创建
        Log.d(MQTT.TAG, handlerHome.toString())
        //连接mqtt之后，mqtt对象不变，除非重连
        Log.d(MQTT.TAG, mqttClient.toString())
    }

    override fun onStop() {
        super.onStop()
        //保存配置信息
        saveMqttConfigData()
        Log.d(MQTT.TAG, "Mqtt Home onStop")
    }

    override fun onDestroy() {
        /**
         * destroy调用saveMqttConfigData会空指针异常
         * 放在onStop里面没问题
         */
        super.onDestroy()
        //退出fragment时 断开mqtt服务器连接
        //为了MQ2服务 退出后不再关闭MQTT服务器 只能手动关闭
        /* if(MQTT.mqttClient?.isConnected == true){
             MQTT.mqttClient?.unregisterResources();
             MQTT.mqttClient?.close()
             MQTT.mqttClient?.disconnect()
             MQTT.mqttClient = null
             Log.d(MQTT.TAG,"MQTT已经关闭")
         }*/
        Log.d(MQTT.TAG, "Mqtt Home onDestroy")
    }

    override fun onDetach() {
        super.onDetach()
        Log.d(MQTT.TAG, "Mqtt Home onDetach")
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.floatButtonConnect -> {
                if (connectFlag) {
                    disconnectMqttService()
                } else {
                    connectMqttService()
                }
                flushFloatButtonUI()
            }
        }
    }

    override fun initView() {
        //fragment获取父级activity
        if (activity != null) pagerActivity = activity as MqttPagerActivity
        //设置字体
        titleMqttName.typeface = FontStyle.setTypeface(pagerActivity.assets, FontStyle.AlimamaFont)
        if (mqttClient?.isConnected == true) {
            floatButtonConnect.setImageResource(R.drawable.disconnect)
            connectFlag = true
        }
    }

    override fun initData() {
        if (mqttClient?.isConnected == true) {
            //每次进入fragment重新绑定
            val intent = Intent(context, AlarmService::class.java)
            MyApplication.context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun initListener() {
        floatButtonConnect.setOnClickListener(this)
    }

    //保存配置数据
    private fun saveMqttConfigData() {
        val editor =
            MyApplication.context.getSharedPreferences("mqtt_config", Context.MODE_PRIVATE).edit()
                .apply {
                    putString("url", editMqttIP.text.toString())
                    putString("user", editMqttUser.text.toString())
                    putString("password", editMqttPassword.text.toString())
                    putString("clientId", editMqttClientId.text.toString())
                    putString("qos", editMqttQos.text.toString())
                    putString("cleanSession", editMqttCleanSession.text.toString())
                    putString("willTopic", editMqttWillTopic.text.toString())
                    putString("willMsg", editMqttWillMsg.text.toString())
                    putString("willQos", editMqttWillQos.text.toString())
                    putString("willRetain", editMqttWillRetain.text.toString())
                }
        editor.apply()
    }

    //恢复配置数据并刷新控件显示
    private fun recoverMqttConfigData() {
        /**
         * getInt会导致闪退 editText.setText(getInt)会导致传入的Int参数闪退
         */
        //editText显示字符串
        val prefs = MyApplication.context.getSharedPreferences("mqtt_config", Context.MODE_PRIVATE)
        editMqttIP.setText(prefs.getString("url", "tcp://120.79.198.205:1883"))
        editMqttUser.setText(prefs.getString("user", "Mr.Five"))
        editMqttPassword.setText(prefs.getString("password", "Hcw.5208"))
        editMqttClientId.setText(prefs.getString("clientId", "onMQ2Android"))
        editMqttQos.setText(prefs.getString("qos", "0"))
        editMqttCleanSession.setText(prefs.getString("cleanSession", "false"))
        editMqttWillTopic.setText(prefs.getString("willTopic", "deviceState"))
        editMqttWillMsg.setText(prefs.getString("willMsg", "onLine"))
        editMqttWillQos.setText(prefs.getString("willQos", "0"))
        editMqttWillRetain.setText(prefs.getString("willRetain", "offLine"))
    }

    //连接MQTT服务器
    private fun connectMqttService() {
        /**
         * 需要引入androidx库的localBroadcastManager 原V4库已经过时无法使用
         */
        //初始化客户端
        val url = editMqttIP.text.toString()
        val clientId = editMqttClientId.text.toString()
        //1.客户端配置
        mqttClient = MqttAndroidClient(context, url, clientId)
        Log.d(MQTT.TAG, mqttClient.toString())
        //2.MQTT回调函数配置 需要在connect连接之前就配置***
        mqttClient?.setCallback(callback)
        val options = MqttConnectOptions()
        options.userName = editMqttUser.text.toString()
        options.password = editMqttPassword.text.toString().toCharArray()
        options.isCleanSession = editMqttCleanSession.text.toString().toBoolean()
        options.connectionTimeout = 5000
        try {
            mqttClient?.connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    "MQTT服务器连接成功".showToast()
                    /**
                     * 不要在这里调用一些控件id的对象参数 会空指针异常
                     * 1.使用handler异步消息处理机制回到主线程处理UI
                     */
                    //连接成功 启动MQ2服务
                    val message = Message()
                    message.what = startMQ2Service
                    handlerHome.sendMessage(message)
                    connectFlag = true
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    "MQTT服务器连接失败，请检查配置参数是否正确".showToast()
                }
            })
        } catch (e: MqttException) {
            Log.e(MQTT.TAG, "连接失败", e)
        }
    }

    //断开连接
    private fun disconnectMqttService() {
        try {
            mqttClient?.disconnect(null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    val message = Message()
                    message.what = stopMQ2Service
                    handlerHome.sendMessage(message)
                    connectFlag = false
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    "无法断开当前连接".showToast()
                }
            })
        } catch (e: MqttException) {
            Log.e(MQTT.TAG, "断开连接失败", e)
        }
    }

    //更新UI
    private fun flushFloatButtonUI() {
        if (connectFlag) floatButtonConnect.setImageResource(R.drawable.connect)
        else floatButtonConnect.setImageResource(R.drawable.disconnect)
    }
}