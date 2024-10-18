dependencies {
    implementation(project(":azure-intellij-plugin-lib"))
    // runtimeOnly project(path: ":azure-intellij-plugin-lib", configuration: "instrumentedJar")
    implementation("com.microsoft.azure:azure-toolkit-ide-common-lib")
    implementation("com.microsoft.azure:azure-toolkit-ide-applicationinsights-lib")
    implementation("com.azure:azure-monitor-query:1.0.10")
    implementation("org.apache.commons:commons-csv:1.9.0")
    implementation("com.michaelbaranov:microba:0.4.4.3")
    intellijPlatform {
        // Plugin Dependencies. Uses `platformBundledPlugins` property from the gradle.properties file for bundled IntelliJ Platform plugins.
        bundledPlugin("com.intellij.modules.json")
    }
}