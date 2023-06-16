package com.mrfive.alarm.bluetooth

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.mrfive.alarm.R
import com.mrfive.alarm.adapter.BluetoothPagerAdapter
import com.mrfive.alarm.bluetooth.Bluetooth.releaseBluetoothResource
import com.mrfive.alarm.ui.StatusBarUtils
import kotlinx.android.synthetic.main.activity_bluetooth.*
import kotlinx.android.synthetic.main.activity_bluetooth_pager.*
import kotlinx.android.synthetic.main.fragment_bluetooth_home.*

class BluetoothPagerActivity : AppCompatActivity() {
    private val fragmentList = ArrayList<Fragment>()

    //Navigation监听事件
    inner class NavigationListener : BottomNavigationView.OnNavigationItemSelectedListener {
        //点击item改变page页面
        override fun onNavigationItemSelected(item: MenuItem): Boolean {
            when (item.itemId) {
                R.id.navItemHome -> {
                    viewPagerBT.currentItem = 0

                }
                R.id.navItemMessage -> {
                    viewPagerBT.currentItem = 1
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
                    bottomNaviViewBT.selectedItemId = R.id.navItemHome
                }
                1 -> {
                    bottomNaviViewBT.selectedItemId = R.id.navItemMessage
                }
            }
        }

        override fun onPageScrollStateChanged(state: Int) {

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bluetooth_pager)
        initData()
        initView()
    }

    override fun onDestroy() {
        super.onDestroy()
        //释放资源
        releaseBluetoothResource()
    }

    //初始化view
    private fun initView() {
        //设置状态栏颜色 (看似沉浸状态栏)
        StatusBarUtils.setColor(
            this,
            resources.getColor(com.google.android.material.R.color.cardview_light_background)
        )
        //初始化Navigation和PagerAdapter
        val adapter = BluetoothPagerAdapter(supportFragmentManager, fragmentList)
        viewPagerBT.adapter = adapter
        //取消item默认样式
        bottomNaviViewBT.itemIconTintList = null
        //监听事件
        viewPagerBT.addOnPageChangeListener(ViewPagerListener())
        bottomNaviViewBT.setOnNavigationItemSelectedListener(NavigationListener())
    }

    //初始化集合
    private fun initData() {
        val fragmentHome = BluetoothHomeFragment()
        fragmentList.add(fragmentHome)
        val fragmentMessage = BluetoothMessageFragment()
        fragmentList.add(fragmentMessage)
    }
}