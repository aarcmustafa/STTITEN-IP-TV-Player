package com.sttiten.iptv.data.tv.http

interface HttpServer {
    fun start(port: Int)
    fun stop()
}
