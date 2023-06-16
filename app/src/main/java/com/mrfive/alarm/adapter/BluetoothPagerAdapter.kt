package com.mrfive.alarm.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter

/**
 * 底部导航栏 使用fragmentPager适配器实现切换
 */
class BluetoothPagerAdapter(fm: FragmentManager,private val fragmentList: List<Fragment>): FragmentPagerAdapter(fm) {
    override fun getCount(): Int {
        return  fragmentList.size
    }

    override fun getItem(position: Int): Fragment {
        return fragmentList[position]
    }
}