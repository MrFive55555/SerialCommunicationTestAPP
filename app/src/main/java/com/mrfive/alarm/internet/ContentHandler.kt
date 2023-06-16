package com.mrfive.alarm.internet

import android.util.Log
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler

/**
 * SAX解析XML方式
 */
class ContentHandler : DefaultHandler() {
    private val TAG = "InternetTag"
    private var nodeName = ""
    private lateinit var id: StringBuilder
    private lateinit var name: StringBuilder
    private lateinit var version: StringBuilder

    override fun startDocument() {
        id = StringBuilder()
        name = StringBuilder()
        version = StringBuilder()
    }

    override fun startElement(
        uri: String,
        localName: String,
        qName: String,
        attributes: Attributes
    ) {
        //记录当前节点
        nodeName = localName
        Log.d(TAG, "uri is $uri")
        Log.d(TAG, "localName is $localName")
        Log.d(TAG, "qName is $qName")
        Log.d(TAG, "attributes is $attributes")
        Log.d(TAG,"-----------------------------------------------------------------")
    }

    override fun characters(ch: CharArray?, start: Int, length: Int) {
        // 根据当前节点名判断将内容添加到哪一个StringBuilder对象中
        when (nodeName) {
            "id" -> id.append(ch, start, length)
            "name" -> name.append(ch, start, length)
            "version" -> version.append(ch, start, length)
        }
    }

    override fun endElement(uri: String?, localName: String?, qName: String?) {
        if ("app" == localName) {
            //trim()去掉空字符
            Log.d(TAG, "id is ${id.toString().trim()}")
            Log.d(TAG, "name is ${name.toString().trim()}")
            Log.d(TAG, "version is ${version.toString().trim()}")
            Log.d(TAG,"-----------------------------------------------------------------")
            //最后将StringBuilder清空
            id.setLength(0)
            name.setLength(0)
            version.setLength(0)
        }
    }

    override fun endDocument() {

    }
}