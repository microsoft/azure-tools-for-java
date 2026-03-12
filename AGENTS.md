# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Azure Toolkits for Java — IDE plugins for IntelliJ IDEA and Eclipse that help Java developers create, develop, configure, test, and deploy applications to Azure. The primary focus of active development is the IntelliJ plugin.

## Build Commands

### Prerequisites
- Java 17 (required for IntelliJ plugin; set `JAVA_HOME` accordingly)
- The shared toolkit libraries from [azure-maven-plugins](https://github.com/microsoft/azure-maven-plugins) must be installed to local Maven repo first

### Build shared toolkit libs (one-time or when libs change)
```bash
# Clone and build azure-toolkit-libs into local Maven repo
git clone https://github.com/microsoft/azure-maven-plugins.git
cd azure-maven-plugins/azure-toolkit-libs
mvn clean install -B -T 4 -Dmaven.test.skip=true
```

### Build Utils (Maven modules in Utils/)
```bash
# From repo root — builds the IDE-specific wrapper libraries
mvn clean install -f Utils/pom.xml
# Or via the root Gradle task:
./gradlew buildUtils -x buildToolkitsLib
```

### Build IntelliJ plugin
```bash
cd PluginsAndFeatures/azure-toolkit-for-intellij
./gradlew clean buildPlugin -s
```
Output ZIP: `PluginsAndFeatures/azure-toolkit-for-intellij/build/distributions/`

### Build Eclipse plugin
```bash
mvn clean install -f Utils/pom.xml
mvn clean install -f PluginsAndFeatures/AddLibrary/AzureLibraries/pom.xml
mvn clean install -f PluginsAndFeatures/azure-toolkit-for-eclipse/pom.xml
```

### Build everything (from repo root)
```bash
./gradlew buildAll
```

### Run/Debug IntelliJ plugin locally
Open `PluginsAndFeatures/azure-toolkit-for-intellij` in IntelliJ and use the Gradle `runIde` task. Remote debug port: 5005.

### Tests
```bash
# IntelliJ plugin tests (run from the IntelliJ plugin dir)
cd PluginsAndFeatures/azure-toolkit-for-intellij
./gradlew test

# Utils tests (Maven)
mvn test -f Utils/pom.xml

# Skip tests during build
./gradlew buildAll -PskipTest=true
# Or for Maven modules:
mvn install -f Utils/pom.xml -Dmaven.test.skip=true
```

### Checkstyle
Checkstyle runs by default on Maven builds. Skip with:
```bash
mvn install -f Utils/pom.xml -Dcheckstyle.skip=true
```
IntelliJ plugin checkstyle config: `PluginsAndFeatures/azure-toolkit-for-intellij/config/checkstyle/checkstyle.xml`

## Architecture

### Three-Layer Dependency Stack

1. **azure-toolkit-libs** (external repo: `azure-maven-plugins`) — Core SDK wrappers for Azure services. IDE-agnostic. Must be `mvn install`ed to local repo before building this project.

2. **Utils/** (this repo, Maven) — IDE-specific wrapper libraries (`azure-toolkit-ide-libs` and `azure-toolkit-ide-hdinsight-libs`). Each Azure service has a corresponding `azure-toolkit-ide-*-lib` module (e.g., `azure-toolkit-ide-appservice-lib`, `azure-toolkit-ide-cosmos-lib`). Built with Maven and published to local repo for the Gradle IntelliJ build to consume.

3. **PluginsAndFeatures/** (this repo) — IDE plugin implementations:
   - `azure-toolkit-for-intellij/` — IntelliJ plugin (Gradle + Kotlin DSL, ~40 submodules)
   - `azure-toolkit-for-eclipse/` — Eclipse plugin (Maven/Tycho)

### IntelliJ Plugin Module Structure

The IntelliJ plugin is a single composite plugin built from ~40 Gradle submodules, each declared in `settings.gradle.kts`. Key module categories:

- **`azure-intellij-plugin-lib`** — Core IntelliJ integration: UI components (AzureComboBox, AzureDialog, Tree), action framework, auth, messager, task manager, telemetry. This is the foundation module.
- **`azure-intellij-plugin-lib-java`** — Java-specific IntelliJ utilities (run configs, artifact handling).
- **`azure-intellij-plugin-base`** — Plugin entry point (`AzurePlugin` as `StartupActivity`), lifecycle management.
- **`azure-intellij-plugin-service-explorer`** — Azure Explorer tool window.
- **`azure-intellij-resource-connector-lib`** — Resource connection framework (connecting Azure resources to project modules).
- **Service modules** — One per Azure service (e.g., `azure-intellij-plugin-appservice`, `azure-intellij-plugin-cosmos`, `azure-intellij-plugin-database`, `azure-intellij-plugin-storage`, etc.)
- **`-java` suffix modules** — Java-language-specific features for that service (e.g., `azure-intellij-plugin-appservice-java`, `azure-intellij-plugin-database-java`).

### Extension Point Pattern

The plugin uses an IntelliJ extension point `com.microsoft.tooling.msservices.intellij.azure.actions` with the `IActionsContributor` interface. Each service module registers its own `ActionsContributor` implementation via its `META-INF/*.xml` descriptor. These are composed into the main `plugin.xml` using `xi:include`.

To add a new service module:
1. Create the module directory under `PluginsAndFeatures/azure-toolkit-for-intellij/`
2. Add it to `settings.gradle.kts`
3. Add an `implementation(project(":module-name"))` dependency in the root `build.gradle.kts`
4. Create a `META-INF/<module-name>.xml` descriptor
5. Include it in `src/main/resources/META-INF/plugin.xml` via `xi:include`

### Build System Details

- **Root Gradle** (`build.gradle` at repo root) — Orchestrates cross-project builds. Uses Groovy DSL with Gradle 8.8.
- **IntelliJ Gradle** (`PluginsAndFeatures/azure-toolkit-for-intellij/build.gradle.kts`) — Uses Kotlin DSL with IntelliJ Platform Gradle Plugin 2.3.0. Targets IntelliJ IDEA Ultimate for compilation. Version catalog at `gradle/libs.versions.toml`.
- **Maven** (`Utils/pom.xml`) — Builds the IDE wrapper libraries. Uses AspectJ for cross-cutting concerns (via `azure-toolkit-common-lib`).
- AspectJ weaving is used at both Maven (aspectj-maven-plugin) and Gradle (io.freefair.aspectj.post-compile-weaving) levels.

### Key Conventions

- Java 17 source/target for all modules.
- Lombok is used project-wide (`@Data`, `@Builder`, etc.).
- All Java source files require the MIT license header (enforced by checkstyle).
- LF line endings required (no CRLF).
- No tabs — spaces only.
- TODOs must be named: `TODO (name): description`.
- Base package: `com.microsoft.azure.toolkit.intellij` for IntelliJ modules, `com.microsoft.azure.toolkit.lib` for library modules.
