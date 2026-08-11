# Stable Release Network Isolation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Enable 1ES Network Isolation for the stable release pipeline and force ordinary Maven and Gradle package restores through the `vscjava` Central Feed Service.

**Architecture:** Add pipeline-only Maven and Gradle policy files under `.azure-pipelines`. Maven mirrors `central` to CFS; a Gradle init script redirects general-purpose public repositories to CFS while preserving Maven Local and specialized vendor repositories. The release pipeline imports the non-secret feed URL, maps `System.AccessToken` only into the build step, and removes the network-isolation opt-out.

**Tech Stack:** Azure Pipelines YAML, Maven settings XML, Gradle 9.1 Groovy init scripts, PowerShell, 1ES Pipeline Templates

---

## File Structure

- Create `.azure-pipelines/cfs-variables.yml`: non-secret CFS endpoint shared by Maven and Gradle.
- Create `.azure-pipelines/cfs-settings.xml`: pipeline-only Maven mirror and server configuration.
- Create `.azure-pipelines/cfs-init.gradle`: pipeline-only Gradle repository redirection, validation, and fail-closed behavior.
- Modify `.azure-pipelines/sign-for-stable-release.yml`: enable network isolation and apply the CFS policies to the build step.
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

### Task 2: Add the Gradle Repository Policy

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
import org.gradle.api.artifacts.repositories.MavenArtifactRepository

/*
 * Pipeline-only repository policy for 1ES Network Isolation.
 *
 * General-purpose Maven repositories are redirected to CFS. Maven Local and
 * repositories that serve CFS-incompatible vendor artifacts remain unchanged.
 * The script is applied only by sign-for-stable-release.yml, so local developer
 * builds keep their existing repository configuration.
 */

def cfsUrl = System.getenv('CFS_MAVEN_URL')
def cfsToken = System.getenv('SYSTEM_ACCESSTOKEN')

if (!cfsUrl?.trim() || !cfsToken?.trim()) {
    throw new GradleException(
        'CFS_MAVEN_URL and SYSTEM_ACCESSTOKEN must both be set when cfs-init.gradle is applied. ' +
        'Failing instead of restoring packages from a public repository.')
}

def forbiddenHosts = [
    'repo.maven.apache.org',
    'repo1.maven.org',
    'plugins.gradle.org',
    'plugins-artifacts.gradle.org',
    'oss.sonatype.org',
    's01.oss.sonatype.org',
    'maven-central.storage-download.googleapis.com',
] as Set

def isForbiddenRepository = { URI uri ->
    def host = uri?.host?.toLowerCase(Locale.ROOT)
    def path = uri?.path?.toLowerCase(Locale.ROOT) ?: ''
    host in forbiddenHosts ||
        (host == 'cache-redirector.jetbrains.com' &&
            (path.startsWith('/repo1.maven.org/') || path.startsWith('/plugins.gradle.org/')))
}

def routeToCfs = { MavenArtifactRepository repository ->
    if (isForbiddenRepository(repository.url)) {
        repository.setUrl(cfsUrl)
        repository.credentials { credentials ->
            credentials.username = 'AzureDevOps'
            credentials.password = cfsToken
        }
    }
}

def watchRepositories = { handler ->
    handler.withType(MavenArtifactRepository).configureEach { repository ->
        routeToCfs(repository)
    }
}

def addCfsRepository = { handler ->
    handler.maven { repository ->
        repository.name = 'vscjava'
        repository.setUrl(cfsUrl)
        repository.credentials { credentials ->
            credentials.username = 'AzureDevOps'
            credentials.password = cfsToken
        }
    }
}

def assertNoForbiddenRepositories = { String owner, handler ->
    def forbidden = handler.withType(MavenArtifactRepository).findAll { repository ->
        isForbiddenRepository(repository.url)
    }
    if (!forbidden.isEmpty()) {
        def descriptions = forbidden.collect { repository ->
            "${repository.name} (${repository.url})"
        }.join(', ')
        throw new GradleException(
            "Forbidden public repositories remain in ${owner}: ${descriptions}")
    }
}

beforeSettings { settings ->
    settings.pluginManagement.repositories { repositories ->
        repositories.clear()
        watchRepositories(repositories)
        addCfsRepository(repositories)
    }

    watchRepositories(settings.dependencyResolutionManagement.repositories)
}

