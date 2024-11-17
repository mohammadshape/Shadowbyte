package io.kayt.shadowbyte_ksp

import com.google.devtools.ksp.processing.*

class ShadowedProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return ShadowedProcessor(environment)
    }
}