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
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wb.swt.SWTResourceManager;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import com.microsoft.azuretools.adauth.AuthException;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.azurecommons.helpers.RedisCacheUtil;
import com.microsoft.azuretools.azurecommons.rediscacheprocessors.ProcessingStrategy;
import com.microsoft.azuretools.azurecommons.rediscacheprocessors.ProcessorBase;
import com.microsoft.azuretools.azureexplorer.Activator;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.ParallelExecutor;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.redis.RedisCache;
import com.microsoft.azure.management.resources.ResourceGroup;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import javax.swing.JOptionPane;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class CreateRedisCacheForm extends TitleAreaDialog {

    private static ILog LOG = Activator.getDefault().getLog();
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

    private Combo comboSubs;
    private Combo useExistingCombo;
    private Combo comboLocations;
    private Combo comboPricetiers;

    private Label lblPricingTier;
    private Label lblLocation;
    private Label lblDnsName;
    private Label lblSubscription;
    private Label lblResourceGroup;
    private Label lblSuffix;

    private Button btnUnblockPort;
    private Button btnUseExisting;
    private Button btnCreateNew;

    private Text dnsName;
    private Text newResGrpName;
    /**
     * Create the dialog.
     * @param parentShell
     * @throws IOException
     */
    public CreateRedisCacheForm(Shell parentShell) throws IOException {
        super(parentShell);
        azureManager = AuthMethodManager.getInstance().getAzureManager();
        allSubs = azureManager.getSubscriptionManager().getSubscriptionDetails();
        allResGrpsofCurrentSub = new HashSet<String>();
        currentSub = null;
        skus = RedisCacheUtil.initSkus();
    }

    /**
     * Create contents of the dialog.
     * @param parent
     */
    @Override
    protected Control createDialogArea(Composite parent) {
        setTitle("New Redis Cache");
        setMessage("Please enter Redis Cache details.");
        Composite area = (Composite) super.createDialogArea(parent);
        
        Composite container = new Composite(area, SWT.NONE);
        container.setFont(SWTResourceManager.getFont("Segoe UI", 8, SWT.NORMAL));
        container.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
        container.setLayoutData(new GridData(GridData.FILL_BOTH));

        ComboViewer comboSubsViewer = new ComboViewer(container, SWT.READ_ONLY);
        comboSubsViewer.setContentProvider(ArrayContentProvider.getInstance());
        comboSubs = comboSubsViewer.getCombo();
        comboSubs.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                allSubs.forEach(s -> {
                    if (s.getSubscriptionName().equals(comboSubs.getText())) {
                        currentSub = s;
                    }
                });
                btnCreateNew.setSelection(true);
                btnUseExisting.setSelection(false);
                newResGrpName.setVisible(true);
                useExistingCombo.setVisible(false);
            }
        });
        comboSubs.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                Set<String> names = new HashSet<String>(5);
                ParallelExecutor.For(
                        allSubs,
                        // The operation to perform with each item
                        new ParallelExecutor.Operation<SubscriptionDetail>() {
                            public void perform(SubscriptionDetail subDetail) {
                                if(subDetail.isSelected()) {
                                    names.add(subDetail.getSubscriptionName());
                                }
                            };
                        });
                comboSubsViewer.setInput(names);
                comboSubsViewer.refresh();
            }
        });
        comboSubs.setBounds(10, 107, 469, 28);

        ComboViewer comboLocationsViewer = new ComboViewer(container, SWT.READ_ONLY);
        comboLocations = comboLocationsViewer.getCombo();
        comboLocations.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                for (Region r : Region.values()) {
                    //Redis unavailable in Azure China Cloud, Azure German Cloud, Azure Government Cloud
                    if (!r.name().equals(Region.CHINA_NORTH.name()) &&
                    		!r.name().equals(Region.CHINA_EAST.name()) &&
                    		!r.name().equals(Region.GERMANY_CENTRAL.name()) &&
                    		!r.name().equals(Region.GERMANY_NORTHEAST.name()) &&
                    		!r.name().equals(Region.GOV_US_VIRGINIA.name()) &&
                    		!r.name().equals(Region.GOV_US_IOWA.name())) {
                    	comboLocations.add(r.label());
                    }
                }
            }
            @Override
            public void focusLost(FocusEvent e) {
                selectedRegionValue = comboLocations.getText();
            }
        });
        comboLocations.setBounds(10, 271, 469, 28);

        ComboViewer useExistingComboViewer = new ComboViewer(container, SWT.READ_ONLY);
        useExistingComboViewer.setContentProvider(ArrayContentProvider.getInstance());
        useExistingCombo = useExistingComboViewer.getCombo();
        useExistingCombo.setVisible(false);
        useExistingCombo.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                useExistingComboViewer.setInput(allResGrpsofCurrentSub);
                useExistingComboViewer.refresh();
            }
            @Override
            public void focusLost(FocusEvent e) {
                selectedResGrpValue = useExistingCombo.getText();
            }
        });
        useExistingCombo.setBounds(10, 203, 469, 28);

        ComboViewer comboPricetiersViewer = new ComboViewer(container, SWT.READ_ONLY);
        comboPricetiersViewer.setContentProvider(ArrayContentProvider.getInstance());
        comboPricetiers = comboPricetiersViewer.getCombo();
        comboPricetiers.addSelectionListener(new SelectionAdapter() {
        	@Override
        	public void widgetSelected(SelectionEvent e) {
        		selectedPriceTierValue = comboPricetiers.getText();
        	}
        });
        comboPricetiers.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                comboPricetiersViewer.setInput(skus.keySet());
                comboPricetiersViewer.refresh();
            }
        });
        comboPricetiers.setBounds(10, 340, 469, 28);

        lblSuffix = new Label(container, SWT.NONE);
        lblSuffix.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
        lblSuffix.setFont(SWTResourceManager.getFont("Segoe UI", 8, SWT.NORMAL));
        lblSuffix.setBounds(311, 55, 168, 20);
        lblSuffix.setText(".redis.cache.windows.net");

        lblDnsName = new Label(container, SWT.NONE);
        lblDnsName.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
        lblDnsName.setFont(SWTResourceManager.getFont("Segoe UI", 8, SWT.NORMAL));
        lblDnsName.setBounds(22, 0, 70, 20);
        lblDnsName.setText("DNS name");

        lblSubscription = new Label(container, SWT.NONE);
        lblSubscription.setText("Subscription");
        lblSubscription.setFont(SWTResourceManager.getFont("Segoe UI", 8, SWT.NORMAL));
        lblSubscription.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
        lblSubscription.setBounds(22, 81, 81, 20);

        lblResourceGroup = new Label(container, SWT.NONE);
        lblResourceGroup.setText("Resource group");
        lblResourceGroup.setFont(SWTResourceManager.getFont("Segoe UI", 8, SWT.NORMAL));
        lblResourceGroup.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
        lblResourceGroup.setBounds(22, 151, 105, 20);

        lblLocation = new Label(container, SWT.NONE);
        lblLocation.setText("Location");
        lblLocation.setFont(SWTResourceManager.getFont("Segoe UI", 8, SWT.NORMAL));
        lblLocation.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
        lblLocation.setBounds(22, 245, 81, 20);

        lblPricingTier = new Label(container, SWT.NONE);
        lblPricingTier.setText("Pricing tier");
        lblPricingTier.setFont(SWTResourceManager.getFont("Segoe UI", 8, SWT.NORMAL));
        lblPricingTier.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
        lblPricingTier.setBounds(22, 314, 81, 20);


        btnCreateNew = new Button(container, SWT.RADIO);
        btnCreateNew.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
        btnCreateNew.setSelection(true);
        btnCreateNew.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                newResGrpName.setVisible(true);
                useExistingCombo.setVisible(false);
                newResGrp = true;
            }
        });
        btnCreateNew.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
        btnCreateNew.setFont(SWTResourceManager.getFont("Segoe UI", 8, SWT.NORMAL));
        btnCreateNew.setBounds(10, 177, 111, 20);
        btnCreateNew.setText("Create new");

        btnUseExisting = new Button(container, SWT.RADIO);
        btnUseExisting.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseUp(MouseEvent e) {
                doRetriveResourceGroups();
            }
        });
        btnUseExisting.setSelection(false);
        btnUseExisting.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                newResGrpName.setVisible(false);
                useExistingCombo.setVisible(true);
                newResGrp = false;
            }
        });
        btnUseExisting.setText("Use existing");
        btnUseExisting.setForeground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
        btnUseExisting.setFont(SWTResourceManager.getFont("Segoe UI", 8, SWT.NORMAL));
        btnUseExisting.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
        btnUseExisting.setBounds(127, 177, 111, 20);

        btnUnblockPort = new Button(container, SWT.CHECK);
        btnUnblockPort.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
            	Button btn = (Button) e.getSource();
            	if(btn.getSelection()) {
            		noSSLPort = true;
            	} else {
            		noSSLPort = false;
            	}
            }
        });
        btnUnblockPort.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
        btnUnblockPort.setFont(SWTResourceManager.getFont("Segoe UI", 8, SWT.NORMAL));
        btnUnblockPort.setBounds(10, 380, 320, 20);
        btnUnblockPort.setText("Unblock port 6379 (not SSL encrypted)");

        newResGrpName = new Text(container, SWT.BORDER);
        newResGrpName.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                selectedResGrpValue = newResGrpName.getText();
            }
        });
        newResGrpName.setBounds(10, 203, 469, 28);

        dnsName = new Text(container, SWT.BORDER);
        dnsName.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                dnsNameValue = dnsName.getText();
            }
        });
        dnsName.setBounds(10, 23, 469, 26);
        
        Label requireSubsLbl = new Label(container, SWT.NONE);
        requireSubsLbl.setText("* ");
        requireSubsLbl.setForeground(SWTResourceManager.getColor(SWT.COLOR_RED));
        requireSubsLbl.setFont(SWTResourceManager.getFont("Segoe UI", 8, SWT.NORMAL));
        requireSubsLbl.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
        requireSubsLbl.setBounds(10, 81, 9, 20);
        
        Label requireResourceGrpLbl = new Label(container, SWT.NONE);
        requireResourceGrpLbl.setText("* ");
        requireResourceGrpLbl.setForeground(SWTResourceManager.getColor(SWT.COLOR_RED));
        requireResourceGrpLbl.setFont(SWTResourceManager.getFont("Segoe UI", 8, SWT.NORMAL));
        requireResourceGrpLbl.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
        requireResourceGrpLbl.setBounds(10, 151, 9, 20);
        
        Label requireLocationLbl = new Label(container, SWT.NONE);
        requireLocationLbl.setText("* ");
        requireLocationLbl.setForeground(SWTResourceManager.getColor(SWT.COLOR_RED));
        requireLocationLbl.setFont(SWTResourceManager.getFont("Segoe UI", 8, SWT.NORMAL));
        requireLocationLbl.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
        requireLocationLbl.setBounds(10, 245, 9, 20);
        
        Label requirePriceLbl = new Label(container, SWT.NONE);
        requirePriceLbl.setText("* ");
        requirePriceLbl.setForeground(SWTResourceManager.getColor(SWT.COLOR_RED));
        requirePriceLbl.setFont(SWTResourceManager.getFont("Segoe UI", 8, SWT.NORMAL));
        requirePriceLbl.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
        requirePriceLbl.setBounds(10, 314, 9, 20);
        
        Label requireDnsNameLbl = new Label(container, SWT.NONE);
        requireDnsNameLbl.setFont(SWTResourceManager.getFont("Segoe UI", 8, SWT.NORMAL));
        requireDnsNameLbl.setBackground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
        requireDnsNameLbl.setForeground(SWTResourceManager.getColor(SWT.COLOR_RED));
        requireDnsNameLbl.setBounds(10, 0, 9, 20);
        requireDnsNameLbl.setText("* ");

        return area;
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
								System.out.println("processor.wait@call@MyCallable: " + ex.getMessage());
								LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "processor.wait@call@MyCallable", ex));
							}
                        }
                    });
    		// consume
    		processor.process().notifyCompletion();
			return null;
    	}
    }

    /**
     * Create contents of the button bar.
     * @param parent
     */
    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        Button button = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
        button.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
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
								System.out.println("processor.pulse@onFailure@createButtonsForButtonBar: " + ex.getMessage());
								LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "processor.pulse@onFailure@createButtonsForButtonBar", ex));
							}
            			}
            		});
                } catch (Exception ex) {
                	JOptionPane.showMessageDialog(null, ex.getMessage(), "Error occurred when creating Redis Cache: " + dnsNameValue, JOptionPane.ERROR_MESSAGE, null);
                }
            }
        });
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    private void doRetriveResourceGroups()
    {
        allResGrpsofCurrentSub.clear();
        IRunnableWithProgress op = new IRunnableWithProgress() {
            @Override
            public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                monitor.beginTask("Getting Resource Groups for Selected Subscription...", IProgressMonitor.UNKNOWN);
                try {
                    if(currentSub != null) {
                        ParallelExecutor.For(
                                azureManager.getAzure(currentSub.getSubscriptionId()).resourceGroups().list(),
                                // The operation to perform with each item
                                new ParallelExecutor.Operation<ResourceGroup>() {
                                    public void perform(ResourceGroup group) {
                                        allResGrpsofCurrentSub.add(group.name());
                                    };
                                });
                    }
                } catch (Exception ex) {
                    System.out.println("run@ProgressDialog@doRetriveResourceGroups@CreateRedisCacheForm: " + ex.getMessage());
                    LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "run@ProgressDialog@doRetriveResourceGroups@CreateRedisCacheForm", ex));
                }
            }
        };
        try {
            new ProgressMonitorDialog(this.getShell()).run(true, false, op);
        } catch (Exception ex) {
            ex.printStackTrace();
            LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "doRetriveResourceGroups@CreateRedisCacheForm", ex));
        }
    }
    /**
     * Return the initial size of the dialog.
     */
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
        super.okPressed();
    }
}
