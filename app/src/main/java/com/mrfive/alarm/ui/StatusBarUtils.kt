package com.mrfive.alarm.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.annotation.ColorInt
import androidx.core.graphics.ColorUtils


/**
 * 沉浸式状态栏
 */
object StatusBarUtils {
    private val TAG ="StatusBar"
    @SuppressLint("InternalInsetResource", "DiscouragedApi")
    //获取状态栏高度
    fun getHeight(context: Context): Int {
        var statusBarHeight = 0
        try {
            val resourceId: Int = context.resources.getIdentifier(
                "status_bar_height", "dimen",
                "android"
            )
            if (resourceId > 0) {
                statusBarHeight = context.getResources().getDimensionPixelSize(resourceId)
            }
        } catch (e: Exception) {
           Log.e(TAG,"获取statusBarHeight失败",e)
        }
        return statusBarHeight
    }
    //设置状态栏颜色
    fun setColor(window: Window, @ColorInt color: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
            window.decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
            window.statusBarColor = color
            setTextDark(window, !isDarkColor(color));
        }
    }
    fun setColor(context: Context?, @ColorInt color: Int) {
        if (context is Activity) {
            setColor(context.window, color)
        }
    }

    //设置状态栏文字颜色
    fun setTextDark(window: Window, isDark: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val decorView = window.decorView
            val systemUiVisibility = decorView.systemUiVisibility
            if (isDark) {
                decorView.systemUiVisibility =
                    systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            } else {
                decorView.systemUiVisibility =
                    systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
            }
        }
    }
    fun setTextDark(context: Context?, isDark: Boolean) {
        if (context is Activity) {
            setTextDark(context.window, isDark)
        }
    }
    //判断颜色模式
    fun isDarkColor(@ColorInt color: Int): Boolean {
        return ColorUtils.calculateLuminance(color) < 0.5
    }

    //设置透明
    fun setTransparent(window: Window) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            window.statusBarColor = Color.TRANSPARENT
        }
    }
    fun setTransparent(context: Context?) {
        if (context is Activity) {
            setTransparent(context.window)
        }
    }
}