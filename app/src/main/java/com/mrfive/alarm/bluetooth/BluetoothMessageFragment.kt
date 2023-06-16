package com.mrfive.alarm.bluetooth

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaPlayer
import android.os.AsyncTask
import android.os.Bundle
import android.os.Vibrator
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mrfive.alarm.R
import com.mrfive.alarm.data.BluetoothMessageData
import com.mrfive.alarm.tool.FontStyle
import com.mrfive.alarm.tool.MyApplication
import com.mrfive.alarm.tool.Tool
import com.mrfive.alarm.tool.Tool.showToast
import kotlinx.android.synthetic.main.activity_first.*
import kotlinx.android.synthetic.main.fragment_bluetooth_message.*
import kotlinx.android.synthetic.main.recycler_view_bluetooth.*
import okio.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets

class BluetoothMessageFragment : Fragment(), View.OnClickListener {
    private val messageList = ArrayList<BluetoothMessageData>()
    private var task: MessageTask? = null
    private val sendDirection = "发送:"
    private val receiveDirection = "接收:"
    private val sendError = "连接已断开，发送失败咯！"
    private val receiveError = "连接已断开，接收失败呀！"

    //语音
    private val mediaPlayer = MediaPlayer()
    //震动
    private val vibrator = MyApplication.context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    //写入数据量小时 可以不放子线程中执行
    private fun sendData(bytes: ByteArray): Boolean {
        val mmOutStream: OutputStream?
        return try {
            //获取输出流
            mmOutStream = Bluetooth.mmSocket?.outputStream
            mmOutStream?.write(bytes)
            true
        } catch (e: IOException) {
            Log.e(Bluetooth.TAG, sendError, e)
            false
        }
    }

