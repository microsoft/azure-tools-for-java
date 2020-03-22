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
package com.microsoft.intellij.docker.wizards.createhost.forms;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.wizard.WizardNavigationState;
import com.intellij.ui.wizard.WizardStep;
import com.microsoft.azure.PagedList;
import com.microsoft.azure.docker.AzureDockerHostsManager;
import com.microsoft.azure.docker.model.*;
import com.microsoft.azure.docker.ops.utils.AzureDockerUtils;
import com.microsoft.azure.docker.ops.utils.AzureDockerValidationUtils;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.VirtualMachineSize;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.Environment;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.azuretools.telemetry.TelemetryProperties;
import com.microsoft.intellij.docker.utils.AzureDockerUIResources;
import com.microsoft.intellij.docker.wizards.createhost.AzureNewDockerWizardModel;
import com.microsoft.intellij.docker.wizards.createhost.AzureNewDockerWizardStep;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import org.jdesktop.swingx.JXHyperlink;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;
import java.util.*;
import java.util.List;

public class AzureNewDockerHostStep extends AzureNewDockerWizardStep implements TelemetryProperties {
  private JPanel rootConfigureContainerPanel;
  private JLabel dockerHostNameLabel;
  private JTextField dockerHostNameTextField;
  private JComboBox<AzureDockerSubscription> dockerSubscriptionComboBox;
  private JTextField dockerSubscriptionIdTextField;
  private JLabel dockerSubscriptionIdLabel;
  private JComboBox<String> dockerLocationComboBox;
  private JTabbedPane hostDetailsTabbedPane;
  private JComboBox<KnownDockerVirtualMachineImage> dockerHostOSTypeComboBox;
  private JComboBox<String> dockerHostVMSizeComboBox;
  private ButtonGroup dockerHostRGGroup;
  private JRadioButton dockerHostNewRGRadioButton;
  private JRadioButton dockerHostSelectRGRadioButton;
  private JLabel dockerHostRGLabel;
  private JTextField dockerHostRGTextField;
  private JComboBox<String> dockerHostSelectRGComboBox;
  private ButtonGroup dockerHostVnetGroup;
  private JRadioButton dockerHostNewVNetRadioButton;
  private JRadioButton dockerHostSelectVNetRadioButton;
  private JComboBox<AzureDockerVnet> dockerHostSelectVnetComboBox;
  private JComboBox<String> dockerHostSelectSubnetComboBox;
  private JLabel dockerHostNewVNetNameLabel;
  private JTextField dockerHostNewVNetNameTextField;
  private JLabel dockerHostNewVNetAddrSpaceLabel;
  private JTextField dockerHostNewVNetAddrSpaceTextField;
  private ButtonGroup dockerHostStorageGroup;
  private JRadioButton dockerHostNewStorageRadioButton;
  private JRadioButton dockerHostSelectStorageRadioButton;
  private JLabel dockerNewStorageLabel;
  private JTextField dockerNewStorageTextField;
  private JComboBox<String> dockerSelectStorageComboBox;
  private JPanel vmKindPanel;
  private JPanel rgPanel;
  private JPanel networkPanel;
  private JPanel storagePanel;
  private JLabel dockerLocationLabel;
  private JCheckBox dockerHostVMPreferredSizesCheckBox;
  private JXHyperlink dockerPricingHyperlink;

  private String preferredLocation;
  private final String SELECT_REGION = "<select region>";

  private AzureNewDockerWizardModel model;
  private AzureDockerHostsManager dockerManager;
  private DockerHost newHost;

