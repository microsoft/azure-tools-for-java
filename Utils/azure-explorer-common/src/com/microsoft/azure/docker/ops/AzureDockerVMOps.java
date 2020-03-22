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
package com.microsoft.azure.docker.ops;

import com.jcraft.jsch.Session;
import com.microsoft.azure.docker.model.*;
import com.microsoft.azure.docker.ops.utils.AzureDockerUtils;
import com.microsoft.azure.keyvault.KeyVaultClient;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.keyvault.Vault;
import com.microsoft.azure.management.network.*;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.management.resources.fluentcore.arm.ResourceUtils;
import com.microsoft.azure.management.resources.fluentcore.utils.ResourceNamer;
import com.microsoft.azure.management.storage.StorageAccount;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.Environment;
import com.microsoft.azuretools.sdkmanage.AzureManager;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.*;

import static com.microsoft.azure.docker.ops.utils.AzureDockerUtils.DEBUG;
import static com.microsoft.azure.docker.ops.utils.AzureDockerUtils.isValid;
import static com.microsoft.azure.docker.ops.utils.AzureDockerVMSetupScriptsForUbuntu.*;

public class AzureDockerVMOps {

  public static VirtualMachine updateDockerHostVM(Azure azureClient, DockerHost dockerHost) throws AzureDockerException {
    try {
      VirtualMachine vm = azureClient.virtualMachines().getByResourceGroup(dockerHost.hostVM.resourceGroupName, dockerHost.hostVM.name);
      HashMap<String, Object> protectedSettings = new HashMap<>();
      protectedSettings.put("username", dockerHost.certVault.vmUsername);
      if (dockerHost.hasPwdLogIn) {
        protectedSettings.put("password", dockerHost.certVault.vmPwd);
      }
      if (dockerHost.hasSSHLogIn) {
        protectedSettings.put("ssh_key", dockerHost.certVault.sshPubKey);
      }
      protectedSettings.put("reset_ssh", "true");

      if (vm.listExtensions().get("VMAccessForLinux") != null) {
        vm.update()
            .updateExtension("VMAccessForLinux")
                .withProtectedSettings(protectedSettings)
            .parent()
            .withoutTag("dockervault")
            .apply();
      } else {
        vm.update()
            .defineNewExtension("VMAccessForLinux")
                .withPublisher("Microsoft.OSTCExtensions")
                .withType("VMAccessForLinux")
                .withVersion("1.4")
                .withProtectedSettings(protectedSettings)
            .attach()
            .withoutTag("dockervault")
            .apply();
      }

      return vm;
    } catch (Exception e) {
      throw new AzureDockerException(e.getMessage(), e);
    }
  }

  public static VirtualMachine createDockerHostVM(Azure azureClient, DockerHost newHost) throws AzureDockerException {
    try {
      String resourceGroupName;
      if (newHost.hostVM.resourceGroupName.contains("@")) {
        // Existing resource group
        resourceGroupName = newHost.hostVM.resourceGroupName.split("@")[0];
      } else {
        // Create a new resource group
        resourceGroupName = newHost.hostVM.resourceGroupName;
        ResourceGroup resourceGroup = azureClient.resourceGroups()
            .define(newHost.hostVM.resourceGroupName)
            .withRegion(newHost.hostVM.region)
            .create();
      }

      Network vnet;
      if (newHost.hostVM.vnetName.contains("@")) {
        // reuse existing virtual network
        String vnetName = newHost.hostVM.vnetName.split("@")[0];
        String vnetResourceGroupName = newHost.hostVM.vnetName.split("@")[1];
        vnet = azureClient.networks().getByResourceGroup(vnetResourceGroupName, vnetName);
      } else {
        // create a new virtual network (a subnet will be automatically created as part of this)
        vnet = azureClient.networks()
            .define(newHost.hostVM.vnetName)
            .withRegion(newHost.hostVM.region)
            .withExistingResourceGroup(resourceGroupName)
            .withAddressSpace(newHost.hostVM.vnetAddressSpace)
            .create();
      }

      VirtualMachine.DefinitionStages.WithLinuxRootPasswordOrPublicKeyManagedOrUnmanaged defStage1 = azureClient.virtualMachines()
          .define(newHost.hostVM.name)
          .withRegion(newHost.hostVM.region)
          .withExistingResourceGroup(resourceGroupName)
          .withExistingPrimaryNetwork(vnet)
          .withSubnet(newHost.hostVM.subnetName)
          .withPrimaryPrivateIPAddressDynamic()
          .withNewPrimaryPublicIPAddress(newHost.hostVM.name)
          .withSpecificLinuxImageVersion(newHost.hostVM.osHost.imageReference())
          .withRootUsername(newHost.certVault.vmUsername);

      VirtualMachine.DefinitionStages.WithLinuxCreateManagedOrUnmanaged defStage2;
      if (newHost.hasPwdLogIn && newHost.hasSSHLogIn) {
        defStage2 = defStage1
            .withRootPassword(newHost.certVault.vmPwd)
            .withSsh(newHost.certVault.sshPubKey);
      } else {
        defStage2 = (newHost.hasSSHLogIn) ?
            defStage1.withSsh(newHost.certVault.sshPubKey) :
            defStage1.withRootPassword(newHost.certVault.vmPwd);
      }
      // todo - temporary not using managed disks as we do not support them yet for docker hosts
      VirtualMachine.DefinitionStages.WithCreate defStage3 = null;
      if (newHost.hostVM.storageAccountName.contains("@")) {
        // Existing storage account
        for (StorageAccount item : azureClient.storageAccounts().list()) {
          String storageAccountName = item.name() + "@";
          if (storageAccountName.equals(newHost.hostVM.storageAccountName)) {
            defStage3 = defStage2.withUnmanagedDisks().withExistingStorageAccount(item);
            break;
          }
        }
        if (defStage3 == null)
          throw new AzureDockerException("Can't find storage account " + newHost.hostVM.storageAccountName.split("@")[0]);
      } else {
        defStage3 = defStage2.withUnmanagedDisks().withNewStorageAccount(newHost.hostVM.storageAccountName);
      }
      defStage3 = defStage3.withSize(newHost.hostVM.vmSize);

      defStage3 = defStage3.withTag("dockerhost", newHost.port);
      if (newHost.hasKeyVault) {
        defStage3 = defStage3.withTag("dockervault", newHost.certVault.name);
      }

      return defStage3.create();
    } catch (Exception e) {
      throw new AzureDockerException(e.getMessage(), e);
    }
  }

