package com.mrfive.alarm.tool

import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.*

//单例类 仿静态类
object Tool {
    private var toast: Toast? = null
    private var toastFirstTime: Long? = null
    private var toastSecondTime: Long? = null
    private var toastOldString: String? = null
    private var toastOldInt: Int? = null

    //获得格式化时间
    fun formatTime(): String {
        val calendarInstance = Calendar.getInstance()
        val formatDate = SimpleDateFormat("HH:mm:ss", Locale.CHINESE)
        return formatDate.format(calendarInstance.timeInMillis) + " "
    }

    /**
     * Toast的改进
     * 使用方法
     * “show me”.showToast(xxx)
     * 123.showToast(xxx)
     * 发现调用show()就重新计时了
     */
    fun String.showToast(duration: Int = Toast.LENGTH_SHORT) { //默认短时间
        if (toast == null) {
            toast = Toast.makeText(MyApplication.context, this, duration) //this代表String
            toast?.show()
            toastFirstTime = System.currentTimeMillis()
        } else {
            //第二次点击
            toastSecondTime = System.currentTimeMillis()
            //如果新文本和旧文本一样
            if (toastOldString == this) {
                //间隔超一秒就重新显示
                if (toastSecondTime!! - toastFirstTime!! >= Toast.LENGTH_SHORT) {
                    //连续点击重复字符串 会重新计时 间隔不超过限定时间 就不会显示
                    toast?.show()
                }
            } else {
                //更新文本
                toastOldString = this
                toast?.setText(this)
                toast?.show()
            }
        }
        //时间递进
        toastFirstTime = toastSecondTime
    }

    fun Int.showToast(duration: Int = Toast.LENGTH_SHORT) { //默认短时间
        if (toast == null) {
            toast = Toast.makeText(MyApplication.context, this, duration) //this代表String
            toast?.show()
            toastFirstTime = System.currentTimeMillis()
        } else {
            //第二次点击
            toastSecondTime = System.currentTimeMillis()
            //如果新文本和旧文本一样
            if (toastOldInt == this) {
                //间隔超一秒就重新显示
                if (toastSecondTime!! - toastFirstTime!! >= Toast.LENGTH_SHORT) {
                    //连续点击重复字符串 会重新计时 间隔不超过限定时间 就不会显示
                    toast?.show()
                }
            } else {
                //更新文本
                toastOldInt = this
                toast?.setText(this)
                toast?.show()
            }
        }
        //时间递进
        toastFirstTime = toastSecondTime
    }
}
