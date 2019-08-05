package com.microsoft.azuretools.azureexplorer.forms;

import static com.microsoft.azuretools.telemetry.TelemetryConstants.CREATE_STORAGE_ACCOUNT;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.STORAGE;

import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.azuretools.telemetrywrapper.EventType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import java.net.URL;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import java.util.stream.Collectors;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
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

import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.SubscriptionManager;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.azureexplorer.Activator;
import com.microsoft.azuretools.core.components.AzureTitleAreaDialogWrapper;
import com.microsoft.azuretools.core.utils.Messages;
import com.microsoft.azuretools.core.utils.PluginUtil;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.azuretools.utils.AzureModel;
import com.microsoft.azuretools.utils.AzureModelController;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.azure.sdk.AzureSDKManager;
import com.microsoft.tooling.msservices.model.ReplicationTypes;
import com.microsoft.azure.management.resources.Location;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.management.storage.AccessTier;
import com.microsoft.azure.management.storage.Kind;
import com.microsoft.azure.management.storage.SkuTier;

public class CreateArmStorageAccountForm extends AzureTitleAreaDialogWrapper {
    private static final String PRICING_LINK = "<a href=\"http://go.microsoft.com/fwlink/?LinkID=400838\">Read more about replication services and pricing details</a>";
    private static Map<String, Kind> ACCOUNT_KIND = new TreeMap<>();
    static {
    	ACCOUNT_KIND.put("General purpose v1", Kind.STORAGE);
    	ACCOUNT_KIND.put("General purpose v2", Kind.STORAGE_V2);
    	ACCOUNT_KIND.put("Blob storage", Kind.BLOB_STORAGE);
    }
    
    private Button buttonOK;
    private Button buttonCancel;

    private Label subscriptionLabel;
    private Combo subscriptionComboBox;
    private Label nameLabel;
    private Text nameTextField;
    private Label resourceGroupLabel;
    private Button createNewRadioButton;
    private Button useExistingRadioButton;
    private Text resourceGrpField;
    private Combo resourceGrpCombo;
    private Label regionLabel;
    private Combo regionComboBox;
    private Label kindLabel;
    private Combo kindCombo;
    private Label performanceLabel;
    private Combo performanceCombo;
    private Label replicationLabel;
    private Combo replicationComboBox;
    private Label accessTierLabel;
    private Combo accessTierComboBox;
    private Link pricingLabel;

    private ComboViewer resourceGroupViewer;

    private Runnable onCreate;
    private SubscriptionDetail subscription;
    private Location region;
    private com.microsoft.tooling.msservices.model.storage.StorageAccount newStorageAccount;

    public CreateArmStorageAccountForm(Shell parentShell, SubscriptionDetail subscription, Location region) {
        super(parentShell);
        this.subscription = subscription;
        this.region = region;
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText("Create Storage Account");
        Image image = PluginUtil.getImage(Messages.strAccDlgImg);
        if (image != null) {
        	setTitleImage(image);
        }
    }

    @Override
    protected Control createButtonBar(Composite parent) {
		Control ctrl = super.createButtonBar(parent);
		buttonOK = getButton(IDialogConstants.OK_ID);
        buttonOK.setEnabled(false);
        buttonOK.setText("Create");
        buttonCancel = getButton(IDialogConstants.CANCEL_ID);
        buttonCancel.setText("Close");
        return ctrl;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
    	setTitle("Create New Storage Account");
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, "com.microsoft.azuretools.azureexplorer.storage_account_dialog");
        
		Composite container = new Composite(parent, SWT.FILL);
        GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		gridLayout.marginBottom = 10;
        container.setLayout(gridLayout);
        GridData gridData = new GridData();
		gridData.grabExcessHorizontalSpace = true;
		gridData.horizontalAlignment = SWT.FILL;
//		gridData.widthHint = 250;
        container.setLayoutData(gridData);

        nameLabel = new Label(container, SWT.LEFT);
        nameLabel.setText("Name:");
        nameTextField = new Text(container, SWT.LEFT | SWT.BORDER);
//        gridData = new GridData(SWT.FILL, SWT.CENTER, true, true);
        nameTextField.setLayoutData(gridDataForText(180));
        subscriptionLabel = new Label(container, SWT.LEFT);
        subscriptionLabel.setText("Subscription:");
        subscriptionComboBox = new Combo(container, SWT.READ_ONLY);
        subscriptionComboBox.setLayoutData(gridDataForText(180));

