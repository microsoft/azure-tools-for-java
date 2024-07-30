dependencies {
    implementation(project(":azure-intellij-plugin-lib"))
    // runtimeOnly project(path: ":azure-intellij-plugin-lib", configuration: "instrumentedJar")
    implementation(project(":azure-intellij-resource-connector-lib"))
    // runtimeOnly project(path: ":azure-intellij-resource-connector-lib", configuration: "instrumentedJar")
    implementation(project(":azure-intellij-resource-connector-lib-java"))
    // runtimeOnly project(path: ":azure-intellij-resource-connector-lib-java", configuration: "instrumentedJar")
    implementation(project(":azure-intellij-plugin-redis"))
    // runtimeOnly project(path: ":azure-intellij-plugin-redis", configuration: "instrumentedJar")
    implementation("com.microsoft.azure:azure-toolkit-redis-lib")
    implementation("com.microsoft.azure:azure-toolkit-ide-common-lib")
    implementation("com.microsoft.azure:azure-toolkit-ide-redis-lib")
}
