import com.android.build.api.variant.AndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class JsonCompressPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        // 等到 Android 插件被应用时，再去拿 AndroidComponentsExtension
        project.plugins.withId("com.android.library") {
            val androidComponents = project.extensions.getByType(AndroidComponentsExtension::class.java)

            androidComponents.onVariants { variant ->
                val taskProvider = project.tasks.register(
                    "compress${variant.name.replaceFirstChar { it.uppercase() }}JsonAssets",
                    CompressJsonAssetsTask::class.java
                ) {
                    inputDir.set(project.layout.projectDirectory.dir("src/main/assets"))
                    outputDir.set(project.layout.buildDirectory.dir("intermediates/compressedAssets/${variant.name}"))
                }

                // 把压缩后的 assets 合并进流程
                variant.sources.assets?.addGeneratedSourceDirectory(
                    taskProvider,
                    CompressJsonAssetsTask::outputDir
                )
            }
        }
    }
}
