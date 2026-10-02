@file:Suppress("unused")
package com.sttiten.iptv.data.parser

import com.sttiten.iptv.data.parser.epg.EpgParser
import com.sttiten.iptv.data.parser.epg.EpgParserImpl
import com.sttiten.iptv.data.parser.m3u.M3UParser
import com.sttiten.iptv.data.parser.m3u.M3UParserImpl
import com.sttiten.iptv.data.parser.xtream.XtreamParser
import com.sttiten.iptv.data.parser.xtream.XtreamParserImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface ParserModule {
    @Binds
    fun bindM3UParser(parser: M3UParserImpl): M3UParser

    @Binds
    fun bindXtreamParser(parser: XtreamParserImpl): XtreamParser

    @Binds
    fun bindEpgParser(parser: EpgParserImpl): EpgParser
}