  public static AzureDockerVM getDockerVM(VirtualMachine vm) {
    try {
      NicIPConfiguration nicIPConfiguration = vm.getPrimaryNetworkInterface().primaryIPConfiguration();
      PublicIPAddress publicIp = nicIPConfiguration.getPublicIPAddress();
      Network vnet = nicIPConfiguration.getNetwork();
      AzureDockerVM dockerVM = new AzureDockerVM();

      dockerVM.name = vm.name();
      // TODO: Azure cloud bug; the resource group name in the id's for the VM's when retrieving as a list is capitalized!
      dockerVM.resourceGroupName = vm.resourceGroupName().toLowerCase();
      dockerVM.region = vm.regionName();
      dockerVM.availabilitySet = (vm.availabilitySetId() != null) ? ResourceUtils.nameFromResourceId(vm.availabilitySetId()) : null;
      dockerVM.privateIp = nicIPConfiguration.privateIPAddress();
      if (publicIp != null) {
        dockerVM.publicIpName = publicIp.name();
        dockerVM.publicIp = publicIp.ipAddress();

        // TODO: since this feature will be removed later, just fix it here for mooncake.
        String dnsSuffix = ".cloudapp.azure.com";
        try {
            AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
            if (azureManager != null && azureManager.getEnvironment().equals(Environment.CHINA)) {
                dnsSuffix = ".cloudapp.chinacloudapi.cn";
            }
        } catch(Exception e){}

        dockerVM.dnsName = (publicIp.fqdn() != null && !publicIp.fqdn().isEmpty()) ?
            publicIp.fqdn() :
            (publicIp.ipAddress() != null && !publicIp.ipAddress().isEmpty()) ?
                publicIp.ipAddress() :
                dockerVM.name + "." + dockerVM.region + dnsSuffix;
      } else {
        dockerVM.publicIpName = "NA";
        dockerVM.publicIp = "";
        dockerVM.dnsName = dockerVM.privateIp;
      }
      dockerVM.nicName = vm.getPrimaryNetworkInterface().name();
      dockerVM.vnetName = vnet.name();
      dockerVM.vnetAddressSpace = vnet.addressSpaces().get(0);
      dockerVM.subnetName = nicIPConfiguration.subnetName();
      dockerVM.subnetAddressRange = vnet.subnets().get(dockerVM.subnetName).addressPrefix();
      dockerVM.networkSecurityGroupName = (nicIPConfiguration.parent().networkSecurityGroupId() != null) ? ResourceUtils.nameFromResourceId(nicIPConfiguration.parent().networkSecurityGroupId()) : null;
      dockerVM.vmSize = vm.size().toString();
      dockerVM.osDiskName = vm.storageProfile().osDisk().name();
      if (vm.storageProfile().imageReference() != null) {
        dockerVM.osHost = new AzureOSHost(vm.storageProfile().imageReference());
      }
      if (vm.storageProfile().osDisk().managedDisk() != null) {
        dockerVM.storageAccountName = vm.storageProfile().osDisk().name();
        dockerVM.storageAccountType = "Managed disk";
      } else {
        dockerVM.storageAccountName = vm.storageProfile().osDisk().vhd().uri().split("[.]")[0].split("/")[2];
        dockerVM.storageAccountType = AzureDockerUtils.getStorageTypeForVMSize(dockerVM.vmSize);
      }
      // "PowerState/running" -> "RUNNING"
      String powerState = (vm.powerState() != null) ? vm.powerState().toString() : "UNKNOWN/UNKNOWN";
      dockerVM.state =  powerState.contains("/") ? powerState.split("/")[1].toUpperCase() : "UNKNOWN";
      dockerVM.tags = vm.tags();

      return dockerVM;
    } catch (Exception e) {
      throw new AzureDockerException(e.getMessage(), e);
    }
  }

