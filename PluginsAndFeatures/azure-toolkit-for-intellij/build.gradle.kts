import io.freefair.gradle.plugins.aspectj.AjcAction
import org.apache.tools.ant.filters.ReplaceTokens
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import java.net.URI
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun properties(key: String) = providers.gradleProperty(key)

fun environment(key: String) = providers.environmentVariable(key)

plugins {
    id("java")
    alias(libs.plugins.kotlin)
    alias(libs.plugins.intellijPlatform)
    alias(libs.plugins.intellijPlatformModule)
    alias(libs.plugins.changelog)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.aspectj)
    alias(libs.plugins.springDependencyManagement)
}

group = properties("pluginGroup").get()

allprojects {
    apply {
        plugin("java")
        plugin("org.jetbrains.kotlin.jvm")
        plugin("io.freefair.aspectj.post-compile-weaving")
        plugin("org.jetbrains.intellij.platform.module")
    }

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(21)
        }
    }

    kotlin {
        jvmToolchain(21)
    }

    repositories {
        mavenCentral()
        mavenLocal()
        maven("https://cache-redirector.jetbrains.com/repo1.maven.org/maven2")
        maven("https://cache-redirector.jetbrains.com/intellij-dependencies")
        maven("https://maven.atlassian.com/repository/public")

        intellijPlatform {
            defaultRepositories()
        }
    }

    intellijPlatform {
        buildSearchableOptions = false
        // instrumentCode = true
    }

    dependencies {
        intellijPlatform {
            intellijIdeaUltimate(properties("platformVersion").get()) {
                useInstaller = false
            }
            // JBR 25 required to run IntelliJ 2026.1 (PathClassLoader is JBR-only)
            jetbrainsRuntime()
            // MavenId/MavenCoordinate classes moved from maven plugin to repository-search plugin in 261
            bundledPlugin("org.jetbrains.idea.reposearch")
            // Test framework classes moved to separate modules in 261
            testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)
            testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Plugin.Java)
        }

        implementation(platform("com.microsoft.azure:azure-toolkit-libs:0.52.2"))
        implementation(platform("com.microsoft.azure:azure-toolkit-ide-libs:0.52.2"))
        implementation(platform("com.microsoft.hdinsight:azure-toolkit-ide-hdinsight-libs:0.1.1"))

        compileOnly("org.projectlombok:lombok:1.18.32")
        compileOnly("org.jetbrains:annotations:24.0.0")
        annotationProcessor("org.projectlombok:lombok:1.18.32")
        implementation("com.microsoft.azure:azure-toolkit-common-lib:0.52.2")
        aspect("com.microsoft.azure:azure-toolkit-common-lib:0.52.2")
        // junit was removed from IntelliJ platform bundled libs in 261
        testImplementation("junit:junit:4.13.2")
    }

    configurations {
        implementation { exclude(module = "slf4j-api") }
        implementation { exclude(module = "log4j") }
        implementation { exclude(module = "stax-api") }
        implementation { exclude(module = "groovy-xml") }
        implementation { exclude(module = "groovy-templates") }
        implementation { exclude(module = "jna") }
        implementation { exclude(module = "xpp3") }
        implementation { exclude(module = "pull-parser") }
        implementation { exclude(module = "xsdlib") }
    }

    tasks.configureEach {
        if (name == "instrumentCode") {
            enabled = file("src/main/java").exists()
        }
    }

    tasks {

        compileJava {
            sourceCompatibility = "21"
            targetCompatibility = "21"
        }

        compileKotlin {
            configure<AjcAction> {
                enabled = false
            }
        }
        compileTestKotlin {
            configure<AjcAction> {
                enabled = false
            }
        }

        withType<Copy> {
            duplicatesStrategy = DuplicatesStrategy.WARN
        }

        // Gradle 9 requires explicit dependency declaration for shared sandbox outputs
        withType<Test> {
            dependsOn(rootProject.tasks.named("prepareTestSandbox"))
            // Each subproject's test sandbox may be produced by other subproject tasks
            rootProject.subprojects.forEach { sub ->
                sub.tasks.matching { it.name == "prepareTestSandbox" }.configureEach {
                    this@withType.dependsOn(this)
                }
            }
        }

        sourceSets {
            main {
                java.srcDirs("src/main/java")
                kotlin.srcDirs("src/main/kotlin")
                resources.srcDirs("src/main/resources")
                resources.exclude("bundle/**")
            }
            test {
                java.srcDir("src/test/java")
                kotlin.srcDirs("src/test/kotlin")
                resources.srcDir("src/test/resources")
                // Exclude legacy duplicate hdinsight test files from root module;
                // they are properly maintained in azure-intellij-plugin-hdinsight-base
                java.exclude("com/microsoft/azure/hdinsight/**")
                kotlin.exclude("com/microsoft/azure/hdinsight/**")
            }
        }
    }
}

