# Stable Release Network Isolation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Enable 1ES Network Isolation for the stable release pipeline and force ordinary Maven and Gradle package restores through the `vscjava` Central Feed Service.

**Architecture:** Add pipeline-only Maven and Gradle policy files under `.azure-pipelines`. The Build Plugin step sets one pipeline-scoped Maven local repository under `$(Agent.TempDirectory)\azure-tools-maven-repository`, passes that path via `maven.repo.local` to both Maven and Gradle, and uses the Gradle init script to make the exact 28 Utils reactor coordinates exclusive to that local handoff. All ordinary packages still route through CFS, while the Atlassian Microba exception and purpose-specific vendor repositories remain narrowly scoped direct endpoints.

**Tech Stack:** Azure Pipelines YAML, Maven settings XML, Gradle 9.1 Groovy init scripts, PowerShell, 1ES Pipeline Templates

---

## File Structure

- Create `.azure-pipelines/cfs-variables.yml`: non-secret CFS endpoint shared by Maven and Gradle.
- Create `.azure-pipelines/cfs-settings.xml`: pipeline-only Maven mirror and server configuration.
- Create `.azure-pipelines/cfs-init.gradle`: pipeline-only Gradle repository redirection, explicit `maven.repo.local` validation, exact Utils allowlisting, exclusive local handoff, and fail-closed behavior.
- Modify `.azure-pipelines/sign-for-stable-release.yml`: enable network isolation, set `CFS_MAVEN_LOCAL_REPOSITORY`, and apply the Maven/Gradle CFS policies plus both `maven.repo.local` CLI properties to the build step.
- Keep `docs/superpowers/specs/2026-08-11-stable-release-network-isolation-design.md` as the approved design record.

### Task 1: Add Shared CFS and Maven Configuration

**Files:**
- Create: `.azure-pipelines/cfs-variables.yml`
- Create: `.azure-pipelines/cfs-settings.xml`

- [ ] **Step 1: Run the precondition check**

Run:

```powershell
$paths = @(
    '.azure-pipelines\cfs-variables.yml',
    '.azure-pipelines\cfs-settings.xml'
)
$existing = $paths | Where-Object { Test-Path $_ }
if ($existing) {
    throw "Expected new files, but these already exist: $($existing -join ', ')"
}
Write-Host 'PASS: CFS configuration files do not exist yet'
```

Expected: `PASS: CFS configuration files do not exist yet`.

- [ ] **Step 2: Create the non-secret CFS variable template**

Create `.azure-pipelines/cfs-variables.yml`:

```yaml
# Non-secret Central Feed Service configuration shared by the Maven and Gradle
# dependency restores in sign-for-stable-release.yml. System.AccessToken is a
# secret and must be mapped directly on the build step instead of being declared
# here.
variables:
  - name: CFS_MAVEN_URL
    value: https://pkgs.dev.azure.com/mseng/VSJava/_packaging/vscjava/maven/v1
```

- [ ] **Step 3: Create the pipeline-only Maven settings**

Create `.azure-pipelines/cfs-settings.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!--
  Pipeline-only Maven settings for 1ES Network Isolation. Maven Central is
  mirrored through CFS, while the URL and credential are supplied by the build
  step environment. Local developer settings are unaffected.
-->
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd">
  <mirrors>
    <mirror>
      <id>vscjava</id>
      <name>Central Feed Service</name>
      <url>${env.CFS_MAVEN_URL}</url>
      <mirrorOf>central</mirrorOf>
    </mirror>
  </mirrors>
  <servers>
    <server>
      <id>vscjava</id>
      <username>AzureDevOps</username>
      <password>${env.SYSTEM_ACCESSTOKEN}</password>
    </server>
  </servers>
</settings>
```

- [ ] **Step 4: Validate the files**

Run:

```powershell
[xml]$settings = Get-Content '.azure-pipelines\cfs-settings.xml' -Raw
$ns = New-Object System.Xml.XmlNamespaceManager($settings.NameTable)
$ns.AddNamespace('m', 'http://maven.apache.org/SETTINGS/1.0.0')

$mirror = $settings.SelectSingleNode('/m:settings/m:mirrors/m:mirror', $ns)
$server = $settings.SelectSingleNode('/m:settings/m:servers/m:server', $ns)
if ($mirror.id -ne 'vscjava' -or $server.id -ne 'vscjava') {
    throw 'Maven mirror and server IDs must both be vscjava'
}
if ($mirror.mirrorOf -ne 'central') {
    throw 'Maven mirror must be scoped to central'
}
if ($mirror.url -ne '${env.CFS_MAVEN_URL}') {
    throw 'Maven mirror must read CFS_MAVEN_URL from the environment'
}
if ($server.password -ne '${env.SYSTEM_ACCESSTOKEN}') {
    throw 'Maven server must read SYSTEM_ACCESSTOKEN from the environment'
}

$variables = Get-Content '.azure-pipelines\cfs-variables.yml' -Raw
if ($variables -notmatch '(?m)^\s*-\s+name:\s+CFS_MAVEN_URL\s*$' -or
    $variables -notmatch 'https://pkgs\.dev\.azure\.com/mseng/VSJava/_packaging/vscjava/maven/v1') {
    throw 'CFS_MAVEN_URL is missing or incorrect'
}
Write-Host 'PASS: Maven CFS configuration is valid'
```

