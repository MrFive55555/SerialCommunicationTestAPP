package com.mrfive.alarm.tool

import android.content.res.AssetManager
import android.graphics.Typeface

/**
 * 字体 工具类
 */
object FontStyle {
    const val AlimamaFont = "fonts/Alimamadfdk"
    fun setTypeface(mgr: AssetManager, fontPath: String): Typeface {
        //获取字体
        return Typeface.createFromAsset(mgr, fontPath)
    }
}
