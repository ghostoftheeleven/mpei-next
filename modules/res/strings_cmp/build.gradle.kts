// The strings of the app as Compose resources (Res.string, Res.plurals, Res.array) for the shared screens.
// The source is the same as of the Android Views: res_strings/src/main/res/values, values-en, values-ru. Nothing is
// copied, so a string is changed in one place and R.string and Res.string always agree.
plugins {
    alias(mpeix.plugins.lib.kmp.android)
    alias(libs.plugins.kotlin.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
}

compose.resources {
    packageOfResClass = "kekmech.ru.res_strings_cmp.resources"
    generateResClass = always
    customDirectory(
        sourceSetName = "commonMain",
        directoryProvider = provider { layout.projectDirectory.dir("../strings/src/main/res") },
    )
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.components.resources)
        }
    }
}
