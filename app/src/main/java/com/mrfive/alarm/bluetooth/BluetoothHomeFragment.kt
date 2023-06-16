package com.mrfive.alarm.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.BLUETOOTH_SERVICE
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.drawable.Animatable
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mrfive.alarm.R
import com.mrfive.alarm.bluetooth.Bluetooth.TAG
import com.mrfive.alarm.bluetooth.Bluetooth.bluetoothAdapter
import com.mrfive.alarm.bluetooth.Bluetooth.device
import com.mrfive.alarm.bluetooth.Bluetooth.mmSocket
import com.mrfive.alarm.bluetooth.Bluetooth.releaseBluetoothResource
import com.mrfive.alarm.tool.FontStyle
import com.mrfive.alarm.tool.MyApplication
import com.mrfive.alarm.tool.Tool.showToast
import kotlinx.android.synthetic.main.activity_bluetooth_pager.*
import kotlinx.android.synthetic.main.fragment_bluetooth_home.*
import kotlinx.android.synthetic.main.fragment_bluetooth_home.view.*
import java.io.IOException

/**
 * fragment监听事件和Activity一样
 * 需要注意的是要获取activity实例
 * 通过activity间接注册广播
 */
class BluetoothHomeFragment : Fragment(), View.OnClickListener {


    private lateinit var pagerActivity: BluetoothPagerActivity
    private val bluetoothDeviceSet = HashSet<BluetoothDevice>() //集合保存设备信息
    private lateinit var bluetoothReceiver: BluetoothReceiver
    private var animationWait: Animatable? = null
    private val REQUEST_ENABLE_BT = 1
    private val REQUEST_PERMISSION_BLUETOOTH_OPEN = 2
    private val REQUEST_PERMISSION_BLUETOOTH_BONDED = 3
    private val REQUEST_PERMISSION_BLUETOOTH_DISCOVERY = 4
    private val REQUEST_FOUND_DEVICE = 5
    private val notSupportBT = "此设备不支持蓝牙功能"
    private val btOpened = "蓝牙已经开启"
    private val btOpenFailure = "蓝牙打开失败"
    private var btState = false