intellijPlatform {
    projectName = "azure-toolkit-for-intellij"
    buildSearchableOptions = false

    pluginConfiguration {
        id = properties("pluginId").get()
        name = properties("pluginName").get()

        ideaVersion {
            sinceBuild = properties("pluginSinceBuild")
            untilBuild = properties("pluginUntilBuild")
        }
    }

    pluginVerification {
        ides {
            // IC (Community) no longer published since 253; use IU (Ultimate) for verification
            create(IntelliJPlatformType.IntellijIdeaUltimate, properties("platformVersion").get())
        }
        // Suppress known structural warnings — plugin ID/name historically contain "intellij"
        freeArgs = listOf(
            "-mute", "TemplateWordInPluginId",
            "-mute", "TemplateWordInPluginName"
        )
    }
}

dependencies {
    intellijPlatform {
        pluginVerifier()

        // Plugin Dependencies. Uses `platformBundledPlugins` property from the gradle.properties file for bundled IntelliJ Platform plugins.
        bundledPlugin("com.intellij.java")
        bundledPlugin("org.jetbrains.idea.maven")
        bundledPlugin("com.intellij.gradle")
        bundledPlugin("com.intellij.properties")
        bundledPlugin("org.jetbrains.plugins.terminal")
        bundledPlugin("org.intellij.plugins.markdown")
        // Plugin Dependencies. Uses `platformPlugins` property from the gradle.properties file for plugin from JetBrains Marketplace.
        plugins(properties("platformPlugins").map { it.split(',') })
    }
    implementation(project(":azure-intellij-plugin-lib"))
    implementation(project(":azure-intellij-plugin-lib-java"))
    implementation(project(":azure-intellij-resource-connector-lib"))
    implementation(project(":azure-intellij-resource-connector-lib-java"))
    implementation(project(":azure-intellij-plugin-service-explorer"))
    implementation(project(":azure-intellij-plugin-guidance"))
    implementation(project(":azure-intellij-plugin-guidance-java"))
    implementation(project(":azure-sdk-reference-book"))
    implementation(project(":azure-intellij-plugin-springcloud"))
    implementation(project(":azure-intellij-plugin-storage"))
    implementation(project(":azure-intellij-plugin-storage-java"))
    implementation(project(":azure-intellij-plugin-appservice"))
    implementation(project(":azure-intellij-plugin-appservice-java"))
    implementation(project(":azure-intellij-plugin-arm"))
    implementation(project(":azure-intellij-plugin-applicationinsights"))
    implementation(project(":azure-intellij-plugin-cosmos"))
    implementation(project(":azure-intellij-plugin-cognitiveservices"))
    implementation(project(":azure-intellij-plugin-monitor"))
    implementation(project(":azure-intellij-plugin-containerregistry"))
    implementation(project(":azure-intellij-plugin-containerservice"))
    implementation(project(":azure-intellij-plugin-containerapps"))
    implementation(project(":azure-intellij-plugin-database"))
    implementation(project(":azure-intellij-plugin-database-java"))
    implementation(project(":azure-intellij-plugin-vm"))
    implementation(project(":azure-intellij-plugin-redis"))
    implementation(project(":azure-intellij-plugin-redis-java"))
    implementation(project(":azure-intellij-plugin-samples"))
    implementation(project(":azure-intellij-plugin-bicep"))
    implementation(project(":azure-intellij-plugin-eventhubs"))
    implementation(project(":azure-intellij-plugin-servicebus"))
    implementation(project(":azure-intellij-plugin-keyvault"))
    implementation(project(":azure-intellij-plugin-keyvault-java"))
    implementation(project(":azure-intellij-resource-connector-aad"))
    implementation(project(":azure-intellij-plugin-hdinsight-lib"))
    implementation(project(":azure-intellij-plugin-sqlserverbigdata"))
    implementation(project(":azure-intellij-plugin-hdinsight"))
    implementation(project(":azure-intellij-plugin-synapse"))
    implementation(project(":azure-intellij-plugin-sparkoncosmos"))
    implementation(project(":azure-intellij-plugin-hdinsight-base"))
    implementation(project(":azure-intellij-plugin-integration-services"))
    implementation(project(":azure-intellij-plugin-cloud-shell"))
    implementation(project(":azure-intellij-plugin-java-sdk"))
    implementation(project(":azure-intellij-plugin-azuremcp"))
    implementation("commons-io:commons-io")
    implementation("org.apache.commons:commons-lang3")
    implementation("com.microsoft.azure:azure-toolkit-common-lib")
    implementation("com.microsoft.azure:azure-toolkit-auth-lib")
    implementation("com.microsoft.azure:azure-toolkit-ide-common-lib")
    implementation("com.microsoft.azure:azure-toolkit-ide-appservice-lib")

    // Test dependencies for root module tests (cucumber, assertj)
    testImplementation("io.cucumber:cucumber-java:7.0.0")
    testImplementation("io.cucumber:cucumber-junit:7.0.0")
    testImplementation("org.assertj:assertj-core:3.19.0")
}

