@file:Suppress("UnstableApiUsage")

import java.util.Properties

plugins {
    alias(libs.plugins.agp.app)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.chaquopy)
    id("kotlin-parcelize")
}

val androidCompileSdkVersion: Int by rootProject.extra
val androidCompileSdkVersionMinor: Int by rootProject.extra
val androidBuildToolsVersion: String by rootProject.extra
val androidMinSdkVersion: Int by rootProject.extra
val androidTargetSdkVersion: Int by rootProject.extra
val androidSourceCompatibility: JavaVersion by rootProject.extra
val androidTargetCompatibility: JavaVersion by rootProject.extra
val managerVersionCode: Int by rootProject.extra
val managerVersionName: String by rootProject.extra

// Signing credentials live in local.properties (git-ignored); passwords are
// read via java.util.Properties so `\#` escapes inside them decode correctly.
fun loadSigningProperties(): Properties {
    val p = Properties()
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { p.load(it) }
    return p
}

val signingProps = loadSigningProperties()
val signingStoreFile = signingProps.getProperty("KEYSTORE_FILE", "keystore/Clover.jks")
val signingStorePassword = signingProps.getProperty("KEYSTORE_PASSWORD", "")
val signingAlias = signingProps.getProperty("KEY_ALIAS", "Clover")
val signingKeyPassword = signingProps.getProperty("KEY_PASSWORD", "")

android {
    namespace = "com.android.avbtoolkit"
    ndkVersion = "29.0.14206865"
    val isPrBuild = project.findProperty("IS_PR_BUILD")?.toString()?.toBoolean() ?: false

    signingConfigs {
        create("clover") {
            storeFile = rootProject.file(signingStoreFile)
            storePassword = signingStorePassword
            keyAlias = signingAlias
            keyPassword = signingKeyPassword
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("clover")
        }
        release {
            signingConfig = signingConfigs.getByName("clover")
            isMinifyEnabled = true
            isShrinkResources = true
            vcsInfo.include = false
            if (isPrBuild) applicationIdSuffix = ".dev"
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    packaging {
        dex {
            useLegacyPackaging = true
        }
        jniLibs {
            useLegacyPackaging = true
            excludes += "lib/*/libandroidx.graphics.path.so"
        }
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    androidResources {
        generateLocaleConfig = true
    }
    compileSdk {
        version =
            release(androidCompileSdkVersion) {
                minorApiLevel = androidCompileSdkVersionMinor
            }
    }
    buildToolsVersion = androidBuildToolsVersion

    defaultConfig {
        applicationId = "com.android.avbtoolkit"
        minSdk = androidMinSdkVersion
        targetSdk = androidTargetSdkVersion
        versionCode = managerVersionCode
        versionName = managerVersionName
        ndk {
            abiFilters += "arm64-v8a"
        }

        buildConfigField("boolean", "IS_PR_BUILD", isPrBuild.toString())
    }

    externalNativeBuild {
        cmake {
            version = "3.28.3"
            path = file("src/main/cpp/CMakeLists.txt")
        }
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = false
        // avbtool 命令/参数文案有意仅提供英文与简体中文（与上游 AVBTool
        // Android 项目一致），其余 40+ 语言区缺失属预期行为。
        disable += "MissingTranslation"
    }

    compileOptions {
        sourceCompatibility = androidSourceCompatibility
        targetCompatibility = androidTargetCompatibility
    }
}

val chaquopyPythonPath: String =
    (project.findProperty("chaquopy.python") as String?)
        ?: "/root/.local/share/uv/python/cpython-3.13-linux-aarch64-gnu/bin/python3.13"

chaquopy {
    defaultConfig {
        version = "3.13"
        buildPython(chaquopyPythonPath)
        pip {
            install("cryptography==42.0.8")
        }
    }
}

androidComponents {
    onVariants(selector().withBuildType("release")) {
        it.packaging.resources.excludes.addAll(listOf("META-INF/**", "kotlin/**", "**.bin"))
    }
}

base {
    archivesName.set(
        "AVBToolkit_${managerVersionName}_${managerVersionCode}"
    )
}

dependencies {
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)

    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)

    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigationevent.compose)

    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.commonmark)
    implementation(libs.commonmark.ext.gfm.tables)
    implementation(libs.commonmark.ext.gfm.strikethrough)
    implementation(libs.commonmark.ext.autolink)
    implementation(libs.commonmark.ext.task.list.items)

    implementation(libs.androidx.webkit)

    implementation(libs.hiddenapibypass)

    implementation(libs.miuix.ui)
    implementation(libs.miuix.icons)
    implementation(libs.miuix.navigation3.ui)
    implementation(libs.miuix.preference)
    implementation(libs.miuix.blur)

    implementation(libs.backdrop.android)

    implementation(platform(libs.okhttp.bom))
    implementation(libs.okhttp)

    implementation(libs.material.kolor)

}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
        )
    }
}
