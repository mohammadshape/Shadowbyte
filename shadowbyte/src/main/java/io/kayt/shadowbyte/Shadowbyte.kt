package io.kayt.shadowbyte

import java.lang.reflect.Proxy
import kotlin.reflect.KClass

inline fun <reified T> shadow(block: T.() -> Unit) {
    val clazz = Class.forName("androidx.compose.material3.TabRowKt")
    val method = clazz.getDeclaredMethod(
        "access\$setScrollableTabRowMinimumTabWidth\$shadow",
        Float::class.java
    )
    method.invoke(null, 0f)
}

class Shadowbyte {

    @Target(
        AnnotationTarget.FUNCTION,
        AnnotationTarget.PROPERTY
    )
    @Retention(AnnotationRetention.RUNTIME)
    @MustBeDocumented
    @Suppress("unused", "PropertyName")
    annotation class Property(val name: String = "NOT_SPECIFIED_FIELD_NAME")

    @Target(
        AnnotationTarget.CLASS,
        AnnotationTarget.PROPERTY
    )
    @Retention(AnnotationRetention.RUNTIME)
    @MustBeDocumented
    @Suppress("unused")
    annotation class Shadowed(
        val qualifiedName: String = "NOT_SPECIFIED_SOURCE",
        val source: KClass<*> = Unit::class
    )
}