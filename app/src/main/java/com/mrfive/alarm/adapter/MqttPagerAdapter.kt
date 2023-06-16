package com.mrfive.alarm.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter

class MqttPagerAdapter(fm: FragmentManager, private val fragmentList: List<Fragment>):FragmentPagerAdapter(fm) {
    override fun getCount(): Int {
        return fragmentList.size
    }

    override fun getItem(position: Int): Fragment {
        return fragmentList[position]
    }
}