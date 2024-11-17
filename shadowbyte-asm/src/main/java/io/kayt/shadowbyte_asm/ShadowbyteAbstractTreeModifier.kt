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
import org.objectweb.asm.MethodVisitor

private const val INSTRUMENT_CLASS = "androidx.compose.material3.TabRowKt"
private const val INSTRUMENT_FIELD = "ScrollableTabRowMinimumTabWidth"

class ShadowbyteAbstractTreeModifier : Plugin<Project> {
    override fun apply(project: Project) {
        project.extensions.getByType(AndroidComponentsExtension::class.java)
            .onVariants { variant ->
                variant.instrumentation.transformClassesWith(
                    CustomAsmClassVisitorFactory::class.java,
                    InstrumentationScope.ALL
                ) {}
                variant.instrumentation.setAsmFramesComputationMode(FramesComputationMode.COMPUTE_FRAMES_FOR_INSTRUMENTED_METHODS)
            }
    }
}

class CustomClassVisitor(api: Int, classVisitor: ClassVisitor) : ClassVisitor(api, classVisitor) {

    override fun visitMethod(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        val mv = super.visitMethod(access, name, descriptor, signature, exceptions)
        if (name == "<clinit>") {
            return object : MethodVisitor(api, mv) {
                override fun visitFieldInsn(
                    opcode: Int,
                    owner: String?,
                    name: String?,
                    descriptor: String?
                ) {
                    if (opcode == Opcodes.PUTSTATIC && owner == INSTRUMENT_CLASS.replace(".", "/")
                        && name == INSTRUMENT_FIELD
                        && descriptor == "F"
                    ) {
                        mv.visitLdcInsn(0f)
                        mv.visitFieldInsn(
                            Opcodes.PUTSTATIC,
                            owner,
                            name,
                            descriptor
                        )
                    } else {
                        super.visitFieldInsn(opcode, owner, name, descriptor)
                    }
                }
            }
        }
        return mv
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