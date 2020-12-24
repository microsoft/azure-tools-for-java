package org.jetbrains.mock

import com.microsoft.azure.management.appservice.*
import com.microsoft.azure.management.appservice.implementation.AppServiceManager
import com.microsoft.azure.management.appservice.implementation.SiteInner
import com.microsoft.azure.management.resources.fluentcore.arm.Region
import org.joda.time.DateTime
import rx.Completable
import rx.Observable
import java.io.File
import java.io.InputStream

class FunctionDeploymentSlotMock(
        private val id: String = "test-function-deployment-slot-id",
        private val name: String = "test-function-deployment-slot",
        private val parent: FunctionApp? = null
) : FunctionDeploymentSlot {

    override fun key(): String {
        TODO("Not yet implemented")
    }

    override fun id(): String = id

    override fun name(): String = name

    override fun type(): String {
        TODO("Not yet implemented")
    }

    override fun regionName(): String {
        TODO("Not yet implemented")
    }

    override fun region(): Region {
        TODO("Not yet implemented")
    }

    override fun tags(): MutableMap<String, String> {
        TODO("Not yet implemented")
    }

    override fun resourceGroupName(): String {
        TODO("Not yet implemented")
    }

    override fun manager(): AppServiceManager {
        TODO("Not yet implemented")
    }

    override fun inner(): SiteInner {
        TODO("Not yet implemented")
    }

    override fun state(): String {
        TODO("Not yet implemented")
    }

    override fun hostNames(): MutableSet<String> {
        TODO("Not yet implemented")
    }

    override fun repositorySiteName(): String {
        TODO("Not yet implemented")
    }

    override fun usageState(): UsageState {
        TODO("Not yet implemented")
    }

    override fun enabled(): Boolean {
        TODO("Not yet implemented")
    }

    override fun enabledHostNames(): MutableSet<String> {
        TODO("Not yet implemented")
    }

    override fun availabilityState(): SiteAvailabilityState {
        TODO("Not yet implemented")
    }

    override fun hostNameSslStates(): MutableMap<String, HostNameSslState> {
        TODO("Not yet implemented")
    }

    override fun appServicePlanId(): String {
        TODO("Not yet implemented")
    }

    override fun lastModifiedTime(): DateTime {
        TODO("Not yet implemented")
    }

    override fun trafficManagerHostNames(): MutableSet<String> {
        TODO("Not yet implemented")
    }

    override fun scmSiteAlsoStopped(): Boolean {
        TODO("Not yet implemented")
    }

    override fun targetSwapSlot(): String {
        TODO("Not yet implemented")
    }

    override fun clientAffinityEnabled(): Boolean {
        TODO("Not yet implemented")
    }

    override fun clientCertEnabled(): Boolean {
        TODO("Not yet implemented")
    }

    override fun hostNamesDisabled(): Boolean {
        TODO("Not yet implemented")
    }

    override fun outboundIPAddresses(): MutableSet<String> {
        TODO("Not yet implemented")
    }

    override fun containerSize(): Int {
        TODO("Not yet implemented")
    }

    override fun cloningInfo(): CloningInfo {
        TODO("Not yet implemented")
    }

    override fun isDefaultContainer(): Boolean {
        TODO("Not yet implemented")
    }

    override fun defaultHostName(): String {
        TODO("Not yet implemented")
    }

    override fun defaultDocuments(): MutableList<String> {
        TODO("Not yet implemented")
    }

    override fun netFrameworkVersion(): NetFrameworkVersion {
        TODO("Not yet implemented")
    }

    override fun phpVersion(): PhpVersion {
        TODO("Not yet implemented")
    }

    override fun pythonVersion(): PythonVersion {
        TODO("Not yet implemented")
    }

    override fun nodeVersion(): String {
        TODO("Not yet implemented")
    }

    override fun remoteDebuggingEnabled(): Boolean {
        TODO("Not yet implemented")
    }

    override fun remoteDebuggingVersion(): RemoteVisualStudioVersion {
        TODO("Not yet implemented")
    }

    override fun webSocketsEnabled(): Boolean {
        TODO("Not yet implemented")
    }

    override fun alwaysOn(): Boolean {
        TODO("Not yet implemented")
    }

    override fun javaVersion(): JavaVersion {
        TODO("Not yet implemented")
    }

    override fun javaContainer(): String {
        TODO("Not yet implemented")
    }

    override fun javaContainerVersion(): String {
        TODO("Not yet implemented")
    }

    override fun managedPipelineMode(): ManagedPipelineMode {
        TODO("Not yet implemented")
    }

    override fun autoSwapSlotName(): String {
        TODO("Not yet implemented")
    }

    override fun httpsOnly(): Boolean {
        TODO("Not yet implemented")
    }

    override fun ftpsState(): FtpsState {
        TODO("Not yet implemented")
    }

    override fun virtualApplications(): MutableList<VirtualApplication> {
        TODO("Not yet implemented")
    }

    override fun http20Enabled(): Boolean {
        TODO("Not yet implemented")
    }

    override fun minTlsVersion(): SupportedTlsVersions {
        TODO("Not yet implemented")
    }

    override fun localMySqlEnabled(): Boolean {
        TODO("Not yet implemented")
    }

    override fun scmType(): ScmType {
        TODO("Not yet implemented")
    }

    override fun documentRoot(): String {
        TODO("Not yet implemented")
    }

    override fun systemAssignedManagedServiceIdentityTenantId(): String {
        TODO("Not yet implemented")
    }

    override fun systemAssignedManagedServiceIdentityPrincipalId(): String {
        TODO("Not yet implemented")
    }

    override fun userAssignedManagedServiceIdentityIds(): MutableSet<String> {
        TODO("Not yet implemented")
    }

    override fun getAppSettings(): MutableMap<String, AppSetting> {
        TODO("Not yet implemented")
    }

    override fun getAppSettingsAsync(): Observable<MutableMap<String, AppSetting>> {
        TODO("Not yet implemented")
    }

    override fun getConnectionStrings(): MutableMap<String, ConnectionString> {
        TODO("Not yet implemented")
    }

    override fun getConnectionStringsAsync(): Observable<MutableMap<String, ConnectionString>> {
        TODO("Not yet implemented")
    }

    override fun getAuthenticationConfig(): WebAppAuthentication {
        TODO("Not yet implemented")
    }

    override fun getAuthenticationConfigAsync(): Observable<WebAppAuthentication> {
        TODO("Not yet implemented")
    }

    override fun operatingSystem(): OperatingSystem {
        TODO("Not yet implemented")
    }

    override fun platformArchitecture(): PlatformArchitecture {
        TODO("Not yet implemented")
    }

    override fun linuxFxVersion(): String {
        TODO("Not yet implemented")
    }

    override fun diagnosticLogsConfig(): WebAppDiagnosticLogs {
        TODO("Not yet implemented")
    }

    override fun getHostNameBindings(): MutableMap<String, HostNameBinding> {
        TODO("Not yet implemented")
    }

    override fun getHostNameBindingsAsync(): Observable<MutableMap<String, HostNameBinding>> {
        TODO("Not yet implemented")
    }

    override fun getPublishingProfile(): PublishingProfile {
        TODO("Not yet implemented")
    }

    override fun getPublishingProfileAsync(): Observable<PublishingProfile> {
        TODO("Not yet implemented")
    }

    override fun getSourceControl(): WebAppSourceControl {
        TODO("Not yet implemented")
    }

    override fun getSourceControlAsync(): Observable<WebAppSourceControl> {
        TODO("Not yet implemented")
    }

    override fun deploy(): WebDeployment.DefinitionStages.WithPackageUri {
        TODO("Not yet implemented")
    }

    override fun getContainerLogs(): ByteArray {
        TODO("Not yet implemented")
    }

    override fun getContainerLogsAsync(): Observable<ByteArray> {
        TODO("Not yet implemented")
    }

    override fun getContainerLogsZip(): ByteArray {
        TODO("Not yet implemented")
    }

    override fun getContainerLogsZipAsync(): Observable<ByteArray> {
        TODO("Not yet implemented")
    }

    override fun streamApplicationLogs(): InputStream {
        TODO("Not yet implemented")
    }

    override fun streamHttpLogs(): InputStream {
        TODO("Not yet implemented")
    }

    override fun streamTraceLogs(): InputStream {
        TODO("Not yet implemented")
    }

    override fun streamDeploymentLogs(): InputStream {
        TODO("Not yet implemented")
    }

    override fun streamAllLogs(): InputStream {
        TODO("Not yet implemented")
    }

    override fun streamApplicationLogsAsync(): Observable<String> {
        TODO("Not yet implemented")
    }

    override fun streamHttpLogsAsync(): Observable<String> {
        TODO("Not yet implemented")
    }

    override fun streamTraceLogsAsync(): Observable<String> {
        TODO("Not yet implemented")
    }

    override fun streamDeploymentLogsAsync(): Observable<String> {
        TODO("Not yet implemented")
    }

    override fun streamAllLogsAsync(): Observable<String> {
        TODO("Not yet implemented")
    }

    override fun verifyDomainOwnership(p0: String?, p1: String?) {
        TODO("Not yet implemented")
    }

    override fun verifyDomainOwnershipAsync(p0: String?, p1: String?): Completable {
        TODO("Not yet implemented")
    }

    override fun start() {
        TODO("Not yet implemented")
    }

    override fun startAsync(): Completable {
        TODO("Not yet implemented")
    }

    override fun stop() {
        TODO("Not yet implemented")
    }

    override fun stopAsync(): Completable {
        TODO("Not yet implemented")
    }

    override fun restart() {
        TODO("Not yet implemented")
    }

    override fun restartAsync(): Completable {
        TODO("Not yet implemented")
    }

    override fun swap(p0: String?) {
        TODO("Not yet implemented")
    }

    override fun swapAsync(p0: String?): Completable {
        TODO("Not yet implemented")
    }

    override fun applySlotConfigurations(p0: String?) {
        TODO("Not yet implemented")
    }

    override fun applySlotConfigurationsAsync(p0: String?): Completable {
        TODO("Not yet implemented")
    }

    override fun resetSlotConfigurations() {
        TODO("Not yet implemented")
    }

    override fun resetSlotConfigurationsAsync(): Completable {
        TODO("Not yet implemented")
    }

    override fun zipDeploy(p0: File?) {
        TODO("Not yet implemented")
    }

    override fun zipDeploy(p0: InputStream?) {
        TODO("Not yet implemented")
    }

    override fun zipDeployAsync(p0: File?): Completable {
        TODO("Not yet implemented")
    }

    override fun zipDeployAsync(p0: InputStream?): Completable {
        TODO("Not yet implemented")
    }

    override fun update(): DeploymentSlotBase.Update<FunctionDeploymentSlot> {
        TODO("Not yet implemented")
    }

    override fun refresh(): FunctionDeploymentSlot {
        TODO("Not yet implemented")
    }

    override fun refreshAsync(): Observable<FunctionDeploymentSlot> {
        TODO("Not yet implemented")
    }

    override fun parent(): FunctionApp = parent ?: throw Exception("No parent was provided for this mock.")
}