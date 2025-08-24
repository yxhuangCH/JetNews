import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

@CacheableTask
abstract class CompressJsonAssetsTask : DefaultTask() {

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inputDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun compress() {
        val input = inputDir.get().asFile
        val output = outputDir.get().asFile

        output.deleteRecursively()
        output.mkdirs()

        if (!input.exists()) return

        input.walkTopDown().forEach { file ->
            if (file.isFile) {
                val targetFile = File(output, file.relativeTo(input).path)
                targetFile.parentFile.mkdirs()
                if (file.extension.equals("json", ignoreCase = true)) {
                    try {
                        val text = file.readText().trim()
                        val compressedContent = when {
                            text.startsWith("{") -> JSONObject(text).toString()
                            text.startsWith("[") -> JSONArray(text).toString()
                            else -> {
                                println("⚠️ Warning: ${file.name} is not valid JSON, copying as-is.")
                                text
                            }
                        }
                        targetFile.writeText(compressedContent)
                        println("✅ Compressed JSON: ${file.relativeTo(input)}")
                    } catch (e: Exception) {
                        println("❌ Error compressing ${file.name}: ${e.message}")
                        file.copyTo(targetFile, overwrite = true)
                    }
                } else {
                    file.copyTo(targetFile, overwrite = true)
                }
            }
        }
    }
}
