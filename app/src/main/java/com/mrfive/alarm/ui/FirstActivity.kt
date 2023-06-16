package com.mrfive.alarm.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.mrfive.alarm.R
import com.mrfive.alarm.bluetooth.BluetoothPagerActivity
import com.mrfive.alarm.mqtt.MqttPagerActivity
import com.mrfive.alarm.tool.FontStyle
import com.mrfive.alarm.tool.Tool.showToast
import kotlinx.android.synthetic.main.activity_first.*

class FirstActivity : AppCompatActivity() {
    private var clickMeCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_first)
        setTypeface()
        //setSupportActionBar(toolbar)
        //设置状态栏颜色
        StatusBarUtils.setColor(
            this,
            resources.getColor(com.google.android.material.R.color.cardview_light_background)
        )
        //设置状态栏透明 文字会无法显示
        //StatusBarUtils.setTransparent(this)
        buttonBluetooth.setOnClickListener {
            startActivity(Intent(this, BluetoothPagerActivity::class.java))
        }
        buttonInternet.setOnClickListener {
            startActivity(Intent(this, MqttPagerActivity::class.java))
        }
        //点击数字5彩蛋
        imageButtonTitle.setOnClickListener {
            clickTitleEffect()
        }
        buttonAboutMe.setOnClickListener {
            aboutMe()
        }

        //启动服务
//        startService(Intent(this,AlarmService::class.java)
        /**
         * binService间接调用的service不是前台service
         * 所以需要先启动service再bind
         */
    }
    //点击彩蛋
    private fun clickTitleEffect() {
        clickMeCount++
        if (clickMeCount <= 10) {
            "您点击了我${clickMeCount}次!".showToast()
            /**
             * 更新通知显示 只需要调用mq2Binder.updateData方法
             */
            //mQ2Binder.updateData(0.88f,0.99f)
        }
        when {
            clickMeCount in 11..14 -> {
                "亲！您点击了我很多次了!".showToast()
            }
            clickMeCount in 15..17 -> {
                "不要再点了！！！".showToast()
            }
            clickMeCount in 18..20 -> {
                "住手！忍不住了！！！".showToast()
            }
            clickMeCount in 21..22 -> {
                "啊啊啊啊~~~~！！！".showToast()
            }
            clickMeCount >= 23 -> {
                "被点坏了~~/(ㄒoㄒ)/~~".showToast()
            }
        }
    }

    //关于我的信息
    private fun aboutMe() {
        AlertDialog.Builder(this).apply {
            setTitle("关于我")
            setMessage("简单的蓝牙上位机\n悄悄告诉你: 作者懂个锤子的蓝牙")
            setCancelable(false)
            setPositiveButton("知道了"){
                    _, _ ->
            }
            setNegativeButton("不知道"){
                _,_ ->
            }
            show()
        }
    }

    //设置字体 阿里妈妈东方大楷
    private fun setTypeface(){
        titleName.typeface = FontStyle.setTypeface(this.assets,FontStyle.AlimamaFont)
    }

}