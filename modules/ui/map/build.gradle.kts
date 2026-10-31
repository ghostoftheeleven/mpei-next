// The map of the campus for Android and iOS: one Compose component that draws the raster tiles (OpenStreetMap, Esri
// imagery) and the markers, and that is moved with the fingers. The math (tiles, camera) is in :feature_map_shared, so it
// is tested on the JVM; this module only loads the tiles and draws them.
plugins {
    alias(mpeix.plugins.lib.kmp.android)
    alias(libs.plugins.kotlin.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":feature_map_shared"))
            implementation(libs.kotlinx.coroutines.core)
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