  public static AzureDockerVM getDockerVM(Azure azureClient, String resourceGroup, String hostName) {
    try {
      AzureDockerVM azureDockerVM = getDockerVM(azureClient.virtualMachines().getByResourceGroup(resourceGroup, hostName));
      azureDockerVM.sid = azureClient.subscriptionId();

      return azureDockerVM;
    } catch (Exception e) {
      throw new AzureDockerException(e.getMessage(), e);
    }
  }

  public static VirtualMachine getVM(Azure azureClient, String resourceGroup, String hostName) throws AzureDockerException {
    try {
      return azureClient.virtualMachines().getByResourceGroup(resourceGroup, hostName);
    } catch (Exception e) {
      throw new AzureDockerException(e.getMessage(), e);
    }
  }

  public static boolean isDeletingDockerHostAllSafe(Azure azureClient, String resourceGroup, String vmName) {
    if (azureClient == null || resourceGroup == null || vmName == null ) {
      return false;
    }

    VirtualMachine vm = azureClient.virtualMachines().getByResourceGroup(resourceGroup, vmName);
    if (vm == null) {
      return false;
    }

    PublicIPAddress publicIp = vm.getPrimaryPublicIPAddress();
    NicIPConfiguration nicIPConfiguration = publicIp.getAssignedNetworkInterfaceIPConfiguration();
    Network vnet = nicIPConfiguration.getNetwork();
    NetworkInterface nic = vm.getPrimaryNetworkInterface();

    return nic.ipConfigurations().size() == 1 &&
        vnet.subnets().size() == 1  &&
        vnet.subnets().values().toArray(new Subnet[1])[0].inner().ipConfigurations().size() == 1;
  }

  public static void deleteDockerHostAll(Azure azureClient, String resourceGroup, String vmName) {
    if (azureClient == null || resourceGroup == null || vmName == null ) {
      throw new AzureDockerException("Unexpected param values; Azure instance, resource group and VM name cannot be null");
    }

    VirtualMachine vm = azureClient.virtualMachines().getByResourceGroup(resourceGroup, vmName);
    if (vm == null) {
      throw new AzureDockerException(String.format("Unexpected error retrieving VM %s from Azure", vmName));
    }

    try {
      PublicIPAddress publicIp = vm.getPrimaryPublicIPAddress();
      NicIPConfiguration nicIPConfiguration = publicIp.getAssignedNetworkInterfaceIPConfiguration();
      Network vnet = nicIPConfiguration.getNetwork();
      NetworkInterface nic = vm.getPrimaryNetworkInterface();

      azureClient.virtualMachines().deleteById(vm.id());
      azureClient.networkInterfaces().deleteById(nic.id());
      azureClient.publicIPAddresses().deleteById(publicIp.id());
      azureClient.networks().deleteById(vnet.id());
    } catch (Exception e) {
      throw new AzureDockerException(String.format("Unexpected error while deleting VM %s and its associated resources", vmName));
    }
  }

  public static void deleteDockerHost(Azure azureClient, String resourceGroup, String vmName) {
    if (azureClient == null || resourceGroup == null || vmName == null ) {
      throw new AzureDockerException("Unexpected param values; Azure instance, resource group and VM name cannot be null");
    }

    VirtualMachine vm = azureClient.virtualMachines().getByResourceGroup(resourceGroup, vmName);
    if (vm == null) {
      throw new AzureDockerException(String.format("Unexpected error retrieving VM %s from Azure", vmName));
    }

    try {
      azureClient.virtualMachines().deleteById(vm.id());
    } catch (Exception e) {
      throw new AzureDockerException(String.format("Unexpected error while deleting VM %s and its associated resources", vmName));
    }
  }

