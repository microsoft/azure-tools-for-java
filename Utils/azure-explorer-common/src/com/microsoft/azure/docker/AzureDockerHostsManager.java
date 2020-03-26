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

package com.microsoft.azure.docker;

import com.microsoft.azure.docker.model.*;
import com.microsoft.azure.docker.ops.AzureDockerCertVaultOps;
import com.microsoft.azure.docker.ops.utils.AzureDockerUtils;
import com.microsoft.azure.keyvault.KeyVaultClient;
import com.microsoft.azure.management.keyvault.Vault;
import com.microsoft.azuretools.authmanage.SubscriptionManager;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.azuretools.utils.AzureUIRefreshCore;
import com.microsoft.azuretools.utils.AzureUIRefreshEvent;
import com.microsoft.azuretools.utils.AzureUIRefreshListener;
import com.microsoft.azuretools.utils.Pair;
import com.microsoft.tooling.msservices.components.DefaultLoader;

import java.util.*;

public class AzureDockerHostsManager {
  private static AzureDockerHostsManager instance = null;
  private static boolean isInitialized = false;

  private AzureManager azureAuthManager;

  private List<AzureDockerSubscription> subscriptionsList;
  private Map<String, AzureDockerSubscription> subscriptionsMap;
  private Map<String, Pair<Vault, KeyVaultClient>> vaultsMap;
  private Map<String, AzureDockerCertVault> dockerVaultsMap;
  private Map<String, DockerHost> dockerHostsMap;
  private List<DockerHost> dockerHostsList;
  private Map<String, List<AzureDockerVnet>> dockerNetworkMap;
  private Map<String, List<AzureDockerStorageAccount>> dockerStorageAccountMap;
  private String userId;
  private AzureDockerPreferredSettings dockerPreferredSettings;

  public AzureDockerPreferredSettings getDockerPreferredSettings() { return dockerPreferredSettings; }

  public synchronized void setDockerPreferredSettings(AzureDockerPreferredSettings dockerPreferredSettings) {
    this.dockerPreferredSettings = dockerPreferredSettings;
  }

  public static AzureDockerHostsManager getAzureDockerHostsManager(AzureManager azureAuthManager) throws Exception {
    if (instance == null || instance.azureAuthManager != azureAuthManager) {
      instance = new AzureDockerHostsManager(azureAuthManager);
      isInitialized = false;
    }

    return instance;
  }

  public static void resetAzureDockerHostsManager() {
    if (instance != null) {
      instance.subscriptionsList = null;
      instance.subscriptionsMap = null;
      instance.vaultsMap = null;
      instance.dockerVaultsMap = null;
      instance.dockerHostsMap = null;
      instance.dockerHostsList = null;
      instance.dockerNetworkMap = null;
      instance.dockerStorageAccountMap = null;
      instance = null;
    }
  }

  private void createAzureDockerHostsManagerListener() {
    String id = "AzureDockerHostsManager";

    AzureUIRefreshListener listener = new AzureUIRefreshListener() {
      @Override
      public void run() {
        if (event.object == null &&
            (event.opsType == AzureUIRefreshEvent.EventType.SIGNIN || event.opsType == AzureUIRefreshEvent.EventType.SIGNOUT ||  event.opsType == AzureUIRefreshEvent.EventType.UPDATE || event.opsType == AzureUIRefreshEvent.EventType.REMOVE)) {
          resetAzureDockerHostsManager();
        }
      }
    };
    AzureUIRefreshCore.addListener(id, listener);
  }

  public static AzureDockerHostsManager getAzureDockerHostsManager() {
    return instance;
  }

  public static AzureDockerHostsManager getAzureDockerHostsManagerEmpty(AzureManager azureAuthManager) throws Exception {
    if (instance == null) {
      instance = new AzureDockerHostsManager(azureAuthManager);
    } else {
      if (azureAuthManager != null && (instance.azureAuthManager != azureAuthManager || subscriptionsChanged(azureAuthManager))) {
        instance.azureAuthManager = azureAuthManager;
        instance.userId = azureAuthManager.getCurrentUserId();
        instance.forceRefreshSubscriptions();
      }
    }

    return instance;
  }

