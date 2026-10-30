// A component of the UI kit for Android and iOS (Compose Multiplatform). The previews are written with the common
// @Preview annotation, the theme comes from :ui_theme.
plugins {
    alias(mpeix.plugins.lib.kmp.android)
    alias(libs.plugins.kotlin.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.cmp.material.icons.core)
            implementation(project(":ui_theme"))
        }
    }
}
