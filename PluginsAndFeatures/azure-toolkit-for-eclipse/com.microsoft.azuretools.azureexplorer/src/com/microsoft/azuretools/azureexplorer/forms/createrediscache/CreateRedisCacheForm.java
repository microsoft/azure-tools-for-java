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

package com.microsoft.azuretools.azureexplorer.forms.createrediscache;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.CREATE_REDIS;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.REDIS;

import com.microsoft.azuretools.telemetrywrapper.ErrorType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.azuretools.telemetrywrapper.TelemetryManager;
import java.io.IOException;
import java.net.URL;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.resources.Location;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.azurecommons.exceptions.InvalidFormDataException;
import com.microsoft.azuretools.azurecommons.helpers.RedisCacheUtil;
import com.microsoft.azuretools.azurecommons.rediscacheprocessors.ProcessingStrategy;
import com.microsoft.azuretools.azurecommons.rediscacheprocessors.ProcessorBase;
import com.microsoft.azuretools.azurecommons.util.Utils;
import com.microsoft.azuretools.azureexplorer.messages.MessageHandler;
import com.microsoft.azuretools.core.Activator;
import com.microsoft.azuretools.core.components.AzureTitleAreaDialogWrapper;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.azuretools.utils.AzureModel;
import com.microsoft.azuretools.utils.AzureModelController;
import com.microsoft.tooling.msservices.components.DefaultLoader;

public class CreateRedisCacheForm extends AzureTitleAreaDialogWrapper {

    private static Activator LOG = Activator.getDefault();
    protected final AzureManager azureManager;
    private List<SubscriptionDetail> selectedSubscriptions;
    private List<Location> sortedLocations;
    private List<String> sortedGroups;
    private SubscriptionDetail currentSub;
    private boolean noSSLPort = false;
    private boolean newResGrp = true;
    private boolean loaded = false;
    private final LinkedHashMap<String, String> skus;

    private String dnsNameValue = null;
    private String selectedLocationValue = null;
    private String selectedResGrpValue = null;
    private String selectedPriceTierValue = null;

    private Combo cbSubs;
    private Combo cbUseExisting;
    private Combo cbLocations;
    private Combo cbPricetiers;

    private Button chkUnblockPort;
    private Button rdoUseExisting;
    private Button rdoCreateNew;
    private Button btnOK;

    private Text txtDnsName;
    private Text txtNewResGrpName;

    private ControlDecoration decoratorDnsName;
    private ControlDecoration decoratorResGrpName;

    private Runnable onCreate;

    private ResourceBundle resourceBundle = null;

    // const variable
    private static final Integer LAYOUT_SPACING = 10;
    private static final Integer REDIS_CACHE_MAX_NAME_LENGTH = 63;
    private static final String MODULE_NAME = "rediscache";
    private static final String SUBS_COMBO_ITEMS_FORMAT = "%s (%s)";
    private static final String DNS_NAME_REGEX = "^[A-Za-z0-9]+(-[A-Za-z0-9]+)*$";

    // const for widgets
    private static final String DIALOG_TITLE = "DIALOG_TITLE";
    private static final String DIALOG_MESSAGE = "DIALOG_MESSAGE";
    private static final String LABEL_DNS_NAME = "LABEL_DNS_NAME";
    private static final String LABEL_DNS_SUFFIX = "LABEL_DNS_SUFFIX";
    private static final String LABEL_SUBSCRIPTION = "LABEL_SUBSCRIPTION";
    private static final String LABEL_RESOURCE_GRP = "LABEL_RESOURCE_GRP";
    private static final String RADIOBUTTON_USE_EXIST_GRP = "RADIOBUTTON_USE_EXIST_GRP";
    private static final String RADIOBUTTON_NEW_GRP = "RADIOBUTTON_NEW_GRP";
    private static final String LABEL_LOCTION = "LABEL_LOCTION";
    private static final String LABEL_PRICING = "LABEL_PRICING";
    private static final String LINK_PRICE = "LINK_PRICE";
    private static final String CHECKBOX_SSL = "CHECKBOX_SSL";

