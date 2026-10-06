import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Pure Kotlin domain: no Android, no Compose, no map/GPS SDK.
// The Gradle boundary is what enforces this — a package alone would not.
plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    jvm {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
