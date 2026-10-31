sourceSets {
    main {
        resources {
            srcDir("src/main/resources")
        }
    }
    test {
        resources {
            srcDir("src/test/resources")
        }
    }
}

dependencies {
    implementation("com.microsoft.azure:azure-toolkit-auth-lib")
    implementation("com.microsoft.azure:azure-toolkit-ide-common-lib")
    implementation("org.dom4j:dom4j:2.1.3") {
        exclude(group="javax.xml.stream", module="stax-api")
        exclude(group="xpp3", module="xpp3")
        exclude(group="pull-parser", module="pull-parser")
        exclude(group="net.java.dev.msv", module="xsdlib")
    }
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.2") {
        exclude(group = "com.fasterxml.jackson", module = "jackson-bom")
    }
    intellijPlatform {
        // Plugin Dependencies. Uses `platformBundledPlugins` property from the gradle.properties file for bundled IntelliJ Platform plugins.
        bundledPlugin("org.jetbrains.plugins.terminal")
    }
}
