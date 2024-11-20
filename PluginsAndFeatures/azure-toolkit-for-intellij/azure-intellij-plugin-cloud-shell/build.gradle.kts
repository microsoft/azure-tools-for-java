plugins {
    alias(libs.plugins.serialization)
}

dependencies {
    implementation("org.java-websocket:Java-WebSocket:1.5.1")
    implementation("com.microsoft.azure:azure-toolkit-ide-common-lib")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    implementation("io.ktor:ktor-client-core:2.3.12") {
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core-jvm")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-jdk8")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-slf4j")
    }
    implementation("io.ktor:ktor-client-cio:2.3.12") {
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-jdk8")
    }
    implementation("io.ktor:ktor-client-content-negotiation:2.3.12") {
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-jdk8")
    }
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12") {
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-jdk8")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-serialization-json")
    }
    implementation("io.ktor:ktor-client-auth:2.3.12") {
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-jdk8")
    }
    implementation("io.ktor:ktor-client-websockets:2.3.12") {
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-jdk8")
    }

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.8.0") {
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core")
    }

    intellijPlatform {
        bundledPlugin("org.jetbrains.plugins.terminal")
    }
}
