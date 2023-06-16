package com.mrfive.alarm.mqtt

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.os.Vibrator
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.mrfive.alarm.R
import com.mrfive.alarm.data.MQ2
import com.mrfive.alarm.data.MqttMessageData
import com.mrfive.alarm.mqtt.MQTT.messageList
import com.mrfive.alarm.mqtt.MQTT.receiveDirection
import com.mrfive.alarm.mqtt.MQTT.sendDirection
import com.mrfive.alarm.mqtt.MQTT.sendError
import com.mrfive.alarm.mqtt.MQTT.subFlag
import com.mrfive.alarm.mqtt.MQTT.updateReceiveData
import com.mrfive.alarm.mqtt.MQTT.updateSendData
import com.mrfive.alarm.mqtt.MQTT.updateSendError
import com.mrfive.alarm.service.ServiceData
import com.mrfive.alarm.tool.FontStyle
import com.mrfive.alarm.tool.MyApplication
import com.mrfive.alarm.tool.Tool
import com.mrfive.alarm.tool.Tool.showToast
import com.mrfive.alarm.ui.InitUI
import kotlinx.android.synthetic.main.fragment_mqtt_message.*
import org.eclipse.paho.client.mqttv3.*

class MqttMessageFragment : Fragment(), View.OnClickListener, InitUI {
    private lateinit var pagerActivity: MqttPagerActivity
    private lateinit var recyclerView: RecyclerView
    private lateinit var handlerMessage: Handler
    private lateinit var handlerFlag: Handler

    //媒体
    private lateinit var mediaPlayer:MediaPlayer
    //震动
    private lateinit var vibrator: Vibrator

    //话题参数
    private lateinit var subTopic: String
    private lateinit var pubTopic: String
    private var qos: Int = 0
    private lateinit var msg: String

