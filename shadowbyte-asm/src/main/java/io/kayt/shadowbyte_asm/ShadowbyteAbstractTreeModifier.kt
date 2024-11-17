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
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.MethodVisitor
import java.util.Locale

private const val INSTRUMENT_CLASS = "androidx.compose.material3.TabRowKt"
private const val INSTRUMENT_FIELD = "ScrollableTabRowMinimumTabWidth"
private const val INSTRUMENT_FIELD_TYPE = "F"

class ShadowbyteAbstractTreeModifier : Plugin<Project> {
    override fun apply(project: Project) {
        project.extensions.getByType(AndroidComponentsExtension::class.java)
            .onVariants { variant ->
                variant.instrumentation.transformClassesWith(
                    CustomAsmClassVisitorFactory::class.java,
                    InstrumentationScope.ALL
                ) {}
                variant.instrumentation.setAsmFramesComputationMode(FramesComputationMode.COMPUTE_FRAMES_FOR_INSTRUMENTED_CLASSES)
            }
    }
}

class CustomClassVisitor(api: Int, classVisitor: ClassVisitor) : ClassVisitor(api, classVisitor) {

    private lateinit var className: String

    override fun visit(version: Int, access: Int, name: String, signature: String?, superName: String?, interfaces: Array<out String>?) {
        className = name
        super.visit(version, access, name, signature, superName, interfaces)
    }

    override fun visitField(access: Int, name: String, descriptor: String, signature: String?, value: Any?): FieldVisitor? {
        return if (name == INSTRUMENT_FIELD && access and Opcodes.ACC_FINAL != 0) {
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
            "access\$set${INSTRUMENT_FIELD.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(
                    Locale.getDefault()
                ) else it.toString()
            }}\$shadow",
            "($INSTRUMENT_FIELD_TYPE)V",
            null,
            null
        )
        setter.visitCode()
        setter.visitVarInsn(getLoadOpcode(INSTRUMENT_FIELD_TYPE), 0) // Load `this`
        setter.visitFieldInsn(Opcodes.PUTSTATIC, className, INSTRUMENT_FIELD, INSTRUMENT_FIELD_TYPE) // Set the field
        setter.visitInsn(Opcodes.RETURN)
        setter.visitMaxs(2, 2)
        setter.visitEnd()

        // Add getter method
        val getter = cv.visitMethod(
            Opcodes.ACC_PUBLIC  or Opcodes.ACC_STATIC,
            "access\$get${INSTRUMENT_FIELD.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(
                    Locale.getDefault()
                ) else it.toString()
            }}\$shadow",
            "()$INSTRUMENT_FIELD_TYPE",
            null,
            null
        )
        getter.visitCode()
        getter.visitFieldInsn(Opcodes.GETSTATIC, className, INSTRUMENT_FIELD, INSTRUMENT_FIELD_TYPE) // Get the field
        getter.visitInsn(getReturnOpcode(INSTRUMENT_FIELD_TYPE))
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

    override fun createClassVisitor(
        classContext: ClassContext,
        nextClassVisitor: ClassVisitor
    ): ClassVisitor {
        return CustomClassVisitor(Opcodes.ASM9, nextClassVisitor)
    }

    override fun isInstrumentable(classData: ClassData): Boolean {
        return classData.className == INSTRUMENT_CLASS
    }
}