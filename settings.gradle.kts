pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    // 1. FIX: Enforce settings repositories across all modules uniformly
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        google()

        // 2. FIX: Position Maven Central at the absolute top of the chain
        mavenCentral()

        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")

        // 3. FIX: Keep JitPack at the very bottom and restrict its content scope
        maven("https://jitpack.io") {
            content {
                // Tells Gradle to ONLY query JitPack if the library starts with "com.github"
                includeGroupAndSubgroups("com.github")
            }
        }
    }
}

rootProject.name = "pEQ-Learn"
include(":composeApp")