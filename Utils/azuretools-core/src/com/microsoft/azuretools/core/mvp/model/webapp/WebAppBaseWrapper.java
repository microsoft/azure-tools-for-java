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

package com.microsoft.azuretools.core.mvp.model.webapp;

import com.google.common.collect.Sets;
import com.microsoft.azure.management.appservice.AppSetting;
import com.microsoft.azure.management.appservice.CloningInfo;
import com.microsoft.azure.management.appservice.ConnectionString;
import com.microsoft.azure.management.appservice.FtpsState;
import com.microsoft.azure.management.appservice.HostNameBinding;
import com.microsoft.azure.management.appservice.HostNameSslState;
import com.microsoft.azure.management.appservice.JavaVersion;
import com.microsoft.azure.management.appservice.ManagedPipelineMode;
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
import com.microsoft.azure.management.appservice.WebAppBase;
import com.microsoft.azure.management.appservice.WebAppDiagnosticLogs;
import com.microsoft.azure.management.appservice.WebAppSourceControl;
import com.microsoft.azure.management.appservice.WebDeployment;
import com.microsoft.azure.management.appservice.implementation.AppServiceManager;
import com.microsoft.azure.management.appservice.implementation.SiteInner;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import org.joda.time.DateTime;
import rx.Completable;
import rx.Observable;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class WebAppBaseWrapper implements WebAppBase {

    private String subscriptionId;
    private SiteInner siteInner;

    public WebAppBaseWrapper(String subscriptionId, SiteInner siteInner) {
        this.subscriptionId = subscriptionId;
        this.siteInner = siteInner;
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
        return getWebAppBase().hostNameSslStates();
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
        return getWebAppBase().targetSwapSlot();
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
        return getWebAppBase().defaultDocuments();
    }

    @Override
    public NetFrameworkVersion netFrameworkVersion() {
        return getWebAppBase().netFrameworkVersion();
    }

    @Override
    public PhpVersion phpVersion() {
        return getWebAppBase().phpVersion();
    }

    @Override
    public PythonVersion pythonVersion() {
        return getWebAppBase().pythonVersion();
    }

    @Override
    public String nodeVersion() {
        return getWebAppBase().nodeVersion();
    }

    @Override
    public boolean remoteDebuggingEnabled() {
        return getWebAppBase().remoteDebuggingEnabled();
    }

    @Override
    public RemoteVisualStudioVersion remoteDebuggingVersion() {
        return getWebAppBase().remoteDebuggingVersion();
    }

    @Override
    public boolean webSocketsEnabled() {
        return getWebAppBase().webSocketsEnabled();
    }

    @Override
    public boolean alwaysOn() {
        return getWebAppBase().alwaysOn();
    }

    @Override
    public JavaVersion javaVersion() {
        return getWebAppBase().javaVersion();
    }

    @Override
    public String javaContainer() {
        return getWebAppBase().javaContainer();
    }

    @Override
    public String javaContainerVersion() {
        return getWebAppBase().javaContainerVersion();
    }

    @Override
    public ManagedPipelineMode managedPipelineMode() {
        return getWebAppBase().managedPipelineMode();
    }

    @Override
    public String autoSwapSlotName() {
        return getWebAppBase().autoSwapSlotName();
    }

    @Override
    public boolean httpsOnly() {
        return siteInner.httpsOnly();
    }

    @Override
    public FtpsState ftpsState() {
        return getWebAppBase().ftpsState();
    }

    @Override
    public List<VirtualApplication> virtualApplications() {
        return getWebAppBase().virtualApplications();
    }

    @Override
    public boolean http20Enabled() {
        return getWebAppBase().http20Enabled();
    }

    @Override
    public boolean localMySqlEnabled() {
        return getWebAppBase().localMySqlEnabled();
    }

    @Override
    public ScmType scmType() {
        return getWebAppBase().scmType();
    }

    @Override
    public String documentRoot() {
        return getWebAppBase().documentRoot();
    }

    @Override
    public String systemAssignedManagedServiceIdentityTenantId() {
        return getWebAppBase().systemAssignedManagedServiceIdentityTenantId();
    }

    @Override
    public String systemAssignedManagedServiceIdentityPrincipalId() {
        return getWebAppBase().systemAssignedManagedServiceIdentityPrincipalId();
    }

    @Override
    public Set<String> userAssignedManagedServiceIdentityIds() {
        return getWebAppBase().userAssignedManagedServiceIdentityIds();
    }

    @Override
    public Map<String, AppSetting> getAppSettings() {
        return getWebAppBase().getAppSettings();
    }

    @Override
    public Observable<Map<String, AppSetting>> getAppSettingsAsync() {
        return getWebAppBase().getAppSettingsAsync();
    }

    @Override
    public Map<String, ConnectionString> getConnectionStrings() {
        return getWebAppBase().getConnectionStrings();
    }

    @Override
    public Observable<Map<String, ConnectionString>> getConnectionStringsAsync() {
        return getWebAppBase().getConnectionStringsAsync();
    }

    @Override
    public WebAppAuthentication getAuthenticationConfig() {
        return getWebAppBase().getAuthenticationConfig();
    }

    @Override
    public Observable<WebAppAuthentication> getAuthenticationConfigAsync() {
        return getWebAppBase().getAuthenticationConfigAsync();
    }

    @Override
    public OperatingSystem operatingSystem() {
        return siteInner.kind() != null && siteInner.kind().toLowerCase().contains("linux") ? OperatingSystem.LINUX : OperatingSystem.WINDOWS;
    }

    @Override
    public PlatformArchitecture platformArchitecture() {
        return getWebAppBase().platformArchitecture();
    }

    @Override
    public String linuxFxVersion() {
        return getWebAppBase().linuxFxVersion();
    }

    @Override
    public WebAppDiagnosticLogs diagnosticLogsConfig() {
        return getWebAppBase().diagnosticLogsConfig();
    }

    @Override
    public Map<String, HostNameBinding> getHostNameBindings() {
        return getWebAppBase().getHostNameBindings();
    }

    @Override
    public Observable<Map<String, HostNameBinding>> getHostNameBindingsAsync() {
        return getWebAppBase().getHostNameBindingsAsync();
    }

    @Override
    public PublishingProfile getPublishingProfile() {
        return getWebAppBase().getPublishingProfile();
    }

    @Override
    public Observable<PublishingProfile> getPublishingProfileAsync() {
        return getWebAppBase().getPublishingProfileAsync();
    }

    @Override
    public WebAppSourceControl getSourceControl() {
        return getWebAppBase().getSourceControl();
    }

    @Override
    public Observable<WebAppSourceControl> getSourceControlAsync() {
        return getWebAppBase().getSourceControlAsync();
    }

    @Override
    public WebDeployment.DefinitionStages.WithPackageUri deploy() {
        return getWebAppBase().deploy();
    }

    @Override
    public byte[] getContainerLogs() {
        return getWebAppBase().getContainerLogs();
    }

    @Override
    public Observable<byte[]> getContainerLogsAsync() {
        return getWebAppBase().getContainerLogsAsync();
    }

    @Override
    public byte[] getContainerLogsZip() {
        return getWebAppBase().getContainerLogsZip();
    }

    @Override
    public Observable<byte[]> getContainerLogsZipAsync() {
        return getWebAppBase().getContainerLogsZipAsync();
    }

    @Override
    public InputStream streamApplicationLogs() {
        return getWebAppBase().streamApplicationLogs();
    }

    @Override
    public InputStream streamHttpLogs() {
        return getWebAppBase().streamHttpLogs();
    }

    @Override
    public InputStream streamTraceLogs() {
        return getWebAppBase().streamTraceLogs();
    }

    @Override
    public InputStream streamDeploymentLogs() {
        return getWebAppBase().streamDeploymentLogs();
    }

    @Override
    public InputStream streamAllLogs() {
        return getWebAppBase().streamAllLogs();
    }

    @Override
    public Observable<String> streamApplicationLogsAsync() {
        return getWebAppBase().streamApplicationLogsAsync();
    }

    @Override
    public Observable<String> streamHttpLogsAsync() {
        return getWebAppBase().streamHttpLogsAsync();
    }

    @Override
    public Observable<String> streamTraceLogsAsync() {
        return getWebAppBase().streamTraceLogsAsync();
    }

    @Override
    public Observable<String> streamDeploymentLogsAsync() {
        return getWebAppBase().streamDeploymentLogsAsync();
    }

    @Override
    public Observable<String> streamAllLogsAsync() {
        return getWebAppBase().streamAllLogsAsync();
    }

    @Override
    public void verifyDomainOwnership(String s, String s1) {
        getWebAppBase().verifyDomainOwnership(s, s1);
    }

    @Override
    public Completable verifyDomainOwnershipAsync(String s, String s1) {
        return getWebAppBase().verifyDomainOwnershipAsync(s, s1);
    }

    @Override
    public void start() {
        getWebAppBase().start();
    }

    @Override
    public Completable startAsync() {
        return getWebAppBase().startAsync();
    }

    @Override
    public void stop() {
        getWebAppBase().stop();
    }

    @Override
    public Completable stopAsync() {
        return getWebAppBase().stopAsync();
    }

    @Override
    public void restart() {
        getWebAppBase().restart();
    }

    @Override
    public Completable restartAsync() {
        return getWebAppBase().restartAsync();
    }

    @Override
    public void swap(String s) {
        getWebAppBase().swap(s);
    }

    @Override
    public Completable swapAsync(String s) {
        return getWebAppBase().swapAsync(s);
    }

    @Override
    public void applySlotConfigurations(String s) {
        getWebAppBase().applySlotConfigurations(s);
    }

    @Override
    public Completable applySlotConfigurationsAsync(String s) {
        return getWebAppBase().applySlotConfigurationsAsync(s);
    }

    @Override
    public void resetSlotConfigurations() {
        getWebAppBase().resetSlotConfigurations();
    }

    @Override
    public Completable resetSlotConfigurationsAsync() {
        return getWebAppBase().resetSlotConfigurationsAsync();
    }

    @Override
    public void zipDeploy(File file) {
        getWebAppBase().zipDeploy(file);
    }

    @Override
    public Completable zipDeployAsync(File file) {
        return getWebAppBase().zipDeployAsync(file);
    }

    @Override
    public void zipDeploy(InputStream inputStream) {
        getWebAppBase().zipDeploy(inputStream);
    }

    @Override
    public Completable zipDeployAsync(InputStream inputStream) {
        return getWebAppBase().zipDeployAsync(inputStream);
    }

    @Override
    public AppServiceManager manager() {
        return getWebAppBase().manager();
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
        return getWebAppBase().tags();
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
        return getWebAppBase().key();
    }

    protected String getSubscriptionId() {
        return this.subscriptionId;
    }

    protected abstract WebAppBase getWebAppBase();
}
