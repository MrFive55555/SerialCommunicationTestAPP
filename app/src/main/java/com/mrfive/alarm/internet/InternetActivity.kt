package com.mrfive.alarm.internet

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.mrfive.alarm.R
import kotlinx.android.synthetic.main.activity_internet.*
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.concurrent.thread

class InternetActivity : AppCompatActivity() {
    private val TAG = "InternetTag"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_internet)
        buttonSendRequest.setOnClickListener {
            val retrofit = Retrofit.Builder().baseUrl("http://120.79.198.205")
                .addConverterFactory(GsonConverterFactory.create()).build()
            val testService = retrofit.create(RetrofitTestService::class.java)
            testService.getData().enqueue(object : Callback<List<GSONTestData>> {
                override fun onResponse(
                    call: Call<List<GSONTestData>>,
                    response: Response<List<GSONTestData>>
                ) {
                    val list = response.body()
                    if(list!=null){
                        for (app in list){
                            Log.d(TAG, "id is ${app.id + 100}")
                            Log.d(TAG, "name is ${app.name}")
                            Log.d(TAG, "version is ${app.version}")
                        }
                    }
                }

                override fun onFailure(call: Call<List<GSONTestData>>, t: Throwable) {
                    Log.e(TAG,"解析失败",t)
                }
            })
        }
    }

    //发送请求
    private fun sendRequestWithOkhttp() {
        //线程发送请求
        thread {
            try {
                val client = OkHttpClient()
                val request = Request.Builder().url("http://192.168.0.104/get_data.json").build()
                val response = client.newCall(request).execute()
                val responseData = response.body?.string()
                if (responseData != null) {
                    showResponse(responseData)
                }
            } catch (e: Exception) {
                Log.e(TAG, "request failed", e)
            }
        }
    }

    //UI的更新不能在子线程中执行 使用异步消息处理机制
    private fun showResponse(responseData: String) {
        runOnUiThread {
            textViewResponseData.text = responseData
        }
    }

}