        resourceGroupLabel = new Label(container, SWT.LEFT);
        resourceGroupLabel.setText("Resource group:");
        gridData = new GridData();
        gridData.verticalAlignment = SWT.TOP;
        resourceGroupLabel.setLayoutData(gridData);
        
        final Composite composite = new Composite(container, SWT.NONE);
        gridLayout = new GridLayout();
        gridLayout.numColumns = 2;
        gridData = new GridData();
        gridData.horizontalAlignment = SWT.FILL;
        gridData.verticalAlignment = GridData.BEGINNING;
        gridData.grabExcessHorizontalSpace = true;
//        gridData.widthHint = 250;
        composite.setLayout(gridLayout);
        composite.setLayoutData(gridData);
        
        createNewRadioButton = new Button(composite, SWT.RADIO);
        createNewRadioButton.setText("Create new");
        createNewRadioButton.setSelection(true);
        resourceGrpField = new Text(composite, SWT.LEFT | SWT.BORDER);
        gridData = new GridData(SWT.FILL, SWT.CENTER, true, true);
        resourceGrpField.setLayoutData(gridData);
        
        useExistingRadioButton = new Button(composite, SWT.RADIO);
        useExistingRadioButton.setText("Use existing");
        resourceGrpCombo = new Combo(composite, SWT.READ_ONLY);
        gridData = new GridData(SWT.FILL, SWT.CENTER, true, true);
        resourceGrpCombo.setLayoutData(gridData);
        resourceGroupViewer = new ComboViewer(resourceGrpCombo);
        resourceGroupViewer.setContentProvider(ArrayContentProvider.getInstance());
        
        SelectionListener updateListener = new SelectionAdapter() {
        	@Override
			public void widgetSelected(SelectionEvent arg0) {
        		 updateResourceGroup();
			}
		};
        createNewRadioButton.addSelectionListener(updateListener);
        useExistingRadioButton.addSelectionListener(updateListener);	
   
        updateResourceGroup();
        
        regionLabel = new Label(container, SWT.LEFT);
        regionLabel.setText("Region:");
        regionComboBox = new Combo(container, SWT.READ_ONLY);
        regionComboBox.setLayoutData(gridDataForText(180));
        
        kindLabel = new Label(container, SWT.LEFT);
        kindLabel.setText("Account kind:");
        kindCombo = new Combo(container, SWT.READ_ONLY);
        kindCombo.setLayoutData(gridDataForText(180));
        
        performanceLabel = new Label(container, SWT.LEFT);
        performanceLabel.setText("Performance:");
        performanceCombo = new Combo(container, SWT.READ_ONLY);
        performanceCombo.setLayoutData(gridDataForText(180));
        
        replicationLabel = new Label(container, SWT.LEFT);
        replicationLabel.setText("Replication:");
        replicationComboBox = new Combo(container, SWT.READ_ONLY);
        replicationComboBox.setLayoutData(gridDataForText(180));
        
        if (subscription == null) { // not showing access tier with general purpose storage account which is used when creating vm
        	accessTierLabel = new Label(container, SWT.LEFT);
        	accessTierLabel.setText("Access Tier:");
        	accessTierComboBox = new Combo(container, SWT.READ_ONLY);
        	accessTierComboBox.setLayoutData(gridDataForText(180));        
        	for (AccessTier type : AccessTier.values()) {
        		accessTierComboBox.add(type.toString());
        		accessTierComboBox.setData(type.toString(), type);
        	}
        	accessTierComboBox.select(0);
        }