    //recyclerView适配器
    inner class MessageRecyclerViewAdapter(private val messageList: List<MqttMessageData>) :
        RecyclerView.Adapter<MessageRecyclerViewAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val direction: TextView = view.findViewById(R.id.messageDirection)
            val time: TextView = view.findViewById(R.id.messageTime)
            val messageContent: TextView = view.findViewById(R.id.messageContent)
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): ViewHolder {
            //加载布局
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.recycler_view_message, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(
            holder: ViewHolder,
            position: Int
        ) {
            //更新textView的UI
            holder.direction.text = messageList[position].direction
            holder.time.text = messageList[position].time
            holder.messageContent.text = messageList[position].messageContent
        }

        override fun getItemCount() = messageList.size

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        //将布局加载进来
        return inflater.inflate(R.layout.fragment_mqtt_message, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        initData()
        initView()
        initListener()
        recoverTopicConfigData()
        Log.d(MQTT.TAG, "Mqtt Message onActivityCreated")
    }

    override fun onStop() {
        super.onStop()
        //保存配置信息
        saveTopicConfigData()
        Log.d(MQTT.TAG, "Mqtt Message onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(MQTT.TAG, "Mqtt Message onDestroy")
    }

    override fun onDetach() {
        super.onDetach()
        Log.d(MQTT.TAG, "Mqtt Message onDetach")
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.buttonSub -> {
                if (MQTT.mqttClient?.isConnected == true) {
                    flushTopicConfigData()
                    if (subFlag) {
                        unSubscribeTopic()
                    } else {
                        subscribeTopic()
                    }
                } else {
                    "还没连接滴捏".showToast()
                }
            }
            R.id.buttonPub -> {
                if (MQTT.mqttClient?.isConnected == true) {
                    flushTopicConfigData()
                    publishMessage()
                } else {
                    "还没连接滴捏".showToast()
                }
            }
            R.id.buttonClearAll -> {
                clearAllMessages()
            }
        }
    }

    override fun initView() {
        //设置字体
        messageTitle.typeface = FontStyle.setTypeface(pagerActivity.assets, FontStyle.AlimamaFont)
        if(subFlag) buttonSub.text = "取阅"
        else buttonSub.text = "订阅"
    }

    override fun initData() {
        //手动获取recyclerView控件ID
        //fragment获取父级activity
        if (activity != null) pagerActivity = activity as MqttPagerActivity
        recyclerView = pagerActivity.findViewById(R.id.recyclerViewMessageMqtt)
        val layoutManager = LinearLayoutManager(MyApplication.context)
        recyclerView.layoutManager = layoutManager
        val adapter = MessageRecyclerViewAdapter(messageList)
        recyclerView.adapter = adapter
        //多媒体初始化
        initMediaPlayer()
        initVibrator()
        //配置handler
        handlerMessage = @SuppressLint("HandlerLeak")
        object : Handler() {
            @SuppressLint("HandlerLeak")
            override fun handleMessage(handlerMsg: Message) {
                super.handleMessage(handlerMsg)
                //数据超过100时，清空数据
                if(messageList.size>100) clearAllMessages()
                when (handlerMsg.what) {
                    updateSendData -> {
                        //发送成功 更新list集合
                        messageList.add(MqttMessageData(sendDirection, Tool.formatTime(), msg))
                    }
                    updateSendError -> {
                        //发送成功 更新list集合
                        messageList.add(
                            MqttMessageData(
                                sendDirection,
                                Tool.formatTime(),
                                sendError
                            )
                        )
                    }
                    /**
                     * 当fragment第二次创建时 receiveData接收的消息不能显示(实际已存储)
                     * 1.该初始化一次对象的就初始化一次 不要创建fragment时多次实例化
                     * 2.将callback和handler作为全局变量即可解决问题
                     */
                    updateReceiveData -> {
                        //解析传过来的数据
                        val mq2 = parseJsonWithGson(handlerMsg.obj as String)
                        if(mq2!=null){
                            val mq2Info = StringBuilder()
                            //保存阈值
                            mq2Info.append("[${MQTT.messageCount++}]烟雾浓度:${mq2[0].con}ppm 电压:${mq2[0].voltage}V")
                            messageList.add(MqttMessageData(receiveDirection, Tool.formatTime(), mq2Info.toString()))
                            //将数据发送到AlarmService中更新后台显示
                            ServiceData.mQ2Binder?.updateData(mq2[0].con, mq2[0].voltage)
                            //报警检测
                            alarmCheck(mq2[0].voltage,mq2[0].threshold)
                        }else{
                            messageList.add(MqttMessageData(receiveDirection,Tool.formatTime(),MQTT.receiveError))
                        }
                        /**
                         * 存在大问题：应该保持MQTT服务器即使在后台也应该保持连接并接收数据才行！！！
                         * 1.连接成功再启动service
                         * 2.只能手动关闭service，并关闭mqtt服务器
                         */
                    }
                }
                /**
                 * 当messageFragment关闭时出现recyclerView 空指针异常
                 * 1.判断messageFragment是否在前台
                 * 2.全局recyclerView
                 * 3.主要出现在订阅未关闭，再创建fragment时重复订阅，导致接收两个信息崩溃
                 * 4.最终发现是recyclerView会有空指针情况的出现 手动获取控件ID 成功！！！ 所以有时候也不能用kotlin自动获取控件id插件
                 *
                 * 还有一个问题 每次重新创建fragment时callback和handlerMessage不是多次创建了吗？
                 * 1.实验证明的确会重新创建callback和handler相关对象，目前未知是否会对配置对象造成影响
                 * 2.水平有限 内存分析那一块确实还不会 暂时先放一放吧...。。。
                 */
                recyclerView.scrollToPosition(messageList.size - 1)
                //直接从后面插入 不全部重开 节省资源
                recyclerView.adapter?.notifyItemInserted(messageList.size - 1)
            }
        }
        //保存handlerMessage对象
        MQTT.handler = handlerMessage
        //配置标志handler
        handlerFlag = @SuppressLint("HandlerLeak")
        object : Handler() {
            @SuppressLint("HandlerLeak")
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)
                when(msg.what){
                    MQTT.subText -> {
                        buttonSub.text = "取阅"
                        subFlag = true
                    }
                    MQTT.unSubText ->{
                        buttonSub.text = "订阅"
                        subFlag = false
                    }
                }
            }
        }
    }

    override fun initListener() {
        buttonSub.setOnClickListener(this)
        buttonPub.setOnClickListener(this)
        buttonClearAll.setOnClickListener(this)
    }


    //保存话题和消息数据
    private fun saveTopicConfigData() {
        val editor =
            MyApplication.context.getSharedPreferences("mqtt_config", Context.MODE_PRIVATE).edit()
                .apply {
                    putString("subTopic", editSubTopic.text.toString())
                    putString("pubTopic", editPubTopic.text.toString())
                    putString("msg", editMsg.text.toString())
                }
        editor.apply()
    }

    //恢复数据
    private fun recoverTopicConfigData() {
        val prefs = MyApplication.context.getSharedPreferences("mqtt_config", Context.MODE_PRIVATE)
        editSubTopic.setText(prefs.getString("subTopic", "test"))
        editPubTopic.setText(prefs.getString("pubTopic", "test"))
        editMsg.setText(prefs.getString("msg", "this is test"))
    }

    //刷新配置数据
    private fun flushTopicConfigData() {
        val prefs = MyApplication.context.getSharedPreferences("mqtt_config", Context.MODE_PRIVATE)
        subTopic = editSubTopic.text.toString()
        pubTopic = editPubTopic.text.toString()
        msg = editMsg.text.toString()
        qos = prefs.getString("qos", "0")?.toInt()!!
    }

    //订阅话题
    private fun subscribeTopic() {
        //获取sharedPreference的参数
        try {
            MQTT.mqttClient?.subscribe(subTopic, qos, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    "订阅成功".showToast()
                    //异步消息处理机制，切换到主线程中执行UI操作
                    val message = Message()
                    message.what = MQTT.subText
                    handlerFlag.handleMessage(message)
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    "订阅失败".showToast()
                }
            })
        } catch (e: MqttException) {
            Log.e(MQTT.TAG, "订阅失败", e)
        }
    }

    //取消订阅
    private fun unSubscribeTopic() {
        MQTT.mqttClient?.unsubscribe(subTopic, null, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                "取消订阅成功".showToast()
                val message = Message()
                message.what = MQTT.unSubText
                handlerFlag.handleMessage(message)
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                "取消订阅失败".showToast()
            }
        })
    }

    //发布消息
    private fun publishMessage() {
        val message = Message()
        val mqttMessage = MqttMessage()
        mqttMessage.payload = msg.toByteArray()
        mqttMessage.qos = qos
        mqttMessage.isRetained = false
        MQTT.mqttClient?.publish(pubTopic, mqttMessage, null, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                message.what = updateSendData
                handlerMessage.sendMessage(message)
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                message.what = updateSendError
                handlerMessage.sendMessage(message)
            }
        })
    }

    /*//更新buttonUI
    private fun flushButtonUI() {
        *//**
         * 逻辑存在问题 应该是subFlag==true时应该显示“取消”
         * 但实际却是相反的
         * 原因在于IMqttActionListener在子线程中执行，在子线程更新UI线程是不安全的操作
         * 导致subFlag不能同步更新
         * 解决方案:
         * 1.使用异步消息机制将线程切换到UI主线程中
         *//*
        //以下故意不使用handler处理
        if (subFlag) {
            buttonSub.text = "订阅"
        } else {
            buttonSub.text = "取消"
        }
    }*/

    //清空消息
    private fun clearAllMessages() {
        val size = messageList.size
        messageList.clear()
        recyclerViewMessageMqtt.adapter?.notifyItemRangeRemoved(0, size)
        MQTT.messageCount = 0
    }

    //解析json数据
    fun parseJsonWithGson(jsonData: String): List<MQ2>? {
        val gson = Gson()
        //构建json对象
        val typeOf = object : TypeToken<List<MQ2>>() {}.type
        //json对象放在List集合中
        return try {
            gson.fromJson<List<MQ2>>(jsonData, typeOf)
        }catch (e:JsonSyntaxException){
            //json语法不完整
            null
        }
    }

    //语音初始化
    private fun initMediaPlayer(){
        mediaPlayer = MediaPlayer()
        val assetManager = MyApplication.context.assets
        val fd = assetManager.openFd("music/warninglb.wav")
        mediaPlayer.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
        mediaPlayer.prepare()
    }

    //震动初始化
    private fun initVibrator(){
        vibrator = MyApplication.context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    //报警检测
    private fun alarmCheck(voltage:Float,threshold:Float){
        if(voltage>=threshold){
            mediaPlayer.start()
            if(vibrator.hasVibrator()){
                val  vibrateMode = longArrayOf(100,3000)
                vibrator.vibrate(vibrateMode,-1) //0无限循环
            }
        }
    }
}