plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.kotlinCompose)
    `maven-publish`
    signing
}

group = "io.github.i-am-manu-singh"
version = "1.0.0"

kotlin {
    jvmToolchain(17)

    androidTarget {
        publishLibraryVariants("release")
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
            baseName = "StaffTabNotation"
            isStatic = true
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(libs.kotlinx.serialization.json)
            }
        }
    }
}

android {
    namespace = "io.github.iammanusingh.notation"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

publishing {
    publications.withType<MavenPublication> {
        pom {
            name.set("Staff and Tab Notation Viewer")
            description.set("Production-grade Kotlin Multiplatform Compose component for musical staff and guitar/bass tablature notation.")
            url.set("https://github.com/I-am-Manu-Singh/staff-tab-notation")

            licenses {
                license {
                    name.set("The Apache Software License, Version 2.0")
                    url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    distribution.set("repo")
                }
            }

            developers {
                developer {
                    id.set("I-am-Manu-Singh")
                    name.set("Manu Singh")
                    url.set("https://github.com/I-am-Manu-Singh")
                }
            }

            scm {
                connection.set("scm:git:git://github.com/I-am-Manu-Singh/staff-tab-notation.git")
                developerConnection.set("scm:git:ssh://github.com:I-am-Manu-Singh/staff-tab-notation.git")
                url.set("https://github.com/I-am-Manu-Singh/staff-tab-notation")
            }
        }
    }

    repositories {
        maven {
            name = "MavenCentral"
            // Maven Central Portal publishing endpoint (OSSRH s01)
            val releasesRepoUrl = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            val snapshotsRepoUrl = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
            url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
            credentials {
                username = project.findProperty("mavenCentralUsername")?.toString() ?: System.getenv("MAVEN_CENTRAL_USERNAME")
                password = project.findProperty("mavenCentralPassword")?.toString() ?: System.getenv("MAVEN_CENTRAL_PASSWORD")
            }
        }
        maven {
            name = "LocalStaging"
            url = uri(layout.buildDirectory.dir("repos/staging"))
        }
    }
}
signing {
    val signingKey = project.findProperty("signingKey")?.toString() ?: System.getenv("SIGNING_KEY")
    // Explicitly guarantee an empty string literal fallback if the environment secret comes down blank or empty
    val signingPassword = project.findProperty("signingPassword")?.toString() 
        ?: System.getenv("SIGNING_PASSWORD") 
        ?: ""

    if (!signingKey.isNullOrBlank()) {
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications)
    }
}