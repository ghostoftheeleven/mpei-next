// The theme of the app (palette, typography, MpeixTheme) for Android and iOS. The fonts are Compose resources
// (src/commonMain/composeResources/font); the Android screens that need the font outside of a composition keep using
// RobotoFontFamily from androidMain (res_fonts).
plugins {
    alias(mpeix.plugins.lib.kmp.android)
    alias(libs.plugins.kotlin.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
}

compose.resources {
    packageOfResClass = "kekmech.ru.ui_theme.resources"
    generateResClass = always
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
        }
        androidMain.dependencies {
            implementation(libs.accompanist.systemUiController)
            implementation(compose.preview)
            implementation(project(":res_fonts"))
        }
    }
}
