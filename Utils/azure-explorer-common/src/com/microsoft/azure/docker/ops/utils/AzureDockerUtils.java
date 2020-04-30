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

package com.microsoft.azure.docker.ops.utils;

import com.microsoft.azure.docker.model.*;
import com.microsoft.azure.docker.ops.AzureDockerCertVaultOps;
import com.microsoft.azure.docker.ops.AzureDockerVMOps;
import com.microsoft.azure.keyvault.KeyVaultClient;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.keyvault.Vault;
import com.microsoft.azure.management.network.Network;
import com.microsoft.azure.management.network.Subnet;
import com.microsoft.azure.management.resources.Location;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azure.management.storage.SkuName;
import com.microsoft.azure.management.storage.StorageAccount;
import com.microsoft.azure.management.storage.StorageAccountSkuType;
import com.microsoft.azuretools.authmanage.SubscriptionManager;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.azuretools.sdkmanage.ServicePrincipalAzureManager;
import com.microsoft.azuretools.utils.AzureRegisterProviderNamespaces;
import com.microsoft.azuretools.utils.Pair;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

public class AzureDockerUtils {
  public static boolean DEBUG = false;

  public static Boolean isValid(String str) {
    return str != null && !str.isEmpty();
  }

  public static KnownDockerVirtualMachineImage getKnownDockerVirtualMachineImage(DockerHost.DockerHostOSType dockerHostOSType) {
    switch (dockerHostOSType) {
      case UBUNTU_SERVER_14_04_LTS:
        return KnownDockerVirtualMachineImage.UBUNTU_SERVER_14_04_LTS;
      case UBUNTU_SERVER_16_04_LTS:
        return KnownDockerVirtualMachineImage.UBUNTU_SERVER_16_04_LTS;
//      case UBUNTU_SNAPPY_CORE_15_04:
//        return KnownDockerVirtualMachineImage.UBUNTU_SNAPPY_CORE_15_04;
//      case COREOS_STABLE_LATEST:
//        return KnownDockerVirtualMachineImage.COREOS_STABLE_LATEST;
//      case OPENLOGIC_CENTOS_7_2:
//        return KnownDockerVirtualMachineImage.OPENLOGIC_CENTOS_7_2;
      default:
        return KnownDockerVirtualMachineImage.UBUNTU_SERVER_16_04_LTS;
    }
  }

  public static String getPathSeparator(){
    //generate TLS certs using keytool
    return System.getProperty("os.name").toLowerCase().contains("windows") ? "\\" : "/";
  }

  public static String getDefaultDockerHostName() {
    return String.format("%s%d", "mydocker", new Random().nextInt(1000000));
  }

  public final static int MAX_RESOURCE_LENGTH = 16;

  public static String getDefaultRandomName(String namePrefix) {
    if (namePrefix.length() > MAX_RESOURCE_LENGTH) {
      return String.format("%s%d", namePrefix.substring(0, MAX_RESOURCE_LENGTH).toLowerCase(), new Random().nextInt(1000000));
    } else {
      return String.format("%s%d", namePrefix.toLowerCase(), new Random().nextInt(1000000));
    }
  }

  public static String getDefaultRandomName(String namePrefix, int maxLength) {
    String randInt = String.format("%d", new Random().nextInt(1000000));
    if (maxLength <= randInt.length()) {
      return null;
    }
    if (namePrefix.length() + randInt.length() > maxLength) {
      return namePrefix.substring(0, maxLength - randInt.length()) + randInt;
    } else {
      return namePrefix + randInt;
    }
  }

  public static String getDefaultName(String projectName) {
    return projectName.toLowerCase().replaceAll("[^a-z0-9]", "");
  }

  public static String getDefaultDockerImageName(String projectName) {
    return String.format("%s%d", getDefaultName(projectName), new Random().nextInt(10000));
  }

  public static String getDefaultDockerContainerName(String imageName) {
    return String.format("%s-%d", imageName.toLowerCase(), new Random().nextInt(10000));
  }

  public static String getDefaultArtifactName(String projectName) {
    return getDefaultName(projectName) + ".war";
  }

