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
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.ListCellRendererWrapper;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.redis.RedisCache;
import com.microsoft.azure.management.resources.Location;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.azurecommons.helpers.RedisCacheUtil;
import com.microsoft.azuretools.azurecommons.rediscacheprocessors.ProcessingStrategy;
import com.microsoft.azuretools.azurecommons.rediscacheprocessors.ProcessorBase;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.azuretools.utils.AzureModel;
import com.microsoft.intellij.helpers.LinkListener;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.intellij.util.FormUtils;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
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

    // LOGGER
    private static final Logger LOGGER = Logger.getInstance(CreateRedisCacheForm.class);

    // Widgets
    private JPanel contentPnl;
    private JTextField redisNameTxt;
    private JComboBox<SubscriptionDetail> subscriptionsCmb;
    private JRadioButton createNewRdoBtn;
    private JTextField newResGrpTxt;
    private JRadioButton useExistRdoBtn;
    private JComboBox<String> useExistCmb;
    private JComboBox<Location> locationsCmb;
    private JComboBox<String> pricingCmb;
    private JCheckBox noSSLChk;
    private JLabel pricingLbl;

    // Util Variables
    private AzureManager azureManager;
    private List<SubscriptionDetail> allSubs;
    private LinkedHashMap<String, String> skus;
    private Runnable onCreate;

    // Form Variables
    private SubscriptionDetail currentSub = null;
    private boolean noSSLPort = false;
    private boolean newResGrp = true;
    private String redisCacheNameValue = null;
    private String selectedLocationValue = null;
    private String selectedResGrpValue = null;
    private String selectedPriceTierValue = null;

    // Const Strings
    private static final String PRICING_LINK = "https://azure.microsoft.com/en-us/pricing/details/cache";
    private static final String INVALID_REDIS_CACHE_NAME = "Invalid Redis Cache name. The name can only contain letters, numbers and hyphens. The first and last characters must each be a letter or a number. Consecutive hyphens are not allowed.";

    public CreateRedisCacheForm(Project project) throws IOException {
        super(project, true);
        initFormContents(project);
        initWidgetListeners();
        init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPnl;
    }

    private void validateEmptyFields() {
        boolean allFieldsCompleted = !(
                redisNameTxt.getText().trim().isEmpty() || locationsCmb.getSelectedObjects().length == 0
                        || (createNewRdoBtn.isSelected() && newResGrpTxt.getText().trim().isEmpty())
                        || (useExistRdoBtn.isSelected() && useExistCmb.getSelectedObjects().length == 0)
                        || subscriptionsCmb.getSelectedObjects().length == 0);
        setOKActionEnabled(allFieldsCompleted);
    }

    @Nullable
    @Override
    protected ValidationInfo doValidate() {
        redisCacheNameValue = redisNameTxt.getText();
        selectedResGrpValue = newResGrp ? newResGrpTxt.getText() : useExistCmb.getSelectedItem().toString();
        selectedLocationValue = ((Location) locationsCmb.getSelectedItem()).inner().name();
        selectedPriceTierValue = pricingCmb.getSelectedItem().toString();

        if (redisCacheNameValue.length() > 63 || !redisCacheNameValue.matches("^[A-Za-z0-9]+(-[A-Za-z0-9]+)*$")) {
            return new ValidationInfo(INVALID_REDIS_CACHE_NAME, redisNameTxt);
        }

        try {
            for (RedisCache existingRedisCache : azureManager.getAzure(currentSub.getSubscriptionId()).redisCaches().list()) {
                if (existingRedisCache.name().equals(redisCacheNameValue)) {
                    return new ValidationInfo("The name " + redisCacheNameValue + " is not available", redisNameTxt);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
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
                                String msg = "An error occurred while attempting to call waitForCompletion." + "\n" + ex.getMessage();
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
            Azure azure = azureManager.getAzure(currentSub.getSubscriptionId());
            ProcessingStrategy processor = RedisCacheUtil.doGetProcessor(azure, skus, redisCacheNameValue, selectedLocationValue, selectedResGrpValue, selectedPriceTierValue, noSSLPort, newResGrp);
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
                    JOptionPane.showMessageDialog(null, throwable.getMessage(), "Error occurred when creating Redis Cache: " + redisCacheNameValue, JOptionPane.ERROR_MESSAGE, null);
                    try {
                        // notify the waitting thread the thread being waited incurred exception to clear blocking queue
                        processorInner.notifyCompletion();
                    } catch (InterruptedException ex) {
                        String msg = "An error occurred while attempting to call notifyCompletion." + "\n" + ex.getMessage();
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

    private void initFormContents(Project project) throws IOException {
        setModal(true);
        setTitle("New Redis Cache");
        final ButtonGroup btnGrp = new ButtonGroup();
        btnGrp.add(createNewRdoBtn);
        btnGrp.add(useExistRdoBtn);
        createNewRdoBtn.setSelected(true);
        useExistRdoBtn.setSelected(false);
        newResGrpTxt.setVisible(true);
        useExistCmb.setVisible(false);
        setOKActionEnabled(false);

        azureManager = getInstance().getAzureManager();
        allSubs = azureManager.getSubscriptionManager().getSubscriptionDetails();
        List<SubscriptionDetail> selectedSubscriptions = allSubs.stream().filter(SubscriptionDetail::isSelected).collect(Collectors.toList());
        subscriptionsCmb.setModel(new DefaultComboBoxModel<>(selectedSubscriptions.toArray(new SubscriptionDetail[selectedSubscriptions.size()])));
        if (selectedSubscriptions.size() > 0) {
            Map<SubscriptionDetail, List<Location>> subscription2Location = AzureModel.getInstance().getSubscriptionToLocationMap();
            SubscriptionDetail selectedSub = (SubscriptionDetail) subscriptionsCmb.getSelectedItem();
            currentSub = selectedSub;
            if (subscription2Location == null || subscription2Location.get(selectedSub) == null) {
                FormUtils.loadLocationsAndResourceGrps(project);
            }
            fillLocationsAndResourceGrps(selectedSub);
        }

        skus = RedisCacheUtil.initSkus();
        pricingCmb.setModel(new DefaultComboBoxModel(skus.keySet().toArray()));
    }

    private void initWidgetListeners() {
        redisNameTxt.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent documentEvent) {
                validateEmptyFields();
            }

            @Override
            public void removeUpdate(DocumentEvent documentEvent) {
                validateEmptyFields();
            }

            @Override
            public void changedUpdate(DocumentEvent documentEvent) {
                validateEmptyFields();
            }
        });

        subscriptionsCmb.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                currentSub = (SubscriptionDetail) subscriptionsCmb.getSelectedItem();
                fillLocationsAndResourceGrps(currentSub);
                validateEmptyFields();
            }
        });

        createNewRdoBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                newResGrpTxt.setVisible(true);
                useExistCmb.setVisible(false);
                newResGrp = true;
                validateEmptyFields();
            }
        });

        newResGrpTxt.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent documentEvent) {
                validateEmptyFields();
            }

            @Override
            public void removeUpdate(DocumentEvent documentEvent) {
                validateEmptyFields();
            }

            @Override
            public void changedUpdate(DocumentEvent documentEvent) {
                validateEmptyFields();
            }
        });

        useExistRdoBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                newResGrpTxt.setVisible(false);
                useExistCmb.setVisible(true);
                newResGrp = false;
                validateEmptyFields();
            }
        });

        useExistCmb.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                validateEmptyFields();
            }
        });


        locationsCmb.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                validateEmptyFields();
            }
        });

        pricingLbl.addMouseListener(new LinkListener(PRICING_LINK));


        pricingCmb.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                validateEmptyFields();
            }
        });

        noSSLChk.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (noSSLChk.isSelected()) {
                    noSSLPort = true;
                } else {
                    noSSLPort = false;
                }
            }
        });

        locationsCmb.setRenderer(new ListCellRendererWrapper<Object>() {
            @Override
            public void customize(JList jList, Object o, int i, boolean b, boolean b1) {
                if (o != null && (o instanceof Location)) {
                    setText("  " + ((Location)o).displayName());
                }
            }
        });
    }

    private void fillLocationsAndResourceGrps(SubscriptionDetail selectedSub) {
        List<Location> locations = AzureModel.getInstance().getSubscriptionToLocationMap().get(selectedSub)
                .stream().sorted(Comparator.comparing(Location::displayName)).collect(Collectors.toList());
        locationsCmb.setModel(new DefaultComboBoxModel(locations.toArray()));
        List<ResourceGroup> groups = AzureModel.getInstance().getSubscriptionToResourceGroupMap().get(selectedSub);
        List<String> sortedGroups = groups.stream().map(ResourceGroup::name).sorted().collect(Collectors.toList());
        useExistCmb.setModel(new DefaultComboBoxModel<>(sortedGroups.toArray(new String[sortedGroups.size()])));
    }
}
