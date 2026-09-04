plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm")
}
dependencies {
    implementation(project(":azure-intellij-plugin-lib"))
    // runtimeOnly project(path: ":azure-intellij-plugin-lib", configuration: "instrumentedJar")
    implementation(project(":azure-intellij-plugin-lib-java"))
    // runtimeOnly project(path: ":azure-intellij-plugin-lib-java", configuration: "instrumentedJar")
    implementation(project(":azure-intellij-plugin-hdinsight"))
    // runtimeOnly project(path: ":azure-intellij-plugin-hdinsight", configuration: "instrumentedJar")
    implementation(project(":azure-intellij-plugin-hdinsight-lib"))
    // runtimeOnly project(path: ":azure-intellij-plugin-hdinsight-lib", configuration: "instrumentedJar")

    implementation("com.microsoft.hdinsight:azure-toolkit-ide-hdinsight-spark-lib")
    implementation("com.microsoft.hdinsight:azure-toolkit-ide-synapse-spark-lib")
    implementation("com.microsoft.hdinsight:azure-toolkit-ide-cosmos-spark-lib")

    implementation("com.microsoft.azure:azure-client-runtime") {
        exclude(group = "com.squareup.okhttp3", module = "okhttp")
        exclude(group = "com.squareup.okhttp3", module = "okhttp-urlconnection")
        exclude(group = "com.squareup.okhttp3", module = "logging-interceptor")
    }

    implementation("org.dom4j:dom4j") {
        exclude(group = "javax.xml.stream", module = "stax-api")
        exclude(group = "xpp3", module = "xpp3")
        exclude(group = "pull-parser", module = "pull-parser")
        exclude(group = "net.java.dev.msv", module = "xsdlib")
    }

    implementation("com.microsoft.hdinsight:azuretools-core") {
        exclude(group = "javax.xml.bind", module = "jaxb-api")
    }
    implementation("com.microsoft.hdinsight:azure-explorer-common") {
        exclude(group = "javax.xml.bind", module = "jaxb-api")
    }
    implementation("com.microsoft.hdinsight:hdinsight-node-common") {
        exclude(group = "javax.xml.bind", module = "jaxb-api")
    }

    testImplementation("io.cucumber:cucumber-java:7.0.0")
    testImplementation("io.cucumber:cucumber-junit:7.0.0")
    testImplementation("org.assertj:assertj-core:3.19.0")
    testImplementation("junit:junit")
    testImplementation("org.mockito:mockito-core")

    testImplementation("org.powermock:powermock-module-junit4:1.7.0RC4")
    testImplementation("org.powermock:powermock-api-mockito2:1.7.0RC4")
    testImplementation("javax.servlet:javax.servlet-api:4.0.1")

    testImplementation("org.jetbrains.kotlin:kotlin-stdlib")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")

    intellijPlatform {
        // Plugin Dependencies. Uses `platformBundledPlugins` property from the gradle.properties file for bundled IntelliJ Platform plugins.
        bundledPlugin("com.intellij.java")
        bundledPlugin("org.jetbrains.idea.maven")
        plugin("org.intellij.scala:2026.2.5")
    }
}

//tasks.register<Jar>("cucumberPackJar") {
//    appendix.set("pathing")
//
//    doFirst {
//        manifest {
//            attributes["Class-Path"] = configurations["cucumberRuntime"].files.joinToString(" ") {
//                it.toURI().toString().replaceFirst("file:/+".toRegex(), "/")
//            }
//        }
//    }
//}
//
//tasks.register("cucumber") {
//    dependsOn("assemble", "testClasses", "compileTestJava", "cucumberPackJar")
//    doLast {
//        javaexec {
//            main = "io.cucumber.core.cli.Main"
//            classpath = files(sourceSets["main"].output, sourceSets["test"].output, tasks["cucumberPackJar"].get().outputs.files)
//            args = listOf("--plugin", "progress", "--glue", "com.microsoft.azure.hdinsight.spark.common", "-m", "Test/resources")
//        }
//    }
//}
//
//tasks.named("test") {
//    dependsOn("cucumber")
//}
// buildPlugin.dependsOn test
