package com.mrfive.alarm.tool

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context

/**
 * 工具类 随时获取全局context
 */
class MyApplication:Application() {

    companion object{
        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context
    }
    //activity创建的时候保存context
    override fun onCreate() {
        super.onCreate()
        context = applicationContext
    }
}