Expected: `PASS: Maven CFS configuration is valid`.

- [ ] **Step 5: Commit the Maven policy**

Run:

```powershell
git add -- .azure-pipelines/cfs-variables.yml .azure-pipelines/cfs-settings.xml
git commit -m "build: add Maven CFS configuration" -m "Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

Expected: one commit containing the two new files.

### Task 2: Add the Gradle Repository Policy and Scoped Utils Handoff

**Files:**
- Create: `.azure-pipelines/cfs-init.gradle`

- [ ] **Step 1: Run the precondition check**

Run:

```powershell
if (Test-Path '.azure-pipelines\cfs-init.gradle') {
    throw '.azure-pipelines\cfs-init.gradle already exists'
}
Write-Host 'PASS: Gradle CFS policy does not exist yet'
```

Expected: `PASS: Gradle CFS policy does not exist yet`.

- [ ] **Step 2: Create the Gradle init script**

Create `.azure-pipelines/cfs-init.gradle`:

```groovy
import org.gradle.api.GradleException
import org.gradle.api.artifacts.repositories.MavenArtifactRepository

import java.io.File
import java.util.Locale

def cfsUrl = System.getenv('CFS_MAVEN_URL')
def cfsToken = System.getenv('SYSTEM_ACCESSTOKEN')

if (!cfsUrl?.trim() || !cfsToken?.trim()) {
    throw new GradleException(
        'CFS_MAVEN_URL and SYSTEM_ACCESSTOKEN must both be set when cfs-init.gradle is applied. ' +
            'Failing instead of restoring packages from a public repository.')
}

def mavenLocalRepositoryPath = System.getProperty('maven.repo.local')
if (!mavenLocalRepositoryPath?.trim()) {
    throw new GradleException(
        'maven.repo.local must be explicitly set when cfs-init.gradle is applied. ' +
            'This pipeline-only init script requires a scoped Maven local repository.')
}

// Keep this exact allowlist in sync with the Utils reactor outputs built into the scoped Maven local handoff.
def utilsReactorModules = [
    ['com.microsoft.azuretools', 'utils'],
    ['com.microsoft.azure', 'azure-toolkit-ide-libs'],
    ['com.microsoft.azure', 'azure-toolkit-ide-applicationinsights-lib'],
    ['com.microsoft.azure', 'azure-toolkit-ide-appservice-lib'],
    ['com.microsoft.azure', 'azure-toolkit-ide-arm-lib'],
    ['com.microsoft.azure', 'azure-toolkit-ide-cognitiveservices-lib'],
    ['com.microsoft.azure', 'azure-toolkit-ide-common-lib'],
    ['com.microsoft.azure', 'azure-toolkit-ide-containerapps-lib'],
    ['com.microsoft.azure', 'azure-toolkit-ide-containerregistry-lib'],
    ['com.microsoft.azure', 'azure-toolkit-ide-containerservice-lib'],
    ['com.microsoft.azure', 'azure-toolkit-ide-cosmos-lib'],
    ['com.microsoft.azure', 'azure-toolkit-ide-database-lib'],
    ['com.microsoft.azure', 'azure-toolkit-ide-eventhubs-lib'],
    ['com.microsoft.azure', 'azure-toolkit-ide-keyvault-lib'],
    ['com.microsoft.azure', 'azure-toolkit-ide-redis-lib'],
    ['com.microsoft.azure', 'azure-toolkit-ide-servicebus-lib'],
    ['com.microsoft.azure', 'azure-toolkit-ide-springcloud-lib'],
    ['com.microsoft.azure', 'azure-toolkit-ide-storage-lib'],
    ['com.microsoft.azure', 'azure-toolkit-ide-vm-lib'],
    ['com.microsoft.hdinsight', 'azure-toolkit-ide-hdinsight-libs'],
    ['com.microsoft.hdinsight', 'azure-explorer-common'],
    ['com.microsoft.hdinsight', 'azure-toolkit-ide-cosmos-spark-lib'],
    ['com.microsoft.hdinsight', 'azure-toolkit-ide-hdinsight-spark-lib'],
    ['com.microsoft.hdinsight', 'azure-toolkit-ide-sqlserver-spark-lib'],
    ['com.microsoft.hdinsight', 'azure-toolkit-ide-synapse-spark-lib'],
    ['com.microsoft.hdinsight', 'azuretools-core'],
    ['com.microsoft.hdinsight', 'hdinsight-node-common'],
    ['com.microsoft.azuretools', 'spark-localrun-mock'],
] as List<List<String>>

def forbiddenHosts = [
    'repo.maven.apache.org',
    'repo1.maven.org',
    'plugins.gradle.org',
    'plugins-artifacts.gradle.org',
    'oss.sonatype.org',
    's01.oss.sonatype.org',
    'maven-central.storage-download.googleapis.com',
] as Set

