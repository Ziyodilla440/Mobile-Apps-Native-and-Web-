kotlin {
    jvm()
    js(IR) {
        browser()
    }
    linuxX64()

    sourceSets {
        val commonMain by getting

        // Create the custom intermediate source set
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


fun greetingFromJvmAndJs(): String {
    return "Shared between JVM and JS only!"
}