  public static String getUrl(AzureDockerImageInstance dockerImageInstance) {
    return String.format("%s://%s:%s/%s", (dockerImageInstance.isHttpsWebApp ? "https" : "http"),
        dockerImageInstance.host.hostVM.dnsName,
        dockerImageInstance.dockerPortSettings.split(":")[0], // "12345:80/tcp" -> "12345"
        dockerImageInstance.hasRootDeployment ? "" : dockerImageInstance.artifactName);
  }

  public static boolean checkKeyvaultNameAvailability(String name) {
    try {
      URL url = new URL("https://" + name + ".vault.azure.net");
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod("GET");
      connection.connect();

      return connection.getResponseCode() != 403;
    } catch (UnknownHostException uhe) {
      return true;
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  public static boolean checkDockerContainerUrlAvailability(String containerUrl) {
    try {
      URL url = new URL(containerUrl);
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod("GET");
      connection.connect();

      return connection.getResponseCode() == 200;
    } catch (Exception e) {
      return false;
    }
  }

  public static List<String> getResourceGroups(Azure azureClient) {
    List<String> result = new ArrayList<>();
    if (azureClient != null) {
      for (ResourceGroup resourceGroup : azureClient.resourceGroups().list()) {
        result.add(resourceGroup.name());
      }
    }

    return result;
  }

  public static List<AzureDockerVnet> getVirtualNetworks(Azure azureClient) {
    List<AzureDockerVnet> result = new ArrayList<>();
    if (azureClient != null) {
      for (Network net : azureClient.networks().list()) {
        AzureDockerVnet vnet = new AzureDockerVnet();
        vnet.name = net.name();
        vnet.addrSpace = net.addressSpaces().get(0);
        vnet.id = net.id();
        vnet.region = net.regionName().toLowerCase();
        vnet.resourceGroup = net.resourceGroupName();
        vnet.subnets = new ArrayList<>();
        for (Subnet subnet : net.subnets().values()) {
          vnet.subnets.add(subnet.name());
        }
        result.add(vnet);
      }
    }

    return result;
  }

  public static Map<String, Pair<String, List<String>>> getVirtualNetworks(Azure azureClient, String region) {
    Map<String, Pair<String, List<String>>> result = new HashMap<>();
    if (azureClient != null) {
      for (Network vnet : azureClient.networks().list()) {
        if (vnet.regionName().toLowerCase().equals(region) || vnet.regionName().toLowerCase().equals(region)) {
          List<String> subnets = new ArrayList<>();
          subnets.addAll(vnet.subnets().keySet());
          result.put(vnet.id(), new Pair<>(vnet.name(), subnets));
        }
      }
    }

    return result;
  }

  public static List<String> getAvailableStorageAccounts(List<AzureDockerStorageAccount> storageAccounts, String vmImageSizeType) {
    List<String> result = new ArrayList<>();

    if (storageAccounts != null) {
      for (AzureDockerStorageAccount storageAccount : storageAccounts) {
        if (vmImageSizeType != null) {
          if (storageAccount.skuType.toLowerCase().startsWith(vmImageSizeType.toLowerCase())) {
            result.add(storageAccount.name);
          }
        } else {
          result.add(storageAccount.name);
        }
      }
    }

    return result;
  }

  public static List<String> getAvailableStorageAccounts(Azure azureClient, String vmImageSizeType) {
    List<String> result = new ArrayList<>();

    if (azureClient != null) {
      for (StorageAccount storageAccount : azureClient.storageAccounts().list()) {
        if (vmImageSizeType != null) {
          if (storageAccount.skuType().name() != null && storageAccount.skuType().name().equals(SkuName.fromString(vmImageSizeType.toLowerCase()))) {
            result.add(storageAccount.name());
          }
        } else {
          result.add(storageAccount.name());
        }
      }
    }

    return result;
  }

  public static List<AzureDockerStorageAccount> getStorageAccounts(Azure azureClient) {
    List<AzureDockerStorageAccount> result = new ArrayList<>();

    if (azureClient != null) {
      for (StorageAccount storageAccount : azureClient.storageAccounts().list()) {
        AzureDockerStorageAccount dockerStorageAccount = new AzureDockerStorageAccount();
        dockerStorageAccount.name = storageAccount.name();
        dockerStorageAccount.region = storageAccount.regionName();
        dockerStorageAccount.resourceGroup = storageAccount.resourceGroupName();
        dockerStorageAccount.sid = azureClient.subscriptionId();
        dockerStorageAccount.skuType = storageAccount.skuType().name().toString();
        result.add(dockerStorageAccount);
      }
    }

    return result;
  }

  public static boolean checkStorageNameAvailability(Azure azureClient, String name) {
    if (azureClient != null) {
      return azureClient.storageAccounts().checkNameAvailability(name).isAvailable();
    } else {
      return false;
    }
  }

  public static String getStorageTypeForVMSize(String name) {
    return (name == null) ? null : name.contains("_DS") ? "Premium_LSR" : "Standard_LSR";
  }

  public static boolean hasServicePrincipalAzureManager(AzureManager azureAuthManager){
    return azureAuthManager.getClass().equals(ServicePrincipalAzureManager.class);
  }

  public static Map<String, AzureDockerSubscription> refreshDockerSubscriptions(AzureManager azureAuthManager) {
    Map<String, AzureDockerSubscription> subsMap = new HashMap<>();

    try {
      if (DEBUG) System.out.format("Get AzureDockerHostsManage subscription details: %s\n", new Date().toString());
      SubscriptionManager subscriptionManager = azureAuthManager.getSubscriptionManager();
      List<SubscriptionDetail> subscriptions = subscriptionManager.getSubscriptionDetails();

      if (subscriptions != null) {
        if (DEBUG) System.out.format("Get AzureDockerHostsManage Docker subscription details: %s\n", new Date().toString());

        Observable.from(subscriptions).flatMap(subscriptionDetail -> {
          return Observable.create(new Observable.OnSubscribe<AzureDockerSubscription>() {
            @Override
            public void call(Subscriber<? super AzureDockerSubscription> dockerSubscriptionSubscriber) {
              if(subscriptionDetail.isSelected()) {
                AzureDockerSubscription dockerSubscription = new AzureDockerSubscription();
                dockerSubscription.id = subscriptionDetail.getSubscriptionId();
                try {
                  if (DEBUG)
                    System.out.format("\tGet AzureDockerHostsManage Docker subscription: %s at %s\n", dockerSubscription.id, new Date().toString());
                  dockerSubscription.tid = subscriptionDetail.getTenantId();
                  dockerSubscription.name = subscriptionDetail.getSubscriptionName();
                  dockerSubscription.azureClient = azureAuthManager.getAzure(dockerSubscription.id);
                  dockerSubscription.keyVaultClient = azureAuthManager.getKeyVaultClient(subscriptionDetail.getTenantId());
                  dockerSubscription.isSelected = true;
                  if (AzureDockerUtils.hasServicePrincipalAzureManager(azureAuthManager)) {
                    dockerSubscription.userId = null;
                    dockerSubscription.servicePrincipalId = azureAuthManager.getCurrentUserId();
                  } else {
                    dockerSubscription.userId = azureAuthManager.getCurrentUserId();
                    dockerSubscription.servicePrincipalId = null;
                  }

                  dockerSubscriptionSubscriber.onNext(dockerSubscription);
                } catch (Exception e) {
                  e.printStackTrace();
                  DefaultLoader.getUIHelper().showError(e.getMessage(), "Error Loading Subscription Details for " + dockerSubscription.id);
                }
              }
              dockerSubscriptionSubscriber.onCompleted();
            }
          }).subscribeOn(Schedulers.io());
        }).subscribeOn(Schedulers.io())
            .toBlocking().subscribe(new Action1<AzureDockerSubscription>() {
          @Override
          public void call(AzureDockerSubscription dockerSubscription) {
            subsMap.put(dockerSubscription.id, dockerSubscription);
          }
        });
      }

      if (DEBUG) System.out.format("Get AzureDockerHostsManage locations: %s\n", new Date().toString());
      List<Subscription> azureSubscriptionList = azureAuthManager.getSubscriptions();
      for (Subscription subscription : azureSubscriptionList) {
        AzureDockerSubscription dockerSubscription = subsMap.get(subscription.subscriptionId());

        if (dockerSubscription != null) {
          List<String> locations = subscription.listLocations().stream().sorted(Comparator.comparing(Location::displayName))
                  .map(o -> o.name().toLowerCase()).collect(Collectors.toList());
          dockerSubscription.locations = locations;
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      DefaultLoader.getUIHelper().showError(e.getMessage(), "Error loading subscription details");
    }

    return subsMap;
  }

  public static Map<String, Pair<Vault, KeyVaultClient>> refreshDockerVaults(List<AzureDockerSubscription> azureDockerSubscriptions) {
    Map<String, Pair<Vault, KeyVaultClient>> vaults = new HashMap<>();

    if (DEBUG) System.out.format("\tGet AzureDockerHostsManage Docker key vault: %s\n", new Date().toString());
    try {
      for (AzureDockerSubscription dockerSubscription : azureDockerSubscriptions) {
        // TODO
        for (ResourceGroup group : dockerSubscription.azureClient.resourceGroups().list()) {
          for (Vault vault : dockerSubscription.azureClient.vaults().listByResourceGroup(group.name())) {
            if (DEBUG) System.out.format("\tGet AzureDockerHostsManage Docker vault: %s at %s\n", vault.name(), new Date().toString());

            if (vault.tags().get("dockerhost") != null) {
              if (DEBUG) System.out.format("\t\t...adding Docker vault: %s at %s\n", vault.name(), new Date().toString());
              vaults.put(vault.name(), new Pair<>(vault, dockerSubscription.keyVaultClient));
            }
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      DefaultLoader.getUIHelper().showError(e.getMessage(), "Error loading key vaults");
    }

    return vaults;
  }

  public static Map<String, AzureDockerCertVault> refreshDockerVaultDetails(List<AzureDockerSubscription> azureDockerSubscriptions) {
    Map<String, AzureDockerCertVault> dockerVaultDetails = new HashMap<>();

    if (DEBUG) System.out.format("\tGet AzureDockerHostsManage Docker key vault details: %s\n", new Date().toString());
    try {


      Observable.from(azureDockerSubscriptions).flatMap(dockerSubscription -> {
        return Observable.create(new Observable.OnSubscribe<AzureDockerCertVault>() {
          @Override
          public void call(Subscriber<? super AzureDockerCertVault> vaultSubscriber) {
            dockerSubscription.azureClient.resourceGroups().listAsync().flatMap(new Func1<ResourceGroup, Observable<Vault>>() {
              @Override
              public Observable<Vault> call(ResourceGroup resourceGroup) {
                return dockerSubscription.azureClient.vaults().listByResourceGroupAsync(resourceGroup.name());
              }
            }).subscribeOn(Schedulers.io()).toBlocking().subscribe(new Action1<Vault>() {
              @Override
              public void call(Vault vaultWithInner) {
                if (DEBUG) System.out.format("\tGet AzureDockerHostsManage Docker vault details for: %s at %s\n", vaultWithInner.name(), new Date().toString());

                try {
                  AzureDockerCertVault certVault = new AzureDockerCertVault();
                  certVault.name = vaultWithInner.name();
                  certVault.id = vaultWithInner.id();
                  certVault.resourceGroupName = vaultWithInner.resourceGroupName();
                  certVault.userId = dockerSubscription.userId;
                  certVault.servicePrincipalId = dockerSubscription.servicePrincipalId;
                  certVault.region = vaultWithInner.regionName();
                  certVault.uri = vaultWithInner.vaultUri();
                  AzureDockerCertVault certVaultTemp = AzureDockerCertVaultOps.getVault(certVault, dockerSubscription.keyVaultClient);
                  if (certVaultTemp == null) {
                    try {
                      // try to assign read permissions to the key vault in case it was created with a different service principal
                      AzureDockerCertVaultOps.setVaultPermissionsRead(dockerSubscription.azureClient, certVault);
                      certVault = AzureDockerCertVaultOps.getVault(certVault, dockerSubscription.keyVaultClient);
                    } catch (Exception ignored) {}
                  } else {
                    certVault = certVaultTemp;
                  }

                  if (certVault != null && certVault.hostName != null) {
                    vaultSubscriber.onNext(certVault);
                  }
                } catch (Exception e) {
                  e.printStackTrace();
                }
              }
            });
            vaultSubscriber.onCompleted();
          }
        }).subscribeOn(Schedulers.io());
      }, 5).subscribeOn(Schedulers.io())
              .toBlocking().subscribe(new Action1<AzureDockerCertVault>() {
                @Override
                public void call(AzureDockerCertVault certVault) {
                  if (certVault != null && certVault.hostName != null && certVault.name != null) {
                    if (DEBUG)
                      System.out.format("\t\t...adding Docker vault details: %s at %s\n", certVault.name, new Date().toString());
                    dockerVaultDetails.put(certVault.name, certVault);
                  }
                }
              });
      if (DEBUG) System.out.format("\tDone getting AzureDockerHostsManage Docker key vault details: %s\n", new Date().toString());

    } catch (Exception e) {
      e.printStackTrace();
      DefaultLoader.getUIHelper().showError(e.getMessage(), "Error loading key vault details");
    }

    return dockerVaultDetails;
  }

  public static Map<String, List<AzureDockerVnet>> refreshDockerVnetDetails(List<AzureDockerSubscription> azureDockerSubscriptions) {
    Map<String, List<AzureDockerVnet>> vnetMaps = new HashMap<>();

    if (DEBUG) System.out.format("\tGet AzureDockerHostsManage Docker virtual network details: %s\n", new Date().toString());
    try {
      for (AzureDockerSubscription dockerSubscription : azureDockerSubscriptions) {
        vnetMaps.put(dockerSubscription.id, AzureDockerUtils.getVirtualNetworks(dockerSubscription.azureClient));
      }
    } catch (Exception e) {
      e.printStackTrace();
      DefaultLoader.getUIHelper().showError(e.getMessage(), "Error loading virtual network details");
    }

    return vnetMaps;
  }

  public static Map<String, List<AzureDockerStorageAccount>> refreshDockerStorageAccountDetails(List<AzureDockerSubscription> azureDockerSubscriptions) {
    Map<String, List<AzureDockerStorageAccount>> storageMaps = new HashMap<>();

    if (DEBUG) System.out.format("\tGet AzureDockerHostsManage Docker storage account details: %s\n", new Date().toString());
    try {
      for (AzureDockerSubscription dockerSubscription : azureDockerSubscriptions) {
        storageMaps.put(dockerSubscription.id, AzureDockerUtils.getStorageAccounts(dockerSubscription.azureClient));
      }
    } catch (Exception e) {
      e.printStackTrace();
      DefaultLoader.getUIHelper().showError(e.getMessage(), "Error loading storage account details");
    }

    return storageMaps;
  }

  public static Map<String, List<DockerHost>> refreshDockerHostDetails(List<AzureDockerSubscription> azureDockerSubscriptions, Map<String, AzureDockerCertVault> dockerVaultsMap) {
    Map<String, List<DockerHost>> dockerHosts = new HashMap<>();

    if (DEBUG) System.out.format("\tGet AzureDockerHostsManage Docker virtual machine details: %s\n", new Date().toString());
    try {
      for (AzureDockerSubscription dockerSubscription : azureDockerSubscriptions) {
        dockerHosts.put(dockerSubscription.id, new ArrayList<>(AzureDockerVMOps.getDockerHosts(dockerSubscription.azureClient, dockerVaultsMap).values()));
      }
    } catch (Exception e) {
      e.printStackTrace();
      DefaultLoader.getUIHelper().showError(e.getMessage(), "Error loading virtual machine details");
    }

    return dockerHosts;
  }
}
