package no.nav.sokos.okosynk.util

import kotlin.div
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

import no.nav.sokos.okosynk.util.CopyBookParseUtil.parseDouble
import no.nav.sokos.okosynk.util.CopyBookParseUtil.toLocalDate

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class CopyBookField(
    val name: String = "",
    val startIndex: Int,
    val endIndex: Int,
    val type: CopyBookType,
)

enum class CopyBookType {
    INTEGER,
    STRING,
    DATE,
    DECIMAL,
    SPECIAL,
    GJERLDER_ID,
}

inline fun <reified T : Any> String.toDataClass(): T {
    val clazz = T::class
    val constructor = clazz.primaryConstructor!!
    val params =
        constructor.parameters.associateWith { param ->
            val property = clazz.memberProperties.find { it.name == param.name }!!
            val annotation = property.findAnnotation<CopyBookField>()!!
            val startIndex = annotation.startIndex
            val endIndex = annotation.endIndex.coerceAtMost(this.length)
            val valueString = this.substring(startIndex, endIndex).trim()

            when (annotation.type) {
                CopyBookType.GJERLDER_ID ->
                    valueString.takeIf { it.startsWith("00") }?.substring(2) ?: valueString
                CopyBookType.INTEGER -> valueString.toIntOrNull()
                CopyBookType.STRING -> valueString
                CopyBookType.DATE -> valueString.toLocalDate()
                CopyBookType.DECIMAL ->
                    valueString.toDoubleOrNull()?.let { value ->
                        value / 100.0
                    }

                CopyBookType.SPECIAL -> valueString.parseDouble()
            }
        }
    return constructor.callBy(params)
}
