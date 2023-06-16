package com.mrfive.alarm.internet

import retrofit2.Call
import retrofit2.http.GET

/**
 * 使用retrofit开源库更方便使用网络
 */
interface RetrofitTestService {
    //对接口归类
    @GET("get_data.json")
    fun getData(): Call<List<GSONTestData>>
}