  private static boolean subscriptionsChanged(AzureManager azureAuthManager) {
    try {
      SubscriptionManager subscriptionManager = azureAuthManager.getSubscriptionManager();
      List<SubscriptionDetail> subscriptions = subscriptionManager.getSubscriptionDetails();
      if (instance.subscriptionsMap == null || instance.subscriptionsMap.isEmpty()) {
        return true;
      }
      for (SubscriptionDetail subscriptionDetail : subscriptions) {
        if (subscriptionDetail.isSelected() != instance.subscriptionsMap.get(subscriptionDetail.getSubscriptionId()).isSelected)
          return true;
      }
      return false;
    } catch (Exception ignored) {return true;}
  }

  private AzureDockerHostsManager(AzureManager azureAuthManager) throws Exception {
    this.azureAuthManager = azureAuthManager;
    this.userId = azureAuthManager.getCurrentUserId();
    createAzureDockerHostsManagerListener();
  }

  public AzureDockerHostsManager forceRefresh(AzureManager azureAuthManager) {
    try {
      instance = new AzureDockerHostsManager(azureAuthManager);
      instance.forceRefreshSubscriptions();
    } catch (Exception e) {
      e.printStackTrace();
      instance = null;
    }

    return instance;
  }

  public boolean isInitialized() {
    isInitialized = subscriptionsList != null && !subscriptionsList.isEmpty() &&
        vaultsMap != null && !vaultsMap.isEmpty() &&
        dockerVaultsMap != null && !dockerVaultsMap.isEmpty() &&
        dockerHostsList != null && !dockerHostsList.isEmpty() &&
        dockerNetworkMap != null && !dockerNetworkMap.isEmpty() &&
        dockerStorageAccountMap != null && !dockerStorageAccountMap.isEmpty();

    return isInitialized;
  }

  public List<AzureDockerSubscription> getSubscriptionsList() { return subscriptionsList; }

  public Map<String, AzureDockerSubscription> getSubscriptionsMap() { return subscriptionsMap; }

  public Map<String, Pair<Vault, KeyVaultClient>> getVaultsMap() { return vaultsMap; }

  public Map<String, AzureDockerCertVault> getDockerVaultsMap() { return dockerVaultsMap;}

  public String getUserId() { return userId; }

  public List<DockerHost> getDockerHostsList() { return dockerHostsList; }

  public Map<String, AzureDockerSubscription> refreshDockerSubscriptions() {
    try {
      subscriptionsMap = AzureDockerUtils.refreshDockerSubscriptions(azureAuthManager);
      subscriptionsList = new ArrayList<>(subscriptionsMap.values());
    } catch (Exception e) {
      e.printStackTrace();
      DefaultLoader.getUIHelper().showError(e.getMessage(), "Error loading subscription details");
    }

    return subscriptionsMap;
  }

  public Map<String, Pair<Vault, KeyVaultClient>> refreshDockerVaults() {
    try {
      vaultsMap = AzureDockerUtils.refreshDockerVaults(subscriptionsList);
    } catch (Exception e) {
      e.printStackTrace();
      DefaultLoader.getUIHelper().showError(e.getMessage(), "Error loading key vaults");
    }

    return vaultsMap;
  }

  public Map<String, AzureDockerCertVault> refreshDockerVaultDetails() {
    try {
      dockerVaultsMap = AzureDockerUtils.refreshDockerVaultDetails(subscriptionsList);
    } catch (Exception e) {
      e.printStackTrace();
      DefaultLoader.getUIHelper().showError(e.getMessage(), "Error loading key vault details");
    }

    return dockerVaultsMap;
  }

  public Map<String, List<AzureDockerVnet>> refreshDockerVnetDetails() {
    try {
      dockerNetworkMap = AzureDockerUtils.refreshDockerVnetDetails(subscriptionsList);
    } catch (Exception e) {
      e.printStackTrace();
      DefaultLoader.getUIHelper().showError(e.getMessage(), "Error loading virtual network details");
    }

    return dockerNetworkMap;
  }

