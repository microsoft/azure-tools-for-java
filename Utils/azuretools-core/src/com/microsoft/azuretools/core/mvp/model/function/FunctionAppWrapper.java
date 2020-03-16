/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package com.microsoft.azuretools.core.mvp.model.function;

import com.google.common.collect.Sets;
import com.microsoft.azure.management.appservice.AppSetting;
import com.microsoft.azure.management.appservice.CloningInfo;
import com.microsoft.azure.management.appservice.ConnectionString;
import com.microsoft.azure.management.appservice.FtpsState;
import com.microsoft.azure.management.appservice.FunctionApp;
import com.microsoft.azure.management.appservice.FunctionDeploymentSlots;
import com.microsoft.azure.management.appservice.HostNameBinding;
import com.microsoft.azure.management.appservice.HostNameSslState;
import com.microsoft.azure.management.appservice.JavaVersion;
import com.microsoft.azure.management.appservice.ManagedPipelineMode;
import com.microsoft.azure.management.appservice.NameValuePair;
import com.microsoft.azure.management.appservice.NetFrameworkVersion;
import com.microsoft.azure.management.appservice.OperatingSystem;
import com.microsoft.azure.management.appservice.PhpVersion;
import com.microsoft.azure.management.appservice.PlatformArchitecture;
import com.microsoft.azure.management.appservice.PublishingProfile;
import com.microsoft.azure.management.appservice.PythonVersion;
import com.microsoft.azure.management.appservice.RemoteVisualStudioVersion;
import com.microsoft.azure.management.appservice.ScmType;
import com.microsoft.azure.management.appservice.SiteAvailabilityState;
import com.microsoft.azure.management.appservice.UsageState;
import com.microsoft.azure.management.appservice.VirtualApplication;
import com.microsoft.azure.management.appservice.WebAppAuthentication;
import com.microsoft.azure.management.appservice.WebAppDiagnosticLogs;
import com.microsoft.azure.management.appservice.WebAppSourceControl;
import com.microsoft.azure.management.appservice.WebDeployment;
import com.microsoft.azure.management.appservice.implementation.AppServiceManager;
import com.microsoft.azure.management.appservice.implementation.SiteInner;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.management.storage.StorageAccount;
import org.joda.time.DateTime;
import rx.Completable;
import rx.Observable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FunctionAppWrapper implements FunctionApp {

    private String subscriptionId;
    private SiteInner siteInner;

    public FunctionAppWrapper(String subscriptionId, SiteInner siteInner) {
        this.subscriptionId = subscriptionId;
        this.siteInner = siteInner;
    }

    @Override
    public FunctionDeploymentSlots deploymentSlots() {
        return getFunctionApp().deploymentSlots();
    }

    @Override
    public StorageAccount storageAccount() {
        return getFunctionApp().storageAccount();
    }

    @Override
    public String getMasterKey() {
        return getFunctionApp().getMasterKey();
    }

    @Override
    public Observable<String> getMasterKeyAsync() {
        return getFunctionApp().getMasterKeyAsync();
    }

    @Override
    public Map<String, String> listFunctionKeys(String s) {
        return getFunctionApp().listFunctionKeys(s);
    }

    @Override
    public Observable<Map<String, String>> listFunctionKeysAsync(String s) {
        return getFunctionApp().listFunctionKeysAsync(s);
    }

    @Override
    public NameValuePair addFunctionKey(String s, String s1, String s2) {
        return getFunctionApp().addFunctionKey(s, s1, s2);
    }

    @Override
    public Observable<NameValuePair> addFunctionKeyAsync(String s, String s1, String s2) {
        return getFunctionApp().addFunctionKeyAsync(s, s1, s2);
    }

    @Override
    public void removeFunctionKey(String s, String s1) {
        getFunctionApp().removeFunctionKey(s, s1);
    }

    @Override
    public Completable removeFunctionKeyAsync(String s, String s1) {
        return getFunctionApp().removeFunctionKeyAsync(s, s1);
    }

    @Override
    public void syncTriggers() {
        getFunctionApp().syncTriggers();
    }

    @Override
    public Completable syncTriggersAsync() {
        return getFunctionApp().syncTriggersAsync();
    }

    @Override
    public String state() {
        return siteInner.state();
    }

    @Override
    public Set<String> hostNames() {
        return Sets.newHashSet(siteInner.hostNames());
    }

    @Override
    public String repositorySiteName() {
        return siteInner.repositorySiteName();
    }

    @Override
    public UsageState usageState() {
        return siteInner.usageState();
    }

    @Override
    public boolean enabled() {
        return siteInner.enabled();
    }

    @Override
    public Set<String> enabledHostNames() {
        return Sets.newHashSet(siteInner.enabledHostNames());
    }

    @Override
    public SiteAvailabilityState availabilityState() {
        return siteInner.availabilityState();
    }

    @Override
    public Map<String, HostNameSslState> hostNameSslStates() {
        return getFunctionApp().hostNameSslStates();
    }

    @Override
    public String appServicePlanId() {
        return siteInner.serverFarmId();
    }

    @Override
    public DateTime lastModifiedTime() {
        return siteInner.lastModifiedTimeUtc();
    }

    @Override
    public Set<String> trafficManagerHostNames() {
        return Sets.newHashSet(siteInner.trafficManagerHostNames());
    }

    @Override
    public boolean scmSiteAlsoStopped() {
        return siteInner.scmSiteAlsoStopped();
    }

    @Override
    public String targetSwapSlot() {
        return getFunctionApp().targetSwapSlot();
    }

    @Override
    public boolean clientAffinityEnabled() {
        return siteInner.clientAffinityEnabled();
    }

    @Override
    public boolean clientCertEnabled() {
        return siteInner.clientCertEnabled();
    }

    @Override
    public boolean hostNamesDisabled() {
        return siteInner.hostNamesDisabled();
    }

    @Override
    public Set<String> outboundIPAddresses() {
        return Sets.newHashSet(siteInner.outboundIpAddresses());
    }

    @Override
    public int containerSize() {
        return siteInner.containerSize();
    }

    @Override
    public CloningInfo cloningInfo() {
        return siteInner.cloningInfo();
    }

    @Override
    public boolean isDefaultContainer() {
        return siteInner.isDefaultContainer();
    }

    @Override
    public String defaultHostName() {
        return siteInner.defaultHostName();
    }

    @Override
    public List<String> defaultDocuments() {
        return getFunctionApp().defaultDocuments();
    }

    @Override
    public NetFrameworkVersion netFrameworkVersion() {
        return getFunctionApp().netFrameworkVersion();
    }

    @Override
    public PhpVersion phpVersion() {
        return getFunctionApp().phpVersion();
    }

    @Override
    public PythonVersion pythonVersion() {
        return getFunctionApp().pythonVersion();
    }

    @Override
    public String nodeVersion() {
        return getFunctionApp().nodeVersion();
    }

    @Override
    public boolean remoteDebuggingEnabled() {
        return getFunctionApp().remoteDebuggingEnabled();
    }

    @Override
    public RemoteVisualStudioVersion remoteDebuggingVersion() {
        return getFunctionApp().remoteDebuggingVersion();
    }

    @Override
    public boolean webSocketsEnabled() {
        return getFunctionApp().webSocketsEnabled();
    }

    @Override
    public boolean alwaysOn() {
        return getFunctionApp().alwaysOn();
    }

    @Override
    public JavaVersion javaVersion() {
        return getFunctionApp().javaVersion();
    }

    @Override
    public String javaContainer() {
        return getFunctionApp().javaContainer();
    }

    @Override
    public String javaContainerVersion() {
        return getFunctionApp().javaContainerVersion();
    }

    @Override
    public ManagedPipelineMode managedPipelineMode() {
        return getFunctionApp().managedPipelineMode();
    }

    @Override
    public String autoSwapSlotName() {
        return getFunctionApp().autoSwapSlotName();
    }

    @Override
    public boolean httpsOnly() {
        return siteInner.httpsOnly();
    }

    @Override
    public FtpsState ftpsState() {
        return getFunctionApp().ftpsState();
    }

    @Override
    public List<VirtualApplication> virtualApplications() {
        return getFunctionApp().virtualApplications();
    }

    @Override
    public boolean http20Enabled() {
        return getFunctionApp().http20Enabled();
    }

    @Override
    public boolean localMySqlEnabled() {
        return getFunctionApp().localMySqlEnabled();
    }

    @Override
    public ScmType scmType() {
        return getFunctionApp().scmType();
    }

    @Override
    public String documentRoot() {
        return getFunctionApp().documentRoot();
    }

    @Override
    public String systemAssignedManagedServiceIdentityTenantId() {
        return getFunctionApp().systemAssignedManagedServiceIdentityTenantId();
    }

    @Override
    public String systemAssignedManagedServiceIdentityPrincipalId() {
        return getFunctionApp().systemAssignedManagedServiceIdentityPrincipalId();
    }

    @Override
    public Set<String> userAssignedManagedServiceIdentityIds() {
        return getFunctionApp().userAssignedManagedServiceIdentityIds();
    }

    @Override
    public Map<String, AppSetting> getAppSettings() {
        return getFunctionApp().getAppSettings();
    }

    @Override
    public Observable<Map<String, AppSetting>> getAppSettingsAsync() {
        return getFunctionApp().getAppSettingsAsync();
    }

    @Override
    public Map<String, ConnectionString> getConnectionStrings() {
        return getFunctionApp().getConnectionStrings();
    }

    @Override
    public Observable<Map<String, ConnectionString>> getConnectionStringsAsync() {
        return getFunctionApp().getConnectionStringsAsync();
    }

    @Override
    public WebAppAuthentication getAuthenticationConfig() {
        return getFunctionApp().getAuthenticationConfig();
    }

    @Override
    public Observable<WebAppAuthentication> getAuthenticationConfigAsync() {
        return getFunctionApp().getAuthenticationConfigAsync();
    }

    @Override
    public OperatingSystem operatingSystem() {
        return siteInner.kind() != null && siteInner.kind().toLowerCase().contains("linux") ? OperatingSystem.LINUX : OperatingSystem.WINDOWS;
    }

    @Override
    public PlatformArchitecture platformArchitecture() {
        return getFunctionApp().platformArchitecture();
    }

    @Override
    public String linuxFxVersion() {
        return getFunctionApp().linuxFxVersion();
    }

    @Override
    public WebAppDiagnosticLogs diagnosticLogsConfig() {
        return getFunctionApp().diagnosticLogsConfig();
    }

    @Override
    public Map<String, HostNameBinding> getHostNameBindings() {
        return getFunctionApp().getHostNameBindings();
    }

    @Override
    public Observable<Map<String, HostNameBinding>> getHostNameBindingsAsync() {
        return getFunctionApp().getHostNameBindingsAsync();
    }

    @Override
    public PublishingProfile getPublishingProfile() {
        return getFunctionApp().getPublishingProfile();
    }

    @Override
    public Observable<PublishingProfile> getPublishingProfileAsync() {
        return getFunctionApp().getPublishingProfileAsync();
    }

    @Override
    public WebAppSourceControl getSourceControl() {
        return getFunctionApp().getSourceControl();
    }

    @Override
    public Observable<WebAppSourceControl> getSourceControlAsync() {
        return getFunctionApp().getSourceControlAsync();
    }

    @Override
    public WebDeployment.DefinitionStages.WithPackageUri deploy() {
        return getFunctionApp().deploy();
    }

    @Override
    public byte[] getContainerLogs() {
        return getFunctionApp().getContainerLogs();
    }

    @Override
    public Observable<byte[]> getContainerLogsAsync() {
        return getFunctionApp().getContainerLogsAsync();
    }

    @Override
    public byte[] getContainerLogsZip() {
        return getFunctionApp().getContainerLogsZip();
    }

    @Override
    public Observable<byte[]> getContainerLogsZipAsync() {
        return getFunctionApp().getContainerLogsZipAsync();
    }

    @Override
    public InputStream streamApplicationLogs() {
        return getFunctionApp().streamApplicationLogs();
    }

    @Override
    public InputStream streamHttpLogs() {
        return getFunctionApp().streamHttpLogs();
    }

    @Override
    public InputStream streamTraceLogs() {
        return getFunctionApp().streamTraceLogs();
    }

    @Override
    public InputStream streamDeploymentLogs() {
        return getFunctionApp().streamDeploymentLogs();
    }

    @Override
    public InputStream streamAllLogs() {
        return getFunctionApp().streamAllLogs();
    }

    @Override
    public Observable<String> streamApplicationLogsAsync() {
        return getFunctionApp().streamApplicationLogsAsync();
    }

    @Override
    public Observable<String> streamHttpLogsAsync() {
        return getFunctionApp().streamHttpLogsAsync();
    }

    @Override
    public Observable<String> streamTraceLogsAsync() {
        return getFunctionApp().streamTraceLogsAsync();
    }

    @Override
    public Observable<String> streamDeploymentLogsAsync() {
        return getFunctionApp().streamDeploymentLogsAsync();
    }

    @Override
    public Observable<String> streamAllLogsAsync() {
        return getFunctionApp().streamAllLogsAsync();
    }

    @Override
    public void verifyDomainOwnership(String s, String s1) {
        getFunctionApp().verifyDomainOwnership(s, s1);
    }

    @Override
    public Completable verifyDomainOwnershipAsync(String s, String s1) {
        return getFunctionApp().verifyDomainOwnershipAsync(s, s1);
    }

    @Override
    public void start() {
        getFunctionApp().start();
    }

    @Override
    public Completable startAsync() {
        return getFunctionApp().startAsync();
    }

    @Override
    public void stop() {
        getFunctionApp().stop();
    }

    @Override
    public Completable stopAsync() {
        return getFunctionApp().stopAsync();
    }

    @Override
    public void restart() {
        getFunctionApp().restart();
    }

    @Override
    public Completable restartAsync() {
        return getFunctionApp().restartAsync();
    }

    @Override
    public void swap(String s) {
        getFunctionApp().swap(s);
    }

    @Override
    public Completable swapAsync(String s) {
        return getFunctionApp().swapAsync(s);
    }

    @Override
    public void applySlotConfigurations(String s) {
        getFunctionApp().applySlotConfigurations(s);
    }

    @Override
    public Completable applySlotConfigurationsAsync(String s) {
        return getFunctionApp().applySlotConfigurationsAsync(s);
    }

    @Override
    public void resetSlotConfigurations() {
        getFunctionApp().resetSlotConfigurations();
    }

    @Override
    public Completable resetSlotConfigurationsAsync() {
        return getFunctionApp().resetSlotConfigurationsAsync();
    }

    @Override
    public void zipDeploy(File file) {
        getFunctionApp().zipDeploy(file);
    }

    @Override
    public Completable zipDeployAsync(File file) {
        return getFunctionApp().zipDeployAsync(file);
    }

    @Override
    public void zipDeploy(InputStream inputStream) {
        getFunctionApp().zipDeploy(inputStream);
    }

    @Override
    public Completable zipDeployAsync(InputStream inputStream) {
        return getFunctionApp().zipDeployAsync(inputStream);
    }

    @Override
    public AppServiceManager manager() {
        return getFunctionApp().manager();
    }

    @Override
    public String resourceGroupName() {
        return siteInner.resourceGroup();
    }

    @Override
    public String type() {
        return siteInner.type();
    }

    @Override
    public String regionName() {
        return siteInner.location();
    }

    @Override
    public Region region() {
        return Region.fromName(regionName());
    }

    @Override
    public Map<String, String> tags() {
        return getFunctionApp().tags();
    }

    @Override
    public String id() {
        return siteInner.id();
    }

    @Override
    public String name() {
        return siteInner.name();
    }

    @Override
    public SiteInner inner() {
        return siteInner;
    }

    @Override
    public String key() {
        return getFunctionApp().key();
    }

    @Override
    public FunctionApp refresh() {
        return getFunctionApp().refresh();
    }

    @Override
    public Observable<FunctionApp> refreshAsync() {
        return getFunctionApp().refreshAsync();
    }

    @Override
    public Update update() {
        return getFunctionApp().update();
    }

    private FunctionApp getFunctionApp() {
        try {
            return AzureFunctionMvpModel.getInstance().getFunctionById(subscriptionId, siteInner.id());
        } catch (IOException e) {
            throw new RuntimeException("Failed to get function instance");
        }
    }
}
