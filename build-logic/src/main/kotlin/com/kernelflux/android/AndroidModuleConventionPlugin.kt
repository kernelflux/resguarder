package com.kernelflux.android

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.gradle.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.JavaVersion
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import kotlin.jvm.optionals.getOrNull


class AndroidModuleConventionPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.configureAndroidModule<LibraryExtension>("com.android.library") { ext ->
            configureLibrary(ext, project)
        }
        project.configureAndroidModule<ApplicationExtension>("com.android.application") { ext ->
            configureApp(ext, project)
        }

        // 配置默认依赖
        val libs = project.extensions.getByType(VersionCatalogsExtension::class.java).named("libs")
        project.afterEvaluate {
            project.dependencies {
                libs.findLibrary("androidx.core.ktx").getOrNull()?.let {
                    add("implementation", it)
                }
                libs.findLibrary("androidx.appcompat").getOrNull()?.let {
                    add("implementation", it)
                }
                libs.findLibrary("material").getOrNull()?.let {
                    add("implementation", it)
                }

            }
        }


        // ✅ Kotlin compilerOptions 设置
        project.plugins.withId("org.jetbrains.kotlin.android") {
            project.extensions.configure<KotlinAndroidProjectExtension> {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_11)
                }
            }
        }
    }

    private fun safeApplyPlugin(project: Project, pluginName: String) {
        if (!project.plugins.hasPlugin(pluginName)) {
            project.plugins.apply(pluginName)
        }
    }

    private inline fun <reified T> Project.configureAndroidModule(
        pluginId: String,
        crossinline configure: (T) -> Unit,
    ) {
        plugins.withId(pluginId) {
            safeApplyPlugin(this@configureAndroidModule, "org.jetbrains.kotlin.android")
            safeApplyPlugin(this@configureAndroidModule, "maven-publish")
            extensions.findByType(T::class.java)?.let { ext ->
                configure(ext)
            }
        }
    }

    private fun configureApp(extension: ApplicationExtension, project: Project) {
        extension.apply {
            compileSdk = 35

            defaultConfig {
                applicationId = "com.kernelflux.resguardersample"
                minSdk = 21
                targetSdk = 35
                versionCode = 1
                versionName = "1.0.0"
            }

            buildTypes {
                release {
                    isMinifyEnabled = false
                    proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
                    proguardFiles(project.file("proguard-rules.pro"))
                }
            }

            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_11
                targetCompatibility = JavaVersion.VERSION_11
            }

            buildFeatures {
                viewBinding = true
                buildConfig = true
            }
        }
    }


    private fun configureLibrary(extension: LibraryExtension, project: Project) {
        extension.apply {
            compileSdk = 35

            defaultConfig {
                minSdk = 21
                consumerProguardFiles(project.file("consumer-rules.pro"))
            }

            buildTypes {
                release {
                    isMinifyEnabled = false
                    proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
                    proguardFiles(project.file("proguard-rules.pro"))
                }
            }

            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_11
                targetCompatibility = JavaVersion.VERSION_11
            }

            buildFeatures {
                viewBinding = true
                buildConfig = true
            }

            publishing {
                singleVariant("release") {
                    withSourcesJar()
                }
            }
        }
    }

}