  public static DockerHost getDockerHost(VirtualMachine vm, Map<String, AzureDockerCertVault> dockerVaultsMap) {
    if (vm.tags().get("dockerhost") != null) {
      DockerHost dockerHost = new DockerHost();
      try {
        dockerHost.hostVM = getDockerVM(vm);
      } catch (Exception e) {
        return null;
      }
      dockerHost.name = dockerHost.hostVM.name;
      dockerHost.apiUrl = dockerHost.hostVM.dnsName;
      dockerHost.port = vm.tags().get("dockerhost");
      if (dockerHost.hostVM.osHost != null) {
        switch (dockerHost.hostVM.osHost.offer) {
          case "Ubuntu_Snappy_Core":
            dockerHost.hostOSType = DockerHost.DockerHostOSType.UBUNTU_SNAPPY_CORE_15_04;
            break;
          case "CoreOS":
            dockerHost.hostOSType = DockerHost.DockerHostOSType.COREOS_STABLE_LATEST;
            break;
          case "CentOS":
            dockerHost.hostOSType = DockerHost.DockerHostOSType.OPENLOGIC_CENTOS_7_2;
            break;
          case "UbuntuServer":
            dockerHost.hostOSType = dockerHost.hostVM.osHost.sku.contains("14.04.4-LTS") ? DockerHost.DockerHostOSType.UBUNTU_SERVER_14_04_LTS : DockerHost.DockerHostOSType.UBUNTU_SERVER_16_04_LTS;
            break;
          default:
            dockerHost.hostOSType = DockerHost.DockerHostOSType.LINUX_OTHER;
            break;
        }
      }
      dockerHost.state = DockerHost.DockerHostVMState.valueOf(dockerHost.hostVM.state);
      dockerHost.hasPwdLogIn = false;
      dockerHost.hasSSHLogIn = false;
      dockerHost.isTLSSecured = false;
      String dockerVaultName = vm.tags().get("dockervault");
      if (dockerVaultName != null && dockerVaultsMap != null) {
        dockerHost.hostVM.vaultName = dockerVaultName;
        AzureDockerCertVault certVault = dockerVaultsMap.get(dockerVaultName);
        if (certVault != null) {
          dockerHost.hasKeyVault = true;
          dockerHost.certVault = certVault;
          dockerHost.hasPwdLogIn = certVault.vmPwd != null && !certVault.vmPwd.isEmpty();
          dockerHost.hasSSHLogIn = certVault.sshPubKey != null && !certVault.sshPubKey.isEmpty();
          dockerHost.isTLSSecured = certVault.tlsServerCert != null && !certVault.tlsServerCert.isEmpty();
        }
      } else {
        dockerHost.hasKeyVault = false;
        dockerHost.certVault = null;
      }
      if (dockerHost.port == null || !dockerHost.port.matches("[0-9]+") ||
          Integer.parseInt(dockerHost.port) < 1 || Integer.parseInt(dockerHost.port) > 65535) {
        dockerHost.port = (dockerHost.isTLSSecured) ? DOCKER_API_PORT_TLS_ENABLED : DOCKER_API_PORT_TLS_DISABLED;
      }
      dockerHost.dockerImages = new HashMap<>();

//      if (dockerHost.certVault != null)
//        try { // it might throw here if the credentials are invalid
//          Map<String, DockerImage> dockerImages = AzureDockerImageOps.getImages(dockerHost);
//          Map<String, DockerContainer> dockerContainers = AzureDockerContainerOps.getContainers(dockerHost);
//          AzureDockerContainerOps.setContainersAndImages(dockerContainers, dockerImages);
//          dockerHost.dockerImages = dockerImages;
//        } catch (Exception e) {}

      return dockerHost;
    }

    return null;
  }

  public static Map<String, DockerHost> getDockerHosts(List<VirtualMachine> virtualMachines, Map<String, AzureDockerCertVault> dockerVaultsMap) {
    Map<String, DockerHost> dockerHostsMap = new HashMap<>();
    for (VirtualMachine vm : virtualMachines) {
      if (vm.tags().get("dockerhost") != null) {
        DockerHost dockerHost = getDockerHost(vm, dockerVaultsMap);
        if (dockerHost != null) {
          dockerHostsMap.put(dockerHost.apiUrl, dockerHost);
        }
      }
    }

    return dockerHostsMap;
  }

  public static Map<String, DockerHost> getDockerHosts(Azure azureClient, Map<String, AzureDockerCertVault> dockerVaultsMap) {
    Map<String, DockerHost> dockerHostMap = getDockerHosts(azureClient.virtualMachines().list(), dockerVaultsMap);
    for (DockerHost dockerHost : dockerHostMap.values()) {
      dockerHost.sid = azureClient.subscriptionId();
      if (dockerHost.hostVM != null) dockerHost.hostVM.sid = azureClient.subscriptionId();
    }

    return dockerHostMap;
  }

  public static void installDocker(DockerHost dockerHost) {
    if (dockerHost == null) {
      throw new AzureDockerException("Unexpected param values; dockerHost cannot be null");
    }

    try {
      switch (dockerHost.hostOSType) {
        case UBUNTU_SERVER_14_04_LTS:
        case UBUNTU_SERVER_16_04_LTS:
          installDockerOnUbuntuServer(dockerHost);
          break;
        default:
          throw new AzureDockerException("Docker dockerHost OS type is not supported");
      }
    } catch (Exception e) {
      throw new AzureDockerException(e.getMessage(), e);
    }
  }

