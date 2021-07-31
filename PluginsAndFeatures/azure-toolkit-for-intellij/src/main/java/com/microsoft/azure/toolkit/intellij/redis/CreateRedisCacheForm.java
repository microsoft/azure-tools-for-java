/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.redis;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.redis.RedisCache;
import com.microsoft.azure.toolkit.intellij.appservice.region.RegionComboBox;
import com.microsoft.azure.toolkit.intellij.common.component.SubscriptionComboBox;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.ResourceGroup;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.azurecommons.helpers.RedisCacheUtil;
import com.microsoft.azuretools.azurecommons.rediscacheprocessors.ProcessingStrategy;
import com.microsoft.azuretools.azurecommons.rediscacheprocessors.ProcessorBase;
import com.microsoft.azuretools.azurecommons.util.Utils;
import com.microsoft.azuretools.core.mvp.model.AzureMvpModel;
import com.microsoft.azuretools.core.mvp.model.ResourceEx;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.azuretools.telemetrywrapper.ErrorType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.azuretools.telemetrywrapper.TelemetryManager;
import com.microsoft.intellij.helpers.LinkListener;
import com.microsoft.intellij.ui.components.AzureDialogWrapper;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.AzureActionEnum;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import com.microsoft.tooling.msservices.serviceexplorer.azure.rediscache.RedisCacheModule;
import org.jetbrains.annotations.Nullable;