    // const for creating information
    private static final String LOADING_LOCATION_AND_GRPS = "LOADING_LOCATION_AND_GRPS";
    private static final String LOADING = "LOADING";
    private static final String DECORACTOR_DNS = "DECORACTOR_DNS";
    private static final String CREATING_INDICATOR_FORMAT = "CREATING_INDICATOR_FORMAT";

    // const for error
    private static final String CREATING_ERROR_INDICATOR_FORMAT = "CREATING_ERROR_INDICATOR_FORMAT";
    private static final String OPEN_BROWSER_ERROR = "OPEN_BROWSER_ERROR";
    private static final String LOAD_LOCATION_AND_RESOURCE_ERROR = "LOAD_LOCATION_AND_RESOURCE_ERROR";
    private static final String RES_GRP_NAME_RULE = "RES_GRP_NAME_RULE";

    /**
     * Create the dialog.
     *
     * @param parentShell
     * @throws IOException
     */
    public CreateRedisCacheForm(Shell parentShell) throws IOException {
        super(parentShell);
        azureManager = AuthMethodManager.getInstance().getAzureManager();
        List<SubscriptionDetail> allSubs = azureManager.getSubscriptionManager().getSubscriptionDetails();
        selectedSubscriptions = allSubs.stream().filter(SubscriptionDetail::isSelected).collect(Collectors.toList());
        if (selectedSubscriptions.size() > 0) {
            currentSub = selectedSubscriptions.get(0);
        }
        skus = RedisCacheUtil.initSkus();
    }