  public static DockerHost installDockerOnUbuntuServer(DockerHost dockerHost) {
    if (dockerHost == null) {
      throw new AzureDockerException("Unexpected param values; dockerHost cannot be null");
    }

    try {
      Session session = AzureDockerSSHOps.createLoginInstance(dockerHost);

      switch (dockerHost.hostOSType) {
        case UBUNTU_SERVER_14_04_LTS:
          installDockerServiceOnUbuntuServer_14_04_LTS(session);
        case UBUNTU_SERVER_16_04_LTS:
          installDockerServiceOnUbuntuServer_16_04_LTS(session);
          break;
        default:
          throw new AzureDockerException("Docker dockerHost OS type is not supported");
      }

      if (dockerHost.isTLSSecured) {
        if (isValid(dockerHost.certVault.tlsServerCert)) {
          // Docker certificates are passed in; copy them to the docker dockerHost
          uploadDockerTlsCertsForUbuntuServer(dockerHost, session);
        } else {
          // Create new TLS certificates and upload them into the current machine representation
          dockerHost = createDockerCertsForUbuntuServer(dockerHost, session);
          dockerHost = downloadDockerTlsCertsForUbuntuServer(dockerHost, session);
        }
        setupDockerTlsCertsForUbuntuServer(session);

        createDockerConfigWithTlsForUbuntuServer(dockerHost, session);
      } else {
        createDockerConfigNoTlsForUbuntuServer(dockerHost, session);
      }

      session.disconnect();

      return dockerHost;

    } catch (Exception e) {
      throw new AzureDockerException(e.getMessage(), e);
    }
  }

  public static void installDockerServiceOnUbuntuServer_14_04_LTS(Session session) {
    if (session == null) {
      throw new AzureDockerException("Unexpected param values; login session cannot be null");
    }

    // Install Docker daemon
    //**********************************//

    try {
      if (!session.isConnected()) session.connect();

      AzureDockerSSHOps.upload(session, (new ByteArrayInputStream(INSTALL_DOCKER_FOR_UBUNTU_SERVER_14_04_LTS.replaceAll("&&", "\n").getBytes())), "INSTALL_DOCKER_FOR_UBUNTU_SERVER_14_04_LTS.sh", ".azuredocker", true, "4095");

      if (DEBUG) System.out.println("Start executing Docker install service command");
      String cmdOut1 = AzureDockerSSHOps.executeCommand("bash -c ~/.azuredocker/INSTALL_DOCKER_FOR_UBUNTU_SERVER_14_04_LTS.sh", session, true);
      if (DEBUG) System.out.println(cmdOut1);
      if (DEBUG) System.out.println("Done executing Docker install service command");

    } catch (Exception e) {
      throw new AzureDockerException(e.getMessage(), e);
    }
  }

  public static void installDockerServiceOnUbuntuServer_16_04_LTS(Session session) {
    if (session == null) {
      throw new AzureDockerException("Unexpected param values; login session cannot be null");
    }

    // Install Docker daemon
    //**********************************//

    try {
      if (!session.isConnected()) session.connect();

      AzureDockerSSHOps.upload(session, (new ByteArrayInputStream(INSTALL_DOCKER_FOR_UBUNTU_SERVER_16_04_LTS.replaceAll("&&", "\n").getBytes())), "INSTALL_DOCKER_FOR_UBUNTU_SERVER_16_04_LTS.sh", ".azuredocker", true, "4095");

      if (DEBUG) System.out.println("Start executing Docker install service command");
      String cmdOut1 = AzureDockerSSHOps.executeCommand("bash -c ~/.azuredocker/INSTALL_DOCKER_FOR_UBUNTU_SERVER_16_04_LTS.sh", session, true);
      if (DEBUG) System.out.println(cmdOut1);
      if (DEBUG) System.out.println("Done executing Docker install service command");

    } catch (Exception e) {
      throw new AzureDockerException(e.getMessage(), e);
    }
  }