  public AzureNewDockerHostStep(String title, AzureNewDockerWizardModel model) {
    // TODO: The message should go into the plugin property file that handles the various dialog titles
    super(title, "Configure the new virtual machine");
    this.model = model;
    this.dockerManager = model.getDockerManager();
    this.newHost = model.getDockerHost();

    preferredLocation = null;

    dockerHostNameLabel.setVisible(false);
    dockerHostNameTextField.setText(newHost.name);
    dockerHostNameTextField.setToolTipText(AzureDockerValidationUtils.getDockerHostNameTip());
    dockerHostNameTextField.setInputVerifier(new InputVerifier() {
      @Override
      public boolean verify(JComponent input) {
        if (AzureDockerValidationUtils.validateDockerHostName(((JTextField) input).getText())) {
          dockerHostNameLabel.setVisible(false);
          setDialogButtonsState(doValidate(false) == null);
          return true;
        } else {
          dockerHostNameLabel.setVisible(true);
          setDialogButtonsState(false);
          return false;
        }
      }
    });
    dockerHostNameTextField.getDocument().addDocumentListener(resetDialogButtonsState(null));

    DefaultComboBoxModel<AzureDockerSubscription> dockerSubscriptionComboModel = new DefaultComboBoxModel<>(new Vector<>(dockerManager.getSubscriptionsList()));
    dockerSubscriptionComboBox.setModel(dockerSubscriptionComboModel);
    dockerSubscriptionIdTextField.setBorder(null);
    AzureDockerSubscription currentSubscription = (AzureDockerSubscription) dockerSubscriptionComboBox.getSelectedItem();
    dockerSubscriptionIdLabel.setVisible(currentSubscription != null);
    dockerSubscriptionIdTextField.setText(currentSubscription != null ? currentSubscription.id : "");
    dockerSubscriptionIdTextField.setName("dockerSubscriptionIdTextField");
    dockerLocationComboBox.setName("dockerLocationComboBox");
    dockerSubscriptionComboBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        AzureDockerSubscription currentSubscription = (AzureDockerSubscription) dockerSubscriptionComboBox.getSelectedItem();
        dockerSubscriptionIdLabel.setVisible(currentSubscription != null);
        dockerSubscriptionIdTextField.setText(currentSubscription != null ? currentSubscription.id : "");
        updateDockerLocationComboBox(currentSubscription);
        updateDockerHostSelectRGComboBox(currentSubscription);
        String region = (String) dockerLocationComboBox.getSelectedItem();
        Region regionObj = Region.findByLabelOrName(region);
        updateDockerSelectVnetComboBox( currentSubscription, regionObj != null ? regionObj.name() : region);
        updateDockerSelectStorageComboBox(currentSubscription);
      }
    });

    updateDockerHostVMSize();
    updateDockerLocationGroup();
    updateDockerHostOSTypeComboBox();
    updateDockerHostRGGroup();
    updateDockerHostVnetGroup();
    updateDockerHostStorageGroup();
    dockerPricingHyperlink.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent event) {
        try {
          Desktop.getDesktop().browse(URI.create("https://azure.microsoft.com/en-us/pricing/details/virtual-machines/linux/"));
        } catch (Exception e) {
          DefaultLoader.getUIHelper().logError("Unexpected exception: " + e.getMessage(), e);
        }
      }
    });
  }

  private void updateDockerLocationGroup() {
    AzureDockerSubscription currentSubscription = (AzureDockerSubscription) dockerSubscriptionComboBox.getSelectedItem();
    updateDockerLocationComboBox(currentSubscription);

    dockerLocationComboBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        AzureDockerSubscription currentSubscription = (AzureDockerSubscription) dockerSubscriptionComboBox.getSelectedItem();
//        updateDockerHostSelectRGComboBox(currentSubscription);
        String region = (String) dockerLocationComboBox.getSelectedItem();
        if (!region.equals(SELECT_REGION)) {
          Region regionObj = Region.findByLabelOrName(region);
          String selectedRegion = regionObj != null ? regionObj.name() : region;
          if (preferredLocation == null && selectedRegion != null) {
            // remove the SELECT_REGION entry (first entry in the list)
            dockerLocationComboBox.removeItemAt(0);
            dockerLocationLabel.setVisible(false);
          }
          preferredLocation = selectedRegion;
          updateDockerSelectVnetComboBox(currentSubscription, selectedRegion);
          setDialogButtonsState(doValidate(false) == null);
        } else {
          updateDockerSelectVnetComboBox(currentSubscription, null);
          setDialogButtonsState(false);
        }
        updateDockerHostVMSizeComboBox(dockerHostVMPreferredSizesCheckBox.isSelected());
      }
    });
  }

  private void updateDockerLocationComboBox(AzureDockerSubscription currentSubscription) {
    if (currentSubscription != null && currentSubscription.locations != null) {
      DefaultComboBoxModel<String> dockerLocationComboModel = new DefaultComboBoxModel<>(); //new Vector<String>(currentSubscription.locations));
      if (currentSubscription.locations.size() > 0) {
        String previousSelection = preferredLocation;
        preferredLocation = null;
        for (String region : currentSubscription.locations) {
          Region regionObj = Region.findByLabelOrName(region);
          dockerLocationComboModel.addElement(regionObj != null ? regionObj.label() : region);
          if ((previousSelection != null && region.equals(previousSelection)) ||
              (newHost.hostVM.region != null && region.equals(newHost.hostVM.region))) {
            preferredLocation = region;
            dockerLocationComboModel.setSelectedItem(regionObj != null ? regionObj.label() : region);
            dockerLocationLabel.setVisible(false);
          }
        }
        if (preferredLocation == null) {
          dockerLocationComboModel.insertElementAt(SELECT_REGION, 0);
          dockerLocationComboModel.setSelectedItem(SELECT_REGION);
          dockerLocationLabel.setVisible(true);
        }
      }
      dockerLocationComboBox.setModel(dockerLocationComboModel);
      updateDockerHostVMSizeComboBox(dockerHostVMPreferredSizesCheckBox.isSelected());
    }
  }

  private void updateDockerHostOSTypeComboBox() {
    DefaultComboBoxModel<KnownDockerVirtualMachineImage> dockerHostOSTypeComboModel = new DefaultComboBoxModel<>();
    for (KnownDockerVirtualMachineImage knownDockerVirtualMachineImage : KnownDockerVirtualMachineImage.values()) {
      dockerHostOSTypeComboModel.addElement(knownDockerVirtualMachineImage);
    }
    dockerHostOSTypeComboBox.setModel(dockerHostOSTypeComboModel);
    dockerHostOSTypeComboBox.setSelectedItem(KnownDockerVirtualMachineImage.valueOf(newHost.hostOSType.name()));
  }

  private void updateDockerHostVMSize() {
    dockerHostVMPreferredSizesCheckBox.setSelected(true);
//    dockerHostVMPreferredSizesCheckBox.setEnabled(false);
    dockerHostVMPreferredSizesCheckBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        updateDockerHostVMSizeComboBox(dockerHostVMPreferredSizesCheckBox.isSelected());
      }
    });

    dockerHostVMSizeComboBox.setName("dockerHostVMSizeComboBox");
    dockerHostVMSizeComboBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        updateDockerSelectStorageComboBox((AzureDockerSubscription) dockerSubscriptionComboBox.getSelectedItem());
      }
    });
    updateDockerHostVMSizeComboBox(true);
  }

  private void updateDockerHostVMSizeComboBox(boolean prefferedSizesOnly) {
    DefaultComboBoxModel<String> dockerHostVMSizeComboModel = new DefaultComboBoxModel<>();
    if (prefferedSizesOnly) {
      for (KnownDockerVirtualMachineSizes knownDockerVirtualMachineSize : KnownDockerVirtualMachineSizes.values()) {
        dockerHostVMSizeComboModel.addElement(knownDockerVirtualMachineSize.name());
      }
      if (dockerHostVMSizeComboModel.getSize() > 0) {
        dockerHostVMSizeComboModel.setSelectedItem(dockerHostVMSizeComboModel.getElementAt(0));
      }
    } else {
      dockerHostVMSizeComboModel.addElement("<Loading...>");
      ProgressManager.getInstance().run(new Task.Backgroundable(model.getProject(), "Loading VM Sizes...", false) {
        @Override
        public void run(@NotNull ProgressIndicator progressIndicator) {
          progressIndicator.setIndeterminate(true);

          Azure azureClient = ((AzureDockerSubscription) dockerSubscriptionComboBox.getSelectedItem()).azureClient;

          PagedList<VirtualMachineSize> sizes = null;
          try {
            sizes = azureClient.virtualMachines().sizes().listByRegion(preferredLocation);
            Collections.sort(sizes, new Comparator<VirtualMachineSize>() {
              @Override
              public int compare(VirtualMachineSize size1, VirtualMachineSize size2) {
                if (size1.name().contains("Basic") && size2.name().contains("Basic")) {
                  return size1.name().compareTo(size2.name());
                } else if (size1.name().contains("Basic")) {
                  return -1;
                } else if (size2.name().contains("Basic")) {
                  return 1;
                }

                int coreCompare = Integer.valueOf(size1.numberOfCores()).compareTo(size2.numberOfCores());

                if (coreCompare == 0) {
                  return Integer.valueOf(size1.memoryInMB()).compareTo(size2.memoryInMB());
                } else {
                  return coreCompare;
                }
              }
            });
          } catch (Exception notHandled) {}
          PagedList<VirtualMachineSize> sortedSizes = sizes;

          ApplicationManager.getApplication().invokeAndWait(new Runnable() {
            @Override
            public void run() {
              dockerHostVMSizeComboModel.removeAllElements();
              if (sortedSizes != null) {
                for (VirtualMachineSize vmSize : sortedSizes) {
                  dockerHostVMSizeComboModel.addElement(vmSize.name());
                }
              }
              dockerHostVMSizeComboBox.repaint();
            }
          }, ModalityState.any());
        }
      });
    }
    dockerHostVMSizeComboBox.setModel(dockerHostVMSizeComboModel);
    dockerHostVMSizeComboBox.setSelectedItem(newHost.hostVM.vmSize);
    if (!newHost.hostVM.vmSize.equals((String) dockerHostVMSizeComboBox.getSelectedItem())) {
      dockerHostVMSizeComboModel.addElement(newHost.hostVM.vmSize);
      dockerHostVMSizeComboBox.setSelectedItem(newHost.hostVM.vmSize);
    }
    updateDockerSelectStorageComboBox((AzureDockerSubscription) dockerSubscriptionComboBox.getSelectedItem());
  }

  private void updateDockerHostRGGroup() {
    dockerHostRGGroup = new ButtonGroup();
    dockerHostRGGroup.add(dockerHostNewRGRadioButton);
    dockerHostRGGroup.add(dockerHostSelectRGRadioButton);
    dockerHostNewRGRadioButton.setSelected(true);
    dockerHostVMSizeComboBox.setName("dockerHostVMSizeComboBox");
    dockerHostNewRGRadioButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        dockerHostRGTextField.setEnabled(true);
        if (AzureDockerValidationUtils.validateDockerHostResourceGroupName(dockerHostRGTextField.getText())) {
          dockerHostRGLabel.setVisible(false);
          setDialogButtonsState(doValidate(false) == null);
        } else {
          dockerHostRGLabel.setVisible(true);
          setDialogButtonsState(false);
        }
        dockerHostSelectRGComboBox.setEnabled(false);
      }
    });
    dockerHostSelectRGRadioButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        dockerHostRGTextField.setEnabled(false);
        dockerHostRGLabel.setVisible(false);
        setDialogButtonsState(doValidate(false) == null);
        dockerHostSelectRGComboBox.setEnabled(true);
      }
    });
    dockerHostRGLabel.setVisible(false);
    dockerHostRGTextField.setText(newHost.hostVM.resourceGroupName);
    dockerHostRGTextField.setToolTipText(AzureDockerValidationUtils.getDockerHostResourceGroupNameTip());
    dockerHostRGTextField.setInputVerifier(new InputVerifier() {
      @Override
      public boolean verify(JComponent input) {
        if (AzureDockerValidationUtils.validateDockerHostResourceGroupName(((JTextField) input).getText())) {
          dockerHostRGLabel.setVisible(false);
          setDialogButtonsState(doValidate(false) == null);
          return true;
        } else {
          dockerHostRGLabel.setVisible(true);
          setDialogButtonsState(false);
          return false;
        }
      }
    });
    dockerHostRGTextField.getDocument().addDocumentListener(resetDialogButtonsState(null));
    dockerHostSelectRGComboBox.setEnabled(false);

    updateDockerHostSelectRGComboBox((AzureDockerSubscription) dockerSubscriptionComboBox.getSelectedItem());
  }

  private void updateDockerHostSelectRGComboBox(AzureDockerSubscription subscription) {
    DefaultComboBoxModel<String> dockerHostSelectRGComboModel = new DefaultComboBoxModel<>(new Vector<>(dockerManager.getResourceGroups(subscription)));
    dockerHostSelectRGComboBox.setModel(dockerHostSelectRGComboModel);
  }

  private void updateDockerHostVnetGroup() {
    dockerHostVnetGroup = new ButtonGroup();
    dockerHostVnetGroup.add(dockerHostNewVNetRadioButton);
    dockerHostVnetGroup.add(dockerHostSelectVNetRadioButton);
    dockerHostNewVNetRadioButton.setSelected(true);
    dockerHostNewVNetRadioButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        dockerHostNewVNetNameTextField.setEnabled(true);
        if (AzureDockerValidationUtils.validateDockerVnetName(dockerHostNewVNetNameTextField.getText())) {
          dockerHostNewVNetNameLabel.setVisible(false);
          setDialogButtonsState(doValidate(false) == null);
        } else {
          dockerHostNewVNetNameLabel.setVisible(true);
          setDialogButtonsState(false);
        }
        dockerHostNewVNetAddrSpaceTextField.setEnabled(true);
        if (AzureDockerValidationUtils.validateDockerVnetAddrSpace(dockerHostNewVNetAddrSpaceTextField.getText())) {
          dockerHostNewVNetAddrSpaceLabel.setVisible(false);
          setDialogButtonsState(doValidate(false) == null);
        } else {
          dockerHostNewVNetAddrSpaceLabel.setVisible(true);
          setDialogButtonsState(false);
        }
        dockerHostSelectVnetComboBox.setEnabled(false);
        dockerHostSelectSubnetComboBox.setEnabled(false);
      }
    });
    dockerHostSelectVNetRadioButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        dockerHostNewVNetNameLabel.setVisible(false);
        dockerHostNewVNetNameTextField.setEnabled(false);
        dockerHostNewVNetAddrSpaceLabel.setVisible(false);
        dockerHostNewVNetAddrSpaceTextField.setEnabled(false);
        dockerHostSelectVnetComboBox.setEnabled(true);
        dockerHostSelectSubnetComboBox.setEnabled(true);
        setDialogButtonsState(doValidate(false) == null);
      }
    });
    dockerHostNewVNetNameLabel.setVisible(false);
    dockerHostNewVNetNameTextField.setText(newHost.hostVM.vnetName);
    dockerHostNewVNetNameTextField.setToolTipText(AzureDockerValidationUtils.getDockerVnetNameTip());
    dockerHostNewVNetNameTextField.setInputVerifier(new InputVerifier() {
      @Override
      public boolean verify(JComponent input) {
        if (AzureDockerValidationUtils.validateDockerVnetName(((JTextField) input).getText())) {
          dockerHostNewVNetNameLabel.setVisible(false);
          setDialogButtonsState(doValidate(false) == null);
          return true;
        } else {
          dockerHostNewVNetNameLabel.setVisible(true);
          setDialogButtonsState(false);
          return false;
        }
      }
    });
    dockerHostNewVNetNameTextField.getDocument().addDocumentListener(resetDialogButtonsState(null));
    dockerHostNewVNetAddrSpaceLabel.setVisible(false);
    dockerHostNewVNetAddrSpaceTextField.setText(newHost.hostVM.vnetAddressSpace);
    dockerHostNewVNetAddrSpaceTextField.setToolTipText(AzureDockerValidationUtils.getDockerVnetAddrspaceTip());
    dockerHostNewVNetAddrSpaceTextField.setInputVerifier(new InputVerifier() {
      @Override
      public boolean verify(JComponent input) {
        if (AzureDockerValidationUtils.validateDockerVnetAddrSpace(((JTextField) input).getText())) {
          dockerHostNewVNetAddrSpaceLabel.setVisible(false);
          setDialogButtonsState(doValidate(false) == null);
          return true;
        } else {
          dockerHostNewVNetAddrSpaceLabel.setVisible(true);
          setDialogButtonsState(false);
          return false;
        }
      }
    });
    dockerHostNewVNetAddrSpaceTextField.getDocument().addDocumentListener(resetDialogButtonsState(null));
    dockerHostSelectVnetComboBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        updateDockerSelectSubnetComboBox((AzureDockerVnet) dockerHostSelectVnetComboBox.getSelectedItem());
      }
    });
    dockerHostSelectVnetComboBox.setEnabled(false);
    dockerHostSelectSubnetComboBox.setEnabled(false);

    String region = (String) dockerLocationComboBox.getSelectedItem();
    Region regionObj = Region.findByLabelOrName(region);
    AzureDockerSubscription currentSubscription = (AzureDockerSubscription) dockerSubscriptionComboBox.getSelectedItem();
    updateDockerSelectVnetComboBox( currentSubscription, regionObj != null ? regionObj.name() : region);
  }

  private void updateDockerSelectVnetComboBox(AzureDockerSubscription subscription, String region) {
    List<AzureDockerVnet> dockerVnets = dockerManager.getNetworksAndSubnets(subscription);
    DefaultComboBoxModel<AzureDockerVnet> dockerHostSelectVnetComboModel = new DefaultComboBoxModel<>();
    for (AzureDockerVnet vnet : dockerVnets) {
      if (region != null && vnet.region.equals(region)) {
        dockerHostSelectVnetComboModel.addElement(vnet);
      }
    }
    dockerHostSelectVnetComboBox.setModel(dockerHostSelectVnetComboModel);
    if (dockerHostSelectVnetComboModel.getSize() > 0) {
      updateDockerSelectSubnetComboBox(dockerHostSelectVnetComboModel.getElementAt(0));
    } else {
      updateDockerSelectSubnetComboBox(null);
    }
  }

  private void updateDockerSelectSubnetComboBox(AzureDockerVnet vnet) {
    DefaultComboBoxModel<String> dockerHostSelectSubnetComboModel = (vnet != null) ?
        new DefaultComboBoxModel<>(new Vector<>(vnet.subnets)) :
        new DefaultComboBoxModel<>();
    dockerHostSelectSubnetComboBox.setModel(dockerHostSelectSubnetComboModel);
  }

  private void updateDockerHostStorageGroup() {
    dockerHostStorageGroup = new ButtonGroup();
    dockerHostStorageGroup.add(dockerHostNewStorageRadioButton);
    dockerHostStorageGroup.add(dockerHostSelectStorageRadioButton);
    dockerHostNewStorageRadioButton.setSelected(true);
    dockerHostNewStorageRadioButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        dockerNewStorageTextField.setEnabled(true);
        if (AzureDockerValidationUtils.validateDockerHostStorageName(dockerNewStorageTextField.getText(), null)) {
          dockerNewStorageLabel.setVisible(false);
          setDialogButtonsState(doValidate(false) == null);
        } else {
          dockerNewStorageLabel.setVisible(true);
          setDialogButtonsState(false);
        }
        dockerSelectStorageComboBox.setEnabled(false);
      }
    });
    dockerHostSelectStorageRadioButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        dockerNewStorageTextField.setEnabled(false);
        dockerNewStorageLabel.setVisible(false);
        setDialogButtonsState(doValidate(false) == null);
        dockerSelectStorageComboBox.setEnabled(true);
      }
    });
    dockerNewStorageLabel.setVisible(false);
    dockerNewStorageTextField.setText(newHost.hostVM.storageAccountName);
    dockerNewStorageTextField.setToolTipText(AzureDockerValidationUtils.getDockerHostStorageNameTip());
    dockerNewStorageTextField.setInputVerifier(new InputVerifier() {
      @Override
      public boolean verify(JComponent input) {
        if (AzureDockerValidationUtils.validateDockerHostStorageName(((JTextField) input).getText(), null)) {
          dockerNewStorageLabel.setVisible(false);
          setDialogButtonsState(doValidate(false) == null);
          return true;
        } else {
          dockerNewStorageLabel.setVisible(true);
          setDialogButtonsState(false);
          return false;
        }
      }
    });
    dockerNewStorageTextField.getDocument().addDocumentListener(resetDialogButtonsState(null));
    dockerSelectStorageComboBox.setEnabled(false);

    updateDockerSelectStorageComboBox((AzureDockerSubscription) dockerSubscriptionComboBox.getSelectedItem());
  }

  private void updateDockerSelectStorageComboBox(AzureDockerSubscription subscription) {
    String vmImageSize = (String) dockerHostVMSizeComboBox.getSelectedItem();
    DefaultComboBoxModel<String> dockerHostStorageComboModel;
    if (vmImageSize != null) {
      List<String> storageAccountsList = dockerManager.getAvailableStorageAccounts(subscription.id, "Standard");
      if (vmImageSize.contains("_D"))
        storageAccountsList.addAll(dockerManager.getAvailableStorageAccounts(subscription.id, "Premium"));
      dockerHostStorageComboModel = new DefaultComboBoxModel<>(new Vector<>(storageAccountsList));
    } else {
      dockerHostStorageComboModel = new DefaultComboBoxModel<>();
    }
    dockerSelectStorageComboBox.setModel(dockerHostStorageComboModel);
  }

  public DockerHost getDockerHost() {
    return newHost;
  }

  @Override
  public ValidationInfo doValidate() {
    return doValidate(true);
  }


  private ValidationInfo validateDockerHostName(boolean shakeOnError) {
    // Docker virtual machine name
    String hostName = dockerHostNameTextField.getText();
    if (hostName == null || hostName.isEmpty() ||
        !AzureDockerValidationUtils.validateDockerHostName(hostName))
    {
      ValidationInfo info = AzureDockerUIResources.validateComponent("Missing virtual machine name", rootConfigureContainerPanel, dockerHostNameTextField, dockerHostNameLabel);
      setDialogButtonsState(false);
      if (shakeOnError) {
        model.DialogShaker(info);
      }
      return info;
    }
    newHost.name = hostName;
    newHost.hostVM.name = hostName;
    newHost.certVault.hostName = hostName;
    newHost.hostVM.publicIpName = hostName + "-pip";

    return null;
  }

  private ValidationInfo validateDockerSubscription(boolean shakeOnError) {
    // Subscription
    AzureDockerSubscription currentSubscription = (AzureDockerSubscription) dockerSubscriptionComboBox.getSelectedItem();
    if (currentSubscription == null || currentSubscription.id == null || currentSubscription.id.isEmpty()) {
      ValidationInfo info = AzureDockerUIResources.validateComponent("Subscription not found", rootConfigureContainerPanel, dockerSubscriptionComboBox, null);
      setDialogButtonsState(false);
      if (shakeOnError) {
        model.DialogShaker(info);
      }
      return info;
    }
    newHost.sid = currentSubscription.id;

    return null;
  }

  private ValidationInfo validateDockerLocation(boolean shakeOnError) {
    // Location/region
    String region = (String) dockerLocationComboBox.getSelectedItem();
    if (preferredLocation == null || region == null || region.isEmpty()) {
      ValidationInfo info = AzureDockerUIResources.validateComponent("Location not found", rootConfigureContainerPanel, dockerLocationComboBox, null);
      setDialogButtonsState(false);
      if (shakeOnError) {
        model.DialogShaker(info);
      }
      return info;
    }
    newHost.hostVM.region = preferredLocation;
    String dnsSuffixFormat = "%s.%s.cloudapp.azure.com";
    // TODO: since this feature will be removed later, just fix it here for mooncake.
    try {
      AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
      if (azureManager != null && azureManager.getEnvironment().equals(Environment.CHINA)) {
        dnsSuffixFormat = "%s.%s.cloudapp.chinacloudapi.cn";
      }
    } catch (Exception e) {}
    newHost.hostVM.dnsName = String.format(dnsSuffixFormat, newHost.hostVM.name, newHost.hostVM.region);
    newHost.apiUrl = newHost.hostVM.dnsName;

    return null;
  }

  private ValidationInfo validateDockerOSType(boolean shakeOnError) {
    // OS type
    KnownDockerVirtualMachineImage osType = (KnownDockerVirtualMachineImage) dockerHostOSTypeComboBox.getSelectedItem();
    if (osType == null) {
      ValidationInfo info = AzureDockerUIResources.validateComponent("OS type not found", vmKindPanel, dockerHostOSTypeComboBox, null);
      hostDetailsTabbedPane.setSelectedComponent(vmKindPanel);
      setDialogButtonsState(false);
      if (shakeOnError) {
        model.DialogShaker(info);
      }
      return info;
    }
    newHost.hostOSType = DockerHost.DockerHostOSType.valueOf(osType.toString());
    newHost.hostVM.osHost = osType.getAzureOSHost();

    return null;
  }

  private ValidationInfo validateDockerVMSize(boolean shakeOnError) {
    // Docker virtual machine size
    String vmSize = (String) dockerHostVMSizeComboBox.getSelectedItem();
    if (vmSize == null || vmSize.isEmpty()) {
      ValidationInfo info = AzureDockerUIResources.validateComponent("Virtual machine size not found", vmKindPanel, dockerHostVMSizeComboBox, null);
      hostDetailsTabbedPane.setSelectedComponent(vmKindPanel);
      setDialogButtonsState(false);
      if (shakeOnError) {
        model.DialogShaker(info);
      }
      return info;
    }
    newHost.hostVM.vmSize = vmSize;

    return null;
  }

  private ValidationInfo validateDockerRG(boolean shakeOnError) {
    // Docker resource group name
    if (dockerHostNewRGRadioButton.isSelected()) {
      // New resource group
      String rgName = dockerHostRGTextField.getText();
      if (rgName == null || rgName.isEmpty() ||
          !AzureDockerValidationUtils.validateDockerHostResourceGroupName(rgName)) {
        ValidationInfo info = AzureDockerUIResources.validateComponent("Missing resource group name", rgPanel, dockerHostRGTextField, dockerHostRGLabel);
        hostDetailsTabbedPane.setSelectedComponent(rgPanel);
        setDialogButtonsState(false);
        if (shakeOnError) {
          model.DialogShaker(info);
        }
        return info;
      }
      newHost.hostVM.resourceGroupName = rgName;

    } else {
      // Existing resource group
      String rgName = (String) dockerHostSelectRGComboBox.getSelectedItem();
      if (rgName == null || rgName.isEmpty()) {
        ValidationInfo info = AzureDockerUIResources.validateComponent("Missing resource group name", rgPanel, dockerHostVMSizeComboBox, null);
        hostDetailsTabbedPane.setSelectedComponent(rgPanel);
        setDialogButtonsState(false);
        if (shakeOnError) {
          model.DialogShaker(info);
        }
        return info;
      }
      // Add "@" to mark this as an existing resource group
      newHost.hostVM.resourceGroupName = rgName + "@";
    }

    return null;
  }

  private ValidationInfo validateDockerVnet(boolean shakeOnError) {
    // Docker virtual network name
    if (dockerHostNewVNetRadioButton.isSelected()) {
      // New virtual network
      String vnetName = dockerHostNewVNetNameTextField.getText();
      if (vnetName == null || vnetName.isEmpty() ||
          !AzureDockerValidationUtils.validateDockerVnetName(vnetName)) {
        ValidationInfo info = AzureDockerUIResources.validateComponent("Missing virtual network name", networkPanel, dockerHostNewVNetNameTextField, dockerHostNewVNetNameLabel);
        hostDetailsTabbedPane.setSelectedComponent(networkPanel);
        setDialogButtonsState(false);
        if (shakeOnError) {
          model.DialogShaker(info);
        }
        return info;
      }
      String vnetAddrSpace = dockerHostNewVNetAddrSpaceTextField.getText();
      if (vnetAddrSpace == null || vnetAddrSpace.isEmpty() ||
          !AzureDockerValidationUtils.validateDockerVnetAddrSpace(vnetAddrSpace)) {
        ValidationInfo info = AzureDockerUIResources.validateComponent("Missing virtual network address space", networkPanel, dockerHostNewVNetAddrSpaceTextField, dockerHostNewVNetAddrSpaceLabel);
        hostDetailsTabbedPane.setSelectedComponent(networkPanel);
        setDialogButtonsState(false);
        if (shakeOnError) {
          model.DialogShaker(info);
        }
        return info;
      }

      newHost.hostVM.vnetName = vnetName;
      newHost.hostVM.vnetAddressSpace = vnetAddrSpace;
      newHost.hostVM.subnetName = "subnet1";
    } else {
      // Existing virtual network and subnet
      AzureDockerVnet vnet = (AzureDockerVnet) dockerHostSelectVnetComboBox.getSelectedItem();
      if (vnet == null || vnet.name == null || vnet.name.isEmpty()) {
        ValidationInfo info = AzureDockerUIResources.validateComponent("Missing virtual network selection", networkPanel, dockerHostSelectVnetComboBox, null);
        hostDetailsTabbedPane.setSelectedComponent(networkPanel);
        setDialogButtonsState(false);
        if (shakeOnError) {
          model.DialogShaker(info);
        }
        return info;
      }
      String subnet = (String) dockerHostSelectSubnetComboBox.getSelectedItem();
      if (subnet == null || subnet.isEmpty()) {
        hostDetailsTabbedPane.setSelectedComponent(networkPanel);
        ValidationInfo info = AzureDockerUIResources.validateComponent("Missing subnet selection", networkPanel, dockerHostSelectSubnetComboBox, null);
        setDialogButtonsState(false);
        if (shakeOnError) {
          model.DialogShaker(info);
        }
        return info;
      }

      // Add "@resourceGroupName" to mark this as an existing virtual network
      newHost.hostVM.vnetName = vnet.name + "@" + vnet.resourceGroup;
      newHost.hostVM.vnetAddressSpace = vnet.addrSpace;
      newHost.hostVM.subnetName = subnet;
    }

    return null;
  }

  private ValidationInfo validateDockerStorage(boolean shakeOnError) {
    // Docker storage account
    String vmSize = (String) dockerHostVMSizeComboBox.getSelectedItem();
    String storageName;
    if (dockerHostNewStorageRadioButton.isSelected()) {
      // New storage account
      storageName = dockerNewStorageTextField.getText();
      if (storageName == null || storageName.isEmpty() || vmSize == null || vmSize.isEmpty() ||
          !AzureDockerValidationUtils.validateDockerHostStorageName(storageName, (AzureDockerSubscription) dockerSubscriptionComboBox.getSelectedItem())) {
        ValidationInfo info = AzureDockerUIResources.validateComponent("Missing storage account name", storagePanel, dockerNewStorageTextField, dockerNewStorageLabel);
        hostDetailsTabbedPane.setSelectedComponent(storagePanel);
        setDialogButtonsState(false);
        if (shakeOnError) {
          model.DialogShaker(info);
        }
        return info;
      }

      newHost.hostVM.storageAccountName = storageName;
      newHost.hostVM.storageAccountType = AzureDockerUtils.getStorageTypeForVMSize(vmSize);
    } else {
      // Existing resource group
      storageName = (String) dockerSelectStorageComboBox.getSelectedItem();
      if (storageName == null || storageName.isEmpty() || vmSize == null || vmSize.isEmpty()) {
        ValidationInfo info = AzureDockerUIResources.validateComponent("Missing storage account selection", storagePanel, dockerSelectStorageComboBox, null);
        hostDetailsTabbedPane.setSelectedComponent(storagePanel);
        setDialogButtonsState(false);
        if (shakeOnError) {
          model.DialogShaker(info);
        }
        return info;
      }
      // Add "@" to mark this as an existing storage account
      newHost.hostVM.storageAccountName = storageName + "@";
      newHost.hostVM.storageAccountType = AzureDockerUtils.getStorageTypeForVMSize(vmSize);
    }

    setDialogButtonsState(true);

    return null;
  }

  private ValidationInfo doValidate(boolean shakeOnError) {
    ValidationInfo result;

    result = validateDockerHostName(shakeOnError);
    if (result != null) {
      return result;
    }

    result = validateDockerSubscription(shakeOnError);
    if (result != null) {
      return result;
    }

    result = validateDockerLocation(shakeOnError);
    if (result != null) {
      return result;
    }

    result = validateDockerOSType(shakeOnError);
    if (result != null) {
      return result;
    }

    result = validateDockerVMSize(shakeOnError);
    if (result != null) {
      return result;
    }

    result = validateDockerRG(shakeOnError);
    if (result != null) {
      return result;
    }

    result = validateDockerVnet(shakeOnError);
    if (result != null) {
      return result;
    }

    result = validateDockerStorage(shakeOnError);
    if (result != null) {
      return result;
    }

    setDialogButtonsState(true);

    return null;
  }

  private void setFinishButtonState(boolean finishButtonState) {
    model.getCurrentNavigationState().FINISH.setEnabled(finishButtonState);
  }

  private void setPreviousButtonState(boolean previousButtonState) {
    model.getCurrentNavigationState().PREVIOUS.setEnabled(previousButtonState);
  }

  private void setNextButtonState(boolean nextButtonState) {
    model.getCurrentNavigationState().NEXT.setEnabled(nextButtonState);
  }

  protected void setDialogButtonsState(boolean buttonsState) {
    setFinishButtonState(buttonsState);
    setNextButtonState(buttonsState);
  }

  @Override
  public JComponent prepare(final WizardNavigationState state) {
    rootConfigureContainerPanel.revalidate();
    setFinishButtonState(true);

    return rootConfigureContainerPanel;
  }

  @Override
  public WizardStep onNext(final AzureNewDockerWizardModel model) {
    if (doValidate() == null) {
      model.setSubscription((AzureDockerSubscription)dockerSubscriptionComboBox.getSelectedItem());
      return super.onNext(model);
    } else {
      return this;
    }
  }

  @Override
  public boolean onFinish() {
    model.setSubscription((AzureDockerSubscription)dockerSubscriptionComboBox.getSelectedItem());
    return model.doValidate() == null && super.onFinish();
  }

  @Override
  public boolean onCancel() {
    model.setSubscription((AzureDockerSubscription)dockerSubscriptionComboBox.getSelectedItem());
    model.finishedOK = true;
    return super.onCancel();
  }

  @Override
  public Map<String, String> toProperties() {
    return model.toProperties();
  }
}
