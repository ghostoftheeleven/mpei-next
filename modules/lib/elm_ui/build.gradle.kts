// The ELM (Elmslie) store of a screen for Compose Multiplatform: it is created when the screen opens, survives
// everything that happens above the screen, and is stopped when the screen leaves the back stack.
plugins {
    alias(mpeix.plugins.lib.kmp)
    alias(libs.plugins.kotlin.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":lib_navigator_compose"))
            api(libs.vivid.elmslie.core)
            implementation(libs.kotlinx.coroutines.core)
            implementation(compose.runtime)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
