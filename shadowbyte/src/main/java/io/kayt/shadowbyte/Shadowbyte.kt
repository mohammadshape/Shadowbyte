package io.kayt.shadowbyte

class Shadowbyte {

    @Target(
        AnnotationTarget.FUNCTION,
        AnnotationTarget.PROPERTY
    )
    @Retention(AnnotationRetention.SOURCE)
    @MustBeDocumented
    annotation class Property(val source: String)


    interface Shadowed {
        fun default(): Nothing =
            throw IllegalStateException("Make sure Shadowbyte-asm plugin is applied")
    }
}