    /**
     * Create contents of the dialog.
     *
     * @param parent
     */
    @Override
    protected Control createDialogArea(Composite parent) {
        resourceBundle = MessageHandler.getResourceBundle(MODULE_NAME);
        if (resourceBundle == null) {
            return null;
        }
        setTitle(MessageHandler.getResourceString(resourceBundle, DIALOG_TITLE));
        setMessage(MessageHandler.getResourceString(resourceBundle, DIALOG_MESSAGE));
        Composite area = (Composite) super.createDialogArea(parent);
        Composite container = new Composite(area, SWT.NONE);
        GridLayout glContainer = new GridLayout(4, false);
        glContainer.horizontalSpacing = LAYOUT_SPACING;
        glContainer.verticalSpacing = LAYOUT_SPACING;
        container.setLayout(glContainer);
        container.setLayoutData(new GridData(GridData.FILL_BOTH));

        Label lblDnsName = new Label(container, SWT.NONE);
        lblDnsName.setText(MessageHandler.getResourceString(resourceBundle, LABEL_DNS_NAME));

        txtDnsName = new Text(container, SWT.BORDER);
        txtDnsName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

        decoratorDnsName = new ControlDecoration(txtDnsName, SWT.CENTER);
        decoratorDnsName.setDescriptionText(MessageHandler.getResourceString(resourceBundle, DECORACTOR_DNS));

        FieldDecoration fieldDecoration = FieldDecorationRegistry.getDefault()
                .getFieldDecoration(FieldDecorationRegistry.DEC_ERROR);
        if (fieldDecoration != null) {
            Image image = fieldDecoration.getImage();
            decoratorDnsName.setImage(image);
        }

        Label lblDnsSuffix = new Label(container, SWT.NONE);
        lblDnsSuffix.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
        lblDnsSuffix.setText(MessageHandler.getResourceString(resourceBundle, LABEL_DNS_SUFFIX));

        Label lblSubscription = new Label(container, SWT.NONE);
        lblSubscription.setText(MessageHandler.getResourceString(resourceBundle, LABEL_SUBSCRIPTION));

        cbSubs = new Combo(container, SWT.READ_ONLY);
        cbSubs.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));

        Label lblResourceGroup = new Label(container, SWT.NONE);
        lblResourceGroup.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 4, 1));
        lblResourceGroup.setText(MessageHandler.getResourceString(resourceBundle, LABEL_RESOURCE_GRP));

        rdoCreateNew = new Button(container, SWT.RADIO);
        rdoCreateNew.setText(MessageHandler.getResourceString(resourceBundle, RADIOBUTTON_NEW_GRP));
        rdoCreateNew.setSelection(true);

        txtNewResGrpName = new Text(container, SWT.BORDER);
        txtNewResGrpName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
        txtNewResGrpName.setEnabled(true);

        decoratorResGrpName = new ControlDecoration(txtNewResGrpName, SWT.CENTER);
        decoratorResGrpName.setDescriptionText(MessageHandler.getResourceString(resourceBundle, RES_GRP_NAME_RULE));
        if (fieldDecoration != null) {
            Image image = fieldDecoration.getImage();
            decoratorResGrpName.setImage(image);
        }

        rdoUseExisting = new Button(container, SWT.RADIO);
        rdoUseExisting.setText(MessageHandler.getResourceString(resourceBundle, RADIOBUTTON_USE_EXIST_GRP));
        rdoUseExisting.setSelection(false);

        cbUseExisting = new Combo(container, SWT.READ_ONLY);
        cbUseExisting.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
        cbUseExisting.add(MessageHandler.getResourceString(resourceBundle, LOADING));
        cbUseExisting.select(0);
        cbUseExisting.setEnabled(false);

        Label lblLocation = new Label(container, SWT.NONE);
        lblLocation.setText(MessageHandler.getResourceString(resourceBundle, LABEL_LOCTION));

        cbLocations = new Combo(container, SWT.READ_ONLY);
        cbLocations.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));
        cbLocations.add(MessageHandler.getResourceString(resourceBundle, LOADING));
        cbLocations.select(0);
        cbLocations.setEnabled(false);

        Label lblPricingTier = new Label(container, SWT.READ_ONLY);
        lblPricingTier.setText(MessageHandler.getResourceString(resourceBundle, LABEL_PRICING));

        cbPricetiers = new Combo(container, SWT.READ_ONLY);
        cbPricetiers.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

        if (skus.keySet().size() > 0) {
            for (String price : skus.keySet()) {
                cbPricetiers.add(price);
            }
            cbPricetiers.select(0);
            selectedPriceTierValue = cbPricetiers.getText();
        }

        Link lnkPrice = new Link(container, SWT.NONE);
        lnkPrice.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1));
        lnkPrice.setText(MessageHandler.getResourceString(resourceBundle, LINK_PRICE));
        lnkPrice.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                try {
                    PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(event.text));
                } catch (Exception ex) {
                    LOG.log(MessageHandler.getCommonStr(OPEN_BROWSER_ERROR), ex);
                }
            }
        });

        chkUnblockPort = new Button(container, SWT.CHECK);
        chkUnblockPort.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 4, 1));
        chkUnblockPort.setText(MessageHandler.getResourceString(resourceBundle, CHECKBOX_SSL));

        this.setHelpAvailable(false);

        txtDnsName.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent arg0) {
                dnsNameValue = txtDnsName.getText();
                validateFields();
            }
        });

        for (SubscriptionDetail sub : selectedSubscriptions) {
            cbSubs.add(String.format(SUBS_COMBO_ITEMS_FORMAT, sub.getSubscriptionName(), sub.getSubscriptionId()));
        }

        cbSubs.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                currentSub = selectedSubscriptions.get(cbSubs.getSelectionIndex());
                if (loaded) {
                    fillLocationsAndResourceGrps(currentSub);
                }
                validateFields();
            }
        });
        if (selectedSubscriptions.size() > 0) {
            cbSubs.select(0);
        }

        rdoCreateNew.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                txtNewResGrpName.setEnabled(true);
                cbUseExisting.setEnabled(false);
                newResGrp = true;
                selectedResGrpValue = txtNewResGrpName.getText();
                validateFields();
            }
        });

        rdoUseExisting.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                txtNewResGrpName.setEnabled(false);
                cbUseExisting.setEnabled(true);
                if (loaded) {
                    newResGrp = false;
                    selectedResGrpValue = sortedGroups.get(cbUseExisting.getSelectionIndex());
                    validateFields();
                }

            }
        });

        txtNewResGrpName.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent arg0) {
                selectedResGrpValue = txtNewResGrpName.getText();
                validateFields();
            }
        });

        cbUseExisting.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                selectedResGrpValue = cbUseExisting.getText();
                validateFields();
            }
        });

        cbLocations.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                selectedLocationValue = sortedLocations.get(cbLocations.getSelectionIndex()).name();
                validateFields();
            }
        });

        cbPricetiers.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                selectedPriceTierValue = cbPricetiers.getText();
                validateFields();
            }
        });

        chkUnblockPort.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                Button btn = (Button) e.getSource();
                if (btn.getSelection()) {
                    noSSLPort = true;
                } else {
                    noSSLPort = false;
                }
            }
        });

        DefaultLoader.getIdeHelper().runInBackground(null,
                MessageHandler.getResourceString(resourceBundle, LOADING_LOCATION_AND_GRPS), false, true,
                MessageHandler.getResourceString(resourceBundle, LOADING_LOCATION_AND_GRPS), new Runnable() {
                    @Override
                    public void run() {
                        try {
                            AzureModelController.updateSubscriptionMaps(null);
                            DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    fillLocationsAndResourceGrps(currentSub);
                                    cbLocations.setEnabled(true);
                                    loaded = true;
                                    validateFields();
                                }
                            });
                        } catch (Exception ex) {
                            LOG.log(MessageHandler.getCommonStr(LOAD_LOCATION_AND_RESOURCE_ERROR), ex);
                        }
                    }
                });

        return area;
    }

    /**
     * Create contents of the button bar.
     *
     * @param parent
     */
    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, false);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
        btnOK = getButton(IDialogConstants.OK_ID);
        btnOK.setEnabled(false);
    }

    /**
     * Return the initial size of the dialog.
     */
    @Override
    protected Point getInitialSize() {
        this.getShell().layout();
        return this.getShell().computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    @Override
    protected void okPressed() {
        try {
            RedisCacheUtil.doValidate(currentSub, dnsNameValue, selectedLocationValue, selectedResGrpValue,
                    selectedPriceTierValue);
            if (newResGrp) {
                for (String resGrp : sortedGroups) {
                    if (selectedResGrpValue.equals(resGrp)) {
                        throw new InvalidFormDataException(
                                "The resource group " + selectedResGrpValue + " is not available");
                    }
                }
            }
        } catch (InvalidFormDataException e) {
            MessageDialog.openError(getShell(), "Form Validation Error", e.getMessage());
            return;
        }
        final Operation operation = TelemetryManager.createOperation(REDIS, CREATE_REDIS);
        try {
            operation.start();
            Azure azure = azureManager.getAzure(currentSub.getSubscriptionId());
            ProcessingStrategy processor = RedisCacheUtil.doGetProcessor(azure, skus, dnsNameValue,
                    selectedLocationValue, selectedResGrpValue, selectedPriceTierValue, noSSLPort, newResGrp);
            ExecutorService executor = Executors.newSingleThreadExecutor();
            ListeningExecutorService executorService = MoreExecutors.listeningDecorator(executor);

            ListenableFuture<Void> futureTask = executorService.submit(new CreateRedisCacheCallable(processor));
            final ProcessingStrategy processorInner = processor;
            Futures.addCallback(futureTask, new FutureCallback<Void>() {
                @Override
                public void onSuccess(Void arg0) {
                    if (onCreate != null) {
                        operation.complete();
                        onCreate.run();
                    }
                }

                @Override
                public void onFailure(Throwable throwable) {
                    EventUtil.logError(operation, ErrorType.userError, new Exception(throwable), null, null);
                    operation.complete();
                    MessageDialog.openError(getShell(),
                            String.format(
                                    MessageHandler.getResourceString(resourceBundle, CREATING_ERROR_INDICATOR_FORMAT),
                                    dnsNameValue),
                            throwable.getMessage());
                    try {
                        processorInner.notifyCompletion();
                    } catch (InterruptedException ex) {
                        LOG.log("Error occurred while notifyCompletion in RedisCache.", ex);
                    }
                }
            });
        } catch (Exception ex) {
            EventUtil.logError(operation, ErrorType.userError, ex, null, null);
            operation.complete();
            MessageDialog.openError(getShell(),
                    String.format(MessageHandler.getResourceString(resourceBundle, CREATING_ERROR_INDICATOR_FORMAT),
                            dnsNameValue),
                    ex.getMessage());
            LOG.log(String.format(MessageHandler.getResourceString(resourceBundle, CREATING_ERROR_INDICATOR_FORMAT),
                    dnsNameValue), ex);
        }
        super.okPressed();
    }

    private void fillLocationsAndResourceGrps(SubscriptionDetail selectedSub) {
        cbLocations.removeAll();
        List<Location> locations = AzureModel.getInstance().getSubscriptionToLocationMap().get(selectedSub);
        if (locations != null) {
            sortedLocations = locations.stream().sorted(Comparator.comparing(Location::displayName))
                    .collect(Collectors.toList());
            for (Location location : sortedLocations) {
                cbLocations.add(location.displayName());
            }
            if (sortedLocations.size() > 0) {
                cbLocations.select(0);
                selectedLocationValue = sortedLocations.get(0).name();
            }
        }
        cbUseExisting.removeAll();
        List<ResourceGroup> groups = AzureModel.getInstance().getSubscriptionToResourceGroupMap().get(selectedSub);
        if (groups != null) {
            sortedGroups = groups.stream().map(ResourceGroup::name).sorted().collect(Collectors.toList());
            for (String group : sortedGroups) {
                cbUseExisting.add(group);
            }
            if (sortedGroups.size() > 0) {
                cbUseExisting.select(0);
                if (rdoUseExisting.getSelection()) {
                    newResGrp = false;
                    selectedResGrpValue = sortedGroups.get(0);
                }
            }
        }
    }

    private void validateFields() {
        boolean dnsValid = !Utils.isEmptyString(dnsNameValue) && dnsNameValue.length() <= REDIS_CACHE_MAX_NAME_LENGTH
                && dnsNameValue.matches(DNS_NAME_REGEX);
        if (dnsValid) {
            decoratorDnsName.hide();
        } else {
            decoratorDnsName.show();
        }

        boolean resGrpValid = Utils.isResGrpNameValid(selectedResGrpValue);
        if (resGrpValid) {
            decoratorResGrpName.hide();
        } else {
            decoratorResGrpName.show();
        }

        boolean allFieldsCompleted = loaded && dnsValid && resGrpValid && !Utils.isEmptyString(selectedLocationValue)
                && !Utils.isEmptyString(selectedPriceTierValue);
        btnOK.setEnabled(allFieldsCompleted);
    }

    @Override
    public Map<String, String> toProperties() {
        final Map<String, String> properties = new HashMap<>();
        if (currentSub != null) {
            if (currentSub.getSubscriptionName() != null)
                properties.put("SubscriptionName", currentSub.getSubscriptionName());
            if (currentSub.getSubscriptionId() != null)
                properties.put("SubscriptionId", currentSub.getSubscriptionId());
        }
        return properties;
    }

    public void setOnCreate(Runnable onCreate) {
        this.onCreate = onCreate;
    }

    class CreateRedisCacheCallable implements Callable<Void> {
        private ProcessingStrategy processor;

        public CreateRedisCacheCallable(ProcessingStrategy processor) {
            this.processor = processor;
        }

        @Override
        public Void call() throws Exception {
            DefaultLoader.getIdeHelper().runInBackground(null,
                    String.format(MessageHandler.getResourceString(resourceBundle, CREATING_INDICATOR_FORMAT),
                            ((ProcessorBase) processor).DNSName()),
                    false, true,
                    String.format(MessageHandler.getResourceString(resourceBundle, CREATING_INDICATOR_FORMAT),
                            ((ProcessorBase) processor).DNSName()),
                    new Runnable() {
                        @Override
                        public void run() {
                            try {
                                processor.waitForCompletion("PRODUCE");
                            } catch (InterruptedException ex) {
                                LOG.log(String.format(MessageHandler.getResourceString(resourceBundle,
                                        CREATING_ERROR_INDICATOR_FORMAT), dnsNameValue), ex);
                            }
                        }
                    });
            // consume
            processor.process().notifyCompletion();
            return null;
        }
    }
}
