plugins {
    kotlin("multiplatform") version "1.9.20"
}

kotlin {
    jvm()
    js(IR) { browser() }
    linuxX64()
    
    // The two new targets added for Question 17:
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        val commonMain by getting
        
        // The manual wiring from Question 16 that disables the default hierarchy:
        val jvmAndJsMain by creating {
            dependsOn(commonMain)
        }
        val jvmMain by getting {
            dependsOn(jvmAndJsMain)
        }
        val jsMain by getting {
            dependsOn(jvmAndJsMain)
        }
    }
}