allprojects { project ->
    watchRepositories(project.repositories)
    watchRepositories(project.buildscript.repositories)
}

settingsEvaluated { settings ->
    assertNoForbiddenRepositories(
        'plugin management',
        settings.pluginManagement.repositories)
    assertNoForbiddenRepositories(
        'dependency resolution management',
        settings.dependencyResolutionManagement.repositories)
}

projectsEvaluated {
    gradle.rootProject.allprojects { project ->
        assertNoForbiddenRepositories(
            "${project.path} project repositories",
            project.repositories)
        assertNoForbiddenRepositories(
            "${project.path} buildscript repositories",
            project.buildscript.repositories)
    }
}
```

- [ ] **Step 3: Verify missing credentials fail before project evaluation**

Run:

```powershell
$tempProject = Join-Path $env:TEMP 'azure-tools-cfs-init-test'
if (Test-Path $tempProject) {
    Remove-Item $tempProject -Recurse -Force
}
New-Item -ItemType Directory -Path $tempProject | Out-Null
Set-Content -Path (Join-Path $tempProject 'settings.gradle') -Value "rootProject.name = 'cfs-init-test'"
Set-Content -Path (Join-Path $tempProject 'build.gradle') -Value ''

Remove-Item Env:CFS_MAVEN_URL -ErrorAction SilentlyContinue
Remove-Item Env:SYSTEM_ACCESSTOKEN -ErrorAction SilentlyContinue

Push-Location 'PluginsAndFeatures\azure-toolkit-for-intellij'
try {
    $output = .\gradlew.bat -p $tempProject help `
        --init-script '..\..\.azure-pipelines\cfs-init.gradle' `
        --no-daemon --no-configuration-cache 2>&1
    $exitCode = $LASTEXITCODE
} finally {
    Pop-Location
}

if ($exitCode -eq 0) {
    throw 'Expected Gradle to reject missing CFS configuration'
}
if (($output -join "`n") -notmatch 'CFS_MAVEN_URL and SYSTEM_ACCESSTOKEN must both be set') {
    throw "Gradle failed for an unexpected reason:`n$($output -join "`n")"
}
Write-Host 'PASS: Gradle policy fails closed when credentials are missing'
```

Expected: `PASS: Gradle policy fails closed when credentials are missing`.

- [ ] **Step 4: Verify public repositories are redirected and vendor/local repositories remain**

Run:

```powershell
$tempProject = Join-Path $env:TEMP 'azure-tools-cfs-init-test'
@'
repositories {
    mavenCentral()
    mavenLocal()
    maven { url = uri('https://cache-redirector.jetbrains.com/repo1.maven.org/maven2') }
    maven { url = uri('https://cache-redirector.jetbrains.com/intellij-dependencies') }
    maven { url = uri('https://maven.atlassian.com/repository/public') }
}

tasks.register('printRepositories') {
    doLast {
        repositories.each { repository ->
            def location = repository.hasProperty('url') ? repository.url : repository.name
            println("REPOSITORY=${repository.name}|${location}")
        }
    }
}
'@ | Set-Content -Path (Join-Path $tempProject 'build.gradle')

$env:CFS_MAVEN_URL = 'https://example.invalid/vscjava/maven/v1'
$env:SYSTEM_ACCESSTOKEN = 'test-token'

Push-Location 'PluginsAndFeatures\azure-toolkit-for-intellij'
try {
    $output = .\gradlew.bat -p $tempProject printRepositories `
        --init-script '..\..\.azure-pipelines\cfs-init.gradle' `
        --no-daemon --no-configuration-cache 2>&1
    $exitCode = $LASTEXITCODE
} finally {
    Pop-Location
    Remove-Item Env:CFS_MAVEN_URL -ErrorAction SilentlyContinue
    Remove-Item Env:SYSTEM_ACCESSTOKEN -ErrorAction SilentlyContinue
    Remove-Item $tempProject -Recurse -Force
}

if ($exitCode -ne 0) {
    throw "Gradle repository test failed:`n$($output -join "`n")"
}
$text = $output -join "`n"
if ($text -match 'repo\.maven\.apache\.org|repo1\.maven\.org') {
    throw "A forbidden repository remains:`n$text"
}
if ($text -notmatch 'https://example\.invalid/vscjava/maven/v1') {
    throw "CFS replacement was not present:`n$text"
}
if ($text -notmatch 'cache-redirector\.jetbrains\.com/intellij-dependencies') {
    throw "The IntelliJ dependency repository was not preserved:`n$text"
}
if ($text -notmatch 'maven\.atlassian\.com/repository/public') {
    throw "The Atlassian repository was not preserved:`n$text"
}
if ($text -notmatch 'MavenLocal') {
    throw "Maven Local was not preserved:`n$text"
}
Write-Host 'PASS: Gradle repositories follow the approved policy'
```

Expected: `PASS: Gradle repositories follow the approved policy`.

- [ ] **Step 5: Commit the Gradle policy**

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
                    mvn clean install --settings "$(Build.SourcesDirectory)\.azure-pipelines\cfs-settings.xml" -f ./Utils/pom.xml -T 1C "-Dcheckstyle.skip=true" "-Dmaven.test.skip=true" "-Dmaven.javadoc.skip=true"
                    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
                    cd PluginsAndFeatures/azure-toolkit-for-intellij
                    ./gradlew clean buildPlugin --init-script "$(Build.SourcesDirectory)\.azure-pipelines\cfs-init.gradle" -s "-Papplicationinsights.key=$(INTELLIJ_KEY)" "-PneedPatchVersion=false" "-Psources=false" "-Porg.gradle.configureondemand=false" "-Porg.gradle.daemon=false" "-Porg.gradle.unsafe.configuration-cache=false" "-Porg.gradle.caching=false"
                env:
                  USE_STABLE_VERSION: $(IsStableBuild)
                  CFS_MAVEN_URL: $(CFS_MAVEN_URL)
                  SYSTEM_ACCESSTOKEN: $(System.AccessToken)
```