  public static DockerHost createDockerCertsForUbuntuServer(DockerHost dockerHost, Session session) {
    if (dockerHost == null || (session == null && dockerHost.session == null)) {
      throw new AzureDockerException("Unexpected param values; dockerHost, dockerHost name, dockerHost dns and login session cannot be null");
    }

    if (session == null) session = dockerHost.session;

    // Generate openssl TLS certs
    //**********************************//

    try {
      if (!session.isConnected()) session.connect();

      // Generate a random password to be used when creating the TLS certificates
      String certCAPwd = new ResourceNamer("").randomName("", 15);
      String createTLScerts = CREATE_OPENSSL_TLS_CERTS_FOR_UBUNTU;
      createTLScerts = createTLScerts.replaceAll(CERT_CA_PWD_PARAM, certCAPwd);
      createTLScerts = createTLScerts.replaceAll(HOSTNAME, dockerHost.hostVM.name);
      createTLScerts = createTLScerts.replaceAll(FQDN_PARAM, dockerHost.hostVM.dnsName);
      createTLScerts = createTLScerts.replaceAll(DOMAIN_PARAM, dockerHost.hostVM.dnsName.substring(dockerHost.hostVM.dnsName.indexOf('.')));

      AzureDockerSSHOps.upload(session, (new ByteArrayInputStream(createTLScerts.replaceAll("&&", "\n").getBytes())), "CREATE_OPENSSL_TLS_CERTS_FOR_UBUNTU.sh", ".azuredocker", true, "4095");

      if (DEBUG) System.out.println("Start executing Docker create TLS certs command");
      String cmdOut1 = AzureDockerSSHOps.executeCommand("bash -c ~/.azuredocker/CREATE_OPENSSL_TLS_CERTS_FOR_UBUNTU.sh", session, true);
      if (DEBUG) System.out.println(cmdOut1);
      if (DEBUG) System.out.println("Done executing Docker create TLS certs command");

      return dockerHost;
    } catch (Exception e) {
      throw new AzureDockerException(e.getMessage(), e);
    }
  }

  public static DockerHost downloadDockerTlsCertsForUbuntuServer(DockerHost dockerHost, Session session) {
    if (dockerHost == null || (session == null && dockerHost.session == null)) {
      throw new AzureDockerException("Unexpected param values; dockerHost, dockerHost name, dockerHost dns and login session cannot be null");
    }

    if (session == null) session = dockerHost.session;

    // Download TLS certs
    //**********************************//

    try {
      if (!session.isConnected()) session.connect();

      if (DEBUG) System.out.println("Start downloading the TLS certs");
      dockerHost.certVault.tlsCACert = AzureDockerSSHOps.download(session, "ca.pem", ".azuredocker/tls", true);
      dockerHost.certVault.tlsCAKey = AzureDockerSSHOps.download(session, "ca-key.pem", ".azuredocker/tls", true);
      dockerHost.certVault.tlsClientCert = AzureDockerSSHOps.download(session, "cert.pem", ".azuredocker/tls", true);
      dockerHost.certVault.tlsClientKey = AzureDockerSSHOps.download(session, "key.pem", ".azuredocker/tls", true);
      dockerHost.certVault.tlsServerCert = AzureDockerSSHOps.download(session, "server.pem", ".azuredocker/tls", true);
      dockerHost.certVault.tlsServerKey = AzureDockerSSHOps.download(session, "server-key.pem", ".azuredocker/tls", true);
      if (DEBUG) System.out.println("Done downloading TLS certs");

      return dockerHost;
    } catch (Exception e) {
      throw new AzureDockerException(e.getMessage(), e);
    }
  }

  public static void uploadDockerTlsCertsForUbuntuServer(DockerHost dockerHost, Session session) {
    if (dockerHost == null || (session == null && dockerHost.session == null)) {
      throw new AzureDockerException("Unexpected param values; dockerHost, dockerHost name, dockerHost dns and login session cannot be null");
    }

    if (session == null) session = dockerHost.session;

    // Upload TLS certificates
    //**********************************//

    try {
      if (!session.isConnected()) session.connect();

      if (DEBUG) System.out.println("Start uploading the TLS certs");
      if (isValid(dockerHost.certVault.tlsCACert))
        AzureDockerSSHOps.upload(session, (new ByteArrayInputStream(dockerHost.certVault.tlsCACert.getBytes())), "ca.pem", ".azuredocker/tls", true, null);
      if (isValid(dockerHost.certVault.tlsCAKey))
        AzureDockerSSHOps.upload(session, (new ByteArrayInputStream(dockerHost.certVault.tlsCAKey.getBytes())), "ca-key.pem", ".azuredocker/tls", true, null);
      if (isValid(dockerHost.certVault.tlsClientCert))
        AzureDockerSSHOps.upload(session, (new ByteArrayInputStream(dockerHost.certVault.tlsClientCert.getBytes())), "cert.pem", ".azuredocker/tls", true, null);
      if (isValid(dockerHost.certVault.tlsClientKey))
        AzureDockerSSHOps.upload(session, (new ByteArrayInputStream(dockerHost.certVault.tlsClientKey.getBytes())), "key.pem", ".azuredocker/tls", true, null);
      if (isValid(dockerHost.certVault.tlsServerCert))
        AzureDockerSSHOps.upload(session, (new ByteArrayInputStream(dockerHost.certVault.tlsServerCert.getBytes())), "server.pem", ".azuredocker/tls", true, null);
      if (isValid(dockerHost.certVault.tlsServerKey))
        AzureDockerSSHOps.upload(session, (new ByteArrayInputStream(dockerHost.certVault.tlsServerKey.getBytes())), "server-key.pem", ".azuredocker/tls", true, null);
      if (DEBUG) System.out.println("Done uploading TLS certs");

    } catch (Exception e) {
      throw new AzureDockerException(e.getMessage(), e);
    }
  }

