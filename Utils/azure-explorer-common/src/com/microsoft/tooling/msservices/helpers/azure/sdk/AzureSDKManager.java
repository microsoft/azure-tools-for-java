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
 */

package com.microsoft.tooling.msservices.helpers.azure.sdk;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.microsoft.applicationinsights.management.rest.ApplicationInsightsManagementClient;
import com.microsoft.applicationinsights.management.rest.client.RestOperationException;
import com.microsoft.applicationinsights.management.rest.model.Resource;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.applicationinsights.v2015_05_01.ApplicationInsightsComponent;
import com.microsoft.azure.management.applicationinsights.v2015_05_01.ApplicationType;
import com.microsoft.azure.management.applicationinsights.v2015_05_01.implementation.InsightsManager;
import com.microsoft.azure.management.compute.AvailabilitySet;
import com.microsoft.azure.management.compute.KnownLinuxVirtualMachineImage;
import com.microsoft.azure.management.compute.KnownWindowsVirtualMachineImage;
import com.microsoft.azure.management.compute.OperatingSystemTypes;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.compute.VirtualMachineImage;
import com.microsoft.azure.management.network.Network;
import com.microsoft.azure.management.network.PublicIPAddress;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.management.resources.fluentcore.model.Creatable;
import com.microsoft.azure.management.storage.AccessTier;
import com.microsoft.azure.management.storage.Kind;
import com.microsoft.azure.management.storage.SkuName;
import com.microsoft.azure.management.storage.StorageAccount;
import com.microsoft.azure.management.storage.StorageAccountSkuType;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.tooling.msservices.model.vm.VirtualNetwork;
import com.sun.tools.sjavac.Log;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AzureSDKManager {

    public static StorageAccount createStorageAccount(String subscriptionId, String name, String region, boolean newResourceGroup, String resourceGroup,
                                                      Kind kind, AccessTier accessTier, boolean enableEncription, String skuName) throws Exception {
        AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
        Azure azure = azureManager.getAzure(subscriptionId);
        StorageAccount.DefinitionStages.WithGroup newStorageAccountBlank = azure.storageAccounts().define(name).withRegion(region);
        StorageAccount.DefinitionStages.WithCreate newStorageAccountWithGroup;
        if (newResourceGroup) {
            newStorageAccountWithGroup = newStorageAccountBlank.withNewResourceGroup(resourceGroup);
        } else {
            newStorageAccountWithGroup = newStorageAccountBlank.withExistingResourceGroup(resourceGroup);
        }

        if (Kind.STORAGE.equals(kind)) {
            newStorageAccountWithGroup = newStorageAccountWithGroup.withGeneralPurposeAccountKind();
        } else if (Kind.STORAGE_V2.equals(kind)) {
            newStorageAccountWithGroup = newStorageAccountWithGroup.withGeneralPurposeAccountKindV2();
        } else if (Kind.BLOB_STORAGE.equals(kind)) {
            newStorageAccountWithGroup = newStorageAccountWithGroup.withBlobStorageAccountKind().withAccessTier(accessTier);
        } else {
            throw new Exception("Unknown Storage Account Kind:" + kind.toString());

        }

        if (enableEncription) {
            newStorageAccountWithGroup = newStorageAccountWithGroup.withBlobEncryption();
        }

        return newStorageAccountWithGroup.withSku(StorageAccountSkuType.fromSkuName(SkuName.fromString(skuName))).create();
    }

    public static VirtualMachine createVirtualMachine(String subscriptionId, @NotNull String name,
                                                      @NotNull String resourceGroup, boolean withNewResourceGroup,
                                                      @NotNull String size, @NotNull String region,
                                                      final VirtualMachineImage vmImage, Object knownImage,
                                                      boolean isKnownImage, final StorageAccount storageAccount,
                                                      com.microsoft.tooling.msservices.model.storage.StorageAccount
                                                              newStorageAccount, boolean withNewStorageAccount,
                                                      final Network network, VirtualNetwork newNetwork,
                                                      boolean withNewNetwork, @NotNull String subnet,
                                                      @Nullable PublicIPAddress pip, boolean withNewPip,
                                                      @Nullable AvailabilitySet availabilitySet,
                                                      boolean withNewAvailabilitySet, @NotNull final String username,
                                                      @Nullable final String password, @Nullable String publicKey)
            throws Exception {
        AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
        Azure azure = azureManager.getAzure(subscriptionId);
        boolean isWindows;
        if (isKnownImage) {
            isWindows = knownImage instanceof KnownWindowsVirtualMachineImage;
        } else {
            isWindows = vmImage.osDiskImage().operatingSystem().equals(OperatingSystemTypes.WINDOWS);
        }
        // ------ Resource Group ------
        VirtualMachine.DefinitionStages.WithGroup withGroup = azure.virtualMachines().define(name)
                .withRegion(region);
        Creatable<ResourceGroup> newResourceGroup = null;
        VirtualMachine.DefinitionStages.WithNetwork withNetwork;
        if (withNewResourceGroup) {
            newResourceGroup = azure.resourceGroups().define(resourceGroup).withRegion(region);
            withNetwork = withGroup.withNewResourceGroup(newResourceGroup);
        } else {
            withNetwork = withGroup.withExistingResourceGroup(resourceGroup);
        }
        // ------ Virtual Network -----
        VirtualMachine.DefinitionStages.WithPublicIPAddress withPublicIpAddress;
        if (withNewNetwork) {
            Network.DefinitionStages.WithGroup networkWithGroup = azure.networks().define(newNetwork.name).withRegion(region);
            Creatable<Network> newVirtualNetwork;
            if (withNewResourceGroup) {
                newVirtualNetwork = networkWithGroup.withNewResourceGroup(newResourceGroup)
                        .withAddressSpace(newNetwork.addressSpace)
                        .withSubnet(newNetwork.subnet.name, newNetwork.subnet.addressSpace);
            } else {
                newVirtualNetwork = networkWithGroup.withExistingResourceGroup(resourceGroup)
                        .withAddressSpace(newNetwork.addressSpace)
                        .withSubnet(newNetwork.subnet.name, newNetwork.subnet.addressSpace);
            }
            withPublicIpAddress = withNetwork.withNewPrimaryNetwork(newVirtualNetwork).withPrimaryPrivateIPAddressDynamic();
            //withPublicIpAddress = withNetwork.withNewPrimaryNetwork("10.0.0.0/28").
            //.withPrimaryPrivateIpAddressDynamic();
        } else {
            withPublicIpAddress = withNetwork.withExistingPrimaryNetwork(network)
                    .withSubnet(subnet)
                    .withPrimaryPrivateIPAddressDynamic();
        }
        // ------ Public IP Address------
        VirtualMachine.DefinitionStages.WithOS withOS;
        if (pip == null) {
            if (withNewPip) {
                withOS = withPublicIpAddress.withNewPrimaryPublicIPAddress(name + "pip");
            } else {
                withOS = withPublicIpAddress.withoutPrimaryPublicIPAddress();
            }
        } else {
            withOS = withPublicIpAddress.withExistingPrimaryPublicIPAddress(pip);
        }
        // ------ OS and credentials -----
        VirtualMachine.DefinitionStages.WithCreate withCreate;
        if (isWindows) {
            VirtualMachine.DefinitionStages.WithWindowsAdminUsernameManagedOrUnmanaged withWindowsAdminUsername;
            if (isKnownImage) {
                withWindowsAdminUsername = withOS.withPopularWindowsImage((KnownWindowsVirtualMachineImage) knownImage);
            } else {
                withWindowsAdminUsername = withOS.withSpecificWindowsImageVersion(vmImage.imageReference());
            }
            withCreate = withWindowsAdminUsername.withAdminUsername(username).withAdminPassword(password).withUnmanagedDisks();
        } else {
            VirtualMachine.DefinitionStages.WithLinuxRootPasswordOrPublicKeyManagedOrUnmanaged withLinuxRootPasswordOrPublicKey;
            if (isKnownImage) {
                withLinuxRootPasswordOrPublicKey = withOS.withPopularLinuxImage((KnownLinuxVirtualMachineImage) knownImage).withRootUsername(username);
            } else {
                withLinuxRootPasswordOrPublicKey = withOS.withSpecificLinuxImageVersion(vmImage.imageReference()).withRootUsername(username);
            }
            VirtualMachine.DefinitionStages.WithLinuxCreateManagedOrUnmanaged withLinuxCreate;
            // we assume either password or public key is not empty
            if (password != null && !password.isEmpty()) {
                withLinuxCreate = withLinuxRootPasswordOrPublicKey.withRootPassword(password);
                if (publicKey != null) {
                    withLinuxCreate = withLinuxCreate.withSsh(publicKey);
                }
            } else {
                withLinuxCreate = withLinuxRootPasswordOrPublicKey.withSsh(publicKey);
            }
            withCreate = withLinuxCreate.withUnmanagedDisks();
        }
        withCreate = withCreate.withSize(size);
        // ---- Storage Account --------
        if (withNewStorageAccount) {
            StorageAccount.DefinitionStages.WithCreate newAccount;
            StorageAccount.DefinitionStages.WithGroup withGroupAccount = azure.storageAccounts()
                    .define(newStorageAccount.getName()).withRegion(newStorageAccount.getLocation());
            if (newStorageAccount.isNewResourceGroup()) {
                newAccount = withGroupAccount.withNewResourceGroup(newStorageAccount.getResourceGroupName());
            } else {
                newAccount = withGroupAccount.withExistingResourceGroup(newStorageAccount.getResourceGroupName());
            }
            // only general purpose accounts used to create vm
            newAccount.withGeneralPurposeAccountKind().withSku(SkuName.fromString(newStorageAccount.getType()));
            withCreate = withCreate.withNewStorageAccount(newAccount);
        } else {
            withCreate = withCreate.withExistingStorageAccount(storageAccount);
        }
        if (withNewAvailabilitySet) {
            withCreate = withCreate.withNewAvailabilitySet(name + "as");
        } else if (availabilitySet != null) {
            withCreate = withCreate.withExistingAvailabilitySet(availabilitySet);
        }
        return withCreate.create();
    }

    public static List<Resource> getApplicationInsightsResources(@NotNull SubscriptionDetail subscription) throws IOException, RestOperationException {
        AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
        if (azureManager == null) { // not signed in
            return null;
        }
        String tenantId = subscription.getTenantId();
        return getApplicationManagementClient(tenantId, azureManager.getAccessToken(tenantId)).getResources(subscription.getSubscriptionId());
    }

    public static Resource createApplicationInsightsResource(@NotNull SubscriptionDetail subscription,
                                                             @NotNull String resourceGroupName,
                                                             boolean isNewGroup,
                                                             @NotNull String resourceName,
                                                             @NotNull String location) throws Exception {

        AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
        if (azureManager == null) { // not signed in
            return null;
        }
        String tenantId = subscription.getTenantId();
        ApplicationInsightsManagementClient client = getApplicationManagementClient(tenantId, azureManager.getAccessToken(tenantId));
        if (isNewGroup) {
            client.createResourceGroup(subscription.getSubscriptionId(), resourceGroupName, location);
        }
        return client.createResource(subscription.getSubscriptionId(), resourceGroupName, resourceName, location);
    }

    public static List<String> getLocationsForApplicationInsights(SubscriptionDetail subscription) throws Exception {
        AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
        if (azureManager == null) { // not signed in
            return null;
        }
        String tenantId = subscription.getTenantId();
        return getApplicationManagementClient(tenantId, azureManager.getAccessToken(tenantId))
                .getAvailableGeoLocations();
    }

    @NotNull
    private static ApplicationInsightsManagementClient getApplicationManagementClient(@NotNull String tenantId, @NotNull String accessToken)
            throws RestOperationException, IOException {
        String userAgent = "Mozilla/5.0 (Windows NT 6.2; WOW64; rv:19.0) Gecko/20100101 Firefox/19.0";
        return new ApplicationInsightsManagementClient(tenantId, accessToken, userAgent);
    }

    @NotNull
    private static InsightsManager getInsightsManagerClient(@NotNull String subscriptionId)
            throws IOException {
        final AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
        return azureManager == null ? null : azureManager.getInsightsManager(subscriptionId);
    }

    public static List<ApplicationInsightsComponent> getInsightsResources(@NotNull SubscriptionDetail subscription) throws IOException, RestOperationException {
        final InsightsManager insightsManager = getInsightsManagerClient(subscription.getSubscriptionId());
        return insightsManager == null ? null : insightsManager.components().list();
    }

    public static ApplicationInsightsComponent createInsightsResource(@NotNull SubscriptionDetail subscription,
                                                             @NotNull String resourceGroupName,
                                                             boolean isNewGroup,
                                                             @NotNull String resourceName,
                                                             @NotNull String location) throws Exception {
        final InsightsManager insightsManager = getInsightsManagerClient(subscription.getSubscriptionId());
        if (insightsManager == null) { // not signed in
            return null;
        }
        final Azure azure = AuthMethodManager.getInstance().getAzureClient(subscription.getSubscriptionId());
        if (isNewGroup) {
            azure.resourceGroups().define(resourceGroupName).withRegion(location).create();
        }
        return insightsManager.components()
                .define(resourceName)
                .withRegion(location)
                .withExistingResourceGroup(resourceGroupName)
                .withApplicationType(ApplicationType.WEB)
                .withKind("web")
                .create();
    }

    // As AI SDK didn't provide list supported regions interface, call the api directly
    public static List<String> getLocationsForInsights(SubscriptionDetail subscription) throws Exception {
        final String requestUrl = "https://management.azure.com/providers/microsoft.insights?api-version=2015-05-01";
        final HttpGet request = new HttpGet(requestUrl);
        final AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
        if (azureManager == null) {
            return Collections.emptyList();
        }
        final String accessToken = azureManager.getAccessToken(azureManager.getTenantIdBySubscription(subscription.getSubscriptionId()));
        request.setHeader("Authorization", accessToken);
        final CloseableHttpResponse response = HttpClients.createDefault().execute(request);
        final InputStream responseStream = response.getEntity().getContent();
        try (final InputStreamReader isr = new InputStreamReader(responseStream)) {
            final Gson gson = new Gson();
            final JsonObject jsonContent = (gson).fromJson(isr, JsonObject.class);
            final JsonArray jsonResourceTypes = jsonContent.getAsJsonArray("resourceTypes");
            for (int i = 0; i < jsonResourceTypes.size(); ++i) {
                Object obj = jsonResourceTypes.get(i);
                if (obj instanceof JsonObject) {
                    JsonObject jsonResourceType = (JsonObject) obj;
                    String resourceType = jsonResourceType.get("resourceType").getAsString();
                    if (resourceType.equalsIgnoreCase("components")) {
                        JsonArray jsonLocations = jsonResourceType.getAsJsonArray("locations");
                        return gson.fromJson(jsonLocations, new ArrayList().getClass());
                    }
                }
            }
        } catch (Exception e) {
            Log.error(e);
        }
        return Collections.emptyList();
    }
}
