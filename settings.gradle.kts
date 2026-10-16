pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "KmpPracticeFour"

include(":q16-custom-sources", ":q17-ios-hierarchy", ":q18-shared-deps")
