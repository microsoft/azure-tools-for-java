dependencies {
    implementation(project(":azure-intellij-plugin-lib"))
    // runtimeOnly project(path: ":azure-intellij-plugin-lib", configuration: "instrumentedJar")
    implementation(project(":azure-intellij-plugin-service-explorer"))
    // runtimeOnly project(path: ":azure-intellij-plugin-service-explorer", configuration: "instrumentedJar")
    implementation("com.microsoft.azure:azure-toolkit-ide-common-lib")
    implementation("io.github.cdimascio:dotenv-java:3.0.0")
    implementation("com.microsoft.azure:azure-toolkit-identity-lib")
    intellijPlatform {
        // Plugin Dependencies. Uses `platformBundledPlugins` property from the gradle.properties file for bundled IntelliJ Platform plugins.
        bundledPlugin("com.intellij.properties")
    }
}
