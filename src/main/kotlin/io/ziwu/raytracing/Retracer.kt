package io.ziwu.raytracing

import proguard.obfuscate.MappingReader
import proguard.retrace.FrameInfo
import proguard.retrace.FrameRemapper
import java.io.File

object Retracer {
    fun feed(mapping: File): (FrameInfo) -> List<FrameInfo> {
        val mapper = FrameRemapper()
        MappingReader(mapping).pump(mapper)
        return { info ->
            mapper.transform(info) ?: listOf()
        }
    }
}