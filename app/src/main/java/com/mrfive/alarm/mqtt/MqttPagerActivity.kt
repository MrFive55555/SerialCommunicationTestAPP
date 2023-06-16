package com.mrfive.alarm.mqtt

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.mrfive.alarm.R
import com.mrfive.alarm.adapter.MqttPagerAdapter
import com.mrfive.alarm.ui.InitUI
import com.mrfive.alarm.ui.StatusBarUtils
import kotlinx.android.synthetic.main.activity_bluetooth_pager.*
import kotlinx.android.synthetic.main.activity_mqtt_pager.*
import kotlinx.android.synthetic.main.fragment_bluetooth_message.*
import kotlinx.android.synthetic.main.fragment_mqtt_message.*

class MqttPagerActivity : AppCompatActivity(),InitUI {
    private val fragmentList = ArrayList<Fragment>()

    //Navigation监听事件
    inner class NavigationListener : BottomNavigationView.OnNavigationItemSelectedListener {
        //点击item改变page页面
        override fun onNavigationItemSelected(item: MenuItem): Boolean {
            when (item.itemId) {
                R.id.navItemHome -> {
                    viewPagerMqtt.currentItem = 0

                }
                R.id.navItemMessage -> {
                    viewPagerMqtt.currentItem = 1
                }
            }
            return true
        }
    }

    //ViewPager监听事件
    inner class ViewPagerListener : ViewPager.OnPageChangeListener {
        override fun onPageScrolled(
            position: Int,
            positionOffset: Float,
            positionOffsetPixels: Int
        ) {

        }

        //改变item样式
        @SuppressLint("ResourceAsColor")
        override fun onPageSelected(position: Int) {
            when (position) {
                0 -> {
                    bottomNaviViewMqtt.selectedItemId = R.id.navItemHome
                }
                1 -> {
                    bottomNaviViewMqtt.selectedItemId = R.id.navItemMessage
                }
            }
        }

        override fun onPageScrollStateChanged(state: Int) {

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mqtt_pager)
        initData()
        initView()
        Log.d(MQTT.TAG,"${localClassName}onCreate")
    }

    override fun onRestart() {
        super.onRestart()
        Log.d(MQTT.TAG,"${localClassName}onRestart")
    }

    override fun onStart() {
        super.onStart()
        Log.d(MQTT.TAG,"${localClassName}onStart")
    }

    override fun onResume() {
        super.onResume()
        Log.d(MQTT.TAG,"${localClassName}onResume")
    }

    override fun onPause() {
        super.onPause()
        Log.d(MQTT.TAG,"${localClassName}onPause")
    }

    override fun onStop() {
        super.onStop()
        Log.d(MQTT.TAG,"${localClassName}onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(MQTT.TAG,"${localClassName}onDestroy")
    }

    //初始化view
    override fun initView() {
        //设置字体
        //设置状态栏颜色 (看似沉浸状态栏)
        StatusBarUtils.setColor(
            this,
            resources.getColor(com.google.android.material.R.color.cardview_light_background)
        )
        //初始化Navigation和PagerAdapter
        val adapter = MqttPagerAdapter(supportFragmentManager, fragmentList)
        viewPagerMqtt.adapter = adapter
        //取消item默认样式
        bottomNaviViewMqtt.itemIconTintList = null
        //监听事件
        viewPagerMqtt.addOnPageChangeListener(ViewPagerListener())
        bottomNaviViewMqtt.setOnNavigationItemSelectedListener(NavigationListener())
    }

    //初始化集合
    override fun initData() {
        val fragmentHome = MqttHomeFragment()
        fragmentList.add(fragmentHome)
        val fragmentMessage = MqttMessageFragment()
        fragmentList.add(fragmentMessage)
    }

    override fun initListener() {
        TODO("Not yet implemented")
    }
}