def normalizeRepositoryPath = { repositoryUrl ->
    def path = (repositoryUrl?.path ?: '').toLowerCase(Locale.ROOT).replaceAll('/+$', '')
    path ?: '/'
}

def normalizeRepositoryUri = { repositoryUrl ->
    def normalizedUri = (repositoryUrl instanceof URI ? repositoryUrl : uri(repositoryUrl)).normalize()

    if (!normalizedUri?.scheme || normalizedUri.scheme.equalsIgnoreCase('file')) {
        return new File(normalizedUri).canonicalFile.toURI().normalize().toString().toLowerCase(Locale.ROOT).replaceAll('/+$', '')
    }

    def scheme = normalizedUri.scheme?.toLowerCase(Locale.ROOT) ?: ''
    def host = normalizedUri.host?.toLowerCase(Locale.ROOT) ?: ''
    def port = normalizedUri.port >= 0 ? ":${normalizedUri.port}" : ''
    def path = normalizeRepositoryPath(normalizedUri)
    "${scheme}://${host}${port}${path}"
}

def scopedMavenLocalRepositoryUri = normalizeRepositoryUri(new File(mavenLocalRepositoryPath).canonicalFile.toURI())

def includeUtilsReactorModules = { contentFilter ->
    utilsReactorModules.each { group, artifact ->
        contentFilter.includeModule(group, artifact)
    }
}

def scopedMavenLocalExclusiveRegistered = Collections.newSetFromMap(new IdentityHashMap<>())

def isAtlassianPublicRepository = { MavenArtifactRepository repository ->
    def repositoryUrl = repository.url
    def host = repositoryUrl?.host?.toLowerCase(Locale.ROOT)
    def path = normalizeRepositoryPath(repositoryUrl)
    host == 'maven.atlassian.com' && path == '/repository/public'
}

def isScopedMavenLocalRepository = { MavenArtifactRepository repository ->
    normalizeRepositoryUri(repository.url) == scopedMavenLocalRepositoryUri
}

def isForbiddenRepository = { MavenArtifactRepository repository ->
    def repositoryUrl = repository.url
    def host = repositoryUrl?.host?.toLowerCase(Locale.ROOT)
    def path = normalizeRepositoryPath(repositoryUrl)

    if (!host) {
        return false
    }

    if (forbiddenHosts.contains(host)) {
        return true
    }

    // Preserve JetBrains-specialized paths such as /intellij-dependencies.
    if (host == 'cache-redirector.jetbrains.com') {
        def upstreamHost = path.tokenize('/').find()
        return upstreamHost != null && forbiddenHosts.contains(upstreamHost)
    }

    return false
}

def restrictAtlassianRepository = { MavenArtifactRepository repository ->
    if (!isAtlassianPublicRepository(repository)) {
        return
    }

    // Microba is only available here; keep Atlassian only for that vendor group.
    repository.content {
        includeGroup 'com.michaelbaranov'
    }
}

def restrictScopedMavenLocalRepository = { repositories, MavenArtifactRepository repository ->
    if (!isScopedMavenLocalRepository(repository)) {
        return
    }

    repository.content {
        includeUtilsReactorModules(delegate)
    }

    if (!scopedMavenLocalExclusiveRegistered.add(repositories)) {
        return
    }

    repositories.exclusiveContent { spec ->
        spec.forRepositories(repository)
        spec.filter { contentFilter ->
            includeUtilsReactorModules(contentFilter)
        }
    }
}

def routeToCfs = { MavenArtifactRepository repository ->
    if (!isForbiddenRepository(repository)) {
        return
    }

    repository.url = uri(cfsUrl)
    repository.credentials {
        username = 'AzureDevOps'
        password = cfsToken
    }
}

def watchRepositories
watchRepositories = { repositories ->
    repositories.all { repository ->
        if (repository instanceof MavenArtifactRepository) {
            restrictScopedMavenLocalRepository(repositories, repository)
            restrictAtlassianRepository(repository)
            routeToCfs(repository)
        }
    }
}

def addCfsRepository = { repositories ->
    repositories.maven { repository ->
        repository.name = 'vscjava'
        repository.url = uri(cfsUrl)
        repository.credentials {
            username = 'AzureDevOps'
            password = cfsToken
        }
    }
}

def describeRepository = { MavenArtifactRepository repository ->
    "${repository.name ?: repository.displayName} -> ${repository.url}"
}

def assertNoForbiddenRepositories = { String scope, repositories ->
    def offendingRepositories = repositories.findAll { repository ->
        repository instanceof MavenArtifactRepository &&
            isForbiddenRepository(repository as MavenArtifactRepository)
    } as List<MavenArtifactRepository>

    if (!offendingRepositories.isEmpty()) {
        throw new GradleException(
            "Forbidden public Maven repository remained in ${scope}: " +
                offendingRepositories.collect(describeRepository).join(', '))
    }
}

