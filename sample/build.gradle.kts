plugins {
    alias(libs.plugins.android.application)
    id("com.kernelflux.android.module")
    id("com.kernelflux.plugin.resguarder")
}

android {
    namespace = "com.kernelflux.resguardersample"
}

dependencies {
    implementation(project(":resguarder"))
}

resguarder{
    maxWidth=400
    maxHeight=400
    maxFileSize= 90*1024
    allBitmapUseImageLoader= true
    enableFileLog= true
}