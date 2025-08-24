import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*
import org.gradle.api.file.DirectoryProperty
import java.io.File

@CacheableTask
abstract class CompressJsonAssetsTask : DefaultTask() {

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inputDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun run() {
        val input = inputDir.get().asFile
        val output = outputDir.get().asFile

        output.deleteRecursively()
        output.mkdirs()

        if (!input.exists()) return

        input.walkTopDown().forEach { file ->
            if (!file.isFile) return@forEach

            val target = File(output, file.relativeTo(input).path)
            target.parentFile.mkdirs()

            if (file.extension.equals("json", ignoreCase = true)) {
                try {
                    val text = file.readText().trim()
                    val compressed = when {
                        text.startsWith("{") -> org.json.JSONObject(text).toString()
                        text.startsWith("[") -> org.json.JSONArray(text).toString()
                        else -> {
                            logger.warn("Not valid JSON: ${file.name}, copied as-is.")
                            text
                        }
                    }
                    target.writeText(compressed)
                } catch (e: Exception) {
                    logger.warn("Compress failed: ${file.name}: ${e.message}")
                    file.copyTo(target, overwrite = true)
                }
            } else {
                file.copyTo(target, overwrite = true)
            }
        }
    }
}
