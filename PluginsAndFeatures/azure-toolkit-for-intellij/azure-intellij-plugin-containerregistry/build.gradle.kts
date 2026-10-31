dependencies {
    implementation(project(":azure-intellij-plugin-lib"))
    // runtimeOnly project(path: ":azure-intellij-plugin-lib", configuration: "instrumentedJar")
    implementation(project(":azure-intellij-plugin-lib-java"))
    // runtimeOnly project(path: ":azure-intellij-plugin-lib-java", configuration: "instrumentedJar")
    implementation(project(":azure-intellij-resource-connector-lib"))
    // runtimeOnly project(path: ":azure-intellij-resource-connector-lib", configuration: "instrumentedJar")
    implementation("com.github.docker-java:docker-java:3.3.0")
    implementation("com.github.docker-java:docker-java-transport-zerodep:3.3.0")
    implementation("com.microsoft.azure:azure-toolkit-storage-lib")
    implementation("com.microsoft.azure:azure-toolkit-ide-common-lib")
    implementation("com.microsoft.azure:azure-toolkit-ide-containerregistry-lib")
    intellijPlatform {
        // Plugin Dependencies. Uses `platformBundledPlugins` property from the gradle.properties file for bundled IntelliJ Platform plugins.
        bundledPlugin("com.intellij.java")
        bundledPlugin("org.jetbrains.idea.maven")
        bundledPlugin("com.intellij.gradle")
        bundledPlugin("org.jetbrains.plugins.terminal")
        bundledPlugin("Docker")
        bundledModules(
            "intellij.platform.remoteServers",
            "intellij.platform.remoteServers.agent.rt",
            "intellij.platform.remoteServers.impl",
        )
    }
}