  public Map<String, List<AzureDockerStorageAccount>> refreshDockerStorageAccountDetails() {
    try {
      dockerStorageAccountMap = AzureDockerUtils.refreshDockerStorageAccountDetails(subscriptionsList);
    } catch (Exception e) {
      e.printStackTrace();
      DefaultLoader.getUIHelper().showError(e.getMessage(), "Error loading storage account details");
    }

    return dockerStorageAccountMap;
  }


  public Map<String, DockerHost> addDockerHostDetails(DockerHost dockerHost) {
    dockerHostsMap.put(dockerHost.apiUrl, dockerHost);
    dockerHostsList = new ArrayList<>(dockerHostsMap.values());

    return dockerHostsMap;
  }

  public Map<String, DockerHost> refreshDockerHostDetails() {
    try {
      Map<String, DockerHost> localDockerHostsMap = new HashMap<>();
      for (List<DockerHost> dockerHostList : AzureDockerUtils.refreshDockerHostDetails(subscriptionsList, dockerVaultsMap).values()) {
        for (DockerHost dockerHost : dockerHostList) {
          localDockerHostsMap.put(dockerHost.apiUrl, dockerHost);
          if (dockerHost.certVault == null && dockerHostsMap != null) {
            DockerHost oldHost = dockerHostsMap.get(dockerHost.apiUrl);
            if (oldHost != null && oldHost.certVault != null) {
              dockerHost.certVault = oldHost.certVault;
              dockerHost.hasPwdLogIn = oldHost.certVault.vmPwd != null && !oldHost.certVault.vmPwd.isEmpty();
              dockerHost.hasSSHLogIn = oldHost.certVault.sshPubKey != null && !oldHost.certVault.sshPubKey.isEmpty();
              dockerHost.isTLSSecured = oldHost.certVault.tlsServerCert != null && !oldHost.certVault.tlsServerCert.isEmpty();
            }
          }
        }
      }

      dockerHostsMap = localDockerHostsMap;
      dockerHostsList = new ArrayList<>(dockerHostsMap.values());
    } catch (Exception e) {
      e.printStackTrace();
      DefaultLoader.getUIHelper().showError(e.getMessage(), "Error loading virtual machine details");
    }

    return dockerHostsMap;
  }

  public void forceRefreshDockerHosts() {
    // call into Ops to retrieve the latest list of Docker VMs
    refreshDockerHostDetails();

//    dockerHostsList = createNewFakeDockerHostList();
  }

  public void forceRefreshSubscriptions() {
    refreshDockerSubscriptions();

    refreshDockerVaults();

    refreshDockerVaultDetails();

    refreshDockerVnetDetails();

    refreshDockerStorageAccountDetails();

    refreshDockerHostDetails();

    isInitialized = true;

  }

  public List<KnownDockerImages> getDefaultDockerImages() {
    List<KnownDockerImages> dockerImagesList = new ArrayList<>();
    dockerImagesList.add(KnownDockerImages.TOMCAT8);
    dockerImagesList.add(KnownDockerImages.TOMCAT8_DEBUG);
    dockerImagesList.add(KnownDockerImages.OPENJDK_LATEST);
    dockerImagesList.add(KnownDockerImages.OPENJDK_LATEST_DEBUG);
    dockerImagesList.add(KnownDockerImages.OPENJDK_7);
    dockerImagesList.add(KnownDockerImages.OPENJDK_7_DEBUG);
    dockerImagesList.add(KnownDockerImages.OPENJDK_8);
    dockerImagesList.add(KnownDockerImages.OPENJDK_8_DEBUG);
    dockerImagesList.add(KnownDockerImages.OPENJDK_9);
    dockerImagesList.add(KnownDockerImages.OPENJDK_9_DEBUG);

    return dockerImagesList;
  }

  public List<AzureDockerCertVault> getDockerKeyVaults() {
    return new ArrayList<>(dockerVaultsMap.values());
  }

