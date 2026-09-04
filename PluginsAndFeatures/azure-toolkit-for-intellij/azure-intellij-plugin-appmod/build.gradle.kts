dependencies {
    implementation(project(":azure-intellij-plugin-lib"))
    implementation(project(":azure-intellij-plugin-lib-java"))
    // runtimeOnly project(path: ":azure-intellij-plugin-lib", configuration: "instrumentedJar")
    implementation("com.microsoft.azure:azure-toolkit-ide-common-lib")
    testImplementation("org.mockito:mockito-core:5.20.0")
    intellijPlatform {
        // Plugin Dependencies. Uses `platformBundledPlugins` property from the gradle.properties file for bundled IntelliJ Platform plugins.
        bundledPlugin("com.intellij.java")
        bundledPlugin("org.jetbrains.idea.maven")
       // bundledPlugin("org.jetbrains.idea.maven.model")
        bundledPlugin("com.intellij.gradle")
        // Copilot plugin for Java upgrade integration
        plugin("com.github.copilot:1.5.59-243")
    }
}
