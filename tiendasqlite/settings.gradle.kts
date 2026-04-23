pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)  // ← CORREGIDO
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }  // ← Agregado para ImagePicker
    }
}

rootProject.name = "tiendasqlite"
include(":app")