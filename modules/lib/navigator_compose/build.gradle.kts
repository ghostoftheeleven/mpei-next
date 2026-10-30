// Draws the back stack of :lib_navigator with Compose Multiplatform: the top screen, the transition between the
// screens and the saved state of every screen that is in the stack.
plugins {
    alias(mpeix.plugins.lib.kmp)
    alias(libs.plugins.kotlin.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":lib_navigator"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.animation)
            implementation(compose.ui)
        }
    }
}
