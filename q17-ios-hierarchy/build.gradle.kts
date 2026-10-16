plugins {
    id("org.jetbrains.kotlin.multiplatform") version "1.9.20"
}

kotlin {
    jvm()
    js(IR) {
        browser()
    }
    linuxX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        val commonMain by getting
        val jvmMain by getting
        val jsMain by getting
        val jvmAndJsMain by creating {
            dependsOn(commonMain)
        }

        jvmMain.dependsOn(jvmAndJsMain)
        jsMain.dependsOn(jvmAndJsMain)
    }
}