  public AzureDockerCertVault getDockerVault(String name) {
    return dockerVaultsMap.get(name);
  }

  public List<String> getResourceGroups(AzureDockerSubscription subscription) {
    if (subscription != null && subscription.azureClient != null) {
      // TODO: use memory cache to retrieve the resource groups
      return AzureDockerUtils.getResourceGroups(subscription.azureClient);
    } else {
      return new ArrayList<>();
    }
  }

  public List<AzureDockerVnet> getNetworksAndSubnets(AzureDockerSubscription subscription) {
    if (subscription != null && subscription.id != null) {
      return dockerNetworkMap.get(subscription.id);
    } else {
      return new ArrayList<>();
    }
  }

  public Map<String, Pair<String, List<String>>> getNetworksAndSubnets(AzureDockerSubscription subscription, String region) {
    if (subscription != null && subscription.azureClient != null) {
      // TODO: use memory cache to retrieve the storage accouns
      return AzureDockerUtils.getVirtualNetworks(subscription.azureClient, region);
    } else {
      return new HashMap<>();
    }
  }

  public List<String> getAvailableStorageAccounts(String sid, String vmImageSizeType) {
    if (sid != null && !sid.isEmpty()) {
      return AzureDockerUtils.getAvailableStorageAccounts(dockerStorageAccountMap.get(sid), vmImageSizeType);
    } else {
      return new ArrayList<>();
    }
  }

  public DockerHost createNewDockerHostDescription(String name) {
    // TODO: limit the number of characters within the name
    DockerHost host = new DockerHost();
    host.name = name.toLowerCase();
    host.apiUrl = "http://" + name.toLowerCase() + ".centralus.cloudapp.azure.com";
    host.port = "2376"; /* Default Docker dockerHost port when TLS is enabled, "2375" otherwise */
    host.state = DockerHost.DockerHostVMState.TO_BE_CREATED;
    host.hostOSType = (dockerPreferredSettings != null && dockerPreferredSettings.vmOS != null) ? DockerHost.DockerHostOSType.valueOf(dockerPreferredSettings.vmOS) : DockerHost.DockerHostOSType.UBUNTU_SERVER_16_04_LTS;
    host.hostVM = new AzureDockerVM();
    host.hostVM.name = host.name;
    host.hostVM.vmSize = (dockerPreferredSettings != null && dockerPreferredSettings.vmSize != null) ? dockerPreferredSettings.vmSize : KnownDockerVirtualMachineSizes.Standard_DS2_v2.name();
    host.hostVM.region = (dockerPreferredSettings != null && dockerPreferredSettings.region != null)? dockerPreferredSettings.region : null;
    host.hostVM.resourceGroupName = name.toLowerCase() + "-rg";
    host.hostVM.vnetName = AzureDockerUtils.getDefaultRandomName(host.name, 20) + "-vnet";
    host.hostVM.vnetAddressSpace = "10.0.0.0/16";
    host.hostVM.subnetName = "subnet1";
    host.hostVM.publicIpName = name.toLowerCase() + "-pip";
    host.hostVM.subnetAddressRange = "10.0.0.0/16";
    host.hostVM.storageAccountName = AzureDockerUtils.getDefaultRandomName(host.name, 22) + "sa";
    host.hostVM.storageAccountType = "Premium_LSR";
    host.hostVM.osDiskName = name.toLowerCase() + "-osdisk.vhd";
    host.hostVM.osHost = AzureDockerUtils.getKnownDockerVirtualMachineImage(host.hostOSType).getAzureOSHost();

    host.hasPwdLogIn = true;
    host.hasSSHLogIn = true;
    host.isTLSSecured = true;
    host.hasKeyVault = true;

    host.certVault = AzureDockerCertVaultOps.generateSSHKeys(null, "SSH key for " + name);
    host.certVault.name = AzureDockerUtils.getDefaultRandomName(host.name.toLowerCase(), 22) + "kv";
    host.certVault.hostName = host.hostVM.name;
    host.certVault.resourceGroupName = host.hostVM.resourceGroupName;
    host.certVault.region = host.hostVM.region;
    host.certVault.uri = "https://" + host.certVault.name + "." + host.certVault.resourceGroupName + ".azure.net";
    if (AzureDockerUtils.hasServicePrincipalAzureManager(azureAuthManager)) {
      host.certVault.userId = null;
      host.certVault.servicePrincipalId = userId;
    } else {
      host.certVault.userId = userId;
      host.certVault.servicePrincipalId = null;
    }
    host.certVault.vmUsername = "dockerUser";
    host.certVault.vmPwd = AzureDockerUtils.getDefaultRandomName(name);
    AzureDockerCertVaultOps.copyVaultTlsCerts(host.certVault, AzureDockerCertVaultOps.generateTLSCerts("TLS certs for " + name));

    host.hostVM.tags = new HashMap<>();
//    dockerHost.hostVM.tags.put("dockerhost", dockerHost.port);
//    dockerHost.hostVM.tags.put("dockervault", dockerHost.certVault.name);

    return host;
  }

