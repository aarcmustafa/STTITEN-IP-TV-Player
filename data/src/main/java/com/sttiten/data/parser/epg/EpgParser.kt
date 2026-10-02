package com.sttiten.iptv.data.parser.epg

import kotlinx.coroutines.flow.Flow
import java.io.InputStream

interface EpgParser {
    fun readProgrammes(
        input: InputStream
    ): Flow<EpgProgramme>
}