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
import com.microsoft.azuretools.azurecommons.util.Utils;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.azuretools.telemetrywrapper.ErrorType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.azuretools.telemetrywrapper.TelemetryManager;
import com.microsoft.azuretools.utils.AzureModel;
import com.microsoft.intellij.helpers.LinkListener;
import com.microsoft.intellij.ui.components.AzureDialogWrapper;
import com.microsoft.intellij.util.FormUtils;
import com.microsoft.intellij.util.PluginUtil;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.Comparator;
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
    private JComboBox<SubscriptionDetail> cbSubs;
    private JRadioButton rdoCreateNewGrp;
    private JTextField txtNewResGrp;
    private JRadioButton rdoUseExist;
    private JComboBox<String> cbUseExist;
    private JComboBox<Location> cbLocations;
    private JComboBox<String> cbPricing;
    private JCheckBox chkNoSSL;
    private JLabel lblPricing;

    // Util Variables
    private AzureManager azureManager;
    private List<SubscriptionDetail> allSubs;
    private LinkedHashMap<String, String> skus;
    private List<String> sortedGroups;
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
    private static final Integer REDIS_CACHE_MAX_NAME_LENGTH = 63;
    private static final String DIALOG_TITLE = "New Redis Cache";
    private static final String PRICING_LINK = "https://azure.microsoft.com/en-us/pricing/details/cache";
    private static final String INVALID_REDIS_CACHE_NAME = "Invalid Redis Cache name. The name can only contain letters, numbers and hyphens. The first and last characters must each be a letter or a number. Consecutive hyphens are not allowed.";
    private static final String DNS_NAME_REGEX = "^[A-Za-z0-9]+(-[A-Za-z0-9]+)*$";
    private static final String VALIDATION_FORMAT = "The name %s is not available.";
    private static final String CREATING_INDICATOR = "Creating Redis Cache %s ...";
    private static final String CREATING_ERROR_INDICATOR = "An error occurred while attempting to %s.\n%s";
    private static final String NEW_RES_GRP_ERROR_FORMAT = "The resource group: %s is already existed.";
    private static final String RES_GRP_NAME_RULE = "Resource group name can only allows up to 90 characters, include"
            + " alphanumeric characters, periods, underscores, hyphens and parenthesis and cannot end in a period.";

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
        selectedLocationValue = ((Location) cbLocations.getSelectedItem()).inner().name();
        selectedPriceTierValue = cbPricing.getSelectedItem().toString();

        if (redisCacheNameValue.length() > REDIS_CACHE_MAX_NAME_LENGTH || !redisCacheNameValue.matches(DNS_NAME_REGEX)) {
            return new ValidationInfo(INVALID_REDIS_CACHE_NAME, txtRedisName);
        }

        try {
            if (newResGrp) {
                for (String resGrp : sortedGroups) {
                    if (resGrp.equals(selectedResGrpValue)) {
                        return new ValidationInfo(String.format(NEW_RES_GRP_ERROR_FORMAT, selectedResGrpValue), txtNewResGrp);
                    }
                }
                if (!Utils.isResGrpNameValid(selectedResGrpValue)) {
                    return new ValidationInfo(RES_GRP_NAME_RULE, txtNewResGrp);
                }
            }
            for (RedisCache existingRedisCache : azureManager.getAzure(currentSub.getSubscriptionId()).redisCaches().list()) {
                if (existingRedisCache.name().equals(redisCacheNameValue)) {
                    return new ValidationInfo(String.format(VALIDATION_FORMAT, redisCacheNameValue), txtRedisName);
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
            DefaultLoader.getIdeHelper().runInBackground(
                    null,
                    String.format(CREATING_INDICATOR, ((ProcessorBase) processor).DNSName()),
                    false,
                    true,
                    String.format(CREATING_INDICATOR, ((ProcessorBase) processor).DNSName()),
                    new Runnable() {
                        @Override
                        public void run() {
                            try {
                                processor.waitForCompletion("PRODUCE");
                            } catch (InterruptedException ex) {
                                String msg = String.format(CREATING_ERROR_INDICATOR, "waitForCompletion", ex.getMessage());
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
        final Operation operation = TelemetryManager.createOperation(REDIS, CREATE_REDIS);
        try {
            operation.start();
            Azure azure = azureManager.getAzure(currentSub.getSubscriptionId());
            setSubscription(currentSub);
            ProcessingStrategy processor = RedisCacheUtil.doGetProcessor(azure, skus, redisCacheNameValue, selectedLocationValue, selectedResGrpValue, selectedPriceTierValue, noSSLPort, newResGrp);
            ExecutorService executor = Executors.newSingleThreadExecutor();
            ListeningExecutorService executorService = MoreExecutors.listeningDecorator(executor);
            ListenableFuture<Void> futureTask =  executorService.submit(new CreateRedisCallable(processor));
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
                    JOptionPane.showMessageDialog(null, throwable.getMessage(), "Error occurred when creating Redis Cache: " + redisCacheNameValue, JOptionPane.ERROR_MESSAGE, null);
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
        allSubs = azureManager.getSubscriptionManager().getSubscriptionDetails();
        List<SubscriptionDetail> selectedSubscriptions = allSubs.stream().filter(SubscriptionDetail::isSelected).collect(Collectors.toList());
        cbSubs.setModel(new DefaultComboBoxModel<>(selectedSubscriptions.toArray(new SubscriptionDetail[selectedSubscriptions.size()])));
        if (selectedSubscriptions.size() > 0) {
            currentSub = (SubscriptionDetail) cbSubs.getSelectedItem();
            FormUtils.loadLocationsAndResourceGrps(project);
            fillLocationsAndResourceGrps(currentSub);
        }

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
                currentSub = (SubscriptionDetail) cbSubs.getSelectedItem();
                fillLocationsAndResourceGrps(currentSub);
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

        cbLocations.setRenderer(new ListCellRendererWrapper<Object>() {
            @Override
            public void customize(JList jList, Object o, int i, boolean b, boolean b1) {
                if (o != null && (o instanceof Location)) {
                    setText("  " + ((Location)o).displayName());
                }
            }
        });
    }

    private void fillLocationsAndResourceGrps(SubscriptionDetail selectedSub) {
        List<Location> locations = AzureModel.getInstance().getSubscriptionToLocationMap().get(selectedSub);
        if (locations != null) {
            List<Location> sortedLocations = locations.stream().sorted(Comparator.comparing(Location::displayName)).collect(Collectors.toList());
            cbLocations.setModel(new DefaultComboBoxModel(sortedLocations.toArray()));
        }
        List<ResourceGroup> groups = AzureModel.getInstance().getSubscriptionToResourceGroupMap().get(selectedSub);
        if (groups != null) {
            sortedGroups = groups.stream().map(ResourceGroup::name).sorted().collect(Collectors.toList());
            cbUseExist.setModel(new DefaultComboBoxModel<>(sortedGroups.toArray(new String[sortedGroups.size()])));
        }
    }
}