beforeSettings { settings ->
    // Pipeline-only policy: route general-purpose public Maven traffic through authenticated CFS.
    settings.pluginManagement.repositories { repositories ->
        repositories.clear()
        watchRepositories(repositories)
        addCfsRepository(repositories)
    }

    // Preserve local and vendor-specific settings repositories while rewriting forbidden Maven sources.
    watchRepositories(settings.buildscript.repositories)
    watchRepositories(settings.dependencyResolutionManagement.repositories)
}

allprojects { project ->
    // Keep watching project and buildscript repositories because plugins can add them later.
    watchRepositories(project.repositories)
    watchRepositories(project.buildscript.repositories)
}

settingsEvaluated { settings ->
    assertNoForbiddenRepositories('plugin management repositories', settings.pluginManagement.repositories)
    assertNoForbiddenRepositories('settings buildscript repositories', settings.buildscript.repositories)
    assertNoForbiddenRepositories(
        'settings dependency resolution repositories',
        settings.dependencyResolutionManagement.repositories)
}

projectsEvaluated {
    gradle.rootProject.allprojects { project ->
        assertNoForbiddenRepositories("project ${project.path} repositories", project.repositories)
        assertNoForbiddenRepositories("project ${project.path} buildscript repositories", project.buildscript.repositories)
    }
}
```

- [ ] **Step 3: Verify fail-fast validation for both CFS credentials and the scoped local handoff**

Run:

```powershell
$testRoot = Join-Path (Get-Location) '.scratch\cfs-init-validation'
$projectDir = Join-Path $testRoot 'project'
$dummyRepo = Join-Path $testRoot 'scoped-m2'
Remove-Item $testRoot -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Path $projectDir, $dummyRepo -Force | Out-Null
Set-Content -Path (Join-Path $projectDir 'settings.gradle') -Value "rootProject.name = 'cfs-init-validation'"
Set-Content -Path (Join-Path $projectDir 'build.gradle') -Value ''

Push-Location 'PluginsAndFeatures\azure-toolkit-for-intellij'
try {
    $env:CFS_MAVEN_URL = 'https://example.invalid/vscjava/maven/v1'
    $env:SYSTEM_ACCESSTOKEN = 'test-token'
    $missingLocalOutput = .\gradlew.bat -p $projectDir help `
        --init-script '..\..\.azure-pipelines\cfs-init.gradle' `
        --no-daemon --no-configuration-cache 2>&1
    $missingLocalExitCode = $LASTEXITCODE

    Remove-Item Env:CFS_MAVEN_URL -ErrorAction SilentlyContinue
    Remove-Item Env:SYSTEM_ACCESSTOKEN -ErrorAction SilentlyContinue
    $missingCredentialsOutput = .\gradlew.bat -p $projectDir help `
        --init-script '..\..\.azure-pipelines\cfs-init.gradle' `
        --no-daemon --no-configuration-cache "-Dmaven.repo.local=$dummyRepo" 2>&1
    $missingCredentialsExitCode = $LASTEXITCODE
} finally {
    Pop-Location
    Remove-Item Env:CFS_MAVEN_URL -ErrorAction SilentlyContinue
    Remove-Item Env:SYSTEM_ACCESSTOKEN -ErrorAction SilentlyContinue
    Remove-Item $testRoot -Recurse -Force -ErrorAction SilentlyContinue
}

