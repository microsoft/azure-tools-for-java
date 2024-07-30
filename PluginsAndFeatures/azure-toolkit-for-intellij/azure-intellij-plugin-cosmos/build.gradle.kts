fun properties(key: String) = providers.gradleProperty(key)

sourceSets {
    main {
        resources.srcDirs("src/main/resources")
    }
}

dependencies {
    implementation(project(":azure-intellij-plugin-lib"))
    // runtimeOnly project(path: ":azure-intellij-plugin-lib", configuration: "instrumentedJar")
    implementation(project(":azure-intellij-resource-connector-lib"))
    // runtimeOnly project(path: ":azure-intellij-resource-connector-lib", configuration: "instrumentedJar")
    implementation(project(":azure-intellij-resource-connector-lib-java"))
    // runtimeOnly project(path: ":azure-intellij-resource-connector-lib-java", configuration: "instrumentedJar")
    implementation("com.microsoft.azure:azure-toolkit-cosmos-lib")
    implementation("com.microsoft.azure:azure-toolkit-ide-common-lib")
    implementation("com.microsoft.azure:azure-toolkit-ide-cosmos-lib")
    implementation("com.microsoft.azure:azure-toolkit-identity-lib")

    intellijPlatform {
        intellijIdeaUltimate(properties("platformVersion").get())
        // Plugin Dependencies. Uses `platformBundledPlugins` property from the gradle.properties file for bundled IntelliJ Platform plugins.
        bundledPlugin("com.intellij.java")
        bundledPlugin("com.intellij.database")
    }
}
