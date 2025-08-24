import org.gradle.api.Plugin
import org.gradle.api.Project

class JsonCompressPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        // Library / Application 两侧都支持
        project.plugins.withId("com.android.library")    { hook(project) }
        project.plugins.withId("com.android.application"){ hook(project) }
    }

    private fun hook(project: Project) {
        // 现在 AGP 已经在这个 module 的 classpath 上了，才能取到扩展
        val androidComponents = project.extensions.getByName("androidComponents")

        // 反射拿 onVariants(selector, action) 以避免 import AGP 类型
        val clazz = androidComponents.javaClass
        val selectorType = Class.forName("com.android.build.api.variant.VariantSelector")
        val onVariants = clazz.methods.firstOrNull {
            it.name == "onVariants" && it.parameterTypes.size == 2 &&
                    it.parameterTypes[0].isAssignableFrom(selectorType)
        } ?: error("AGP onVariants not found")

        // 调用 androidComponents.selector().withBuildType("release")
        val selector = clazz.getMethod("selector").invoke(androidComponents)
        val withBuildType = selectorType.getMethod("withBuildType", String::class.java)
        val releaseSelector = withBuildType.invoke(selector, "release")

        // 构造 VariantAction：接收 Variant 参数
        val variantType = Class.forName("com.android.build.api.variant.Variant")
        val action = java.lang.reflect.Proxy.newProxyInstance(
            project.javaClass.classLoader,
            arrayOf(Class.forName("com.android.build.api.extension.VariantAction"))
        ) { _, method, args ->
            if (method.name == "execute" && args?.size == 1 && variantType.isInstance(args[0])) {
                val variant = args[0]!!
                val nameProp = variant.javaClass.getMethod("getName").invoke(variant) as String

                // 注册任务
                val taskProvider = project.tasks.register(
                    "compress${nameProp.replaceFirstChar { it.uppercase() }}JsonAssets",
                    CompressJsonAssetsTask::class.java
                ) {
                    inputDir.set(project.layout.projectDirectory.dir("src/main/assets"))
                    outputDir.set(project.layout.buildDirectory.dir("intermediates/compressedAssets/$nameProp"))
                }

                // variant.sources.assets?.addGeneratedSourceDirectory(task, CompressJsonAssetsTask::outputDir)
                val sources = variant.javaClass.getMethod("getSources").invoke(variant)
                val assetsOpt = sources.javaClass.methods
                    .firstOrNull { it.name == "getAssets" && it.parameterTypes.isEmpty() }
                    ?.invoke(sources)
                if (assetsOpt != null) {
                    val addGen = assetsOpt.javaClass.methods
                        .first { it.name == "addGeneratedSourceDirectory" && it.parameterTypes.size == 2 }
                    val outProp = CompressJsonAssetsTask::class.java.getMethod("getOutputDir")
                    addGen.invoke(assetsOpt, taskProvider, outProp)
                } else {
                    project.logger.warn("No assets source for variant $nameProp")
                }
            }
            null
        }

        // 真正注册 onVariants
        onVariants.invoke(androidComponents, releaseSelector, action)
    }
}