  public AzureDockerImageInstance getDefaultDockerImageDescription(String projectName, DockerHost dockerHost) {
    AzureDockerImageInstance dockerImageDescription = new AzureDockerImageInstance();
    dockerImageDescription.dockerImageName = AzureDockerUtils.getDefaultDockerImageName(projectName).toLowerCase();
    dockerImageDescription.dockerContainerName = AzureDockerUtils.getDefaultDockerContainerName(dockerImageDescription.dockerImageName);
    dockerImageDescription.artifactName = AzureDockerUtils.getDefaultArtifactName(projectName).toLowerCase();
    dockerImageDescription.isHttpsWebApp = false;
    dockerImageDescription.hasNewDockerHost = false;
    if (dockerHost != null) {
      dockerImageDescription.host = dockerHost;
      dockerImageDescription.sid = dockerHost.sid;
    } else {
      dockerImageDescription.host = createNewDockerHostDescription(AzureDockerUtils.getDefaultRandomName(AzureDockerUtils.getDefaultName(projectName)));
//      dockerImageDescription.hasNewDockerHost = true;
    }
    if (dockerPreferredSettings != null) {
      dockerImageDescription.predefinedDockerfile = dockerPreferredSettings.dockerfileOption;
    }

    return dockerImageDescription;
  }

  /* Retrieves a Docker dockerHost object for a given API
   *   The API URL is unique and can be safely used to get a specific docker dockerHost description
   */
  public DockerHost getDockerHostForURL(String apiURL) {
    return dockerHostsMap.get(apiURL);
  }

  public void updateDockerHost(DockerHost host) {
    if (host != null) {
      dockerHostsMap.put(host.apiUrl, host);
      dockerHostsList = new ArrayList<>(dockerHostsMap.values());
    }
  }

  public List<String> getDockerVMStates() {
    List<String> result = new ArrayList<>();
    for (DockerHost.DockerHostVMState state : DockerHost.DockerHostVMState.values()) {
      result.add(state.name());
    }

    return result;
  }

  public static List<String> getDockerVMStateToActionList(DockerHost.DockerHostVMState currentVMState) {
    if (currentVMState == DockerHost.DockerHostVMState.RUNNING) {
      return Arrays.asList(currentVMState.toString(), "Stop", "Restart", "Delete");
    } else if (currentVMState == DockerHost.DockerHostVMState.STOPPED) {
      return Arrays.asList(currentVMState.toString(), "Start", "Delete");
    } else if (currentVMState == DockerHost.DockerHostVMState.UNKNOWN) {
      return Arrays.asList(currentVMState.toString(), "Stop", "Restart", "Delete");
    } else {
      return Arrays.asList(currentVMState.toString());
    }
  }

  public void updateDockerHost(DockerHost originalDockerHost, DockerHost updatedDockerHost) {
    try {
      Thread.sleep(20000);
    } catch (Exception ignored) {}
  }

}
