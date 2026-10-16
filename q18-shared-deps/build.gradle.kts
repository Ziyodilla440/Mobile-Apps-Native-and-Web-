plugins {
    id("org.jetbrains.kotlin.multiplatform") version "1.9.20"
}

kotlin {
    jvm()
    js(IR) {
        browser()
    }
    linuxX64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
            }
        }
    }
}