- [ ] **Step 5: Run static pipeline assertions**

Run:

```powershell
$pipeline = Get-Content '.azure-pipelines\sign-for-stable-release.yml' -Raw
$required = @(
    '/.azure-pipelines/cfs-variables.yml@self',
    '--settings "$(Build.SourcesDirectory)\.azure-pipelines\cfs-settings.xml"',
    '--init-script "$(Build.SourcesDirectory)\.azure-pipelines\cfs-init.gradle"',
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
Write-Host 'PASS: stable release pipeline CFS wiring is complete'
```

Expected: `PASS: stable release pipeline CFS wiring is complete`.

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
git --no-pager diff --check d116c373d..HEAD
git --no-pager status --short
git --no-pager status --short --ignored
git --no-pager log --oneline --decorate --reverse d116c373d..HEAD
```

Expected:

- `git diff --check` prints no errors.
- `git status --short` is empty, confirming tracked/untracked source cleanliness.
- `git status --short --ignored` is reported separately and may still list ignored
  build/cache outputs; those do not count as source changes.
- The log shows the design commit, plan commit, three primary implementation
  commits, and two reviewer-requested Gradle policy correction commits.

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

Repeat Task 2 Steps 3 and 4.

Expected:

- Missing credentials fail with the explicit CFS configuration error.
- General-purpose public repositories are replaced with CFS.
- Maven Local, IntelliJ dependencies, and Atlassian remain available.

- [ ] **Step 4: Preview or run the Azure Pipeline**

Use the Azure Pipelines definition backed by
`.azure-pipelines/sign-for-stable-release.yml` to compile the updated YAML and run a
non-publishing validation build from the implementation branch. Set
`ForceRealSign=false` and `ForceTestSignRelease=false`.

Expected:

- YAML compilation succeeds.
- `Build_Plugin.Build_and_Sign` completes.
- `Release_Plugin` is skipped.
- Maven logs show the `vscjava` mirror.
- Gradle resolves general-purpose packages from `pkgs.dev.azure.com`.
- JDK, Gradle distribution, and JetBrains-specific endpoints remain the only direct
  public artifact endpoints.

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
- Remaining `DefaultDeny` entries correspond only to JDK 25 acquisition, the Gradle
  distribution, JetBrains/IntelliJ artifacts, signing, or Marketplace publishing.

If a general-purpose package source remains, add it to the Gradle script's forbidden
source classification and repeat Tasks 2 through 4. Do not add a broad direct-access
exception.