  public static void setupDockerTlsCertsForUbuntuServer(Session session) {

    // Install TLS certificates
    //**********************************//

    try {
      if (!session.isConnected()) session.connect();

      AzureDockerSSHOps.upload(session, (new ByteArrayInputStream(INSTALL_DOCKER_TLS_CERTS_FOR_UBUNTU.replaceAll("&&", "\n").getBytes())), "INSTALL_DOCKER_TLS_CERTS_FOR_UBUNTU.sh", ".azuredocker", true, "4095");

      if (DEBUG) System.out.println("Start executing Docker install TLS certs command");
      String cmdOut1 = AzureDockerSSHOps.executeCommand("bash -c ~/.azuredocker/INSTALL_DOCKER_TLS_CERTS_FOR_UBUNTU.sh", session, true);
      if (DEBUG) System.out.println(cmdOut1);
      if (DEBUG) System.out.println("Done executing Docker install TLS certs command");

    } catch (Exception e) {
      throw new AzureDockerException(e.getMessage(), e);
    }
  }

  public static void createDockerConfigNoTlsForUbuntuServer(DockerHost dockerHost, Session session) {
    if (dockerHost == null || (session == null && dockerHost.session == null)) {
      throw new AzureDockerException("Unexpected param values; dockerHost, dockerHost name, dockerHost dns and login session cannot be null");
    }

    if (session == null) session = dockerHost.session;

    // Setup Docker daemon port and log in configuration
    //**********************************//

    try {
      if (!session.isConnected()) session.connect();

      if (dockerHost.port == null || dockerHost.port.isEmpty()) {
        dockerHost.port = dockerHost.isTLSSecured ? DOCKER_API_PORT_TLS_ENABLED : DOCKER_API_PORT_TLS_DISABLED;
      }

      String createDockerOpts = CREATE_DEFAULT_DOCKER_OPTS_TLS_DISABLED;
      createDockerOpts = createDockerOpts.replaceAll(DOCKER_API_PORT_PARAM, dockerHost.port);
      AzureDockerSSHOps.upload(session, (new ByteArrayInputStream(createDockerOpts.replaceAll("&&", "\n").getBytes())), "CREATE_DEFAULT_DOCKER_OPTS_TLS_DISABLED.sh", ".azuredocker", true, "4095");

      if (DEBUG) System.out.println("Start executing docker config setup");
      String cmdOut1 = AzureDockerSSHOps.executeCommand("bash -c ~/.azuredocker/CREATE_DEFAULT_DOCKER_OPTS_TLS_DISABLED.sh", session, true);
      if (DEBUG) System.out.println(cmdOut1);
      if (DEBUG) System.out.println("Done executing docker config setup");

    } catch (Exception e) {
      throw new AzureDockerException(e.getMessage(), e);
    }
  }

  public static void createDockerConfigWithTlsForUbuntuServer(DockerHost dockerHost, Session session) {
    if (dockerHost == null || (session == null && dockerHost.session == null)) {
      throw new AzureDockerException("Unexpected param values; dockerHost, dockerHost name, dockerHost dns and login session cannot be null");
    }

    if (session == null) session = dockerHost.session;

    // Setup Docker daemon port and log in configuration
    //**********************************//

    try {
      if (!session.isConnected()) session.connect();

      if (dockerHost.port == null || dockerHost.port.isEmpty()) {
        dockerHost.port = dockerHost.isTLSSecured ? DOCKER_API_PORT_TLS_ENABLED : DOCKER_API_PORT_TLS_DISABLED;
      }

      String createDockerOpts = CREATE_DEFAULT_DOCKER_OPTS_TLS_ENABLED;
      createDockerOpts = createDockerOpts.replaceAll(DOCKER_API_PORT_PARAM, dockerHost.port);
      AzureDockerSSHOps.upload(session, (new ByteArrayInputStream(createDockerOpts.replaceAll("&&", "\n").getBytes())), "CREATE_DEFAULT_DOCKER_OPTS_TLS_ENABLED.sh", ".azuredocker", true, "4095");

      if (DEBUG) System.out.println("Start executing docker config setup");
      String cmdOut1 = AzureDockerSSHOps.executeCommand("bash -c ~/.azuredocker/CREATE_DEFAULT_DOCKER_OPTS_TLS_ENABLED.sh", session, true);
      if (DEBUG) System.out.println(cmdOut1);
      if (DEBUG) System.out.println("Done executing docker config setup");

    } catch (Exception e) {
      throw new AzureDockerException(e.getMessage(), e);
    }
  }

