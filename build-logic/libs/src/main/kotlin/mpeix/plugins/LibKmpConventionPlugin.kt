package mpeix.plugins

import mpeix.plugins.ext.configure
import mpeix.plugins.features.unittesting.setupUnitTests
import mpeix.plugins.setup.Plugins
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * A library without Android and UI dependencies, shared between Android and iOS.
 *
 * Targets: `jvm` (this is what the Android modules consume, the same way they consume `mpeix.lib.kotlin` modules),
 * `iosArm64` and `iosSimulatorArm64`. Code that needs `java.*` or `android.*` goes to `jvmMain`.
 */
internal abstract class LibKmpConventionPlugin : Plugin<Project> {

    override fun apply(target: Project): Unit = with(target) {
        plugins.apply(Plugins.KotlinMultiplatform)
        setupUnitTests()
        extensions.configure<KotlinMultiplatformExtension> {
            explicitApi()
            jvm {
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
