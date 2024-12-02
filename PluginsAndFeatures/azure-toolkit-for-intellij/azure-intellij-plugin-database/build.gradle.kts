dependencies {
    implementation(project(":azure-intellij-plugin-lib"))
    // runtimeOnly project(path: ":azure-intellij-plugin-lib", configuration: "instrumentedJar")
    implementation(project(":azure-intellij-resource-connector-lib"))
    implementation(project(":azure-intellij-plugin-dbtools"))
    // runtimeOnly project(path: ":azure-intellij-resource-connector-lib", configuration: "instrumentedJar")
    implementation("com.microsoft.azure:azure-toolkit-database-lib")
    implementation("com.microsoft.azure:azure-toolkit-mysql-lib")
    implementation("com.microsoft.azure:azure-toolkit-sqlserver-lib")
    implementation("com.microsoft.azure:azure-toolkit-postgre-lib")
    implementation("com.microsoft.azure:azure-toolkit-ide-common-lib")
    implementation("com.microsoft.azure:azure-toolkit-ide-database-lib")
    implementation("mysql:mysql-connector-java:8.0.29")
    implementation("com.microsoft.sqlserver:mssql-jdbc:9.3.1.jre8-preview")
    implementation("org.postgresql:postgresql:42.4.1")
    intellijPlatform {
        // Plugin Dependencies. Uses `platformBundledPlugins` property from the gradle.properties file for bundled IntelliJ Platform plugins.
        bundledPlugin("com.intellij.database")
    }

}