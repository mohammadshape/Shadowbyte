import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.android.tools.common)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
}

group = "io.kayt.shadowbyte_asm"
version = "1.0.0"

gradlePlugin {
    plugins {
        register("ShadowByte") {
            id = "io.kayt.shadowbyte"
            implementationClass = "io.kayt.shadowbyte_asm.ShadowbyteAbstractTreeModifier"
        }
    }
}