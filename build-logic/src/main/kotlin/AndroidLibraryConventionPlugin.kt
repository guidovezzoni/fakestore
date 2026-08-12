import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.library")
                apply("dev.detekt")
                apply("org.jetbrains.kotlinx.kover")
            }

            val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
            dependencies.add("detektPlugins", libs.findLibrary("detekt-compose-rules").get())

            extensions.configure<LibraryExtension> {
                compileSdk = 37
                defaultConfig {
                    minSdk = 24
                }
                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_11
                    targetCompatibility = JavaVersion.VERSION_11
                }
            }

            extensions.configure<dev.detekt.gradle.extensions.DetektExtension> {
                config.setFrom("${rootDir}/config/detekt/detekt.yml")
                buildUponDefaultConfig.set(true)
            }

            extensions.configure<kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension> {
                reports {
                    filters {
                        excludes {
                            classes(
                                "*.BuildConfig",
                                "*.ComposableSingletons*",
                                "*_Factory*",
                                "*_HiltModules*",
                                "*_Impl",
                                "*_MembersInjector",
                                "hilt_aggregated_deps.*",
                                "dagger.hilt.*",
                                "*.Hilt_*",
                                "*.di.*",
                                "*.database.*Dao_Impl*",
                                "*.database.AppDatabase*",
                                "*.database.*Database*",
                                "*.ui.theme.*",
                                "*.ui.screens.*",
                                "*.MainActivity",
                            )
                            annotatedBy(
                                "androidx.compose.ui.tooling.preview.Preview",
                                "androidx.compose.runtime.Composable",
                                "dagger.hilt.android.lifecycle.HiltViewModel",
                                // Exclude kotlinx-serialization generated companion objects (serializers)
                                "kotlinx.serialization.Serializable",
                            )
                        }
                    }
                    verify {
                        rule {
                            minBound(95)
                        }
                    }
                }
            }
        }
    }
}
