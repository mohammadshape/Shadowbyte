package io.kayt.shadowbyte

class Shadowbyte {

    @Target(
        AnnotationTarget.FUNCTION,
        AnnotationTarget.PROPERTY
    )
    @Retention(AnnotationRetention.SOURCE)
    @MustBeDocumented
    @Suppress("unused")
    annotation class Property

    @Target(
        AnnotationTarget.CLASS,
        AnnotationTarget.PROPERTY
    )
    @Retention(AnnotationRetention.SOURCE)
    @MustBeDocumented
    @Suppress("unused")
    annotation class Shadowed(val source: String)


//    interface Shadowed {
//        fun default(): Nothing =
//            throw IllegalStateException("Make sure Shadowbyte-asm plugin is applied")
//    }
}