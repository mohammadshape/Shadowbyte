@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")

package io.kayt.shadowbyte_asm

import com.android.build.api.instrumentation.AsmClassVisitorFactory
import com.android.build.api.instrumentation.ClassContext
import com.android.build.api.instrumentation.ClassData
import com.android.build.api.instrumentation.FramesComputationMode
import com.android.build.api.instrumentation.InstrumentationParameters
import com.android.build.api.instrumentation.InstrumentationScope
import com.android.build.api.variant.AndroidComponentsExtension
import jdk.internal.org.objectweb.asm.Opcodes
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.provideDelegate
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.FieldVisitor
import java.io.File
import java.util.Locale

private lateinit var shadowPath: File

class ShadowbyteAbstractTreeModifier : Plugin<Project> {
    override fun apply(project: Project) {
        shadowPath = File(
            project.layout.buildDirectory.dir("intermediates/shadowed").get().asFile.path,
            "Shadowfile"
        )
        project.extensions.getByType(AndroidComponentsExtension::class.java)
            .onVariants { variant ->
                if (shadowPath.exists()) {
                    variant.instrumentation.transformClassesWith(
                        CustomAsmClassVisitorFactory::class.java,
                        InstrumentationScope.ALL
                    ) {}
                    variant.instrumentation.setAsmFramesComputationMode(FramesComputationMode.COMPUTE_FRAMES_FOR_INSTRUMENTED_CLASSES)
                }
            }
    }
}

class Shadowfile private constructor(val properties: List<Properties>) {

    data class Properties(val className: String, val propertyName: String, val type: String)
    companion object {
        fun load(file: File): Shadowfile {
            val properties = mutableListOf<Properties>()
            file.bufferedReader().forEachLine { line ->
                val (className, _, property, type) = line.split("->").let { it + it[1].split(":") }
                properties.add(Properties(className, property, type))
            }
            return Shadowfile(properties)
        }
    }
}

class CustomClassVisitor(api: Int, classVisitor: ClassVisitor, private val shadowfile: Shadowfile) :
    ClassVisitor(api, classVisitor) {

    private lateinit var currentProperty: Shadowfile.Properties

    override fun visit(
        version: Int,
        access: Int,
        name: String,
        signature: String?,
        superName: String?,
        interfaces: Array<out String>?
    ) {
        val classes = shadowfile.properties
        val indexOfClassInShadowFile = classes.indexOfFirst { it.className == name }
        if (indexOfClassInShadowFile >= 0) {
            currentProperty = shadowfile.properties[indexOfClassInShadowFile]
        } else {
            throw Exception("Class ${name} not found in Shadowfile, ${classes}")
        }
        super.visit(version, access, name, signature, superName, interfaces)
    }

    override fun visitField(
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        value: Any?
    ): FieldVisitor? {
        return if (name == currentProperty.propertyName && access and Opcodes.ACC_FINAL != 0) {
            // Remove the final modifier
            super.visitField(access and Opcodes.ACC_FINAL.inv(), name, descriptor, signature, value)
        } else {
            super.visitField(access, name, descriptor, signature, value)
        }
    }

    override fun visitEnd() {
        // Add setter method
        val setter = cv.visitMethod(
            Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC,
            "access\$set${
                currentProperty.propertyName.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(
                        Locale.getDefault()
                    ) else it.toString()
                }
            }\$shadow",
            "(${currentProperty.type})V",
            null,
            null
        )
        setter.visitCode()
        setter.visitVarInsn(getLoadOpcode(currentProperty.type), 0) // Load `this`
        setter.visitFieldInsn(
            Opcodes.PUTSTATIC,
            currentProperty.className,
            currentProperty.propertyName,
            currentProperty.type
        ) // Set the field
        setter.visitInsn(Opcodes.RETURN)
        setter.visitMaxs(2, 2)
        setter.visitEnd()

        // Add getter method
        val getter = cv.visitMethod(
            Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC,
            "access\$get${
                currentProperty.propertyName.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(
                        Locale.getDefault()
                    ) else it.toString()
                }
            }\$shadow",
            "()${currentProperty.type}",
            null,
            null
        )
        getter.visitCode()
        getter.visitFieldInsn(
            Opcodes.GETSTATIC,
            currentProperty.className,
            currentProperty.propertyName,
            currentProperty.type
        ) // Get the field
        getter.visitInsn(getReturnOpcode(currentProperty.type))
        getter.visitMaxs(1, 1)
        getter.visitEnd()

        super.visitEnd()
    }

    private fun getLoadOpcode(fieldType: String): Int {
        return when (fieldType) {
            "I", "Z", "B", "C", "S" -> Opcodes.ILOAD
            "J" -> Opcodes.LLOAD
            "F" -> Opcodes.FLOAD
            "D" -> Opcodes.DLOAD
            else -> Opcodes.ALOAD
        }
    }

    private fun getReturnOpcode(fieldType: String): Int {
        return when (fieldType) {
            "I", "Z", "B", "C", "S" -> Opcodes.IRETURN
            "J" -> Opcodes.LRETURN
            "F" -> Opcodes.FRETURN
            "D" -> Opcodes.DRETURN
            else -> Opcodes.ARETURN
        }
    }
}

abstract class CustomAsmClassVisitorFactory :
    AsmClassVisitorFactory<InstrumentationParameters.None> {

    companion object {
        val shadowfile: Shadowfile by lazy { Shadowfile.load(shadowPath) }
    }

    override fun createClassVisitor(
        classContext: ClassContext,
        nextClassVisitor: ClassVisitor
    ): ClassVisitor {

        return CustomClassVisitor(Opcodes.ASM9, nextClassVisitor, shadowfile)
    }

    override fun isInstrumentable(classData: ClassData): Boolean {
        return shadowfile.properties.map { it.className.replace("/",".") }.contains(classData.className)
    }
}