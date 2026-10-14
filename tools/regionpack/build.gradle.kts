import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Dev-time tool: turns OpenStreetMap data into a region pack (docs/osm-data-strategy.md).
// Never shipped in the app; reuses :core:exploration so cells are computed by the same code.
plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    jvm {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

    sourceSets {
        jvmMain.dependencies {
            implementation(projects.core.exploration)
            implementation(libs.kotlinx.serialization.json)
        }
        jvmTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

// ./gradlew :tools:regionpack:run --args="<overpass.json> <out.region>"
tasks.register<JavaExec>("run") {
    group = "application"
    val jvm = kotlin.jvm().compilations.getByName("main")
    classpath = files(jvm.output.allOutputs, jvm.runtimeDependencyFiles)
    mainClass = "com.rideak.tools.regionpack.MainKt"
}
