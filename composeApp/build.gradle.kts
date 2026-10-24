import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.kotlinCompose)
}

kotlin {

    jvmToolchain(17)

    androidTarget {

        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    jvm("desktop")

    listOf(

        iosX64(),

        iosArm64(),

        iosSimulatorArm64()

    ).forEach { iosTarget ->

        iosTarget.binaries.framework {

            baseName = "ComposeApp"

            isStatic = true
        }
    }

    sourceSets {

        val commonMain by getting {

            dependencies {

                implementation(kotlin("stdlib-common"))

                implementation(compose.runtime)

                implementation(compose.foundation)

                implementation(compose.material3)

                implementation(compose.ui)

                implementation(compose.materialIconsExtended)

                implementation(compose.components.resources)

                implementation(compose.components.uiToolingPreview)

                implementation(libs.kamel.image)

                implementation(
                    "org.jetbrains.kotlinx:kotlinx-datetime:0.6.0"
                )

                implementation(
                    libs.kotlinx.coroutines.core
                )

                implementation("org.jetbrains:annotations:23.0.0")


                implementation(
                    "org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2"
                )

                implementation(
                    libs.ktor.client.core
                )

                implementation(
                    libs.ktor.client.content.negotiation
                )

                implementation(
                    "io.ktor:ktor-serialization-kotlinx-json:2.3.8"
                )
//                implementation("com.benasher44:uuid:0.8.2")
            implementation(compose.materialIconsExtended) 

            }
        }

        val androidMain by getting {

            dependencies {

                implementation(
                    libs.androidx.core.ktx
                )

                implementation(
                    libs.androidx.activity.compose
                )

                implementation(
                    libs.media3.exoplayer
                )

                implementation(
                    libs.media3.session
                )

                implementation(
                    libs.media3.ui
                )

                implementation(
                    libs.media3.cast
                )

                implementation(
                    libs.androidx.biometric
                )

                implementation(
                    libs.androidx.palette
                )

                implementation(
                    libs.androidx.splashscreen
                )

                implementation(
                    "androidx.palette:palette-ktx:1.0.0"
                )

                implementation(
                    "com.microsoft.onnxruntime:onnxruntime-android:1.17.1"
                )

                implementation(
                    "androidx.work:work-runtime-ktx:2.9.0"
                )

                implementation(
                    "io.github.junkfood02.youtubedl-android:library:0.18.1"
                )

                implementation(
                    "io.github.junkfood02.youtubedl-android:ffmpeg:0.18.1"
                )

                implementation(
                    "com.google.android.gms:play-services-cast-framework:21.4.0"
                )

                implementation(
                    "androidx.mediarouter:mediarouter:1.7.0"
                )

                implementation(
                    "io.ktor:ktor-client-okhttp:2.3.8"
                )



                implementation(
                    "net.jthink:jaudiotagger:3.0.1"
                )

                // SF2 SoundFont rendering for realistic piano sounds
              implementation(
                     "dev.kotlinds:fluidsynth-kmp:1.1.0"
                 )
             }
        }

        val desktopMain by getting {

            dependencies {

                implementation(
                    compose.desktop.currentOs
                )

                implementation(
                    libs.kotlinx.coroutines.swing
                )

                implementation(
                    libs.ktor.client.cio
                )

                /*
                 * =========================
                 * JAVAFX
                 * =========================
                 */

                val javafxVersion = "21.0.2"

                val platform =
                    when {

                        System.getProperty("os.name")
                            .lowercase()
                            .contains("mac")
                                &&
                                System.getProperty("os.arch")
                                    .contains("aarch64") -> {

                            "mac-aarch64"
                        }

                        System.getProperty("os.name")
                            .lowercase()
                            .contains("mac") -> {

                            "mac"
                        }

                        System.getProperty("os.name")
                            .lowercase()
                            .contains("win") -> {

                            "win"
                        }

                        else -> {

                            "linux"
                        }
                    }

                implementation(
                    "org.openjfx:javafx-base:$javafxVersion:$platform"
                )

                implementation(
                    "org.openjfx:javafx-graphics:$javafxVersion:$platform"
                )

                implementation(
                    "org.openjfx:javafx-controls:$javafxVersion:$platform"
                )



                implementation(
                    "net.java.dev.jna:jna:5.14.0"
                )

                implementation(
                    "net.java.dev.jna:jna-platform:5.14.0"
                )

                implementation(
                    "net.jthink:jaudiotagger:3.0.1"
                )
            }
        }

        val iosX64Main by getting

        val iosArm64Main by getting

        val iosSimulatorArm64Main by getting

        val iosMain by creating {

            dependsOn(commonMain)

            iosX64Main.dependsOn(this)

            iosArm64Main.dependsOn(this)

            iosSimulatorArm64Main.dependsOn(this)

            dependencies {

                implementation(
                    libs.ktor.client.darwin
                )
            }
        }
    }
}

android {

    namespace = "com.creativeblisslabs.peqlearn"

    compileSdk = 34

    sourceSets["main"].manifest.srcFile(
        "src/androidMain/AndroidManifest.xml"
    )

    sourceSets["main"].res.srcDirs(
        "src/androidMain/res"
    )

    defaultConfig {

        applicationId = "com.creativeblisslabs.peqlearn"

        minSdk = 24

        targetSdk = 34

        versionCode = 1

        versionName = "1.0.0"

        // ndk {
        //     abiFilters.addAll(setOf("arm64-v8a"))
        //     // abiFilters.addAll(setOf("arm64-v8a", "x86_64"))
        // }
    }

    androidResources {
        noCompress.add("sf2")
        noCompress.add("sf")
    }

    packaging {

        jniLibs {

            useLegacyPackaging = true
        }

        resources {

            excludes +=
                "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    val localProps = Properties().apply {
        val envFile = rootProject.file(".env")
        if (envFile.exists()) {
            envFile.inputStream().use { load(it) }
        }
        val locFile = rootProject.file("local.properties")
        if (locFile.exists()) {
            locFile.inputStream().use { load(it) }
        }
    }

    signingConfigs {
        create("release") {
            val keyPath = localProps["RELEASE_KEYSTORE_PATH"] as? String ?: "peq-learn-release.jks"
            val kFile = if (File(keyPath).isAbsolute) File(keyPath) else rootProject.file(keyPath)
            if (kFile.exists()) {
                storeFile = kFile
                storePassword = localProps["RELEASE_STORE_PASSWORD"] as? String ?: ""
                keyAlias = localProps["RELEASE_KEY_ALIAS"] as? String ?: ""
                keyPassword = localProps["RELEASE_KEY_PASSWORD"] as? String ?: ""
            }
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a")
            // include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            isUniversalApk = false
        }
    }

    buildTypes {

        getByName("release") {

            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val releaseSigning = signingConfigs.getByName("release")
            if (releaseSigning.storeFile != null && releaseSigning.storeFile!!.exists()) {
                signingConfig = releaseSigning
            }
        }
    }

    compileOptions {

        sourceCompatibility =
            JavaVersion.VERSION_17

        targetCompatibility =
            JavaVersion.VERSION_17
    }

    dependencies {

        debugImplementation(
            compose.uiTooling
        )
    }
}

compose.desktop {

    application {

        mainClass =
            "com.creativeblisslabs.peqlearn.MainKt"

        /*
         * Disable Proguard
         */

        buildTypes.release.proguard {

            isEnabled.set(false)
        }

        /*
         * IMPORTANT
         *
         * DO NOT ADD:
         *
         * -Djna.library.path
         * -Djava.library.path
         * -Dvlcj.plugin.path
         *
         * These are now set dynamically
         * inside DesktopAudioEngine.
         *
         * Humans keep hardcoding paths,
         * then wonder why packaging breaks.
         */

        nativeDistributions {

            modules(

                "java.base",

                "java.desktop",

                "java.logging",

                "java.sql",

                "jdk.unsupported"
            )

            targetFormats(

                TargetFormat.Dmg,

                TargetFormat.Msi,

                TargetFormat.Deb
            )


    jvmArgs(
        "-Djava.awt.headless=false",
        "-Dapple.awt.transparentTitleBar=true"
    )
            packageName =
                "pEQ Learning App"

            packageVersion =
                "1.0.0"

            description =
                "Interactive Music Learning"

            vendor =
                "pEQ Learn Team"

            macOS {

                bundleID =
                    "com.creativeblisslabs.peqlearn"

                dockName =
                    "pEQLearn"

                minimumSystemVersion =
                    "12.0"

                infoPlist {
                    extraKeysRawXml = """
                        <key>CFBundleDocumentTypes</key>
                        <array>
                            <dict>
                                <key>CFBundleTypeRole</key>
                                <string>Viewer</string>
                                <key>LSHandlerRank</key>
                                <string>Alternate</string>
                                <key>CFBundleTypeName</key>
                                <string>Audio File</string>
                                <key>CFBundleTypeExtensions</key>
                                <array>
                                    <string>mp3</string>
                                    <string>flac</string>
                                    <string>wav</string>
                                    <string>m4a</string>
                                    <string>ogg</string>
                                    <string>aac</string>
                                </array>
                            </dict>
                            <dict>
                                <key>CFBundleTypeRole</key>
                                <string>Viewer</string>
                                <key>LSHandlerRank</key>
                                <string>Alternate</string>
                                <key>CFBundleTypeName</key>
                                <string>Video File</string>
                                <key>CFBundleTypeExtensions</key>
                                <array>
                                    <string>mp4</string>
                                    <string>mkv</string>
                                    <string>mov</string>
                                    <string>avi</string>
                                    <string>webm</string>
                                </array>
                            </dict>
                        </array>
                    """.trimIndent()
                }
            }
        }
    }
}