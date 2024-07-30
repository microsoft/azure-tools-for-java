dependencies {
    implementation(project(":azure-intellij-plugin-lib"))
    // runtimeOnly project(path: ":azure-intellij-plugin-lib", configuration: "instrumentedJar")
    implementation(project(":azure-intellij-resource-connector-lib"))
    // runtimeOnly project(path: ":azure-intellij-resource-connector-lib", configuration: "instrumentedJar")
    implementation("com.microsoft.azure:azure-toolkit-keyvault-lib")
    implementation("com.microsoft.azure:azure-toolkit-ide-common-lib")
    implementation("com.microsoft.azure:azure-toolkit-ide-keyvault-lib")
    implementation("com.microsoft.azure:azure-toolkit-identity-lib")
    intellijPlatform {
        // Plugin Dependencies. Uses `platformBundledPlugins` property from the gradle.properties file for bundled IntelliJ Platform plugins.
        bundledPlugin("org.jetbrains.plugins.yaml")
        bundledPlugin("com.intellij.properties")
    }
}
