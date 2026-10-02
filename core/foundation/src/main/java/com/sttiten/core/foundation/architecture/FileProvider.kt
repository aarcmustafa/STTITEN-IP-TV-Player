package com.sttiten.iptv.core.foundation.architecture

import java.io.File

interface FileProvider {
    fun readAll(): List<File>
    fun read(path: String): File?
    fun write(value: Throwable)
}
