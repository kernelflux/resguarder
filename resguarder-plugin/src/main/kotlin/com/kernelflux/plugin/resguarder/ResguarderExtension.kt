package com.kernelflux.plugin.resguarder

open class ResguarderExtension {
    var maxWidth: Int = 400
    var maxHeight: Int = 400
    var maxFileSize: Long = 100 * 1024

    var resguarderResGenerateClassName: String = "ResguarderBigImages"
    var allBitmapUseImageLoader: Boolean = true
    var enableLog: Boolean = false
    var enableFileLog: Boolean = false
}