        pricingLabel = new Link(container, SWT.LEFT);
        pricingLabel.setText(PRICING_LINK);
        gridData = new GridData();
        gridData.horizontalSpan = 2;
        pricingLabel.setLayoutData(gridData);
        pricingLabel.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                try {
                    PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(event.text));
                } catch (Exception ex) {
					/*
					 * only logging the error in log file
					 * not showing anything to end user
					 */
                    Activator.getDefault().log("Error occurred while opening link in default browser.", ex);
                }
            }
        });

        nameTextField.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent modifyEvent) {
                validateEmptyFields();
            }
        });

        regionComboBox.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                validateEmptyFields();
            }
        });
        
        resourceGrpField.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent modifyEvent) {
                validateEmptyFields();
            }
        });
        
        resourceGrpCombo.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                validateEmptyFields();
            }
        });
        
        fillFields();

        return super.createDialogArea(parent);
    }
    
	private GridData gridDataForText(int width) {
		GridData gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.widthHint = width;
		gridData.verticalIndent = 10;
		gridData.grabExcessHorizontalSpace = true;
		return gridData;
	}
    
    private void updateResourceGroup() {
    	final boolean isNewGroup = createNewRadioButton.getSelection();
        resourceGrpField.setEnabled(isNewGroup);
        resourceGrpCombo.setEnabled(!isNewGroup);
	}

    private void validateEmptyFields() {
        boolean allFieldsCompleted = !(nameTextField.getText().isEmpty() || regionComboBox.getText().isEmpty()
        		|| (createNewRadioButton.getSelection() && resourceGrpField.getText().trim().isEmpty())
                || (useExistingRadioButton.getSelection() && resourceGrpCombo.getText().isEmpty()));

        buttonOK.setEnabled(allFieldsCompleted);
    }

    @Override
    protected void okPressed() {
		if (nameTextField.getText().length() < 3 || nameTextField.getText().length() > 24
				|| !nameTextField.getText().matches("[a-z0-9]+")) {
			DefaultLoader.getUIHelper()
					.showError("Invalid storage account name. The name should be between 3 and 24 characters long and "
							+ "can contain only lowercase letters and numbers.", "Azure Explorer");
			return;
		}
		final boolean isNewResourceGroup = createNewRadioButton.getSelection();
		final String resourceGroupName = isNewResourceGroup ? resourceGrpField.getText() : resourceGrpCombo.getText();
		String replication = replicationComboBox.getData(replicationComboBox.getText()).toString();
		String region = ((Location) regionComboBox.getData(regionComboBox.getText())).name();
		Kind kind = (Kind) kindCombo.getData(kindCombo.getText());
		if (subscription == null) {
			String name = nameTextField.getText();
			AccessTier accessTier = (AccessTier) accessTierComboBox.getData(accessTierComboBox.getText());
			SubscriptionDetail subscriptionDetail = (SubscriptionDetail) subscriptionComboBox.
                getData(subscriptionComboBox.getText());
			setSubscription(subscriptionDetail);
			DefaultLoader.getIdeHelper().runInBackground(null, "Creating storage account", false, true,
					"Creating storage account " + name + "...", new Runnable() {
						@Override
						public void run() {
                            EventUtil.executeWithLog(STORAGE, CREATE_STORAGE_ACCOUNT, (operation) -> {
                                AzureSDKManager
                                    .createStorageAccount(subscriptionDetail.getSubscriptionId(), name, region,
                                        isNewResourceGroup, resourceGroupName, kind, accessTier, false, replication);
                                // update resource groups cache if new resource group was created when creating
                                // storage account
                                if (isNewResourceGroup) {
                                    AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
                                    if (azureManager != null) {
                                        ResourceGroup rg = azureManager.getAzure(subscriptionDetail.getSubscriptionId())
                                            .resourceGroups().getByName(resourceGroupName);
                                        AzureModelController.addNewResourceGroup(subscriptionDetail, rg);
                                    }
                                }

                                if (onCreate != null) {
                                    onCreate.run();
                                }
                            }, (e) ->
                                DefaultLoader.getIdeHelper().invokeLater(() ->
                                    PluginUtil.displayErrorDialog(PluginUtil.getParentShell(), Messages.err,
                                        "An error occurred while creating the storage account: " + e.getMessage())
                                )
                            );
						}
					});
		} else {
            EventUtil.executeWithLog(STORAGE, CREATE_STORAGE_ACCOUNT, (operation) -> {
                //creating from 'create vm'
                newStorageAccount =
                    new com.microsoft.tooling.msservices.model.storage.StorageAccount(nameTextField.getText(),
                        subscription.getSubscriptionId());
                newStorageAccount.setResourceGroupName(resourceGroupName);
                newStorageAccount.setNewResourceGroup(isNewResourceGroup);
                newStorageAccount.setType(replication);
                newStorageAccount.setLocation(region);
                newStorageAccount.setKind(kind);

                if (onCreate != null) {
                    onCreate.run();
                }
            });

		}
		super.okPressed();
    }

    public void fillFields() {
        if (subscription == null) {
            try {
                subscriptionComboBox.setEnabled(true);
                AzureManager azureManager = AuthMethodManager.getInstance().getAzureManager();
                // not signed in
                if (azureManager == null) {
                    return;
                }
                SubscriptionManager subscriptionManager = azureManager.getSubscriptionManager();
                List<SubscriptionDetail> subscriptionDetails = subscriptionManager.getSubscriptionDetails();
                for (SubscriptionDetail sub : subscriptionDetails) {
                	if (sub.isSelected()) {
                		subscriptionComboBox.add(sub.getSubscriptionName());
                		subscriptionComboBox.setData(sub.getSubscriptionName(), sub);
                	}
                }
                subscriptionComboBox.addSelectionListener(new SelectionAdapter() {
                    public void widgetSelected(SelectionEvent e) {
                        loadRegionsAndGroups();
                    }
                });

                if (subscriptionDetails.size() > 0) {
                    subscriptionComboBox.select(0);
                    loadRegionsAndGroups();
                }
            } catch (Exception e) {
            	PluginUtil.displayErrorDialogWithAzureMsg(PluginUtil.getParentShell(), Messages.err,
            			"An error occurred while loading subscriptions.", e);
            }
        	for (Map.Entry<String, Kind> entry : ACCOUNT_KIND.entrySet()) {
            	kindCombo.add(entry.getKey());
            	kindCombo.setData(entry.getKey(), entry.getValue());
            }
        	kindCombo.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					fillPerformanceComboBox();
                    fillReplicationTypes();
     
                    showAccessTier();					
				}
			});
        	kindCombo.select(1);
        	showAccessTier();
        } else { // create form create VM form
            subscriptionComboBox.setEnabled(false);
            subscriptionComboBox.add(subscription.getSubscriptionName());
            subscriptionComboBox.setData(subscription.getSubscriptionName(), subscription);
            subscriptionComboBox.select(0);
            kindCombo.add("General purpose"); // only General purpose accounts supported for VMs
            kindCombo.setData(Kind.STORAGE);
            kindCombo.setEnabled(false);
            kindCombo.select(0);
            
            regionComboBox.add(region.displayName());
            regionComboBox.setData(region.displayName(), region);
            regionComboBox.setEnabled(false);
            regionComboBox.select(0);
            loadGroups();
            //loadRegions();
        }
        fillPerformanceComboBox();
    	//performanceCombo.select(0);
    	performanceCombo.addSelectionListener(new SelectionAdapter() {
    		public void widgetSelected(SelectionEvent e) {
    			fillReplicationTypes();
            }
		});
    	fillReplicationTypes();
    }
    
    private void fillPerformanceComboBox() {
    	performanceCombo.removeAll();
    	if ((Kind)kindCombo.getData(kindCombo.getText()) == Kind.BLOB_STORAGE) {
    		performanceCombo.add(SkuTier.STANDARD.toString());
    	} else {
    		for (SkuTier skuTier : SkuTier.values()) {
        		performanceCombo.add(skuTier.toString());
        	}
    	}
    	performanceCombo.select(0);
    }
    
    private void fillReplicationTypes() {
    	replicationComboBox.removeAll();
    	if (performanceCombo.getText().equals(SkuTier.STANDARD.toString())) {
    		// Create storage account from Azure Explorer
    		if (regionComboBox.getEnabled()) {
    			if ((Kind)kindCombo.getData(kindCombo.getText()) != Kind.BLOB_STORAGE) {
	    			for (ReplicationTypes replicationType : new ReplicationTypes[] {ReplicationTypes.Standard_ZRS, ReplicationTypes.Standard_LRS, ReplicationTypes.Standard_GRS, ReplicationTypes.Standard_RAGRS}) {
	                    replicationComboBox.add(replicationType.getDescription());
	                    replicationComboBox.setData(replicationType.getDescription(), replicationType);
	    			}
                    replicationComboBox.select(3);
    			} else {
    				for (ReplicationTypes replicationType : new ReplicationTypes[] {ReplicationTypes.Standard_LRS, ReplicationTypes.Standard_GRS, ReplicationTypes.Standard_RAGRS}) {
	                    replicationComboBox.add(replicationType.getDescription());
	                    replicationComboBox.setData(replicationType.getDescription(), replicationType);
	    			}
                    replicationComboBox.select(2);
    			}
    		} else {
        		// Create storage account from VM creation
    			for (ReplicationTypes replicationType : new ReplicationTypes[] {ReplicationTypes.Standard_LRS, ReplicationTypes.Standard_GRS, ReplicationTypes.Standard_RAGRS}) {
                    replicationComboBox.add(replicationType.getDescription());
                    replicationComboBox.setData(replicationType.getDescription(), replicationType);
                }
                replicationComboBox.select(2);
    		}
    	} else {    		
    		replicationComboBox.add(ReplicationTypes.Premium_LRS.getDescription());
            replicationComboBox.setData(ReplicationTypes.Premium_LRS.getDescription(), ReplicationTypes.Premium_LRS);
            replicationComboBox.select(0);
    	}
    }

    public void setOnCreate(Runnable onCreate) {
        this.onCreate = onCreate;
    }

    public com.microsoft.tooling.msservices.model.storage.StorageAccount getStorageAccount() {
        return newStorageAccount;
    }

    public void loadRegionsAndGroups() {
    	Map<SubscriptionDetail, List<Location>> subscription2Location = AzureModel.getInstance().getSubscriptionToLocationMap();
        if (subscription2Location == null || subscription2Location.get(subscriptionComboBox.getData(subscriptionComboBox.getText())) == null) {
        	DefaultLoader.getIdeHelper().runInBackground(null, "Loading Available Locations...", true, true, "", new Runnable() {
    			@Override
    			public void run() {
                    try {
                        AzureModelController.updateSubscriptionMaps(null);
                        DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
							@Override
							public void run() {
                                fillRegions();
                                fillGroups();
                            }
                        });
                    } catch (Exception ex) {
                    	PluginUtil.displayErrorDialogWithAzureMsg(PluginUtil.getParentShell(), Messages.err, "Error loading locations", ex);
                    }
                }
            });
        } else {
            fillRegions();
            fillGroups();
        }
        
        
    }
    private void fillRegions() {
		List<Location> locations = AzureModel.getInstance().getSubscriptionToLocationMap().get(subscriptionComboBox.getData(subscriptionComboBox.getText()))
                .stream().sorted(Comparator.comparing(Location::displayName)).collect(Collectors.toList());
		for (Location location : locations) {
			regionComboBox.add(location.displayName());
			regionComboBox.setData(location.displayName(), location);
		}
        if (locations.size() > 0) {
            regionComboBox.select(0);
        }
    }
    
    public void loadGroups() {
    	resourceGrpCombo.add("<Loading...>");
    	Map<SubscriptionDetail, List<ResourceGroup>> subscription2Group = AzureModel.getInstance().getSubscriptionToResourceGroupMap();
    	if (subscription2Group == null || subscription2Group.get(subscriptionComboBox.getData(subscriptionComboBox.getText())) == null) {
        	DefaultLoader.getIdeHelper().runInBackground(null, "Loading Resource Groups", true, true, "", new Runnable() {
    			@Override
    			public void run() {
                    try {
                        AzureModelController.updateSubscriptionMaps(null);
                        DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
							@Override
							public void run() {
                                fillGroups();
                            }
                        });
                    } catch (Exception ex) {
                    	PluginUtil.displayErrorDialogWithAzureMsg(PluginUtil.getParentShell(), Messages.err, "Error loading resource groups", ex);
                    }
                }
            });
        } else {
            fillGroups();
        }
    }
    
    public void fillGroups() {
    	List<ResourceGroup> resourceGroups = AzureModel.getInstance().getSubscriptionToResourceGroupMap().get(subscriptionComboBox.getData(subscriptionComboBox.getText()));
        List<String> sortedGroups = resourceGroups.stream().map(ResourceGroup::name).sorted().collect(Collectors.toList());
		DefaultLoader.getIdeHelper().invokeLater(new Runnable() {
			@Override
			public void run() {
				final Vector<Object> vector = new Vector<Object>();
				vector.addAll(sortedGroups);
				resourceGroupViewer.setInput(vector);
				if (sortedGroups.size() > 0) {
					resourceGrpCombo.select(0);
				}
			}
		});
    }

	private void showAccessTier() {
		boolean isBlobKind = (Kind)kindCombo.getData(kindCombo.getText()) == Kind.BLOB_STORAGE;
		accessTierComboBox.setVisible(isBlobKind);
		accessTierLabel.setVisible(isBlobKind);
	}
	
	public SubscriptionDetail getSubscription() {
		return subscription;
	}
	
	public void setSubscription(SubscriptionDetail subscription) {
		this.subscription = subscription;
	}
	
	@Override
	public Map<String, String> toProperties() {
		final Map<String, String> properties = new HashMap<>();

        if (this.getSubscription() != null) {
            if(this.getSubscription().getSubscriptionName() != null)  properties.put("SubscriptionName", this.getSubscription().getSubscriptionName());
            if(this.getSubscription().getSubscriptionId() != null)  properties.put("SubscriptionId", this.getSubscription().getSubscriptionId());
        }

        return properties;
	}
}