if ($missingLocalExitCode -eq 0 -or ($missingLocalOutput -join "`n") -notmatch 'maven\.repo\.local must be explicitly set') {
    throw "Expected the scoped local handoff validation error:`n$($missingLocalOutput -join "`n")"
}
if ($missingCredentialsExitCode -eq 0 -or ($missingCredentialsOutput -join "`n") -notmatch 'CFS_MAVEN_URL and SYSTEM_ACCESSTOKEN must both be set') {
    throw "Expected the CFS credential validation error:`n$($missingCredentialsOutput -join "`n")"
}
Write-Host 'PASS: Gradle rejects missing scoped local handoff and missing CFS credentials'
```

Expected: `PASS: Gradle rejects missing scoped local handoff and missing CFS credentials`.

- [ ] **Step 4: Verify the exact local allowlist stays synchronized with the Utils reactor**

Run:

```powershell
@'
import re
import xml.etree.ElementTree as ET
from pathlib import Path

root = Path.cwd()
utils_root = root / 'Utils'
ns = {'m': 'http://maven.apache.org/POM/4.0.0'}


def pom_coords(pom_path: Path):
    tree = ET.parse(pom_path)
    group = tree.findtext('m:groupId', namespaces=ns)
    if group is None:
        group = tree.findtext('m:parent/m:groupId', namespaces=ns)
    artifact = tree.findtext('m:artifactId', namespaces=ns)
    return group, artifact


reactor = []
for pom_path in [
    utils_root / 'pom.xml',
    utils_root / 'azure-toolkit-ide-libs' / 'pom.xml',
    utils_root / 'azure-toolkit-ide-hdinsight-libs' / 'pom.xml',
]:
    tree = ET.parse(pom_path)
    reactor.append(pom_coords(pom_path))
    for module in tree.findall('m:modules/m:module', ns):
        reactor.append(pom_coords(pom_path.parent / module.text / 'pom.xml'))

reactor = list(dict.fromkeys(reactor))
allowlist = re.findall(r"\['([^']+)', '([^']+)'\]", (root / '.azure-pipelines' / 'cfs-init.gradle').read_text(encoding='utf-8'))
missing = sorted(set(reactor) - set(allowlist))
extra = sorted(set(allowlist) - set(reactor))

if len(reactor) != 28:
    raise SystemExit(f'Expected 28 Utils reactor coordinates, found {len(reactor)}: {reactor}')
if missing or extra:
    raise SystemExit(f'Allowlist drift detected. Missing={missing} Extra={extra}')

print('PASS: Gradle allowlist exactly matches all 28 Utils reactor coordinates')
'@ | python -
```

Expected: `PASS: Gradle allowlist exactly matches all 28 Utils reactor coordinates`.

- [ ] **Step 5: Verify normalized scoped-local matching, future handler coverage, exclusive provenance, and no fallback**

Run:

```powershell
$testRoot = Join-Path (Get-Location) '.scratch\cfs-init-provenance'
$projectDir = Join-Path $testRoot 'project'
$scopedRepo = Join-Path $testRoot 'scoped-m2'
$cfsRepo = Join-Path $testRoot 'cfs-m2'
Remove-Item $testRoot -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Path $projectDir, $scopedRepo, $cfsRepo -Force | Out-Null

function New-MavenStubArtifact {
    param(
        [string]$RepoRoot,
        [string]$GroupId,
        [string]$ArtifactId,
        [string]$Version
    )

    $groupPath = $GroupId -replace '\.', '\\'
    $artifactDir = Join-Path $RepoRoot "$groupPath\$ArtifactId\$Version"
    New-Item -ItemType Directory -Path $artifactDir -Force | Out-Null
    @"
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>
  <groupId>$GroupId</groupId>
  <artifactId>$ArtifactId</artifactId>
  <version>$Version</version>
</project>
"@ | Set-Content -Path (Join-Path $artifactDir "$ArtifactId-$Version.pom")
    [System.IO.File]::WriteAllBytes((Join-Path $artifactDir "$ArtifactId-$Version.jar"), [byte[]]@())
}

New-MavenStubArtifact $scopedRepo 'com.microsoft.azure' 'azure-toolkit-ide-common-lib' '1.0.0-test'
New-MavenStubArtifact $scopedRepo 'org.example' 'warmed-cache-only' '1.0.0-test'
New-MavenStubArtifact $scopedRepo 'com.microsoft.azure' 'azure-toolkit-common-lib' '1.0.0-test'

New-MavenStubArtifact $cfsRepo 'org.example' 'warmed-cache-only' '1.0.0-test'
New-MavenStubArtifact $cfsRepo 'com.microsoft.azure' 'azure-toolkit-common-lib' '1.0.0-test'
New-MavenStubArtifact $cfsRepo 'com.microsoft.azure' 'azure-toolkit-ide-appservice-lib' '1.0.0-test'

Set-Content -Path (Join-Path $projectDir 'settings.gradle') -Value "rootProject.name = 'cfs-init-provenance'"
@'
def scopedRepo = new File(System.getProperty('maven.repo.local')).canonicalFile

repositories {
    maven {
        name = 'manualScopedVariant'
        url = uri(scopedRepo.toURI().toString() + '../' + scopedRepo.name + '/./')
    }
    mavenCentral()
    maven {
        name = 'intellijVendor'
        url = uri('https://cache-redirector.jetbrains.com/intellij-dependencies')
    }
    maven {
        name = 'atlassianPublic'
        url = uri('https://maven.atlassian.com/repository/public')
    }
}

afterEvaluate {
    repositories.mavenLocal()
    repositories.maven {
        name = 'lateForbidden'
        url = uri('https://cache-redirector.jetbrains.com/repo1.maven.org/maven2')
    }
}

configurations {
    localProbe
    thirdPartyProbe
    microsoftProbe
    missingLocalProbe
}

dependencies {
    localProbe 'com.microsoft.azure:azure-toolkit-ide-common-lib:1.0.0-test'
    thirdPartyProbe 'org.example:warmed-cache-only:1.0.0-test'
    microsoftProbe 'com.microsoft.azure:azure-toolkit-common-lib:1.0.0-test'
    missingLocalProbe 'com.microsoft.azure:azure-toolkit-ide-appservice-lib:1.0.0-test'
}

tasks.register('printRepositories') {
    doLast {
        repositories.each { repository ->
            def location = repository.hasProperty('url') ? repository.url : repository.name
            println("REPOSITORY=${repository.name}|${location}")
        }
    }
}

tasks.register('printProvenance') {
    doLast {
        [
            localProbe: configurations.localProbe.singleFile,
            thirdPartyProbe: configurations.thirdPartyProbe.singleFile,
            microsoftProbe: configurations.microsoftProbe.singleFile,
        ].each { name, file ->
            println("PROVENANCE=${name}|${file}")
        }
    }
}

tasks.register('resolveMissingLocal') {
    doLast {
        configurations.missingLocalProbe.resolve()
    }
}
'@ | Set-Content -Path (Join-Path $projectDir 'build.gradle')

$cfsRepoUri = 'file:///' + ((Resolve-Path $cfsRepo).Path -replace '\\', '/')
if (-not $cfsRepoUri.EndsWith('/')) {
    $cfsRepoUri += '/'
}
$env:CFS_MAVEN_URL = $cfsRepoUri
$env:SYSTEM_ACCESSTOKEN = 'test-token'

Push-Location 'PluginsAndFeatures\azure-toolkit-for-intellij'
try {
    $successOutput = .\gradlew.bat -p $projectDir printRepositories printProvenance `
        --init-script '..\..\.azure-pipelines\cfs-init.gradle' `
        --no-daemon --no-configuration-cache "-Dmaven.repo.local=$scopedRepo" 2>&1
    $successExitCode = $LASTEXITCODE

    $failureOutput = .\gradlew.bat -p $projectDir resolveMissingLocal `
        --init-script '..\..\.azure-pipelines\cfs-init.gradle' `
        --no-daemon --no-configuration-cache "-Dmaven.repo.local=$scopedRepo" 2>&1
    $failureExitCode = $LASTEXITCODE
} finally {
    Pop-Location
    Remove-Item Env:CFS_MAVEN_URL -ErrorAction SilentlyContinue
    Remove-Item Env:SYSTEM_ACCESSTOKEN -ErrorAction SilentlyContinue
    Remove-Item $testRoot -Recurse -Force -ErrorAction SilentlyContinue
}

if ($successExitCode -ne 0) {
    throw "Gradle provenance test failed:`n$($successOutput -join "`n")"
}
$successText = $successOutput -join "`n"
$localJar = Join-Path $scopedRepo 'com\microsoft\azure\azure-toolkit-ide-common-lib\1.0.0-test\azure-toolkit-ide-common-lib-1.0.0-test.jar'
$thirdPartyCfsJar = Join-Path $cfsRepo 'org\example\warmed-cache-only\1.0.0-test\warmed-cache-only-1.0.0-test.jar'
$msCfsJar = Join-Path $cfsRepo 'com\microsoft\azure\azure-toolkit-common-lib\1.0.0-test\azure-toolkit-common-lib-1.0.0-test.jar'

