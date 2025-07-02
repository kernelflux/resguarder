plugins {
    `kotlin-dsl`
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    //noinspection UseTomlInstead
    implementation("com.android.tools.build:gradle:8.11.0")
    implementation(kotlin("gradle-plugin"))
}

gradlePlugin {
    plugins {
        val androidConfigPlugin = this.create("androidConventionConfig")
        androidConfigPlugin.id = "com.kernelflux.android.module"
        androidConfigPlugin.implementationClass =
            "com.kernelflux.android.AndroidModuleConventionPlugin"
    }
}