    //消息任务
    @SuppressLint("StaticFieldLeak")
    inner class MessageTask : AsyncTask<Unit, Int, Boolean>() {
        //数据流
        private var mmInStream: InputStream? = null

        //数据缓存
        private val mmBuffer: ByteArray = ByteArray(1024)
        private val mmPackageHead: ByteArray = ByteArray(6) //包头
        private val mmPackageData: ByteArray = ByteArray(1024) //数据限定长1024byte=1K
        private val mmPackageTail: ByteArray = ByteArray(2) //包围
        private var headCount = 0
        private var dataCount = 0
        private var tailCount = 0
        private var numBytes = 0

        //后台任务开始执行之前调用
        @SuppressLint("MissingPermission")
        override fun onPreExecute() {
            //获取输入流
            mmInStream = Bluetooth.mmSocket?.inputStream
        }

        //此方法都在子线程中运行
        @SuppressLint("MissingPermission")
        override fun doInBackground(vararg params: Unit?): Boolean {
            //读取消息因为要一直检测是否收到消息 所以需要放在子线程中执行
            try {
                while (true) {
                    //任务终止
                    if (task?.isCancelled == true) return false
                    //读取字节长度
                    numBytes = mmInStream?.read(mmBuffer)!!
                    //将字节长度推送到主线程
                    //这不是一直在循环里吗 应该会一直推送才对 可能是socket内部定义了结束判断 卡在上面只有收到字符才进行下一步操作
                    //Log.d(Bluetooth.TAG,"循环中${numBytes}")
                    publishProgress()
                    //只能定义数据包了
                }

            } catch (e: IOException) {
                Log.e(Bluetooth.TAG, "连接设备失败", e)
                return false
            }
        }

        //更新UI处理
        override fun onProgressUpdate(vararg values: Int?) {
            //任务终止
            if (task?.isCancelled == true) return
            //更新recyclerView
            //Log.d(Bluetooth.TAG,"小兔崽子又给我推送了")
            ///checkDataPackage()
            //暂时先不检查 通过率实在过低
            val messages = String(mmBuffer, 0, numBytes, StandardCharsets.UTF_8)
            judgeVoltage(messages)
            messageList.add(BluetoothMessageData(receiveDirection, Tool.formatTime(), messages))
            recyclerViewMessageBT.adapter?.notifyItemInserted(messageList.size - 1)
            //recyclerView随着新消息滚动 注意：int数据过多会溢出 导致滚动异常
            recyclerViewMessageBT.scrollToPosition(messageList.size - 1) //跳转到指定item 只要item显示就行
        }

        //收尾工作
        override fun onPostExecute(result: Boolean?) {
            //任务终止
            if (task?.isCancelled == true) return
            //接收失败
            if (result == false) {
                messageList.add(BluetoothMessageData(receiveDirection, Tool.formatTime(), receiveError))
                recyclerViewMessageBT.adapter?.notifyItemInserted(messageList.size - 1)
            }
        }

        //取消调用
        override fun onCancelled() {
            super.onCancelled()
            /**一定要清空对象 否则影响下次开启*
             * 尤其是mmInStream不删除对象 之前的就还存在
             * 不能调用close 会断开连接
             */
            mmInStream = null
            task = null
            Log.d(Bluetooth.TAG, "任务取消了")
        }

        /**
         * 蓝牙串口数据分段处理
         * 1.判断字符结束符
         * 无限循环了 不行
         *
         * 2.定义数据包格式 接收分段数据 之后再封包处理 (原理就是合二为一)
         * 使其间接的成为一段完整的数据包
         * 具体原因不清楚 粗略知道是socket通病
         */

        //数据包=包头+数据+包尾部
        //一次数据分段后 会多次调用此方法 注意考虑情况
        private fun checkDataPackage() {

            /**
             * 波特率尽可能高些（过低的波特率分段次数越明显） 本次以115200为例
             * 格式：包头mrfive +数据 + 包尾(mrfive) 包围包头相同判断法//包尾(结束符 13 10)
             * 1. 13 10 是整个字符串的结束符
             * 2. UTF-8下中文是三个字节构成且均为负数
             * 3. stm32发送UART的\r\n就是结束符13 10
             */

            /**
             * 基本解决了分段的问题
             * 但还有问题就是：1.假设包头包围同时不满 那么系统就收不到任何提示 2.数据过长时 会产生多次结束符 如何汇总
             * 暂时解决不了 就这样吧
             */

            //1.数据统计
            for (i in 0 until numBytes) {
                val ch = mmBuffer[i].toInt()
                //包头检测
                if ((ch == 109 || ch == 114 || ch == 102 || ch == 105 || ch == 118 || ch == 101) && headCount < 6) {
                    mmPackageHead[headCount++] = mmBuffer[i]
//                    Log.d(
//                        Bluetooth.TAG,
//                        "包头：" + String(mmPackageHead, 0, headCount, StandardCharsets.UTF_8)
//                    )
                } else if (mmBuffer[i].toInt() == 13 || mmBuffer[i].toInt() == 10) {
                    /**
                     * 一个数据包 可能出现多次结束符 需要改进
                     *  1.自定义包尾
                     *  2.累次判断
                     */
                    if (tailCount >= 2) {
                        //因为可能会多次结束 所以判断最终的是否通过
                        tailCount = 0
                    }
                    mmPackageTail[tailCount++] = mmBuffer[i] //便于判断
//                    Log.d(
//                        Bluetooth.TAG,
//                        "包尾：${mmPackageTail[tailCount].toInt()+5}"
//                    )
//                    tailCount++
                } else {
                    mmPackageData[dataCount++] = mmBuffer[i]
//                    Log.d(
//                        Bluetooth.TAG,
//                        "数据：" + String(mmPackageData, 0, dataCount, StandardCharsets.UTF_8)
//                    )
                }
            }

            /**
             * 丢包问题不显示了  因为影响阅读体验
             */
            //2.判断数据包数据完整性
            if (headCount == 6) { //包头是否满了
                val head = String(mmPackageHead, 0, headCount, StandardCharsets.UTF_8)
                if (head == "mrfive") { //包头是否相等
                    if (tailCount == 2) { //包尾是否满了
                        if (mmPackageTail[0].toInt() == 13 && mmPackageTail[1].toInt() == 10) {//包尾是否相等
                            val messages =
                                String(mmPackageData, 0, dataCount, StandardCharsets.UTF_8)
                            messageList.add(
                                BluetoothMessageData(
                                    receiveDirection,
                                    Tool.formatTime(),
                                    messages
                                )
                            )
                            recyclerViewMessageBT.adapter?.notifyItemInserted(messageList.size - 1)
                            recyclerViewMessageBT.scrollToPosition(messageList.size - 1)
                            //取出电压值
                            judgeVoltage(messages)
                        } else { //不相等
                            //messageList.add(BluetoothMessage(receiveDirection, Tool.formatTime(), dataLose))
                        }
                    } else { //不满
                        //messageList.add(BluetoothMessage(receiveDirection, Tool.formatTime(), dataLose))
                    }
                } else { //不相等
                    //messageList.add(BluetoothMessage(receiveDirection, Tool.formatTime(), dataLose))
                }
                //recyclerViewMessageBT.adapter?.notifyItemInserted(messageList.size - 1)
                //计数清零
                headCount = 0
                tailCount = 0
                dataCount = 0
            }
        }

        //对电压值判断
        private fun judgeVoltage(messages: String) {
            //取出电压值
            val builder = StringBuilder()
            var count = 0
            for (ch in messages) {
                if (ch in '0'..'9') {
                    builder.append(ch)
                    count++
                    if (count == 1) {
                        //添加小数点
                        builder.append('.')
                    } else if (count == 3) {
                        break
                    }
                }
            }
            //字符串转换成浮点数
            val voltage = builder.toString().toFloatOrNull()
            //Log.d(Bluetooth.TAG, "电压是:${voltage}"+"V")
            if (voltage != null) {
                //温度超过阈值 发出警报
                if (voltage > 0.8) {
                    mediaPlayer.start()
                    //震动
                    if(vibrator.hasVibrator()){
                        val  vibrateMode = longArrayOf(100,3000)
                        vibrator.vibrate(vibrateMode,-1) //0无限循环
                    }
                }
            }
        }

    }