import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static com.microsoft.azuretools.authmanage.AuthMethodManager.getInstance;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.CREATE_REDIS;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.REDIS;
import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class CreateRedisCacheForm extends AzureDialogWrapper {

    // LOGGER
    private static final Logger LOGGER = Logger.getInstance(CreateRedisCacheForm.class);

    // Widgets
    private JPanel pnlContent;
    private JTextField txtRedisName;
    private SubscriptionComboBox cbSubs;
    private JRadioButton rdoCreateNewGrp;
    private JTextField txtNewResGrp;
    private JRadioButton rdoUseExist;
    private JComboBox<String> cbUseExist;
    // TODO(qianjin) : use AzureComboBox
    private com.microsoft.azure.toolkit.intellij.appservice.region.RegionComboBox cbLocations;
    private JComboBox<String> cbPricing;
    private JCheckBox chkNoSSL;
    private JLabel lblPricing;

    // Util Variables
    private AzureManager azureManager;
    private LinkedHashMap<String, String> skus;
    private List<String> sortedGroups;
    private Runnable onCreate;

    // Form Variables
    private Subscription currentSub = null;
    private boolean noSSLPort = false;
    private boolean newResGrp = true;
    private String redisCacheNameValue = null;
    private String selectedLocationValue = null;
    private String selectedResGrpValue = null;
    private String selectedPriceTierValue = null;

    // Const Strings
    private static final Integer REDIS_CACHE_MAX_NAME_LENGTH = 63;
    private static final String DIALOG_TITLE = "New Redis Cache";
    private static final String PRICING_LINK = "https://azure.microsoft.com/en-us/pricing/details/cache";
    private static final String INVALID_REDIS_CACHE_NAME = "Invalid Redis Cache name. The name can only contain letters,"
            + " numbers and hyphens. The first and last characters must each be a letter or a number. "
            + "Consecutive hyphens are not allowed.";
    private static final String DNS_NAME_REGEX = "^[A-Za-z0-9]+(-[A-Za-z0-9]+)*$";
    private static final String VALIDATION_FORMAT = "The name %s is not available.";
    private static final String CREATING_ERROR_INDICATOR = "An error occurred while attempting to %s.\n%s";
    private static final String NEW_RES_GRP_ERROR_FORMAT = "The resource group: %s is already existed.";
    private static final String RES_GRP_NAME_RULE = "Resource group name can only allows up to 90 characters, include"
            + " alphanumeric characters, periods, underscores, hyphens and parenthesis and cannot end in a period.";
    private static final List<String> SUPPORTED_REGIONS = Arrays.asList("centralus", "eastasia", "southeastasia", "eastus", "eastus2", "westus",
            "westus2", "northcentralus", "southcentralus", "westcentralus", "northeurope", "westeurope", "japaneast", "japanwest", "brazilsouth",
            "australiasoutheast", "australiaeast", "westindia", "southindia", "centralindia", "canadacentral", "canadaeast", "uksouth", "ukwest",
            "koreacentral", "koreasouth", "francecentral", "southafricanorth", "uaenorth", "australiacentral", "switzerlandnorth", "germanywestcentral",
            "norwayeast", "australiacentral2", "eastus2euap", "centraluseuap");

    public CreateRedisCacheForm(Project project) throws IOException {
        super(project, true);
        initFormContents(project);
        initWidgetListeners();
        init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return pnlContent;
    }

    private void validateEmptyFields() {
        boolean allFieldsCompleted = !(
                txtRedisName.getText().trim().isEmpty() || cbLocations.getSelectedObjects().length == 0
                        || (rdoCreateNewGrp.isSelected() && txtNewResGrp.getText().trim().isEmpty())
                        || (rdoUseExist.isSelected() && cbUseExist.getSelectedObjects().length == 0)
                        || cbSubs.getSelectedObjects().length == 0);
        setOKActionEnabled(allFieldsCompleted);
    }

    @Nullable
    @Override
    protected ValidationInfo doValidate() {
        redisCacheNameValue = txtRedisName.getText();
        selectedResGrpValue = newResGrp ? txtNewResGrp.getText() : cbUseExist.getSelectedItem().toString();
        selectedLocationValue = ((Region) cbLocations.getSelectedItem()).getName();
        selectedPriceTierValue = cbPricing.getSelectedItem().toString();

        if (redisCacheNameValue.length() > REDIS_CACHE_MAX_NAME_LENGTH || !redisCacheNameValue.matches(DNS_NAME_REGEX)) {
            return new ValidationInfo(INVALID_REDIS_CACHE_NAME, txtRedisName);
        }

        if (newResGrp) {
            for (final String resGrp : sortedGroups) {
                if (resGrp.equals(selectedResGrpValue)) {
                    return new ValidationInfo(String.format(NEW_RES_GRP_ERROR_FORMAT, selectedResGrpValue), txtNewResGrp);
                }
            }
            if (!Utils.isResGrpNameValid(selectedResGrpValue)) {
                return new ValidationInfo(RES_GRP_NAME_RULE, txtNewResGrp);
            }
        }
        for (final RedisCache existingRedisCache : azureManager.getAzure(currentSub.getId()).redisCaches().list()) {
            if (existingRedisCache.name().equals(redisCacheNameValue)) {
                return new ValidationInfo(String.format(VALIDATION_FORMAT, redisCacheNameValue), txtRedisName);
            }
        }

        return null;
    }

    @Override
    protected void doOKAction() {
        onOK();
        super.doOKAction();
    }

    public void setOnCreate(Runnable onCreate) {
        this.onCreate = onCreate;
    }

    class CreateRedisCallable implements Callable<Void> {
        private ProcessingStrategy processor;

        public CreateRedisCallable(ProcessingStrategy processor) {
            this.processor = processor;
        }

        public Void call() throws Exception {
            String progressMessage = Node.getProgressMessage(AzureActionEnum.CREATE.getDoingName(),
                    RedisCacheModule.MODULE_NAME, ((ProcessorBase) processor).DNSName());
            AzureTaskManager.getInstance().runInBackground(new AzureTask(null, progressMessage, false, () -> {
                try {
                    processor.waitForCompletion("PRODUCE");
                } catch (InterruptedException ex) {
                    String msg = String.format(CREATING_ERROR_INDICATOR, "waitForCompletion", ex.getMessage());
                    PluginUtil.displayErrorDialogAndLog(message("errTtl"), msg, ex);
                }
            }));
            // consume
            processor.process().notifyCompletion();
            return null;
        }
    }

    private void onOK() {
        final Operation operation = TelemetryManager.createOperation(REDIS, CREATE_REDIS);
        try {
            operation.start();
            Azure azure = azureManager.getAzure(currentSub.getId());
            setSubscription(currentSub);
            ProcessingStrategy processor = RedisCacheUtil.doGetProcessor(
                    azure, skus, redisCacheNameValue, selectedLocationValue, selectedResGrpValue,
                    selectedPriceTierValue, noSSLPort, newResGrp);
            ExecutorService executor = Executors.newSingleThreadExecutor();
            ListeningExecutorService executorService = MoreExecutors.listeningDecorator(executor);
            ListenableFuture<Void> futureTask = executorService.submit(new CreateRedisCallable(processor));
            final ProcessingStrategy processorInner = processor;
            Futures.addCallback(futureTask, new FutureCallback<Void>() {
                @Override
                public void onSuccess(Void arg0) {
                    if (onCreate != null) {
                        onCreate.run();
                        operation.complete();
                    }
                }

                @Override
                public void onFailure(Throwable throwable) {
                    DefaultLoader.getUIHelper().showError(throwable.getMessage(),
                                                          "Error occurred when creating Redis Cache: "
                                                                  + redisCacheNameValue);
                    EventUtil.logError(operation, ErrorType.userError, new Exception(throwable), null, null);
                    operation.complete();
                    try {
                        // notify the waitting thread the thread being waited incurred exception to clear blocking queue
                        processorInner.notifyCompletion();
                    } catch (InterruptedException ex) {
                        String msg = String.format(CREATING_ERROR_INDICATOR, "notifyCompletion", ex.getMessage());
                        PluginUtil.displayErrorDialogAndLog(message("errTtl"), msg, ex);
                    }
                }
            }, MoreExecutors.directExecutor());
            close(DialogWrapper.OK_EXIT_CODE, true);
        } catch (Exception ex) {
            ex.printStackTrace();
            EventUtil.logError(operation, ErrorType.userError, ex, null, null);
            operation.complete();
        }
    }

    private void initFormContents(Project project) throws IOException {
        setModal(true);
        setTitle(DIALOG_TITLE);
        final ButtonGroup btnGrp = new ButtonGroup();
        btnGrp.add(rdoCreateNewGrp);
        btnGrp.add(rdoUseExist);
        rdoCreateNewGrp.setSelected(true);
        rdoUseExist.setSelected(false);
        txtNewResGrp.setVisible(true);
        cbUseExist.setVisible(false);
        setOKActionEnabled(false);

        azureManager = getInstance().getAzureManager();
        skus = RedisCacheUtil.initSkus();
        cbPricing.setModel(new DefaultComboBoxModel(skus.keySet().toArray()));
    }

    private void initWidgetListeners() {
        txtRedisName.getDocument().addDocumentListener(new DocumentListener() {
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

        cbSubs.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                currentSub = (Subscription) cbSubs.getSelectedItem();
                cbLocations.setSubscription(currentSub);
                fillResourceGrps(currentSub);
                validateEmptyFields();
            }
        });

        rdoCreateNewGrp.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                txtNewResGrp.setVisible(true);
                cbUseExist.setVisible(false);
                newResGrp = true;
                validateEmptyFields();
            }
        });

        txtNewResGrp.getDocument().addDocumentListener(new DocumentListener() {
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

        rdoUseExist.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                txtNewResGrp.setVisible(false);
                cbUseExist.setVisible(true);
                newResGrp = false;
                validateEmptyFields();
            }
        });

        cbUseExist.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                validateEmptyFields();
            }
        });

        cbLocations.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                validateEmptyFields();
            }
        });

        lblPricing.addMouseListener(new LinkListener(PRICING_LINK));

        cbPricing.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                validateEmptyFields();
            }
        });

        chkNoSSL.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (chkNoSSL.isSelected()) {
                    noSSLPort = true;
                } else {
                    noSSLPort = false;
                }
            }
        });
    }

    private void createUIComponents() {
        this.cbSubs = new SubscriptionComboBox();
        this.cbLocations = new RegionComboBox();
    }

    private void fillResourceGrps(Subscription selectedSub) {
        List<ResourceGroup> groups = AzureMvpModel.getInstance().getResourceGroups(selectedSub.getId()).stream()
                .map(ResourceEx::getResource).collect(Collectors.toList());
        if (groups != null) {
            sortedGroups = groups.stream().map(ResourceGroup::getName).sorted().collect(Collectors.toList());
            cbUseExist.setModel(new DefaultComboBoxModel<>(sortedGroups.toArray(new String[sortedGroups.size()])));
        }
    }
}