tasks {
    wrapper {
        gradleVersion = properties("gradleVersion").get()
    }

    val aiKey = properties("applicationinsights.key").get()
    processResources {
        filesMatching("ApplicationInsights.xml") {
            filter<ReplaceTokens>("tokens" to mapOf("applicationinsights.key" to aiKey))
        }
    }

    val currentDateTime: LocalDateTime = LocalDateTime.now()
    val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
    val timestamp = currentDateTime.format(formatter)
    val needPatchVersion = properties("needPatchVersion").get()
    val pluginVersion = properties("pluginVersion").get()
    val intellijDisplayVersion = properties("intellijDisplayVersion").get()
    val getPatchedVersion: String by lazy {
        if (needPatchVersion.toBoolean() || pluginVersion.endsWith("SNAPSHOT")) {
            if (pluginVersion.endsWith("SNAPSHOT")) {
                val fixedPluginVersion = pluginVersion.split("-")
                "${fixedPluginVersion[0]}-$intellijDisplayVersion-SNAPSHOT.$timestamp"
            } else {
                "$pluginVersion-$intellijDisplayVersion-BETA.$timestamp"
            }
        } else {
            "$pluginVersion-$intellijDisplayVersion"
        }
    }

    patchPluginXml {
        version = getPatchedVersion
    }

    register<Copy>("downloadBicepLanguageServer") {
        val langServerDir = file("azure-intellij-plugin-bicep/binaries/bicep/bicep-langserver")
        if (!langServerDir.exists()) {
            logger.info("Downloading bicep language server ...")
            val zipFile = file("azure-intellij-plugin-bicep/downloaded.zip")
            URI("https://aka.ms/java-toolkit-bicep-ls").toURL().openStream().use { input ->
                zipFile.outputStream().use { it.write(input.readBytes()) }
            }
            logger.info("Unzipping bicep language server ...")
            from(zipTree(zipFile))
            into(langServerDir)
        }
    }

    buildPlugin {
        archiveVersion = getPatchedVersion
        from("$projectDir/NOTICE")
        from("$projectDir/azure-intellij-plugin-hdinsight/hdinsight_jobview_html")
        from("$projectDir/azure-intellij-plugin-bicep/binaries")
    }

    prepareSandbox {
        dependsOn("downloadBicepLanguageServer")
        from("$projectDir/NOTICE")
        from("$projectDir/azure-intellij-plugin-hdinsight/hdinsight_jobview_html")
        from("$projectDir/azure-intellij-plugin-bicep/binaries")
    }

    runIde {
        jvmArgs("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005")
    }

    // refers https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-testing-extension.html#intellijPlatformTesting
    val testIde by intellijPlatformTesting.runIde.registering {
        type = IntelliJPlatformType.IntellijIdeaCommunity
        version = properties("platformVersion").get()
    }

    // Configure UI tests plugin
    // Read more: https://github.com/JetBrains/intellij-ui-test-robot
    //    testIdeUi {
    //        systemProperty("robot-server.port", "8082")
    //        systemProperty("ide.mac.message.dialogs.as.sheets", "false")
    //        systemProperty("jb.privacy.policy.text", "<!--999.999-->")
    //        systemProperty("jb.consents.confirmation.enabled", "false")
    //    }
    //
    //    publishPlugin {
    //        dependsOn(patchChangelog)
    //    }
}
