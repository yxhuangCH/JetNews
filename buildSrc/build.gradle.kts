plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
}

dependencies {
    // 添加 JSON 解析库
    implementation("org.json:json:20240303")

    compileOnly("com.android.tools.build:gradle:8.7.2")
}

gradlePlugin {
    plugins {
        register("jsonCompress") {
            id = "json.compress"
            implementationClass = "JsonCompressPlugin"
        }
    }
}