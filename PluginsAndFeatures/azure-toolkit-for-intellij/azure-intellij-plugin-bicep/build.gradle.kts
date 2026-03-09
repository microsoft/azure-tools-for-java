dependencies {
    implementation(project(":azure-intellij-plugin-lib"))
    // runtimeOnly project(path: ":azure-intellij-plugin-lib", configuration: "instrumentedJar")
    implementation(project(":azure-intellij-plugin-lib-java"))
    // runtimeOnly project(path: ":azure-intellij-plugin-lib-java", configuration: "instrumentedJar")
    implementation("com.microsoft.azure:azure-toolkit-ide-common-lib")
    implementation("org.eclipse.lsp4j:org.eclipse.lsp4j:0.15.0")
    implementation("com.vladsch.flexmark:flexmark:0.64.0")
    implementation("com.vladsch.flexmark:flexmark-util:0.64.0")
    implementation("org.apache.commons:commons-lang3:3.12.0")
    // ComparableVersion is no longer on the platform classpath in 261
    implementation("org.apache.maven:maven-artifact:3.9.11")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:3.9.0")
    testImplementation("org.powermock:powermock-api-mockito2:2.0.9")
    testImplementation("org.powermock:powermock-module-junit4:2.0.9")
    intellijPlatform {
        // Plugin Dependencies. Uses `platformBundledPlugins` property from the gradle.properties file for bundled IntelliJ Platform plugins.
        bundledPlugin("org.jetbrains.plugins.textmate")
    }
}
