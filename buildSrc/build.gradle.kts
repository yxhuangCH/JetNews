plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    // 添加 JSON 解析库
    implementation("org.json:json:20240303")

    // Gradle 插件开发 API
    implementation(gradleApi())
    implementation(localGroovy())

    // 使用较稳定版本的 AGP
    compileOnly("com.android.tools.build:gradle:8.7.2")

    // 添加 Kotlin 依赖
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.20")
}

gradlePlugin {
    plugins {
        create("jsonCompress") {
            id = "json.compress"
            implementationClass = "com.example.jsoncompress.JsonCompressPlugin"
            displayName = "JSON Compress for Android Assets"
            description = "Minify JSON under src/<flavor>/assets before packaging"
        }
    }
}