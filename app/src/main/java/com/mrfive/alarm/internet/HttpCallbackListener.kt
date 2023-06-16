package com.mrfive.alarm.internet

/**
 * 回调机制
 * 子线程中结束时 服务器来不及响应 所以需要回调数据
 */
interface HttpCallbackListener {
    fun onFinish(response:String)
    fun onError(e:Exception)
}