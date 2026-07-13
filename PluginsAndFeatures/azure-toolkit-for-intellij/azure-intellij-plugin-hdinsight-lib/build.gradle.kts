fun properties(key: String) = providers.gradleProperty(key)

dependencies {
    implementation(project(":azure-intellij-plugin-lib"))
    // runtimeOnly project(path: ":azure-intellij-plugin-lib", configuration: "instrumentedJar")
    implementation(project(":azure-intellij-plugin-lib-java"))
    // runtimeOnly project(path: ":azure-intellij-plugin-lib-java", configuration: "instrumentedJar")
    implementation("com.microsoft.azure:azure-toolkit-ide-common-lib")
    implementation("com.microsoft.hdinsight:azure-toolkit-ide-hdinsight-spark-lib")

    intellijPlatform {
        // intellijIdeaUltimate(properties("platformVersion").get())
        // Plugin Dependencies. Uses `platformBundledPlugins` property from the gradle.properties file for bundled IntelliJ Platform plugins.
        bundledPlugin("com.intellij.java")
        plugin("org.intellij.scala:2026.2.5")
    }

}