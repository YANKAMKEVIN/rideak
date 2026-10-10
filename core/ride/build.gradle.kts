import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Pure ride logic (metrics, filtering): no Android, no GPS SDK, no database.
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
        commonMain.dependencies {
            api(projects.core.exploration)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
