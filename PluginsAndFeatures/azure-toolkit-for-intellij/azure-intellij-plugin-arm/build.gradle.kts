dependencies {
    implementation(project(":azure-intellij-plugin-lib"))
    // runtimeOnly project(path: ":azure-intellij-plugin-lib", configuration: "instrumentedJar")
    implementation("com.microsoft.azure:azure-toolkit-ide-common-lib")
    implementation("com.microsoft.azure:azure-toolkit-ide-arm-lib")
    intellijPlatform {
        // Plugin Dependencies. Uses `platformBundledPlugins` property from the gradle.properties file for bundled IntelliJ Platform plugins.
        bundledPlugin("com.intellij.modules.json")
    }
}
