package net.bi4vmr.tool.kotlin.external.adb.proxy.serializer

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import net.bi4vmr.tool.kotlin.external.adb.common.model.DisplayInfo

/**
 * 序列化配置：[DisplayInfo] 。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
object DisplayInfoSerializer : KSerializer<DisplayInfo> {

    override val descriptor = buildClassSerialDescriptor("DisplayInfo") {
        element<String>("id")
        element<String>("uniqueID")
        element<Int>("width")
        element<Int>("height")
        element<String>("sn")
    }

    override fun serialize(encoder: Encoder, value: DisplayInfo) {
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, value.id)
            encodeStringElement(descriptor, 1, value.uniqueID)
            encodeIntElement(descriptor, 2, value.width)
            encodeIntElement(descriptor, 3, value.height)
            encodeStringElement(descriptor, 4, value.sn)
        }
    }

    override fun deserialize(decoder: Decoder): DisplayInfo =
        decoder.decodeStructure(descriptor) {
            var id = "0"
            var uniqueID = ""
            var width = 0
            var height = 0
            var sn = ""

            while (true) {
                when (val i = decodeElementIndex(descriptor)) {
                    0 -> id = decodeStringElement(descriptor, 0)
                    1 -> uniqueID = decodeStringElement(descriptor, 1)
                    2 -> width = decodeIntElement(descriptor, 2)
                    3 -> height = decodeIntElement(descriptor, 3)
                    4 -> sn = decodeStringElement(descriptor, 4)
                    CompositeDecoder.DECODE_DONE -> break
                    else -> error("Unexpected index $i")
                }
            }

            DisplayInfo(
                id = id,
                uniqueID = uniqueID,
                width = width,
                height = height,
                sn = sn
            )
        }
}
