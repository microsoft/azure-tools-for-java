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
import com.intellij.openapi.ui.ValidationInfo;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.resources.Location;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.azurecommons.helpers.RedisCacheUtil;
import com.microsoft.azuretools.azurecommons.rediscacheprocessors.ProcessingStrategy;
import com.microsoft.azuretools.azurecommons.rediscacheprocessors.ProcessorBase;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.microsoft.azuretools.authmanage.AuthMethodManager.getInstance;
import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class CreateRedisCacheForm extends DialogWrapper {

    private static final Logger LOGGER = Logger.getInstance(CreateRedisCacheForm.class);
    private JPanel contentPane;
    private JTextField DNSNameTextField;
    private JComboBox SubscriptionsComboBox;
    private JRadioButton CreateNewRadioButton;
    private JRadioButton UseExistRadioButton;
    private JTextField NewResGrpTextField;
    private JComboBox LocationsComboBox;
    private JComboBox PricingTierComboBox;
    private JCheckBox noSSLCheckBox;
    private JComboBox UseExistComboBox;

    protected final AzureManager azureManager;
    protected List<SubscriptionDetail> allSubs;
    protected Set<String> allResGrpsofCurrentSub;
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
        allResGrpsofCurrentSub = new HashSet<String>();
        currentSub = null;
        skus = RedisCacheUtil.initSkus();

        SubscriptionsComboBox.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                SubscriptionsComboBox.removeAllItems();
                for (SubscriptionDetail subDetail : allSubs) {
                    if (subDetail.isSelected()) {
                        SubscriptionsComboBox.addItem(subDetail.getSubscriptionName());
                    }
                    SubscriptionsComboBox.updateUI();
                }
            }
            @Override
            public void focusLost(FocusEvent e) {
                allSubs.forEach((SubscriptionDetail s) -> {
                    if (SubscriptionsComboBox.getSelectedItem().equals(s.getSubscriptionName()))
                        currentSub = s;
                });
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
                UseExistRadioButton.setSelected(true);
                NewResGrpTextField.setVisible(false);
                UseExistComboBox.setVisible(true);
                newResGrp = false;
                doRetriveResourceGroups();
            }
        });

        UseExistComboBox.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (UseExistComboBox != null && UseExistComboBox.isVisible()) {
                    UseExistComboBox.removeAllItems();
                    allResGrpsofCurrentSub.forEach((String resGrp) -> {
                        UseExistComboBox.addItem(resGrp);
                    });
                    UseExistComboBox.updateUI();
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (UseExistComboBox != null && UseExistComboBox.getSelectedItem() != null) {
                    selectedResGrpValue = UseExistComboBox.getSelectedItem().toString();
                } else {
                    selectedResGrpValue = null;
                }
            }
        });

        LocationsComboBox.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                LocationsComboBox.removeAllItems();
                for (Region r : Region.values()) {
                    //Redis unavailable in Azure China Cloud, Azure German Cloud, Azure Government Cloud
                    if (!r.name().equals(Region.CHINA_NORTH.name()) &&
                            !r.name().equals(Region.CHINA_EAST.name()) &&
                            !r.name().equals(Region.GERMANY_CENTRAL.name()) &&
                            !r.name().equals(Region.GERMANY_NORTHEAST.name()) &&
                            !r.name().equals(Region.GOV_US_VIRGINIA.name()) &&
                            !r.name().equals(Region.GOV_US_IOWA.name())) {
                        LocationsComboBox.addItem(r.label());
                    }
                }
                LocationsComboBox.updateUI();
            }

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

    private void doRetriveResourceGroups()
    {
        allResGrpsofCurrentSub.clear();
        ProgressManager.getInstance().run(new Task.Modal(project, "Loading Resource Groups...", true) {
            @Override
            public void run(ProgressIndicator progressIndicator) {
                try {
                    progressIndicator.setIndeterminate(true);
                    if(currentSub != null) {
                        azureManager.getAzure(currentSub.getSubscriptionId()).resourceGroups().list().forEach(
                                (ResourceGroup group) -> {
                                    allResGrpsofCurrentSub.add(group.name());
                                });
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    LOGGER.error("doRetriveResourceGroups", ex);
                }
            }
        });
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
