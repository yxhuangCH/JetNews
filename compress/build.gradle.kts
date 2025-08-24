plugins {
    id("com.android.library")  // 不使用别名，直接使用插件 ID
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
}

android {
    namespace = "com.example.compress"
    compileSdk = 35

    defaultConfig {
//        applicationId = "com.example.compress"
        minSdk = 24
        targetSdk = 35

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }

    // 配置发布变体
    publishing {
        singleVariant("release") {
//            withSourcesJar()
            withJavadocJar()
        }
    }
}

// 3. 配置发布物
afterEvaluate {
    publishing {
        publications {
            // 创建一个名为 “release” 的发布
            create<MavenPublication>("release") {
                groupId = "com.example.compress"
                artifactId = "json-library"
                version = "1.0.0"

                // 使用 components.release 而不是动态选择
                from(components["release"])

            }
        }
    }
}



// 创建压缩后的assets目录
val compressedAssetsDir = file("$buildDir/intermediates/compressedAssets")

// 自定义Task用于压缩assets中的JSON文件（减少AAR包大小）
tasks.register("compressJsonAssets") {
    group = "build"
    description = "Compresses JSON assets to reduce AAR size by minifying JSON files"

    // 设置输入和输出目录用于增量构建
    inputs.dir(file("src/main/assets"))
    outputs.dir(compressedAssetsDir)

    doLast {
        // 清理并创建压缩后的assets目录
        compressedAssetsDir.deleteRecursively()
        compressedAssetsDir.mkdirs()

        val originalAssetsDir = file("src/main/assets")
        if (originalAssetsDir.exists()) {
            println("Compressing JSON assets from: ${originalAssetsDir.absolutePath}")

            // 复制所有原始assets文件到压缩目录
            originalAssetsDir.copyRecursively(compressedAssetsDir, overwrite = true)

            // 查找并压缩所有JSON文件
            compressedAssetsDir.walk().filter { it.isFile && it.extension.equals("json", ignoreCase = true) }.forEach { jsonFile ->
                try {
                    val originalContent = jsonFile.readText()
                    // 使用更可靠的JSON压缩方法：移除所有不必要的空白字符
                    val compressedContent = originalContent
                        .replace("\\s*(?=([^\"\\\\]*(\\\\.|\"([^\"\\\\]*\\\\.)*[^\"\\\\]*\"))*[^\"]*$)\\s*".toRegex(), "") // 移除JSON结构外的空白
                        .trim()

                    jsonFile.writeText(compressedContent)
                    println("Compressed: ${jsonFile.relativeTo(compressedAssetsDir)}")
                    println("  Original size: ${originalContent.length} bytes")
                    println("  Compressed size: ${compressedContent.length} bytes")
                    println("  Size reduction: ${originalContent.length - compressedContent.length} bytes (${"%.1f".format((1 - compressedContent.length.toDouble() / originalContent.length) * 100)}%)")
                } catch (e: Exception) {
                    println("Error compressing ${jsonFile.name}: ${e.message}")
                }
            }
        }
    }
}

// 配置mergeAssets任务使用压缩后的assets
afterEvaluate {
    android.libraryVariants.forEach { variant ->
        val variantName = variant.name
        val mergeAssetsTaskName = "merge${variantName.replaceFirstChar { it.uppercase() }}Assets"
        
        tasks.matching { it.name == mergeAssetsTaskName }.forEach { mergeAssetsTask ->
            // 添加对压缩任务的依赖
            mergeAssetsTask.dependsOn("compressJsonAssets")
            
            // 在任务执行前添加压缩后的assets目录到输入源
            mergeAssetsTask.doFirst {
                // 获取当前assets源目录
                val assetsDirs = mergeAssetsTask.inputs.files.files.filter { it.isDirectory }
                // 添加压缩后的assets目录
                mergeAssetsTask.inputs.files(assetsDirs + compressedAssetsDir)
                println("Added compressed assets directory to mergeAssets task: ${compressedAssetsDir.absolutePath}")
            }
        }
    }

    // 为publish任务也添加依赖，确保发布时包含压缩后的assets
    tasks.matching { it.name.startsWith("publish") }.forEach {
        it.dependsOn("compressJsonAssets")
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
}
