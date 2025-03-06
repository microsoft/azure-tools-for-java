dependencies {
  implementation(project(":azure-intellij-plugin-lib"))
  implementation("com.microsoft.azure:azure-toolkit-ide-common-lib")

  intellijPlatform {
    bundledPlugin("org.jetbrains.plugins.terminal")
  }
}