    /**recyclerView适配器配置 内部类
     * 外部类实在不方便 因为难免要与activity或者fragment交互
     * 而且重用性不高 所以内部类更为方便
     */
    inner class BluetoothRecyclerViewAdapter(private val bluetoothDeviceSet: Set<BluetoothDevice>) :
        RecyclerView.Adapter<BluetoothRecyclerViewAdapter.ViewHolder>() {
        private var task: AsyncTask<Unit, Int, Boolean>? = null
        private var clickPosition: Int? = null
        private var isConnecting: Boolean? = false
        private var isConnected: Boolean? = false
        private var connectButton: Button? = null


        /**
         * 蓝牙连接相关的类使用内部类 原因和adapter一样
         * ASyncTask 异步消息处理机制的封装 便于需要UI的操作
         *
         */
        @SuppressLint("StaticFieldLeak")
        inner class ClientTask : AsyncTask<Unit, Int, Boolean>() {
            private val connectSuccess = 1
            private val connectFailed = 0

            //后台任务开始执行之前调用
            @SuppressLint("MissingPermission")
            override fun onPreExecute() {
                closeDiscoveryBT()
                try {
                    //建立管道
                    mmSocket = device?.createInsecureRfcommSocketToServiceRecord(Bluetooth.MY_UUID)
                    if (mmSocket != null) {
                        isConnecting = true
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "创建管道失败", e)
                }
            }

            //此方法都在子线程中运行
            @SuppressLint("MissingPermission")
            override fun doInBackground(vararg params: Unit?): Boolean {
                try {
                    //管道建立成功 开始连接
                    while (true) {
                        //任务终止
                        if (task?.isCancelled == true) {
                            releaseBluetoothResource()
                            return false
                        }
                        mmSocket?.connect()
                        //成功连接
                        if (mmSocket?.isConnected == true) {
                            publishProgress(connectSuccess)
                            break
                        }
                    }
                    return true
                } catch (e: IOException) {
                    Log.e(TAG, "连接设备失败", e)
                    publishProgress(connectFailed)
                    return false
                }
            }

            //更新UI处理
            override fun onProgressUpdate(vararg values: Int?) {
                //任务终止
                if (task?.isCancelled == true) return
                if (values[0] == connectSuccess) {
                    isConnected = true
                    connectButton?.text = "断开"
                } else {
                    "连接失败咯~".showToast()
                }

            }

            //收尾工作
            override fun onPostExecute(result: Boolean?) {
                //任务终止
                if (task?.isCancelled == true) return
                //最终都会关闭动画
                stopAnim()
                noWaitImage()
                task = null
                isConnecting = false
            }

        }

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val deviceName: TextView = view.findViewById(R.id.textViewBTName)
            val imageId: ImageView = view.findViewById(R.id.imageViewBTImage)
            val deviceConnect: Button = view.findViewById(R.id.buttonBTConnect)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.recycler_view_bluetooth, parent, false)
            //Log.d(TAG,"HomeFragment createViewHOlder被调用了")
            val viewHolder = ViewHolder(view)
            viewHolder.deviceConnect?.setOnClickListener {
                //正在连接 就不处理点击事件
                if (isConnecting == true) {
                    "正在连接哟，客官请稍等~~".showToast()
                    return@setOnClickListener
                } else if (clickPosition != viewHolder.adapterPosition && isConnected == true) { //点击位置和上一个位置不一样
                    "还有连接未关闭，请先关闭上一个连接吧~".showToast()
                    return@setOnClickListener
                }
                when (isConnected) {
                    true -> {
                        //Log.d(TAG,"${connectButton.toString()}是同一个吗？")
                        //断开连接
                        isConnected = false
                        connectButton?.text = "连接"
                        releaseBluetoothResource()
                    }
                    false -> {
                        //把连接的button对象保存
                        connectButton = viewHolder.deviceConnect
                        //Log.d(TAG,"${connectButton.toString()}是同一个吗？")
                        //记录位置
                        clickPosition = viewHolder.adapterPosition
                        //取出device设备
                        device = bluetoothDeviceSet.elementAt(viewHolder.adapterPosition) //作用域延伸
                        //创建任务对象
                        task = ClientTask()
                        //执行连接任务
                        task?.execute()
                        startAnim()
                    }
                    else -> {
                        return@setOnClickListener
                    }
                }
            }
            return viewHolder
        }

        @SuppressLint("MissingPermission")
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            /**
             * 问题：
             * 如果发送消息 onBindViewHolder会重新执行 连接断开逻辑混乱
             * 1.connectButton对象发生改变 不再是之前连接的那个
             * 2.每次点击 都会重新创建button对象
             *
             *解决：
             * 1.createViewHolder发送消息后不会重新执行 移动到createViewHolder中注册
             * 2.点击事件注册是对应每一个button对象的 所以要注意button对象的管理
             * 解决问题！！！
             */
            //Log.d(TAG, "HomeFragment BindViewHolder被调用了")
            val device = bluetoothDeviceSet.elementAt(holder.adapterPosition) //List顺序获取
            holder.imageId.setImageResource(R.drawable.bluetooth_icon)
            holder.deviceName.text = device.name
            /**
             * 注意：connectButton始终保存最后一个button对象
             */
            //connectButton = holder.deviceConnect
        }

        //获得item数量 后面与notify判断数量有关
        override fun getItemCount() = bluetoothDeviceSet.size
    }


    //广播接收者 蓝牙状态变化 内部类
    inner class BluetoothReceiver : BroadcastReceiver() {
        private var preAddress: String = ""

        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            //检测蓝牙状况
            when (intent.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0)) {
                        BluetoothAdapter.STATE_TURNING_ON -> "蓝牙正在打开".showToast()
                        BluetoothAdapter.STATE_ON -> {
                            "蓝牙已打开".showToast()
                            btState = true
                            buttonOpenBT.text = "关闭蓝牙"
                        }
                        BluetoothAdapter.STATE_TURNING_OFF -> "蓝牙正在关闭".showToast()
                        BluetoothAdapter.STATE_OFF -> {
                            "蓝牙已关闭".showToast()
                            btState = false
                            buttonOpenBT.text = "开启蓝牙"
                        }
                    }
                }
                BluetoothDevice.ACTION_ACL_CONNECTED -> "蓝牙已连接".showToast()
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> "蓝牙已断开".showToast()
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> "开始检测设备".showToast()
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    stopAnim()
                    noWaitImage()
                    "检测设备结束".showToast()
                }
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    if (device != null) {
                        /**
                         * 调用closeDiscoveryBT()发现只有一个设备不会闪退
                         * 一个设备重复发现会导致异常
                         * 1.去重判断
                         * 成功解决
                         * 2.不行 一个设备居然会出现地址不一样的情况
                         * 暂时先这么用吧 嗐！
                         */
                        if (device.address != null) {
                            if (preAddress != device.address) {
                                preAddress = device.address
                                bluetoothDeviceSet.add(device)
                                //更新显示
                                recyclerViewBT.adapter?.notifyItemInserted(bluetoothDeviceSet.size - 1)
                                Log.d(TAG, "founded bluetooth address is $preAddress")
                            }
                        }

                    }
                }
            }
        }
    }

    //fragment 生命周期-------------------------------------------------------------------------------
    //1.
    override fun onAttach(context: Context) {
        super.onAttach(context)
        Log.d(TAG, "HomeFragment Attach")
    }

    //2.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "HomeFragment create")
    }

    //3.
    //加载fragment
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d(TAG, "HomeFragment createView")
        return inflater.inflate(R.layout.fragment_bluetooth_home, container, false)
    }

    //4.
    //与fragment相关联的activity创建完毕调用
    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        Log.d(TAG, "HomeFragment activityCreated")
        initBluetooth()
        initView()
        openBroadcast()
        setRecyclerView()
        buttonOpenBT.setOnClickListener(this)
        buttonFindBondedBT.setOnClickListener(this)
        buttonDiscoveryBT.setOnClickListener(this)
    }

    //5.
    override fun onStart() {
        super.onStart()
        Log.d(TAG, "HomeFragment start")
    }

    //6.
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "HomeFragment resume")
    }

    //7.
    override fun onPause() {
        super.onPause()
        Log.d(TAG, "HomeFragment pause")
    }

    //8.
    override fun onStop() {
        super.onStop()
        Log.d(TAG, "HomeFragment stop")
    }

    //9.
    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "HomeFragment destroyView")
    }

    //10.
    //销毁destroy时注销广播
    override fun onDestroy() {
        super.onDestroy()
        pagerActivity.unregisterReceiver(bluetoothReceiver)
        releaseBluetoothResource()
        Log.d(TAG, "HomeFragment destroy")
    }

    //11.
    override fun onDetach() {
        super.onDetach()
        Log.d(TAG, "HomeFragment detach")
    }


