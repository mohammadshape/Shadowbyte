plugins {
    kotlin("jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

group = "io.kayt.shadowbyte_ksp"
version = "1.0.0"

dependencies {
    implementation(kotlin("stdlib"))
    implementation(libs.ksp.symbolProcessing.api)
}