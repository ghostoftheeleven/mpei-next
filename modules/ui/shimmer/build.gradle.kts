// The shimmer of the loading states (Modifier.shimmer) for Android and iOS: common Compose code, no android.graphics.
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
            implementation(compose.ui)
        }
    }
}
