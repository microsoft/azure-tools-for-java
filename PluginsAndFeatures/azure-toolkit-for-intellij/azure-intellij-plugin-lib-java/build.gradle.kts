plugins {
    id("org.jetbrains.kotlin.jvm")
}

sourceSets {
    main {
        resources {
            exclude("whatsnew.assets/**", "whatsnew-history.md")
        }
    }
    test {
        resources {
            srcDir("src/main/resources")
        }
    }
}

dependencies {
    implementation("com.microsoft.azure:azure-toolkit-auth-lib")
    implementation("com.microsoft.azure:azure-toolkit-ide-common-lib")
    implementation(project(":azure-intellij-plugin-lib"))
    // runtimeOnly project(path: ":azure-intellij-plugin-lib", configuration: "instrumentedJar")
    implementation("org.dom4j:dom4j:2.1.3") {
        exclude(group="javax.xml.stream", module="stax-api")
        exclude(group="xpp3", module="xpp3")
        exclude(group="pull-parser", module="pull-parser")
        exclude(group="net.java.dev.msv", module="xsdlib")
    }
    intellijPlatform {
        // Plugin Dependencies. Uses `platformBundledPlugins` property from the gradle.properties file for bundled IntelliJ Platform plugins.
        bundledPlugin("com.intellij.java")
        bundledPlugin("org.jetbrains.idea.maven")
        bundledPlugin("com.intellij.gradle")
    }
}
