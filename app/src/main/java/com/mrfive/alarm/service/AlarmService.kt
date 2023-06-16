package com.mrfive.alarm.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.mrfive.alarm.R
import com.mrfive.alarm.tool.MyApplication

/**
 * 有了服务 不只是服务类的变量在后台运行 其他类的全局变量也在后台运行中.......
 */
class AlarmService : Service() {
    //activity和service通信
    private lateinit var notificationBuilder: NotificationCompat.Builder
    inner class MQ2Binder : Binder() {
        private val contentText = "烟雾检测中..."
        private val mq2ConText = " 浓度:"
        private val mq2VolText = " 电压:"
        private val notificationIdMQ2 = 1
        //MQ2数据更新显示
        fun updateData(contra: Int = 0, voltage: Float = 0.00f) {
            /**
             * 问题 FirstActivity每次点击会重复创建activity对象
             * 1.禁用点击跳转 (不太好的方法！！)
             */
            //val intent = Intent(MyApplication.context, FirstActivity::class.java)
            //val pi = PendingIntent.getActivity(MyApplication.context, 0, intent, 0)
            //.setContentIntent(pi)
            notificationBuilder.setContentText(contentText + mq2ConText + contra + "ppm" + mq2VolText + voltage + 'V')
            startForeground(notificationIdMQ2, notificationBuilder.build())
        }
    }

    //只在创建时调用
    override fun onCreate() {
        super.onCreate()
        //前台service 持续执行
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        //Android8以上需要通道
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "alarm_service",
                "mq2",
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }
        //初始化notification配置 调用build
        notificationBuilder = NotificationCompat.Builder(MyApplication.context, "alarm_service")
            .setContentTitle("MQ-2")
            .setSmallIcon(R.drawable.warning_mq2)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder = MQ2Binder()
}