if ($successText -notmatch [regex]::Escape("REPOSITORY=lateForbidden|$cfsRepoUri")) {
    throw "Late-added forbidden repository was not rewritten to CFS:`n$successText"
}
if ($successText -notmatch 'REPOSITORY=atlassianPublic\|https://maven\.atlassian\.com/repository/public') {
    throw "Atlassian exception was not preserved:`n$successText"
}
if ($successText -notmatch 'REPOSITORY=intellijVendor\|https://cache-redirector\.jetbrains\.com/intellij-dependencies') {
    throw "JetBrains vendor repository was not preserved:`n$successText"
}
if ($successText -notmatch [regex]::Escape("PROVENANCE=localProbe|$localJar")) {
    throw "Allowlisted Utils module did not resolve from the scoped local handoff:`n$successText"
}
if ($successText -notmatch [regex]::Escape("PROVENANCE=thirdPartyProbe|$thirdPartyCfsJar")) {
    throw "Third-party dependency did not resolve from CFS:`n$successText"
}
if ($successText -notmatch [regex]::Escape("PROVENANCE=microsoftProbe|$msCfsJar")) {
    throw "Non-reactor Microsoft dependency did not resolve from CFS:`n$successText"
}
if ($successText -match [regex]::Escape((Join-Path $scopedRepo 'org\example\warmed-cache-only'))) {
    throw "Third-party warmed-cache content leaked through the scoped local handoff:`n$successText"
}
if ($successText -match [regex]::Escape((Join-Path $scopedRepo 'com\microsoft\azure\azure-toolkit-common-lib'))) {
    throw "Non-reactor Microsoft warmed-cache content leaked through the scoped local handoff:`n$successText"
}
if ($failureExitCode -eq 0) {
    throw 'Expected an allowlisted module that is absent locally to fail'
}
if (($failureOutput -join "`n") -notmatch 'Could not find com\.microsoft\.azure:azure-toolkit-ide-appservice-lib:1\.0\.0-test') {
    throw "Gradle failed for an unexpected reason:`n$($failureOutput -join "`n")"
}
Write-Host 'PASS: Gradle keeps the scoped local handoff exclusive to allowlisted Utils modules'
```

Expected: `PASS: Gradle keeps the scoped local handoff exclusive to allowlisted Utils modules`.

- [ ] **Step 6: Commit the Gradle policy**

Run:

```powershell
git add -- .azure-pipelines/cfs-init.gradle
git commit -m "build: route Gradle packages through CFS" -m "Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

Expected: one commit containing the Gradle init script.

### Task 3: Wire CFS into the Stable Release Pipeline

**Files:**
- Modify: `.azure-pipelines/sign-for-stable-release.yml:2-4`
- Modify: `.azure-pipelines/sign-for-stable-release.yml:68-75`
- Modify: `.azure-pipelines/sign-for-stable-release.yml:125-137`

- [ ] **Step 1: Verify the pipeline is not isolated yet**

Run:

```powershell
$pipeline = Get-Content '.azure-pipelines\sign-for-stable-release.yml' -Raw
if ($pipeline -notmatch 'disableNetworkIsolation:\s*true') {
    throw 'Expected the existing network-isolation opt-out'
}
if ($pipeline -match 'cfs-variables\.yml|cfs-settings\.xml|cfs-init\.gradle') {
    throw 'Expected CFS wiring to be absent before this task'
}
Write-Host 'PASS: pipeline still has the expected pre-change behavior'
```

Expected: `PASS: pipeline still has the expected pre-change behavior`.

- [ ] **Step 2: Import the shared CFS variables**

Add this entry after the `Codeql.Enabled` variable in
`.azure-pipelines/sign-for-stable-release.yml`:

```yaml
  - template: /.azure-pipelines/cfs-variables.yml@self
```

- [ ] **Step 3: Enable 1ES Network Isolation**

Remove this block from the `extends.parameters` section:

```yaml
    featureFlags:
      disableNetworkIsolation: true
```

Leave the existing `pool` and all stages unchanged.

- [ ] **Step 4: Apply the Maven and Gradle policies to the build step**

Replace the `Build Plugin` script and environment block with:

```yaml
              - task: PowerShell@2
                displayName: Build Plugin
                inputs:
                  targetType: inline
                  script: |
                    mvn -v
                    # ./gradlew buildUtils || exit -1
                    mvn -Dmaven.repo.local="$env:CFS_MAVEN_LOCAL_REPOSITORY" -s "$(Build.SourcesDirectory)\.azure-pipelines\cfs-settings.xml" clean install -f "$(Build.SourcesDirectory)\Utils\pom.xml" -T 1C "-Dcheckstyle.skip=true" "-Dmaven.test.skip=true" "-Dmaven.javadoc.skip=true"
                    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
                    cd PluginsAndFeatures/azure-toolkit-for-intellij
                    ./gradlew -Dmaven.repo.local="$env:CFS_MAVEN_LOCAL_REPOSITORY" -I "$(Build.SourcesDirectory)\.azure-pipelines\cfs-init.gradle" clean buildPlugin -s "-Papplicationinsights.key=$(INTELLIJ_KEY)" "-PneedPatchVersion=false" "-Psources=false" "-Porg.gradle.configureondemand=false" "-Porg.gradle.daemon=false" "-Porg.gradle.unsafe.configuration-cache=false" "-Porg.gradle.caching=false"
                env:
                  USE_STABLE_VERSION: $(IsStableBuild)
                  CFS_MAVEN_LOCAL_REPOSITORY: $(Agent.TempDirectory)\azure-tools-maven-repository
                  CFS_MAVEN_URL: $(CFS_MAVEN_URL)
                  SYSTEM_ACCESSTOKEN: $(System.AccessToken)
```

- [ ] **Step 5: Run static pipeline assertions**

Run:

```powershell
$pipeline = Get-Content '.azure-pipelines\sign-for-stable-release.yml' -Raw
$required = @(
    '/.azure-pipelines/cfs-variables.yml@self',
    '-Dmaven.repo.local="$env:CFS_MAVEN_LOCAL_REPOSITORY" -s "$(Build.SourcesDirectory)\.azure-pipelines\cfs-settings.xml"',
    '-Dmaven.repo.local="$env:CFS_MAVEN_LOCAL_REPOSITORY" -I "$(Build.SourcesDirectory)\.azure-pipelines\cfs-init.gradle"',
    'CFS_MAVEN_LOCAL_REPOSITORY: $(Agent.TempDirectory)\azure-tools-maven-repository',
    'CFS_MAVEN_URL: $(CFS_MAVEN_URL)',
    'SYSTEM_ACCESSTOKEN: $(System.AccessToken)'
)
foreach ($value in $required) {
    if (-not $pipeline.Contains($value)) {
        throw "Missing pipeline wiring: $value"
    }
}
if ($pipeline -match 'disableNetworkIsolation') {
    throw 'Network isolation is still disabled'
}
if (($pipeline | Select-String -Pattern 'SYSTEM_ACCESSTOKEN:' -AllMatches).Matches.Count -ne 1) {
    throw 'System.AccessToken must be scoped to exactly one build step'
}
if (($pipeline | Select-String -Pattern 'CFS_MAVEN_LOCAL_REPOSITORY:' -AllMatches).Matches.Count -ne 1) {
    throw 'CFS_MAVEN_LOCAL_REPOSITORY must be scoped to exactly one build step'
}
if (($pipeline | Select-String -Pattern '-Dmaven\.repo\.local=' -AllMatches).Matches.Count -ne 2) {
    throw 'Expected exactly two maven.repo.local command-line properties'
}
Write-Host 'PASS: stable release pipeline CFS wiring and scoped local handoff are complete'
```

Expected: `PASS: stable release pipeline CFS wiring and scoped local handoff are complete`.

- [ ] **Step 6: Commit the pipeline wiring**

Run:

```powershell
git add -- .azure-pipelines/sign-for-stable-release.yml
git commit -m "build: enable stable release network isolation" -m "Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

Expected: one commit containing only the stable release pipeline update.

### Task 4: Verify Behavior and Compliance

**Files:**
- Verify: `.azure-pipelines/cfs-variables.yml`
- Verify: `.azure-pipelines/cfs-settings.xml`
- Verify: `.azure-pipelines/cfs-init.gradle`
- Verify: `.azure-pipelines/sign-for-stable-release.yml`

- [ ] **Step 1: Check formatting, source cleanliness, and the final change set**

Run:

```powershell
git --no-pager diff --check d116c373b^..HEAD
git --no-pager status --short
git --no-pager status --short --ignored
git --no-pager log --oneline --decorate --reverse d116c373b^..HEAD
```

Expected:

- `git diff --check` prints no errors.
- `git status --short` is empty, confirming tracked/untracked source cleanliness.
- `git status --short --ignored` is reported separately and may still list ignored
  build/cache outputs; those do not count as source changes.
- The log includes these milestones in order: design `d116c373b`, plan
  `afcae1c19`, Maven primary `849056f7f`, Gradle primary `fcc8d0cc9`, Gradle
  reviewer cache fix `fc6f8293f`, pipeline primary `946b7378f`, Atlassian
  reviewer fix `04c5fbedb`, verification-plan correction `75fc90433`,
  verification-range correction `96300b929`, scoped local handoff `2464ba5ef`,
  and exclusive handoff `73c9f421c3`.
- Later documentation commits, including `98d5c48ea0` and the current docs
  sync commit, may follow after those implementation milestones. Do not rely on
  the total number of log entries.

- [ ] **Step 2: Verify local Gradle configuration is unaffected**

Run:

```powershell
Push-Location 'PluginsAndFeatures\azure-toolkit-for-intellij'
try {
    .\gradlew.bat help --no-daemon --no-configuration-cache
    if ($LASTEXITCODE -ne 0) {
        exit $LASTEXITCODE
    }
} finally {
    Pop-Location
}
```

Expected: `BUILD SUCCESSFUL`. The command does not reference `cfs-init.gradle` and
does not require `CFS_MAVEN_URL` or `SYSTEM_ACCESSTOKEN`.

- [ ] **Step 3: Re-run the policy tests**

Repeat Task 2 Steps 3 through 5.

Expected:

- Missing `maven.repo.local` and missing CFS credentials both fail fast with the
  explicit configuration errors.
- The extracted allowlist exactly matches all 28 Utils reactor coordinates,
  including the parent/aggregator POMs.
- Allowlisted Utils modules resolve from the scoped
  `$(Agent.TempDirectory)\azure-tools-maven-repository` handoff.
- Warmed third-party and non-reactor Microsoft coordinates resolve from CFS even
  when matching artifacts exist in the scoped local repository.
- An allowlisted module that is absent locally fails rather than falling back to
  CFS.
- JetBrains vendor repositories and the Atlassian Microba exception remain
  scoped direct exceptions.

- [ ] **Step 4: Preview or run the Azure Pipeline**

Use the Azure Pipelines definition backed by
`.azure-pipelines/sign-for-stable-release.yml` to compile the updated YAML and run a
non-publishing validation build from the implementation branch. Set
`ForceRealSign=false` and `ForceTestSignRelease=false`.

Expected:

- YAML compilation succeeds.
- `Build_Plugin.Build_and_Sign` completes.
- `Release_Plugin` is skipped.
- Maven logs show the `vscjava` mirror and `-Dmaven.repo.local` pointing at
  `$(Agent.TempDirectory)\azure-tools-maven-repository`.
- Gradle resolves allowlisted Utils reactor coordinates from
  `azure-tools-maven-repository`.
- Gradle resolves ordinary third-party and non-reactor Microsoft coordinates
  from `pkgs.dev.azure.com` rather than Maven Local.
- JDK, Gradle distribution, JetBrains-specific endpoints, and the Atlassian
  Microba exception remain the only direct package-source exceptions.

If Azure Pipelines access is unavailable, record this as the only unverified item;
do not claim CFS feed coverage or policy compliance from local checks alone.

- [ ] **Step 5: Inspect 1ES network policy telemetry**

Inspect the validation run's 1ES policy results.

Expected:

- `CFSClean`: zero findings.
- `CFSClean2`: zero findings.
- `CFSClean3`: zero findings.
- Maven Central, Gradle Plugin Portal, Sonatype general-purpose endpoints, and
  Maven Central public mirrors: zero requests.
- Remaining `DefaultDeny` entries correspond only to JDK 25 acquisition, the
  Gradle distribution, JetBrains/IntelliJ artifacts, the Atlassian Microba
  exception when exercised, signing, or Marketplace publishing.

If a general-purpose package source remains, add it to the Gradle script's forbidden
source classification and repeat Tasks 2 through 4. Do not add a broad direct-access
exception.
