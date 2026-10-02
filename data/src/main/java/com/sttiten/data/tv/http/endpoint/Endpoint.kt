package com.sttiten.iptv.data.tv.http.endpoint

import io.ktor.server.routing.Route

sealed interface Endpoint {
    fun apply(route: Route)
}