//fragment 生命周期-----------------------------------------------------------------------------------

    //初始化View
    private fun initView() {
        //fragment获取父级activity
        if (activity != null) pagerActivity = activity as BluetoothPagerActivity
        //设置字体
        titleBTName.typeface = FontStyle.setTypeface(pagerActivity.assets, FontStyle.AlimamaFont)
        //如果当前系统已经开启蓝牙那么设置按钮为关闭蓝牙
        //检测蓝牙是否开启
        if (bluetoothAdapter?.isEnabled == true) {
            btState = true
            buttonOpenBT.text = "关闭蓝牙"
        }
    }

    //设置imageView未等待时的图片充当背景
    private fun noWaitImage() {
        imageViewWaitAnim.setImageResource(R.drawable.orange_cat_icon)
    }

    //初始化动画
    private fun startAnim() {
        //清空图片
        imageViewWaitAnim.setImageDrawable(null)
        imageViewWaitAnim.setBackgroundResource(R.drawable.frame_anim)
        val anim = imageViewWaitAnim.background
        if (anim is Animatable) {
            animationWait = anim
            animationWait?.start()
        }
    }

    //关闭动画
    private fun stopAnim() {
        imageViewWaitAnim.background = null
        animationWait?.stop()
    }

    //关于蓝牙的各种操作能否进行判断
    private fun remindBluetooth(): Boolean {
        //开启提示
        //检测蓝牙是否开启
        if (bluetoothAdapter?.isEnabled != true) {
            "请先开启蓝牙再操作!".showToast()
            return true
        }
        if (mmSocket?.isConnected == true) {
            "有未关闭的连接，请关闭后再操作".showToast()
            return true
        }
        return false
    }

    //点击事件合集
    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.buttonOpenBT -> {
                openBluetooth()
            }
            R.id.buttonFindBondedBT -> {
                queryBondedBluetoothDevice()
            }
            R.id.buttonDiscoveryBT -> {
                discoveryBluetoothDevice()
            }
        }
    }

    //配置recyclerView
    private fun setRecyclerView() {
        val layoutManager = LinearLayoutManager(pagerActivity)
        recyclerViewBT.layoutManager = layoutManager
        val adapter = BluetoothRecyclerViewAdapter(bluetoothDeviceSet)
        recyclerViewBT.adapter = adapter
    }

    //注册广播
    private fun openBroadcast() {
        //侦听蓝牙状态广播
        val intentFilter = IntentFilter()
        //监视蓝牙关闭和打开的状态
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        //监视蓝牙设备与APP连接的状态
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
        //发现设备广播
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND)
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
        //动态注册 fragment中调用activity间接注册
        bluetoothReceiver = BluetoothReceiver()
        pagerActivity.registerReceiver(bluetoothReceiver, intentFilter)
    }

    //初始化蓝牙
    private fun initBluetooth() {
        //获取蓝牙适配器 弃用BluetoothAdapter.getAdapter
        val bluetoothManager =
            MyApplication.context.getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        Log.d(TAG, bluetoothAdapter.toString())
    }

    //开启蓝牙
    private fun openBluetooth() {
        //不支持蓝牙
        if (bluetoothAdapter == null) {
            notSupportBT.showToast()
            return
        }
        if (!btState) {
            //蓝牙已经开启
            if (bluetoothAdapter?.isEnabled == true) {
                btOpened.showToast()
                return
            }
            //请求打开蓝牙
            val intentOpenBT = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            //connect权限是android 12需要使用的 本设备使用荣耀9x 所以不需要
            if (Build.VERSION.SDK_INT >= 31) {
                if (!checkPermission(
                        arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                        REQUEST_PERMISSION_BLUETOOTH_OPEN
                    )
                ) {
                    return
                }
            }
            startActivityForResult(intentOpenBT, REQUEST_ENABLE_BT)
        } else {
            /**
             * disable异步调用 没有回调方法
             * 所以需要注意释放资源
             */
            if (remindBluetooth()) return
            releaseBluetoothResource()
            val bluetoothDeviceSize = bluetoothDeviceSet.size
            bluetoothDeviceSet.clear()
            recyclerViewBT.adapter?.notifyItemRangeRemoved(0, bluetoothDeviceSize) //删除数量个
            bluetoothAdapter?.disable()
        }

    }

    //查询已配对设备
    private fun queryBondedBluetoothDevice() {
        if (remindBluetooth()) return
        if (Build.VERSION.SDK_INT >= 31) {
            if (!checkPermission(
                    arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                    REQUEST_PERMISSION_BLUETOOTH_BONDED
                )
            ) {
                Log.d(TAG, "权限没过？")
                return
            }
        }
        closeDiscoveryBT()
        //删除recyclerView所有item  达到了清空的目的（集合内容还存在） 而且adapter对象不变
        val bluetoothDeviceSize = bluetoothDeviceSet.size
        //清空集合
        bluetoothDeviceSet.clear()
        recyclerViewBT.adapter?.notifyItemRangeRemoved(0, bluetoothDeviceSize) //删除数量个
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        //Log.d(TAG, "测试通过？${pairedDevices?.size}")
        //安卓12以下代码没执行 pairedDevice为空 原来手机压根就没有配对设备
        if(pairedDevices?.size==0){
            "这个手机还没有配对的设备，和作者一样是个单身狗".showToast()
        }
        pairedDevices?.forEach { device ->
            //将数据放入recyclerView的List集合
            bluetoothDeviceSet.add(device)
            recyclerViewBT.adapter?.notifyItemInserted(bluetoothDeviceSet.size - 1) //从0开始插入
            Log.d(TAG, "bluetooth length is ${bluetoothDeviceSet.size}")
        }
        //更新recyclerView数据
        /**
         * 注意：
         * 重新建立是适配器 recyclerView所有对象会发生改变！！！
         * 所以使用adapter自带更新方法 自带动画 更流畅自然
         */
        //updateRecyclerView()
    }

    //发现设备
    private fun discoveryBluetoothDevice() {
        if (remindBluetooth()) return
        if (Build.VERSION.SDK_INT >= 31) {
            if (!checkPermission(
                    arrayOf(
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.BLUETOOTH_SCAN
                    ), REQUEST_PERMISSION_BLUETOOTH_DISCOVERY
                )
            ) {
                return
            }
        } else {
            if (!checkPermission(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    REQUEST_PERMISSION_BLUETOOTH_DISCOVERY
                )
            ) {
                return
            }
        }
        closeDiscoveryBT()
        /**
         * 注意：
         * 要先处理集合 再通知适配器！！！
         */
        //保存数据个数 通知删除多少
        val bluetoothDeviceSize = bluetoothDeviceSet.size
        //清空集合
        bluetoothDeviceSet.clear()
        recyclerViewBT.adapter?.notifyItemRangeRemoved(0, bluetoothDeviceSize) //删除数量个
        //开始搜索动画
        startAnim()
        //异步执行
        bluetoothAdapter?.startDiscovery()
    }

    //关闭检测设备
    @SuppressLint("MissingPermission")
    private fun closeDiscoveryBT() {
        //正在检测 就关闭检测
        if (bluetoothAdapter?.isDiscovering == true) {
            Log.d(TAG, "关闭正在检测设备")
            bluetoothAdapter?.cancelDiscovery()
        }
    }

    //请求蓝牙后的处理
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_ENABLE_BT -> {
                if (resultCode == AppCompatActivity.RESULT_OK) {

                } else {
                    btOpenFailure.showToast()
                }
            }
        }
    }

    //检查权限
    private fun checkPermission(permissions: Array<String>, requestCode: Int): Boolean {
        //依次检查传过来的权限是否已经申请
        val listPermissions = ArrayList<String>()
        for (permission in permissions) {
            if (
                ContextCompat.checkSelfPermission(
                    pagerActivity,
                    permission
                )
                != PackageManager.PERMISSION_GRANTED
            ) {
                //未申请 添加到集合
                Log.d(TAG, "request $permission")
                listPermissions.add(permission)
            }
        }
        //申请权限
        if (listPermissions.size > 0) {
            ActivityCompat.requestPermissions(pagerActivity, permissions, requestCode)
            return false
        }
        return true
    }

    //请求权限后的处理
    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        var permissionPast = true
        if (grantResults.isNotEmpty()) {
            for (i in permissions.indices) {
                if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                    permissionPast = false //有拒绝的权限
                }
            }
        } else {
            permissionPast = false
        }
        when (requestCode) {
            REQUEST_PERMISSION_BLUETOOTH_OPEN -> {
                if (permissionPast) {
                    openBluetooth()
                } else {
                    Toast.makeText(pagerActivity, "拒绝了权限申请", Toast.LENGTH_SHORT).show()
                }
            }
            REQUEST_PERMISSION_BLUETOOTH_BONDED -> {
                if (permissionPast) {
                    queryBondedBluetoothDevice()
                } else {
                    Toast.makeText(pagerActivity, "拒绝了权限申请", Toast.LENGTH_SHORT).show()
                }
            }
            REQUEST_PERMISSION_BLUETOOTH_DISCOVERY -> {
                if (permissionPast) {
                    discoveryBluetoothDevice()
                } else {
                    Toast.makeText(pagerActivity, "拒绝了权限申请", Toast.LENGTH_SHORT).show()
                }
            }
            REQUEST_FOUND_DEVICE -> {
                if (permissionPast) {
                    return
                } else {
                    Toast.makeText(pagerActivity, "拒绝了权限申请", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}