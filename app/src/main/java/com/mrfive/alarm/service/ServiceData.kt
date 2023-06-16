package com.mrfive.alarm.service

/**
 * Service公共类
 */
object ServiceData {
    //mq2Binder对象 activity与service通信的对象
    var mQ2Binder: AlarmService.MQ2Binder? = null
}
