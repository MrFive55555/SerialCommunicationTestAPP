package com.mrfive.alarm.internet

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.xml.sax.InputSource
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import javax.xml.parsers.SAXParserFactory

/**
 * 单例网络类 因为整个应用可能随时调用网络
 * 使用OKhttp
 */
object HttpUtil {
    private val TAG = "InternetTag"
    fun sendOKHttpRequest(address: String, callback: Callback) {
        val client = OkHttpClient()
        val request = Request.Builder().url(address).build()
        //使用okhttp自带的回调方法  内部开启子线程
        client.newCall(request).enqueue(callback)
    }

    //PULL解析XML
    fun parseXMLWithPull(xmlData: String) {
        try {
            val factory = XmlPullParserFactory.newInstance()
            val xmlPullParser = factory.newPullParser()
            xmlPullParser.setInput(StringReader(xmlData))
            var eventType = xmlPullParser.eventType
            var id = ""
            var name = ""
            var version = ""
            while (eventType != XmlPullParser.END_DOCUMENT) {
                val nodeName = xmlPullParser.name
                when (eventType) {
                    //开始解析某个节点
                    XmlPullParser.START_TAG -> {
                        when (nodeName) {
                            "id" -> id = xmlPullParser.nextText()
                            "name" -> name = xmlPullParser.nextText()
                            "version" -> version = xmlPullParser.nextText()
                        }
                    }
                    //完成解析某个节点
                    XmlPullParser.END_TAG -> {
                        if ("app" == nodeName) {
                            Log.d(TAG, "id is $id")
                            Log.d(TAG, "name is $name")
                            Log.d(TAG, "version is $version")
                        }
                    }
                }
                eventType = xmlPullParser.next()
            }
        } catch (e: Exception) {
            Log.e(TAG, "解析失败", e)
        }
    }

    //SAX解析XML
    fun parseXMLWithSAX(xmlData: String) {
        try {
            val factory = SAXParserFactory.newInstance()
            val xmlReader = factory.newSAXParser().xmlReader
            val handler = ContentHandler()
            //将ContentHandler的实例设置到XMLReader中
            xmlReader.contentHandler = handler
            //开始执行解析
            xmlReader.parse(InputSource(StringReader(xmlData)))
        } catch (e: Exception) {
            Log.e(TAG, "解析失败", e)
        }
    }

    //JSONObject解析json
    fun parseJSONWithJSONObject(jsonData: String) {
        try {
            val jsonArray = JSONArray(jsonData)
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val id = jsonObject.getInt("id")
                val name = jsonObject.getString("name")
                val version = jsonObject.getString("version")
                Log.d(TAG, "id is ${id + 100}")
                Log.d(TAG, "name is $name")
                Log.d(TAG, "version is $version")
            }
        } catch (e: Exception) {
            Log.e(TAG, "解析失败", e)
        }
    }

    //GSON解析json
    fun parseJSONWithGSON(jsonData: String) {
        val gson = Gson()
        //json数组解析 将所有对象放入一个数组
        val typeOf = object : TypeToken<List<GSONTestData>>() {}.type
        val list = gson.fromJson<List<GSONTestData>>(jsonData, typeOf)
        for (test in list) {
            Log.d(TAG, "id is ${test.id + 200}")
            Log.d(TAG, "name is ${test.name}")
            Log.d(TAG, "version is ${test.version}")
        }
    }

}

