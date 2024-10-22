dependencies {
    implementation(project(":azure-intellij-plugin-lib"))
    implementation(project(":azure-intellij-resource-connector-lib"))
    implementation("com.microsoft.azure:azure-toolkit-ide-common-lib")
    intellijPlatform {
        // Plugin Dependencies. Uses `platformBundledPlugins` property from the gradle.properties file for bundled IntelliJ Platform plugins.
        bundledPlugin("com.intellij.database")
    }

}