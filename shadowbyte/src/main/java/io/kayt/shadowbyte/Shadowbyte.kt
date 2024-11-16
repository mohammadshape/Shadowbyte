package io.kayt.shadowbyte

import jdk.internal.org.objectweb.asm.ClassReader
import jdk.internal.org.objectweb.asm.ClassVisitor
import jdk.internal.org.objectweb.asm.ClassWriter
import jdk.internal.org.objectweb.asm.FieldVisitor
import jdk.internal.org.objectweb.asm.Opcodes
import java.io.File
import java.nio.file.Files

class Shadowbyte {

//    @Target(AnnotationTarget.CLASS)
//    @Retention(AnnotationRetention.SOURCE)
//    @MustBeDocumented
//    annotation class Shadowed

    @Target(
        AnnotationTarget.FUNCTION,
        AnnotationTarget.PROPERTY
    )
    @Retention(AnnotationRetention.SOURCE)
    @MustBeDocumented
    annotation class Property(val source: String)


    interface Shadowed {
        fun default() : Nothing = throw Exception("This exception should never happen in runtime")
    }
}

class ConstantFieldModifier {
    fun main () {
        val className = "com.example.TargetClass" // Replace with the target class
        val fieldName = "CONSTANT_FIELD" // Replace with the target field
        val newValue: Any = 42 // Replace with the new value

        // Path to the input class file
        val inputFile = File("path/to/TargetClass.class")
        val classBytes = Files.readAllBytes(inputFile.toPath())

        // Modify the class using ASM
        val classReader = ClassReader(classBytes)
        val classWriter = ClassWriter(ClassWriter.COMPUTE_FRAMES)

        classReader.accept(object : ClassVisitor(Opcodes.ASM9, classWriter) {
            override fun visitField(
                access: Int,
                name: String,
                descriptor: String?,
                signature: String?,
                value: Any?
            ): FieldVisitor? {
                if (name == fieldName) {
                    println("Modifying constant field: $name")
                    // Replace the value of the constant field
                    return super.visitField(access, name, descriptor, signature, newValue)
                }
                return super.visitField(access, name, descriptor, signature, value)
            }
        }, 0)

        // Write the modified class to a file
        val modifiedClass = classWriter.toByteArray()
        val outputFile = File("path/to/ModifiedTargetClass.class")
        outputFile.writeBytes(modifiedClass)

        println("Modified class written to: ${outputFile.absolutePath}")
    }
}