  public static void waitForVirtualMachineStartup(Azure azureClient, DockerHost dockerHost) {

  // vault is not immediately available so the next operation could fail
  // add a retry policy to make sure it got created
    boolean isRunning = false;
    int sleepTime = 50000;
    for (int sleepMs = 5000; sleepMs <= 200000; sleepMs += sleepTime ) {
      try {
        if (!isRunning) {
          VirtualMachine vm = getVM(azureClient, dockerHost.hostVM.resourceGroupName, dockerHost.hostVM.name);
          if (vm != null && vm.powerState().toString().split("/")[1].toUpperCase().equals("RUNNING")) { // "PowerState/running"
            isRunning = true;
          } else {
            if (DEBUG) System.out.format("Warning: can't connect to %s (%d sec have passed)\n", dockerHost.hostVM.dnsName, sleepMs/1000);
            try {
              Thread.sleep(sleepTime);
            } catch (Exception e2) {}
          }
        } else {
          // VM is running; try to SSH connect to it
          Session session = AzureDockerSSHOps.createLoginInstance(dockerHost);
          String result = AzureDockerSSHOps.executeCommand("ls -l /", session, true);
          if (DEBUG) System.out.println(result);
          session.disconnect();
          break;
        }
      } catch (Exception e) {
        try {
          if (DEBUG) System.out.format("Warning: can't connect to %s (%d sec have passed)\n", dockerHost.hostVM.dnsName, sleepMs/1000);
          if (DEBUG) System.out.println(e.getMessage());
          Thread.sleep(sleepTime);
        } catch (Exception e3) {}
      }
    }
  }

  public static void uploadDockerfileAndArtifact(AzureDockerImageInstance dockerImageInstance, Session session) {
    if (dockerImageInstance == null || dockerImageInstance.host == null || (session == null && dockerImageInstance.host.session == null)) {
      throw new AzureDockerException("Unexpected param values; dockerHost, dockerHost name, dockerHost dns and login session cannot be null");
    }

    if (session == null) session = dockerImageInstance.host.session;

    // Setup Docker daemon port and log in configuration
    //**********************************//

    try {
      if (!session.isConnected()) session.connect();

      if (DEBUG) System.out.println("Start uploading Dockerfile and artifact");
      String toPath = ".azuredocker/images/" + dockerImageInstance.dockerImageName;
      AzureDockerSSHOps.upload(session,
          new File(dockerImageInstance.artifactPath).getName(),
          new File(dockerImageInstance.artifactPath).getParent(),
          toPath, true, null);
      AzureDockerSSHOps.upload(session, (new ByteArrayInputStream(dockerImageInstance.dockerfileContent.getBytes())),
          "Dockerfile", toPath, true, null);
      if (DEBUG) System.out.println("Done uploading Dockerfile and artifact");

    } catch (Exception e) {
      throw new AzureDockerException(e.getMessage(), e);
    }
  }

  public static void UpdateCurrentDockerUser(Session session) {

    // Adds current user as a recognized Docker user

    try {
      if (!session.isConnected()) session.connect();

      if (DEBUG) System.out.format("Executing %s", UPDATE_CURRENT_DOCKER_USER);
      String cmdOut1 = AzureDockerSSHOps.executeCommand(UPDATE_CURRENT_DOCKER_USER, session, true);
      if (DEBUG) System.out.println(cmdOut1);
      if (DEBUG) System.out.format("Done executing %s", UPDATE_CURRENT_DOCKER_USER);
    } catch (Exception e) {
      throw new AzureDockerException(e.getMessage(), e);
    }
  }

  public static void waitForDockerDaemonStartup(Session session) {

    // Start Docker daemon and wait until timeout or "docker ps" returns errorcode 0
    //**********************************//

    try {
      if (!session.isConnected()) session.connect();

      if (DEBUG) System.out.format("Executing \"sudo service docker start\"");
      String cmdOut1 = AzureDockerSSHOps.executeCommand("sudo service docker start \n", session, true);
      if (DEBUG) System.out.println(cmdOut1);
      if (DEBUG) System.out.println("Done executing \"sudo service docker start\"");

      for (int timeout = 500; timeout < 60000; timeout += 500) {
        if (DEBUG) System.out.println("\tExecuting \"docker ps\"");
        cmdOut1 = AzureDockerSSHOps.executeCommand("docker ps\n", session, true);
        if (DEBUG) System.out.println(cmdOut1);
        if (DEBUG) System.out.println("\tDone executing \"docker ps\"");
        if (cmdOut1.contains("exit-status: 0")) break;
        Thread.sleep(500);
      }

    } catch (Exception e) {
      throw new AzureDockerException(e.getMessage(), e);
    }
  }
}