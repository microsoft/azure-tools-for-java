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
package com.microsoft.azuretools.azureexplorer.forms.createrediscache;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.azurecommons.helpers.RedisCacheUtil;
import com.microsoft.azuretools.azurecommons.rediscacheprocessors.ProcessingStrategy;
import com.microsoft.azuretools.azurecommons.rediscacheprocessors.ProcessorBase;
import com.microsoft.azuretools.core.Activator;
import com.microsoft.azuretools.azureexplorer.forms.FormUtils;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.azuretools.utils.AzureModel;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.resources.Location;
import com.microsoft.azure.management.resources.ResourceGroup;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import javax.swing.JOptionPane;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class CreateRedisCacheForm extends TitleAreaDialog {

    private static Activator LOG = Activator.getDefault();
    protected final AzureManager azureManager;
    protected List<SubscriptionDetail> allSubs;
    private List<SubscriptionDetail> selectedSubscriptions;
    private List<Location> sortedLocations;
    private List<String> sortedGroups;
    protected Set<String> allResGrpsofCurrentSub;
    private SubscriptionDetail currentSub;
    private boolean noSSLPort = false;
    private boolean newResGrp = true;
    private final LinkedHashMap<String, String> skus;

    private String dnsNameValue;
    private String selectedLocationValue;
    private String selectedResGrpValue;
    private String selectedPriceTierValue;

    private Combo cbSubs;
    private Combo cbUseExisting;
    private ComboViewer cbvUseExisting;
    private Combo cbLocations;
    private ComboViewer cbvLocations;
    private Combo cbPricetiers;

    private Label lblDnsSuffix;

    private Button chkUnblockPort;
    private Button rdoUseExisting;
    private Button rdoCreateNew;
    private Button btnOK;

    private Text txtDnsName;
    private Text txtNewResGrpName;

    private static final Integer REDIS_CACHE_MAX_NAME_LENGTH = 63;
    private static final String DIALOG_TITLE = "New Redis Cache";
    private static final String DIALOG_MESSAGE = "Please enter Redis Cache details.";
    private static final String LABEL_DNS_NAME = "* DNS name";
    private static final String LABEL_DNS_SUFFIX = ".redis.cache.windows.net";
    private static final String LABEL_SUBSCRIPTION = "* Subscription";
    private static final String LABEL_RESOURCE_GRP = "* Resource group";
    private static final String RADIOBUTTON_USE_EXIST_GRP = "Use existing";
    private static final String RADIOBUTTON_NEW_GRP = "Create new";
    private static final String LABEL_LOCTION = "* Location";
    private static final String LABEL_PRICING = "* Pricing tier";
    private static final String CHECKBOX_SSL = "Unblock port 6379 (not SSL encrypted)";
    private static final String BUTTON_CREATE = "Create";
    private static final String SUBS_COMBO_ITEMS_FORMAT = "%s (%s)";
    private static final String DNS_NAME_REGEX = "^[A-Za-z0-9]+(-[A-Za-z0-9]+)*$";
    private static final String CREATING_INDICATOR_FORMAT = "Creating Redis Cache %s ...";
    private static final String CREATING_ERROR_INDICATOR_FORMAT = "Error occurred when creating Redis Cache: %s";
    private static final String DECORACTOR_DNS = "Invalid Redis Cache name. The name can only contain letters, numbers and hyphens. The first and last characters must each be a letter or a number. Consecutive hyphens are not allowed.";

    /**
     * Create the dialog.
     * 
     * @param parentShell
     * @throws IOException
     */
    public CreateRedisCacheForm(Shell parentShell) throws IOException {
        super(parentShell);
        azureManager = AuthMethodManager.getInstance().getAzureManager();
        allSubs = azureManager.getSubscriptionManager().getSubscriptionDetails();
        selectedSubscriptions = allSubs.stream().filter(SubscriptionDetail::isSelected).collect(Collectors.toList());
        if (selectedSubscriptions.size() > 0) {
        	Map<SubscriptionDetail, List<Location>> subscription2Location = AzureModel.getInstance().getSubscriptionToLocationMap();
        	currentSub = selectedSubscriptions.get(0);
        	if (subscription2Location == null || subscription2Location.get(currentSub) == null) {
                FormUtils.loadLocationsAndResourceGrps(parentShell);
            }
        }
        allResGrpsofCurrentSub = new HashSet<String>();
        skus = RedisCacheUtil.initSkus();
    }

    /**
     * Create contents of the dialog.
     * 
     * @param parent
     */
    @Override
    protected Control createDialogArea(Composite parent) {
        setTitle(DIALOG_TITLE);
        setMessage(DIALOG_MESSAGE);
        
        Composite container = new Composite(parent, SWT.FILL);
        container.setLayoutData(new GridData(GridData.FILL_BOTH));
        
        // Dns name
        Label lblRequireDnsName = new Label(container, SWT.NONE);
        lblRequireDnsName.setBounds(10, 0, 90, 20);
        lblRequireDnsName.setText(LABEL_DNS_NAME);
        
        txtDnsName = new Text(container, SWT.BORDER);
        txtDnsName.addModifyListener(new ModifyListener() {
        	ControlDecoration decorator;
        	{
                decorator = new ControlDecoration(txtDnsName, SWT.CENTER);
                decorator.setDescriptionText(DECORACTOR_DNS);
                Image image = FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage();
                decorator.setImage(image);
            }
			@Override
			public void modifyText(ModifyEvent arg0) {
				dnsNameValue = txtDnsName.getText();
				if (dnsNameValue.length() > REDIS_CACHE_MAX_NAME_LENGTH || !dnsNameValue.matches(DNS_NAME_REGEX)) {
					decorator.show();
		        } else {
		        	decorator.hide();
		        }
				validateFields();
			}
        });
        txtDnsName.setBounds(10, 23, 469, 26);
        
        lblDnsSuffix = new Label(container, SWT.NONE);
        lblDnsSuffix.setBounds(311, 55, 168, 20);
        lblDnsSuffix.setText(LABEL_DNS_SUFFIX);

        //Subscription
        Label lblRequireSubs = new Label(container, SWT.NONE);
        lblRequireSubs.setText(LABEL_SUBSCRIPTION);
        lblRequireSubs.setBounds(10, 81, 90, 20);
        
        cbSubs = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
        for (SubscriptionDetail sub : selectedSubscriptions) {
            cbSubs.add(String.format(SUBS_COMBO_ITEMS_FORMAT, sub.getSubscriptionName(), sub.getSubscriptionId()));
        }
        
        cbSubs.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                currentSub = selectedSubscriptions.get(cbSubs.getSelectionIndex());
                fillLocationsAndResourceGrps(currentSub);
                validateFields();
            }
        });
        if (selectedSubscriptions.size() > 0) {
        	cbSubs.select(0);
        }
        cbSubs.setBounds(10, 107, 469, 28);
        
        // Resource Group
        Label lblRequireResourceGrp = new Label(container, SWT.NONE);
        lblRequireResourceGrp.setText(LABEL_RESOURCE_GRP);
        lblRequireResourceGrp.setBounds(10, 151, 90, 20);
        
        rdoCreateNew = new Button(container, SWT.RADIO);
        rdoCreateNew.setSelection(true);
        rdoCreateNew.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                txtNewResGrpName.setVisible(true);
                cbUseExisting.setVisible(false);
                newResGrp = true;
                validateFields();
            }
        });
        rdoCreateNew.setBounds(10, 177, 111, 20);
        rdoCreateNew.setSelection(true);
        rdoCreateNew.setText(RADIOBUTTON_NEW_GRP);

        rdoUseExisting = new Button(container, SWT.RADIO);
        rdoUseExisting.setSelection(false);
        rdoUseExisting.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                txtNewResGrpName.setVisible(false);
                cbUseExisting.setVisible(true);
                newResGrp = false;
                validateFields();
            }
        });
        rdoUseExisting.setText(RADIOBUTTON_USE_EXIST_GRP);
        rdoUseExisting.setSelection(false);
        rdoUseExisting.setBounds(127, 177, 111, 20);
        
        txtNewResGrpName = new Text(container, SWT.BORDER);
        txtNewResGrpName.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(ModifyEvent arg0) {
				selectedResGrpValue = txtNewResGrpName.getText();
				validateFields();
			}
        });
        txtNewResGrpName.setVisible(true);
        txtNewResGrpName.setBounds(10, 203, 469, 28);
        
        cbvUseExisting = new ComboViewer(container, SWT.READ_ONLY);
        cbvUseExisting.setContentProvider(ArrayContentProvider.getInstance());
        cbUseExisting = cbvUseExisting.getCombo();
        cbUseExisting.setVisible(false);
        cbUseExisting.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
            	selectedResGrpValue = cbUseExisting.getText();
            	validateFields();
            }
        });
        cbUseExisting.setVisible(false);
        cbUseExisting.setBounds(10, 203, 469, 28);

        // Location
        Label lblRequireLocation = new Label(container, SWT.NONE);
        lblRequireLocation.setText(LABEL_LOCTION);
        lblRequireLocation.setBounds(10, 245, 90, 20);
        
        cbvLocations = new ComboViewer(container, SWT.READ_ONLY);
        cbvLocations.setContentProvider(ArrayContentProvider.getInstance());
        cbvLocations.setLabelProvider(new LabelProvider() {
            @Override
            public String getText(Object element) {
            	if (element == null) {
            		return "";
            	}
            	if (element instanceof Location) {
            		Location loc = (Location) element;
                    return loc.displayName();
            	}
            	return element.toString();
            }
        });
        cbLocations = cbvLocations.getCombo();
        cbLocations.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
            	selectedLocationValue = sortedLocations.get(cbLocations.getSelectionIndex()).name();
            	validateFields();
            }
        });
        cbLocations.setBounds(10, 271, 469, 28);
        fillLocationsAndResourceGrps(currentSub);
        
        // Price
        Label lblRequirePrice = new Label(container, SWT.NONE);
        lblRequirePrice.setText(LABEL_PRICING);
        lblRequirePrice.setBounds(10, 314, 90, 20);
        
        ComboViewer cbvPriceTiers = new ComboViewer(container, SWT.READ_ONLY);
        cbvPriceTiers.setContentProvider(ArrayContentProvider.getInstance());
        cbPricetiers = cbvPriceTiers.getCombo();
        cbPricetiers.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                selectedPriceTierValue = cbPricetiers.getText();
                validateFields();
            }
        });
        cbvPriceTiers.setInput(skus.keySet());
        cbvPriceTiers.refresh();
        if (skus.keySet().size() > 0) {
        	cbPricetiers.select(0);
        	selectedPriceTierValue = cbPricetiers.getText();
        }
        cbPricetiers.setBounds(10, 340, 469, 28);

        // SSL
        chkUnblockPort = new Button(container, SWT.CHECK);
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
        chkUnblockPort.setBounds(10, 380, 320, 20);
        chkUnblockPort.setText(CHECKBOX_SSL);

        return super.createDialogArea(parent);
    }

    class CreateRedisCacheCallable implements Callable<Void> {
        private ProcessingStrategy processor;

        public CreateRedisCacheCallable(ProcessingStrategy processor) {
            this.processor = processor;
        }

        public Void call() throws Exception {
            DefaultLoader.getIdeHelper().runInBackground(null,
            		String.format(CREATING_INDICATOR_FORMAT, ((ProcessorBase) processor).DNSName()), false, true,
            		String.format(CREATING_INDICATOR_FORMAT, ((ProcessorBase) processor).DNSName()), new Runnable() {
                        @Override
                        public void run() {
                            try {
                                processor.waitForCompletion("PRODUCE");
                            } catch (InterruptedException ex) {
                            	LOG.log("Error occurred while waitForCompletion in RedisCache.", ex);
                            }
                        }
                    });
            // consume
            processor.process().notifyCompletion();
            return null;
        }
    }

    private void fillLocationsAndResourceGrps(SubscriptionDetail selectedSub) {
    	List<Location> locations = AzureModel.getInstance().getSubscriptionToLocationMap().get(selectedSub);
    	if (locations != null) {
    		sortedLocations = locations.stream().sorted(Comparator.comparing(Location::displayName)).collect(Collectors.toList());
            cbvLocations.setInput(sortedLocations);
            cbvLocations.refresh();
            if (sortedLocations.size() > 0) {
            	cbLocations.select(0);
            	selectedLocationValue = sortedLocations.get(0).name();
            }
    	}
        
        List<ResourceGroup> groups = AzureModel.getInstance().getSubscriptionToResourceGroupMap().get(selectedSub);
        if (groups != null) {
        	sortedGroups = groups.stream().map(ResourceGroup::name).sorted().collect(Collectors.toList());
            cbvUseExisting.setInput(sortedGroups);
        	cbvUseExisting.refresh();
        	if (sortedGroups.size() > 0) {
        		cbUseExisting.select(0);
        		selectedResGrpValue = sortedGroups.get(0);
        	}
        }
    }

    @Override
    protected Control createButtonBar(Composite parent) {
		Control ctrl = super.createButtonBar(parent);
		btnOK = getButton(IDialogConstants.OK_ID);
		btnOK.setEnabled(false);
		btnOK.setText(BUTTON_CREATE);
        return ctrl;
    }
    
    @Override
    protected Point getInitialSize() {
        return new Point(495, 596);
    }

    @Override
    protected boolean isResizable() {
        return false;
    }

    @Override
    public void create() {
        super.create();
    }

    @Override
    protected void okPressed() {
        try {
            if (!RedisCacheUtil.doValidate(azureManager, currentSub, dnsNameValue, selectedLocationValue,
                    selectedResGrpValue, selectedPriceTierValue)) {
                return;
            }
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
                	//TODO: send telemetry
                }

                @Override
                public void onFailure(Throwable throwable) {
                    JOptionPane.showMessageDialog(null, throwable.getMessage(),
                    		String.format(CREATING_ERROR_INDICATOR_FORMAT, dnsNameValue), JOptionPane.ERROR_MESSAGE,
                            null);
                    try {
                        processorInner.notifyCompletion();
                    } catch (InterruptedException ex) {
                    	LOG.log("Error occurred while notifyCompletion in RedisCache.", ex);
                    }
                }
            });
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, ex.getMessage(),
            		String.format(CREATING_ERROR_INDICATOR_FORMAT, dnsNameValue), JOptionPane.ERROR_MESSAGE,
                    null);
            LOG.log(String.format(CREATING_ERROR_INDICATOR_FORMAT, dnsNameValue), ex);
        }
        super.okPressed();
    }
    
    private void validateFields() {
        boolean allFieldsCompleted = !(dnsNameValue == null || dnsNameValue.isEmpty() || dnsNameValue.length() > REDIS_CACHE_MAX_NAME_LENGTH || !dnsNameValue.matches(DNS_NAME_REGEX)
        		|| selectedLocationValue == null ||selectedLocationValue.isEmpty() || selectedResGrpValue == null 
        		|| selectedResGrpValue.isEmpty() || selectedPriceTierValue == null || selectedPriceTierValue.isEmpty());

        btnOK.setEnabled(allFieldsCompleted);
    }
}
