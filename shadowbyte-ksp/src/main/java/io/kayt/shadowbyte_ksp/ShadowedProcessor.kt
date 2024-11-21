package io.kayt.shadowbyte_ksp


import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeReference
import java.io.File

class ShadowedProcessor(
    private val environment: SymbolProcessorEnvironment
) : SymbolProcessor {
    private val logger = environment.logger
    private val codeGenerator = environment.codeGenerator

    private val primitiveTypeMap = mapOf(
        "kotlin.Int" to "I",
        "kotlin.Long" to "J",
        "kotlin.Float" to "F",
        "kotlin.Double" to "D",
        "kotlin.Short" to "S",
        "kotlin.Byte" to "B",
        "kotlin.Boolean" to "Z",
        "kotlin.Char" to "C",
        "kotlin.Unit" to "V" // For return type
    )

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation("io.kayt.shadowbyte.Shadowbyte.Shadowed")
        val invalidAnnotations = mutableListOf<KSAnnotated>()

        // Output builder
        val outputBuilder = StringBuilder()

        // Process annotated classes
        symbols.filterIsInstance<KSClassDeclaration>().forEach { classDeclaration ->
            val annotation = classDeclaration.annotations.find {
                it.shortName.asString() == "Shadowed" &&
                        it.annotationType.resolve().declaration.qualifiedName?.asString() == "io.kayt.shadowbyte.Shadowbyte.Shadowed"
            }

            if (annotation == null) {
                invalidAnnotations.add(classDeclaration)
                return@forEach
            }

            // Extract the "source" argument from the annotation
            val qualifiedNameArgument =
                annotation.arguments.find { it.name?.asString() == "qualifiedName" }
            val qualifiedName =
                (qualifiedNameArgument?.value as? String).takeIf { it != "NOT_SPECIFIED_SOURCE" }

            // Extract the KClass value
            val sourceArgument = annotation.arguments.find { it.name?.asString() == "source" }
            val kClassValue =
                (sourceArgument?.value as? KSType)?.takeIf { it.declaration.qualifiedName?.asString() != "kotlin.Unit" }
            val className = kClassValue?.declaration?.qualifiedName?.asString()

            val sourceName = (qualifiedName ?: className)?.replace(".", "/")
            if (sourceName == null) {
                logger.error(
                    "One of 'qualifiedName' or 'source' must be provided in @Shadowbyte.Shadowed.",
                    classDeclaration
                )
                throw IllegalArgumentException(
                    "Invalid annotation value for @Shadowbyte.Shadowed at: ${classDeclaration.qualifiedName?.asString()}"
                )
            }
            // List properties
            classDeclaration.getAllProperties().forEach { property ->

                val type = property.type.toJvmDescriptor()
                val propertyName = property.annotations
                    .find { it.shortName.asString() == "Property" }?.arguments
                    ?.find { it.name?.asString() == "name" }?.value
                    ?.takeIf { it is String }
                    ?.takeIf { it != "NOT_SPECIFIED_FIELD_NAME" }
                    ?: property.simpleName.asString()

                outputBuilder.appendLine("$sourceName->$propertyName:$type")
            }
        }
        // Write output to "output.text"
        val projectBaseDir = environment.options["buildDir"]
            ?: throw IllegalStateException("projectBaseDir not available in KSP options")

        // Construct the intermediates directory path
        val intermediatesDir = File(projectBaseDir)
        if (!intermediatesDir.exists()) {
            intermediatesDir.mkdirs()
        }
        val intermediateFile = File(intermediatesDir, "Shadowfile")
        if (intermediateFile.exists() && outputBuilder.toString().isBlank()) {
        } else {
            intermediateFile.writeText(outputBuilder.toString())
        }

        return invalidAnnotations
    }

    private fun KSTypeReference.toJvmDescriptor(): String {
        val resolvedType = this.resolve()

        // Check if it's a primitive
        resolvedType.declaration.qualifiedName?.asString()?.let { qualifiedName ->
            primitiveTypeMap[qualifiedName]?.let { return it }
        }

        // Handle class types
        if (resolvedType.declaration is KSClassDeclaration) {
            val qualifiedName = resolvedType.declaration.qualifiedName?.asString()
                ?: throw IllegalArgumentException("Unnamed type found")
            return "L${qualifiedName.replace('.', '/')};"
        }

        // Handle arrays
        if (resolvedType.isArrayType()) {
            val elementType = resolvedType.arguments.firstOrNull()?.type
                ?: throw IllegalArgumentException("Array type has no element type")
            return "[${elementType.toJvmDescriptor()}"
        }

        throw IllegalArgumentException("Unsupported type: $resolvedType")
    }

    private fun KSType.isArrayType(): Boolean {
        return declaration.qualifiedName?.asString() == "kotlin.Array"
    }
}
