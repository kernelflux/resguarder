import org.gradle.kotlin.dsl.implementation

plugins {
    alias(libs.plugins.android.application)
    id("com.kernelflux.android.module")
    id("com.kernelflux.plugin.resguarder")
    id("kotlin-kapt")
}

android {
    namespace = "com.kernelflux.resguardersample"
}

dependencies {
    implementation(project(":resguarder"))
    implementation(libs.glide)
    //noinspection KaptUsageInsteadOfKsp
    kapt(libs.glide.compiler)
}

resguarder {
    maxWidth = 400
    maxHeight = 400
    maxFileSize = 90 * 1024
    allBitmapUseImageLoader = true
    enableFileLog = true
}