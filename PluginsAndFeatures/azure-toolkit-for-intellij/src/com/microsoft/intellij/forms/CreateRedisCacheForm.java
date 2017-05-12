/**
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.microsoft.intellij.forms;

import com.google.common.util.concurrent.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.ListCellRendererWrapper;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.resources.Location;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.azurecommons.helpers.RedisCacheUtil;
import com.microsoft.azuretools.azurecommons.rediscacheprocessors.ProcessingStrategy;
import com.microsoft.azuretools.azurecommons.rediscacheprocessors.ProcessorBase;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.azuretools.utils.AzureModel;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.intellij.util.FormUtils;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static com.microsoft.azuretools.authmanage.AuthMethodManager.getInstance;
import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class CreateRedisCacheForm extends DialogWrapper {

    private static final Logger LOGGER = Logger.getInstance(CreateRedisCacheForm.class);
    private JPanel contentPane;
    private JTextField DNSNameTextField;
    private JComboBox<SubscriptionDetail> SubscriptionsComboBox;
    private JRadioButton CreateNewRadioButton;
    private JRadioButton UseExistRadioButton;
    private JTextField NewResGrpTextField;
    private JComboBox<Location> LocationsComboBox;
    private JComboBox<String> PricingTierComboBox;
    private JCheckBox noSSLCheckBox;
    private JComboBox<String> UseExistComboBox;

    private final AzureManager azureManager;
    private List<SubscriptionDetail> allSubs;
    private Set<String> allResGrpsOfCurrentSub;
    private SubscriptionDetail currentSub;
    private boolean noSSLPort = false;
    private boolean newResGrp = true;
    private final LinkedHashMap<String, String> skus;

    private String dnsNameValue;
    private String selectedRegionValue;
    private String selectedResGrpValue;
    private String selectedPriceTierValue;

    private Runnable onCreate;
    private Project project;

    public CreateRedisCacheForm(Project project) throws IOException {
        super(project, true);
        this.project = project;

        setModal(true);
        setTitle("New Redis Cache");

        azureManager = getInstance().getAzureManager();
        allSubs = azureManager.getSubscriptionManager().getSubscriptionDetails();
        allResGrpsOfCurrentSub = new HashSet<String>();
        currentSub = null;
        skus = RedisCacheUtil.initSkus();

        List<SubscriptionDetail> selectedSubscriptions = allSubs.stream().filter(SubscriptionDetail::isSelected).collect(Collectors.toList());

        SubscriptionsComboBox.setModel(new DefaultComboBoxModel<>(selectedSubscriptions.toArray(new SubscriptionDetail[selectedSubscriptions.size()])));
        if (selectedSubscriptions.size() > 0) {
            Map<SubscriptionDetail, List<Location>> subscription2Location = AzureModel.getInstance().getSubscriptionToLocationMap();
            SubscriptionDetail selectedSub = (SubscriptionDetail) SubscriptionsComboBox.getSelectedItem();
            currentSub = selectedSub;
            if (subscription2Location == null || subscription2Location.get(selectedSub) == null) {
                FormUtils.loadLocationsAndResourceGrps(project);
                fillLocationsAndResourceGrps(selectedSub);
            } else {
                fillLocationsAndResourceGrps(selectedSub);
            }
        }

        SubscriptionsComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

            }
        });

        SubscriptionsComboBox.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                currentSub = (SubscriptionDetail) SubscriptionsComboBox.getSelectedItem();
            }
        });

        DNSNameTextField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                if(DNSNameTextField != null) {
                    dnsNameValue = DNSNameTextField.getText();
                } else {
                    dnsNameValue = null;
                }
            }
        });

        CreateNewRadioButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                CreateNewRadioButton.setSelected(true);
                UseExistRadioButton.setSelected(false);
                NewResGrpTextField.setVisible(true);
                UseExistComboBox.setVisible(false);
                newResGrp = true;
            }
        });

        NewResGrpTextField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                if(NewResGrpTextField != null && NewResGrpTextField.isVisible()) {
                    selectedResGrpValue = NewResGrpTextField.getText();
                } else {
                    selectedResGrpValue = null;
                }
            }
        });

        UseExistRadioButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                CreateNewRadioButton.setSelected(false);
                CreateNewRadioButton.updateUI();
                UseExistRadioButton.setSelected(true);
                NewResGrpTextField.setVisible(false);
                UseExistComboBox.setVisible(true);
                newResGrp = false;
            }
        });

        UseExistComboBox.addFocusListener(new FocusAdapter() {

            @Override
            public void focusLost(FocusEvent e) {
                if (UseExistComboBox != null && UseExistComboBox.getSelectedItem() != null) {
                    selectedResGrpValue = UseExistComboBox.getSelectedItem().toString();
                } else {
                    selectedResGrpValue = null;
                }
            }
        });

        LocationsComboBox.setRenderer(new ListCellRendererWrapper<Object>() {
            @Override
            public void customize(JList jList, Object o, int i, boolean b, boolean b1) {
                if (o != null && (o instanceof Location)) {
                    setText("  " + ((Location)o).displayName());
                }
            }
        });

        LocationsComboBox.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                if (LocationsComboBox.getSelectedItem() != null) {
                    selectedRegionValue = LocationsComboBox.getSelectedItem().toString();
                } else {
                    selectedRegionValue = null;
                }
            }
        });

        PricingTierComboBox.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                skus.keySet().forEach((String key) -> {
                    PricingTierComboBox.addItem(key);
                });
                PricingTierComboBox.updateUI();
            }
            @Override
            public void focusLost(FocusEvent e) {
                if (PricingTierComboBox.getSelectedItem() != null) {
                    selectedPriceTierValue = PricingTierComboBox.getSelectedItem().toString();
                } else {
                    selectedPriceTierValue = null;
                }
            }
        });

        noSSLCheckBox.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (noSSLCheckBox.isSelected()) {
                    noSSLPort = true;
                } else {
                    noSSLPort = false;
                }
            }
        });

        CreateNewRadioButton.setSelected(true);
        UseExistRadioButton.setSelected(false);
        NewResGrpTextField.setVisible(true);
        UseExistComboBox.setVisible(false);

        init();
    }

    private void fillLocationsAndResourceGrps(SubscriptionDetail selectedSub) {
        List<Location> locations = AzureModel.getInstance().getSubscriptionToLocationMap().get(selectedSub)
                .stream().sorted(Comparator.comparing(Location::displayName)).collect(Collectors.toList());
        LocationsComboBox.setModel(new DefaultComboBoxModel(locations.toArray()));
        List<ResourceGroup> groups = AzureModel.getInstance().getSubscriptionToResourceGroupMap().get(selectedSub);
        List<String> sortedGroups = groups.stream().map(ResourceGroup::name).sorted().collect(Collectors.toList());
        UseExistComboBox.setModel(new DefaultComboBoxModel<>(sortedGroups.toArray(new String[sortedGroups.size()])));
    }


    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }

    @Override
    protected void doOKAction() {
        onOK();
    }

    public void setOnCreate(Runnable onCreate) {
        this.onCreate = onCreate;
    }

    class MyCallable implements Callable<Void> {
        private ProcessingStrategy processor;
        public MyCallable(ProcessingStrategy processor) {
            this.processor = processor;
        }
        public Void call() throws Exception {
            DefaultLoader.getIdeHelper().runInBackground(
                    null,
                    "Creating Redis Cache " + ((ProcessorBase) processor).DNSName() + "...",
                    false,
                    true,
                    "Creating Redis Cache " + ((ProcessorBase) processor).DNSName() + "...",
                    new Runnable() {
                        @Override
                        public void run() {
                            try {
                                processor.waitForCompletion("PRODUCE");
                            } catch (InterruptedException ex) {
                                String msg = "An error occurred while attempting to call waitForCompletion." + "\n" + String.format(message("rediscacheExpMsg"), ex.getMessage());
                                PluginUtil.displayErrorDialogAndLog(message("errTtl"), msg, ex);
                            }
                        }
                    });
            // consume
            processor.process().notifyCompletion();
            return null;
        }
    }

    private void onOK() {
        try {
            if(!RedisCacheUtil.doValidate(azureManager, currentSub, dnsNameValue, selectedRegionValue, selectedResGrpValue, selectedPriceTierValue))
            {
                return;
            }
            Azure azure = azureManager.getAzure(currentSub.getSubscriptionId());
            ProcessingStrategy processor = RedisCacheUtil.doGetProcessor(azure, skus, dnsNameValue, selectedRegionValue, selectedResGrpValue, selectedPriceTierValue, noSSLPort, newResGrp);
            ExecutorService executor = Executors.newSingleThreadExecutor();
            ListeningExecutorService executorService = MoreExecutors.listeningDecorator(executor);
            ListenableFuture<Void> futureTask =  executorService.submit(new MyCallable(processor));
            final ProcessingStrategy processorInner = processor;
            Futures.addCallback(futureTask, new FutureCallback<Void>() {
                @Override
                public void onSuccess(Void arg0) {
                }
                @Override
                public void onFailure(Throwable throwable) {
                    JOptionPane.showMessageDialog(null, throwable.getMessage(), "Error occurred when creating Redis Cache: " + dnsNameValue, JOptionPane.ERROR_MESSAGE, null);
                    try {
                        // notify the waitting thread the thread being waited incurred exception to clear blocking queue
                        processorInner.notifyCompletion();
                    } catch (InterruptedException ex) {
                        String msg = "An error occurred while attempting to call notifyCompletion." + "\n" + String.format(message("rediscacheExpMsg"), ex.getMessage());
                        PluginUtil.displayErrorDialogAndLog(message("errTtl"), msg, ex);
                    }
                }
            });
            if (onCreate != null) {
                onCreate.run();
            }
            close(DialogWrapper.OK_EXIT_CODE, true);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }
}