    //适配器
    inner class MessageRecyclerViewAdapter(private val messageList: List<BluetoothMessageData>) :
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
            //更新text
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
        return inflater.inflate(R.layout.fragment_bluetooth_message, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        initView()
        setRecyclerView()
        initMediaPlayer()
        buttonSendDataBT.setOnClickListener(this)
        buttonClearAll.setOnClickListener(this)
        buttonReceive.setOnClickListener(this)
    }

    //fragment销毁 释放资源
    override fun onDestroy() {
        super.onDestroy()
        task?.cancel(true)
        task = null
        mediaPlayer.stop()
        mediaPlayer.release()
    }

    //配置recyclerView布局
    private fun setRecyclerView() {
        val layoutManager = LinearLayoutManager(MyApplication.context)
        recyclerViewMessageBT.layoutManager = layoutManager
        val adapter = MessageRecyclerViewAdapter(messageList)
        recyclerViewMessageBT.adapter = adapter
    }

    /**更新recyclerView数据
     * 耗费资源 因为是重新创建对象抛弃之前的对象
     */
    /* private fun updateRecyclerView() {
        val adapter = MessageRecyclerViewAdapter(messageList)
        recyclerViewMessageBT.adapter = adapter
     }*/

    //点击事件合集
    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.buttonSendDataBT -> {
                writeData()
            }
            R.id.buttonClearAll -> {
                clearAllMessages()
            }
            R.id.buttonReceive -> {
                receiveData()
            }
        }
    }

    //初始化UI
    private fun initView() {
        //设置字体
        messageTitle.typeface =
            FontStyle.setTypeface(MyApplication.context.assets, FontStyle.AlimamaFont)
    }

    //发送消息
    private fun writeData() {
        if (Bluetooth.mmSocket == null) {
            "还没连接滴捏".showToast()
            return
        } else if (TextUtils.isEmpty(editTextDataBT.text)) {
            "输入一些内容呗,都是可以碰撞的滑梯".showToast()
            return
        }
        //判断发送情况
        val message = editTextDataBT.text.toString()
        if (sendData(message.toByteArray(StandardCharsets.UTF_8))) {
            //发送成功 更新list集合
            messageList.add(BluetoothMessageData(sendDirection, Tool.formatTime(), message))
        } else {
            messageList.add(BluetoothMessageData(sendDirection, Tool.formatTime(), sendError))
        }
        //直接从后面插入 不全部重开 节省资源
        recyclerViewMessageBT.adapter?.notifyItemInserted(messageList.size - 1)
        //发送后 清空发送栏
        editTextDataBT.text.clear()
    }

    //接收消息
    private fun receiveData() {
        if (Bluetooth.mmSocket == null) {
            "还没连接滴捏".showToast()
            return
        } else if (task == null) {
            buttonReceive.text = "停止"
            task = MessageTask()
            task?.execute()
        } else {
            buttonReceive.text = "接收"
            task?.cancel(true)
        }
        /**
         * 注意：cancel并没有真正结束调用
         * 所以需要isCancelled进行辅助判断
         * 因此再次调用execute会异常 因为根本就没结束 只是标记为停止而已
         *
         * 1.cancel传入false继续运行 不行 传入true的时候子线程return结束了
         * 2.重建对象
         */
    }

    //清空消息
    private fun clearAllMessages() {
        val size = messageList.size
        messageList.clear()
        recyclerViewMessageBT.adapter?.notifyItemRangeRemoved(0, size)
    }

    //语音初始化
    private fun initMediaPlayer() {
        val assetManager = MyApplication.context.assets
        //val fd = assetManager.openFd("music/warning.mp3")
        val fd = assetManager.openFd("music/warninglb.wav")
        mediaPlayer.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
        mediaPlayer.prepare()
    }
}
