package io.kayt.shadowbyte

import io.kayt.shadowbyte.Shadowbyte.Shadowed
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.Locale
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.jvmErasure


inline fun <reified T : Any> shadow(block: T.() -> Unit) {
    val clazz = T::class
    val shadowedAnnotation = clazz.findAnnotation<Shadowed>()
    val sourceClass = shadowedAnnotation?.qualifiedName.takeIf { it != "NOT_SPECIFIED_SOURCE" }
        ?: shadowedAnnotation?.source.takeIf { it != Unit::class }?.qualifiedName
        ?: error("One of 'qualifiedName' or 'source' must be provided in @Shadowbyte.Shadowed.")

    val properties = clazz.memberProperties

    (Proxy.newProxyInstance(
        Shadowed::class.java.classLoader,
        arrayOf(T::class.java)
    ) { proxy: Any, method: Method, args: Array<out Any>? ->
        val methodName = method.name
        when {
            methodName.startsWith("get") && args == null -> {
                val prop = properties.first { methodName.contains(it.name) }
                val propertyAnnotation = prop.findAnnotation<Shadowbyte.Property>()
                val sourceProperty = propertyAnnotation!!.name
                return@newProxyInstance getShadow(
                    sourceClass,
                    sourceProperty,
                    prop.returnType.jvmErasure.javaPrimitiveType ?: prop.returnType.jvmErasure.java
                )
            }

            methodName.startsWith("set") && args != null && args.size == 1 -> {
                val prop = properties.first { methodName.contains(it.name) }
                val propertyAnnotation = prop.findAnnotation<Shadowbyte.Property>()
                val sourceProperty = propertyAnnotation!!.name
                return@newProxyInstance setShadow(
                    sourceClass,
                    sourceProperty,
                    prop.returnType.jvmErasure.javaPrimitiveType ?: prop.returnType.jvmErasure.java,
                    args.first()
                )
            }

            else -> throw UnsupportedOperationException("Unknown method: $methodName")
        }
    } as T).block()
}

fun getShadow(clazz: String, property: String, propertyType: Class<*>): Any? {
    return accessShadow(Accessor.GET, clazz, property, propertyType, Unit)
}

fun setShadow(clazz: String, property: String, propertyType: Class<*>, value: Any?) {
    accessShadow(Accessor.SET, clazz, property, propertyType, value)
}

private enum class Accessor {
    GET, SET;

    override fun toString(): String {
        return when (this) {
            GET -> "get"
            SET -> "set"
        }
    }
}

private fun accessShadow(
    accessor: Accessor,
    className: String,
    property: String,
    propertyType: Class<*>,
    value: Any?
): Any? {
    val clazz = Class.forName(className)
    val method = clazz.getDeclaredMethod(
        "access\$${accessor}${
            property.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(
                    Locale.getDefault()
                ) else it.toString()
            }
        }\$shadow",
        propertyType
    )
    return if (accessor == Accessor.GET) {
        method.invoke(null)
    } else {
        method.invoke(null, value)
    }
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