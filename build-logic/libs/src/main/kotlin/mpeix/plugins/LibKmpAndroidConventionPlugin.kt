package mpeix.plugins

import com.android.build.api.variant.LibraryAndroidComponentsExtension
import com.android.build.gradle.LibraryExtension
import mpeix.plugins.ext.configure
import mpeix.plugins.features.android.configureAndroidNamespace
import mpeix.plugins.features.android.disableAndroidTestTasks
import mpeix.plugins.features.android.enableOnlyDebugBuildType
import mpeix.plugins.features.globalconfig.getAndroidProjectConfiguration
import mpeix.plugins.features.unittesting.setupUnitTests
import mpeix.plugins.setup.Plugins
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * A library shared between Android and iOS that also has code for Android (`androidMain`): resources, system bars,
 * previews. The Android modules consume its `android` target, the iOS app its `ios*` targets.
 *
 * The Compose plugins are applied by the module itself (`kotlin.compose.compiler`, `compose.multiplatform`).
 */
internal abstract class LibKmpAndroidConventionPlugin : Plugin<Project> {

    override fun apply(target: Project): Unit = with(target) {
        with(plugins) {
            apply(Plugins.KotlinMultiplatform)
            apply(Plugins.AndroidLibrary)
            apply(Plugins.GradleAndroidCacheFix)
        }
        setupUnitTests()
        val config = getAndroidProjectConfiguration()
        extensions.configure<LibraryExtension> {
            configureAndroidNamespace(this)
            compileSdk = config.targetSdk.get()
            buildToolsVersion = config.buildToolsVersion.get()
            defaultConfig {
                minSdk = config.minSdk.get()
            }
            compileOptions {
                targetCompatibility = JavaVersion.VERSION_17
                sourceCompatibility = JavaVersion.VERSION_17
            }
        }
        extensions.configure<LibraryAndroidComponentsExtension> {
            enableOnlyDebugBuildType()
            disableAndroidTestTasks()
        }
        extensions.configure<KotlinMultiplatformExtension> {
            explicitApi()
            // the same opt-ins as the Android convention gives to the Compose modules
            compilerOptions {
                optIn.addAll(
                    "androidx.compose.foundation.ExperimentalFoundationApi",
                    "androidx.compose.foundation.layout.ExperimentalLayoutApi",
                    "androidx.compose.material3.ExperimentalMaterial3Api",
                    "androidx.compose.ui.ExperimentalComposeUiApi",
                )
            }
            androidTarget {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_17)
                    freeCompilerArgs.add("-Xannotation-default-target=param-property")
                }
            }
            iosArm64()
            iosSimulatorArm64()
        }
    }
}
