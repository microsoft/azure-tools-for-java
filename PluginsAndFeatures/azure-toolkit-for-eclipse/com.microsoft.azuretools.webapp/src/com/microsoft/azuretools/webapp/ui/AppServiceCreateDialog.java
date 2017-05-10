/*
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
package com.microsoft.azuretools.webapp.ui;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.PricingTier;
import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.resources.Location;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.core.components.AzureTitleAreaDialogWrapper;
import com.microsoft.azuretools.core.ui.ErrorWindow;
import com.microsoft.azuretools.core.utils.PluginUtil;
import com.microsoft.azuretools.core.utils.ProgressDialog;
import com.microsoft.azuretools.core.utils.UpdateProgressIndicator;
import com.microsoft.azuretools.utils.AzulZuluModel;
import com.microsoft.azuretools.utils.AzureModel;
import com.microsoft.azuretools.utils.AzureModelController;
import com.microsoft.azuretools.utils.StorageAccoutUtils;
import com.microsoft.azuretools.utils.WebAppUtils;
import com.microsoft.azuretools.webapp.Activator;

public class AppServiceCreateDialog extends AzureTitleAreaDialogWrapper {
    private static ILog LOG = Activator.getDefault().getLog();

    private Text textAppName;
    private Text textResourceGroupName;
    private Text textAppSevicePlanName;
    private Text textJdkOwnUrl;
    private Text textJdkOwnStorageAccountKey;
    private Combo comboWebContainer;
    private Combo comboSubscription;
    private Combo comboResourceGroup;
    private Combo comboAppServicePlan;
    private Combo comboAppServicePlanLocation;
    private Combo comboAppServicePlanPricingTier;
    private Combo comboJdk3Party;
    private Label lblAppSevicePlanLocation;
    private Label lblAppServicePlanPricingTier;
    
    private ControlDecoration dec_textAppName;
    private ControlDecoration dec_textNewResGrName;
    private ControlDecoration dec_textAppSevicePlanName;
    private ControlDecoration dec_textJdkOwnUrl;
    private ControlDecoration dec_comboWebContainer;
    private ControlDecoration dec_comboSubscription;
    private ControlDecoration dec_comboSelectResGr;
    private ControlDecoration dec_comboAppServicePlan;
    private ControlDecoration dec_comboAppServicePlanLocation;
    //private ControlDecoration dec_comboAppServicePlanPricingTier;
    private ControlDecoration dec_comboJdk3Party;

    private final static String textNotAvailable = "N/A";
    
    // controls to types bindings by index 
    private List<WebAppUtils.WebContainerMod> binderWebConteiners;
    private List<SubscriptionDetail> binderSubscriptionDetails;
    private List<ResourceGroup> binderResourceGroup;
    private List<AppServicePlan> binderAppServicePlan;
    private List<Location> binderAppServicePlanLocation;
    private List<PricingTier> binderAppServicePlanPricingTier;
    private List<AzulZuluModel> binderJdk3Party;
    
    TabFolder tabFolder;
    
    TabItem tabItemAppServicePlan;
    Composite compositeAppServicePlan;
    Button btnAppServiceCreateNew;
    Label lblAppServiceCreateNewPricingTier;    
    Label lblAppServiceCreateNewLocation;
    
    Button btnAppServiceUseExisting;
    Label lblAppServiceUseExictingLocation;
    Label lblAppServiceUseExistiogPrisingTier;
    Link linkAppServicePricing;
    
    TabItem tabItemResourceGroup;
    Composite compositeResourceGroup;
    Button btnResourceGroupCreateNew;
    Button btnResourceGroupUseExisting;
    
    TabItem tabItemJDK;
    Composite compositeJDK;
    Button btnJdkDefault;
    Label lblJdkDefaultComment;
    Button btnJdk3rdParty;
    Link linkJdkLicense;
    Button btnJdkOwnDownloadUrl;
    Label lblJdkOwnSrorageAccountKey;
    Label lblJdkOwnComment;
    
    protected WebApp webApp;

    public WebApp getWebApp() {
        return this.webApp;
    }

    public static AppServiceCreateDialog go(Shell parentShell) {
        AppServiceCreateDialog d = new AppServiceCreateDialog(parentShell);
        if (d.open() == Window.OK) {
            return d;
        }
        return null;
    }

    private AppServiceCreateDialog(Shell parentShell) {
        super(parentShell);
        setHelpAvailable(false);
        setShellStyle(SWT.CLOSE | SWT.TITLE | SWT.APPLICATION_MODAL);
    } 
    
    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        Image image = PluginUtil.getImage("icons/large/Azure.png");
        if (image != null) {
        	setTitleImage(image);
        }
    }
    
    @Override
    protected Control createDialogArea(Composite parent) {
        setMessage("Create Azure App Service");
        setTitle("Create App Service");
        Composite area = (Composite) super.createDialogArea(parent);
        
        Composite composite = new Composite(area, SWT.NONE);
        composite.setLayout(new GridLayout(1, false));
        composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
        
        Group grpAppService = new Group(composite, SWT.NONE);
        grpAppService.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
        grpAppService.setLayout(new GridLayout(3, false));
        
        Label lblAppName = new Label(grpAppService, SWT.NONE);
        lblAppName.setText("Enter name");
        
        textAppName = new Text(grpAppService, SWT.BORDER);
        textAppName.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                cleanError();
            }
        });
        textAppName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        textAppName.setMessage("<enter name>");
        dec_textAppName = decorateContorolAndRegister(textAppName);
        
        Label lblazurewebsitescom = new Label(grpAppService, SWT.NONE);
        lblazurewebsitescom.setText(".azurewebsites.net");
        
        Label lblWebContainer = new Label(grpAppService, SWT.NONE);
        lblWebContainer.setText("Web container");
        
        comboWebContainer = new Combo(grpAppService, SWT.READ_ONLY);
        comboWebContainer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
        dec_comboWebContainer = decorateContorolAndRegister(comboWebContainer);
        
        Label lblSubscription = new Label(grpAppService, SWT.NONE);
        lblSubscription.setText("Subscription");
        
        comboSubscription = new Combo(grpAppService, SWT.READ_ONLY);
        comboSubscription.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                fillResourceGroups();
                fillAppServicePlans();
                fillAppServicePlansDetails();
                fillAppServicePlanLocations();
            }
        });
        dec_comboSubscription = decorateContorolAndRegister(comboSubscription);
        comboSubscription.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
        
        // ====
        
        tabFolder = new TabFolder(composite, SWT.NONE);
        tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        
        tabItemAppServicePlan = new TabItem(tabFolder, SWT.NONE);
        tabItemAppServicePlan.setText("App service plan");
        
        compositeAppServicePlan = new Composite(tabFolder, SWT.NONE);
        tabItemAppServicePlan.setControl(compositeAppServicePlan);
        compositeAppServicePlan.setLayout(new GridLayout(2, false));
        
        btnAppServiceCreateNew = new Button(compositeAppServicePlan, SWT.RADIO);
        btnAppServiceCreateNew.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                radioAppServicePlanLogic();
            }
        });
        btnAppServiceCreateNew.setSelection(true);
        btnAppServiceCreateNew.setBounds(0, 0, 90, 16);
        btnAppServiceCreateNew.setText("Create new");
        
        textAppSevicePlanName = new Text(compositeAppServicePlan, SWT.BORDER);
        textAppSevicePlanName.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                cleanError();
            }
        });
        textAppSevicePlanName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        textAppSevicePlanName.setMessage("<enter name>");
        dec_textAppSevicePlanName = decorateContorolAndRegister(textAppSevicePlanName);
        
        lblAppServiceCreateNewLocation = new Label(compositeAppServicePlan, SWT.NONE);
        GridData gd_lblAppServiceCreateNewLocation = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_lblAppServiceCreateNewLocation.horizontalIndent = 15;
        lblAppServiceCreateNewLocation.setLayoutData(gd_lblAppServiceCreateNewLocation);
        lblAppServiceCreateNewLocation.setText("Location");
        
        comboAppServicePlanLocation = new Combo(compositeAppServicePlan, SWT.READ_ONLY);
        comboAppServicePlanLocation.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                cleanError();
            }
        });
        comboAppServicePlanLocation.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        dec_comboAppServicePlanLocation = decorateContorolAndRegister(comboAppServicePlanLocation);
        
        
        lblAppServiceCreateNewPricingTier = new Label(compositeAppServicePlan, SWT.NONE);
        GridData gd_lblAppServiceCreateNewPricingTier = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_lblAppServiceCreateNewPricingTier.horizontalIndent = 15;
        lblAppServiceCreateNewPricingTier.setLayoutData(gd_lblAppServiceCreateNewPricingTier);
        lblAppServiceCreateNewPricingTier.setText("Pricing tier");
        
        comboAppServicePlanPricingTier = new Combo(compositeAppServicePlan, SWT.READ_ONLY);
        comboAppServicePlanPricingTier.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        
        btnAppServiceUseExisting = new Button(compositeAppServicePlan, SWT.RADIO);
        btnAppServiceUseExisting.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                radioAppServicePlanLogic();
            }
        });
        btnAppServiceUseExisting.setText("Use existing");
        
        comboAppServicePlan = new Combo(compositeAppServicePlan, SWT.READ_ONLY);
        comboAppServicePlan.setEnabled(false);
        comboAppServicePlan.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        comboAppServicePlan.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                fillAppServicePlansDetails();
            }
        });
        comboAppServicePlan.setBounds(0, 0, 26, 22);
        dec_comboAppServicePlan = decorateContorolAndRegister(comboAppServicePlan);
        
        lblAppServiceUseExictingLocation = new Label(compositeAppServicePlan, SWT.NONE);
        lblAppServiceUseExictingLocation.setEnabled(false);
        GridData gd_lblAppServiceUseExictingLocation = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_lblAppServiceUseExictingLocation.horizontalIndent = 15;
        lblAppServiceUseExictingLocation.setLayoutData(gd_lblAppServiceUseExictingLocation);
        lblAppServiceUseExictingLocation.setText("Location");
        
        lblAppSevicePlanLocation = new Label(compositeAppServicePlan, SWT.NONE);
        lblAppSevicePlanLocation.setEnabled(false);
        lblAppSevicePlanLocation.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        lblAppSevicePlanLocation.setText("N/A");
        
        lblAppServiceUseExistiogPrisingTier = new Label(compositeAppServicePlan, SWT.NONE);
        lblAppServiceUseExistiogPrisingTier.setEnabled(false);
        GridData gd_lblAppServiceUseExistiogPrisingTier = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_lblAppServiceUseExistiogPrisingTier.horizontalIndent = 15;
        lblAppServiceUseExistiogPrisingTier.setLayoutData(gd_lblAppServiceUseExistiogPrisingTier);
        lblAppServiceUseExistiogPrisingTier.setText("Pricing tier");
        
        lblAppServicePlanPricingTier = new Label(compositeAppServicePlan, SWT.NONE);
        lblAppServicePlanPricingTier.setEnabled(false);
        lblAppServicePlanPricingTier.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        lblAppServicePlanPricingTier.setText("N/A");
        new Label(compositeAppServicePlan, SWT.NONE);
        //dec_comboAppServicePlanPricingTier = decorateContorolAndRegister(comboAppServicePlanPricingTier);
        
        linkAppServicePricing = new Link(compositeAppServicePlan, SWT.NONE);
        linkAppServicePricing.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        linkAppServicePricing.setText("<a>App service pricing details</a>");
        linkAppServicePricing.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                try {
                    PlatformUI.getWorkbench().getBrowserSupport().
                    getExternalBrowser().openURL(new URL("https://azure.microsoft.com/en-us/pricing/details/app-service/"));
                } catch (PartInitException | MalformedURLException ex) {
                    LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "widgetSelected@SelectionAdapter@linkAppServicePricing@AppServiceCreateDialog", ex));
                }
            }
        });
        
        tabItemResourceGroup = new TabItem(tabFolder, SWT.NONE);
        tabItemResourceGroup.setText("Resource group");
        
        compositeResourceGroup = new Composite(tabFolder, SWT.NONE);
        tabItemResourceGroup.setControl(compositeResourceGroup);
        compositeResourceGroup.setLayout(new GridLayout(2, false));
        
        btnResourceGroupCreateNew = new Button(compositeResourceGroup, SWT.RADIO);
        btnResourceGroupCreateNew.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                radioResourceGroupLogic();
            }
        });
        btnResourceGroupCreateNew.setSelection(true);
        btnResourceGroupCreateNew.setText("Create new");
        
        textResourceGroupName = new Text(compositeResourceGroup, SWT.BORDER);
        textResourceGroupName.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                cleanError();
            }
        });
        textResourceGroupName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        textResourceGroupName.setBounds(0, 0, 64, 19);
        textResourceGroupName.setMessage("<enter name>");
        dec_textNewResGrName = decorateContorolAndRegister(textResourceGroupName);
        
        btnResourceGroupUseExisting = new Button(compositeResourceGroup, SWT.RADIO);
        btnResourceGroupUseExisting.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                radioResourceGroupLogic();
            }
        });
        btnResourceGroupUseExisting.setText("Use existing");
        
        comboResourceGroup = new Combo(compositeResourceGroup, SWT.READ_ONLY);
        comboResourceGroup.setEnabled(false);
        comboResourceGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        comboResourceGroup.setBounds(0, 0, 26, 22);
        dec_comboSelectResGr = decorateContorolAndRegister(comboResourceGroup);
        
        tabItemJDK = new TabItem(tabFolder, SWT.NONE);
        tabItemJDK.setText("JDK");
        
        compositeJDK = new Composite(tabFolder, SWT.NONE);
        tabItemJDK.setControl(compositeJDK);
        compositeJDK.setLayout(new GridLayout(3, false));
        
        btnJdkDefault = new Button(compositeJDK, SWT.RADIO);
        btnJdkDefault.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                radioJdkLogic();
            }
        });
        btnJdkDefault.setSelection(true);
        btnJdkDefault.setText("Default");
        
        lblJdkDefaultComment = new Label(compositeJDK, SWT.NONE);
        lblJdkDefaultComment.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        lblJdkDefaultComment.setText("Deploy the default JDK (JDK 8)");
        new Label(compositeJDK, SWT.NONE);
        
        btnJdk3rdParty = new Button(compositeJDK, SWT.RADIO);
        btnJdk3rdParty.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                radioJdkLogic();
            }
        });
        btnJdk3rdParty.setText("3rd party");
        
        comboJdk3Party = new Combo(compositeJDK, SWT.READ_ONLY);
        comboJdk3Party.setEnabled(false);
        comboJdk3Party.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        dec_comboJdk3Party = decorateContorolAndRegister(comboJdk3Party);
        
        linkJdkLicense = new Link(compositeJDK, SWT.NONE);
        linkJdkLicense.setEnabled(false);
        linkJdkLicense.setText("<a>License</a>");
        linkJdkLicense.addSelectionListener(new SelectionAdapter() {
         @Override
         public void widgetSelected(SelectionEvent event) {
            try {
               PlatformUI.getWorkbench().getBrowserSupport().
               getExternalBrowser().openURL(new URL(AzulZuluModel.getLicenseUrl()));
            }
            catch (Exception ex) {
               LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "widgetSelected@SelectionAdapter@AppServiceCreateDialo", ex));
            }
         }
      });

        
        btnJdkOwnDownloadUrl = new Button(compositeJDK, SWT.RADIO);
        btnJdkOwnDownloadUrl.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                radioJdkLogic();
            }
        });
        btnJdkOwnDownloadUrl.setText("Download URL");
        
        textJdkOwnUrl = new Text(compositeJDK, SWT.BORDER);
        textJdkOwnUrl.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                cleanError();
            }
        });
        textJdkOwnUrl.setEnabled(false);
        textJdkOwnUrl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        textJdkOwnUrl.setMessage("<enter url>");
        dec_textJdkOwnUrl = decorateContorolAndRegister(textJdkOwnUrl);
        new Label(compositeJDK, SWT.NONE);
        
        lblJdkOwnSrorageAccountKey = new Label(compositeJDK, SWT.NONE);
        GridData gd_lblJdkOwnSrorageAccountKey = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
        gd_lblJdkOwnSrorageAccountKey.horizontalIndent = 15;
        lblJdkOwnSrorageAccountKey.setLayoutData(gd_lblJdkOwnSrorageAccountKey);
        lblJdkOwnSrorageAccountKey.setText("Storage account key");
        
        textJdkOwnStorageAccountKey = new Text(compositeJDK, SWT.BORDER);
        textJdkOwnStorageAccountKey.setEnabled(false);
        textJdkOwnStorageAccountKey.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        textJdkOwnStorageAccountKey.setMessage("<enter storage account key>");
        decorateContorolAndRegister(textJdkOwnStorageAccountKey);
        new Label(compositeJDK, SWT.NONE);
        new Label(compositeJDK, SWT.NONE);
        
        lblJdkOwnComment = new Label(compositeJDK, SWT.NONE);
        lblJdkOwnComment.setEnabled(false);
        lblJdkOwnComment.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        lblJdkOwnComment.setText("(If the URL above is a private blob)");
        new Label(compositeJDK, SWT.NONE);
        
        
        DateFormat df = new SimpleDateFormat("yyMMddHHmmss");
        String date = df.format(new Date());
        textAppName.setText("webapp-" + date);
        textAppSevicePlanName.setText("asp-" + date);
        textResourceGroupName.setText("rg-webapp-" + date);
        
        fillWebContainers();
        fillSubscriptions();
        fillResourceGroups();
        fillAppServicePlans();
        fillAppServicePlansDetails();
        fillAppServicePlanLocations();
        fillAppServicePlanPricingTiers();
        fill3PartyJdk();
        
        return area;
    }
    
    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        cleanError();
        super.createButtonsForButtonBar(parent);
        Button okButton = getButton(IDialogConstants.OK_ID);
        okButton.setText("Create");
    }
    
    private void radioAppServicePlanLogic() { 
        cleanError();
        boolean enabled = btnAppServiceCreateNew.getSelection();
        
        //btnAppServiceCreateNew.setEnabled(enabled);
        textAppSevicePlanName.setEnabled(enabled);
        
        lblAppServiceCreateNewLocation.setEnabled(enabled);
        comboAppServicePlanLocation.setEnabled(enabled);
        
        lblAppServiceCreateNewPricingTier.setEnabled(enabled);
        comboAppServicePlanPricingTier.setEnabled(enabled);
        
        //btnAppServiceUseExisting.setEnabled(!enabled);
        comboAppServicePlan.setEnabled(!enabled);
        
        lblAppServiceUseExictingLocation.setEnabled(!enabled);
        lblAppSevicePlanLocation.setEnabled(!enabled);
        
        lblAppServiceUseExistiogPrisingTier.setEnabled(!enabled);
        lblAppServicePlanPricingTier.setEnabled(!enabled);
    }

    private void radioResourceGroupLogic() {
        cleanError();
        boolean enabled = btnResourceGroupCreateNew.getSelection();
        textResourceGroupName.setEnabled(enabled);
        comboResourceGroup.setEnabled(!enabled);
    }

    private void radioJdkLogic() {
        cleanError();
        boolean enabledDefault = btnJdkDefault.getSelection();
        lblJdkDefaultComment.setEnabled(enabledDefault);

        boolean enabled3Party = btnJdk3rdParty.getSelection();
        comboJdk3Party.setEnabled(enabled3Party);
        linkJdkLicense.setEnabled(enabled3Party);

        boolean enabledOwn = btnJdkOwnDownloadUrl.getSelection();
        textJdkOwnUrl.setEnabled(enabledOwn);
        lblJdkOwnSrorageAccountKey.setEnabled(enabledOwn);
        textJdkOwnStorageAccountKey.setEnabled(enabledOwn);
        lblJdkOwnComment.setEnabled(enabledOwn);
    }

    protected void fillWebContainers() {
        try {
            comboWebContainer.removeAll();
            binderWebConteiners = new ArrayList<>();
            for (WebAppUtils.WebContainerMod wc : WebAppUtils.WebContainerMod.values()) {
                comboWebContainer.add(wc.toString());
                binderWebConteiners.add(wc);
            }

            if (comboWebContainer.getItemCount() > 0) {
                comboWebContainer.select(0);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "fillWebContainers@AppServiceCreateDialog", ex));
        }
    }
    
    protected static <T> List<T> createListFromClassFields(Class<?> c) throws IllegalAccessException {
        List<T> list = new LinkedList<T>();

        Field[] declaredFields = c.getDeclaredFields();
        for (Field field : declaredFields) {
            int modifiers = field.getModifiers();
            if (Modifier.isStatic(modifiers)
                    && Modifier.isFinal(modifiers)
                    && Modifier.isPublic(modifiers)) {
                @SuppressWarnings("unchecked")
                T value = (T)field.get(null);
                list.add(value);
            }
        }
        return list;
    }

    protected void fillSubscriptions(){
        if (AzureModel.getInstance().getResourceGroupToWebAppMap() == null) {
            updateAndFillSubscriptions();
        } else {
            doFillSubscriptions();
        }
    }
    
    private void updateAndFillSubscriptions() {
        try {
            ProgressDialog.get(this.getShell(), "Getting App Services...").run(true, true, new IRunnableWithProgress() {
                @Override
                public void run(IProgressMonitor monitor) {
                       monitor.beginTask("Updating Azure local cache...", IProgressMonitor.UNKNOWN);
                    if (monitor.isCanceled()) {
                        AzureModel.getInstance().setResourceGroupToWebAppMap(null);
                        Display.getDefault().asyncExec(new Runnable() {
                            @Override
                            public void run() {
                                AppServiceCreateDialog.super.cancelPressed();
                            }
                        });
                    }
                    
                    try {
                        AzureModelController.updateResourceGroupMaps(new UpdateProgressIndicator(monitor));
                        Display.getDefault().asyncExec(new Runnable() {
                            @Override
                            public void run() {
                                doFillSubscriptions();
                            };
                        });                    
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "run@ProgressDialog@updateAndFillSubscriptions@AppServiceCreateDialog", ex));
                    }
                }
            });
        } catch (InvocationTargetException | InterruptedException ex) {
            ex.printStackTrace();
            LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "updateAndFillSubscriptions@AppServiceCreateDialog", ex));
        }
    }
    
    private void doFillSubscriptions() {
        try {
            // reset model
            Set<SubscriptionDetail> sdl = AzureModel.getInstance().getSubscriptionToResourceGroupMap().keySet();
            if (sdl == null) {
                System.out.println("sdl is null");
                return;
            }
            
            comboSubscription.removeAll();;
            binderSubscriptionDetails = new ArrayList<SubscriptionDetail>();
            for (SubscriptionDetail sd : sdl) {
                if (!sd.isSelected()) continue;
                comboSubscription.add(sd.getSubscriptionName());
                binderSubscriptionDetails.add(sd);
            }
            if (comboSubscription.getItemCount() > 0) {
                comboSubscription.select(0);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "doFillSubscriptions@AppServiceCreateDialog", ex));
        }
    }
    
    protected void fillResourceGroups(){
        int i = comboSubscription.getSelectionIndex();
        if (i < 0) { // empty
            System.out.println("No subscription is selected");
            return;
        }

        List<ResourceGroup> rgl = AzureModel.getInstance().getSubscriptionToResourceGroupMap().get(binderSubscriptionDetails.get(i));
        if (rgl == null) {
            System.out.println("rgl is null");
            return;
        }

        comboResourceGroup.removeAll();
        binderResourceGroup = new ArrayList<ResourceGroup>();
        for (ResourceGroup rg : rgl) {
            comboResourceGroup.add(rg.name());
            binderResourceGroup.add(rg);
        }
        
        if (comboResourceGroup.getItemCount() > 0) {
            comboResourceGroup.select(0);
        }
    }
    
    protected void fillAppServicePlans() {
        int i = comboSubscription.getSelectionIndex();
        if (i < 0) { // empty
            System.out.println("No subscription is selected");
            return;
        }

        List<ResourceGroup> rgl = AzureModel.getInstance().getSubscriptionToResourceGroupMap().get(binderSubscriptionDetails.get(i));
        if (rgl == null) {
            System.out.println("rgl is null");
            return;
        }
        
        comboAppServicePlan.removeAll();
        binderAppServicePlan = new ArrayList<AppServicePlan>();
        for (ResourceGroup rg : rgl) {
            List<AppServicePlan> aspl = AzureModel.getInstance().getResourceGroupToAppServicePlanMap().get(rg);
            for (AppServicePlan asp : aspl) {
                if (asp.pricingTier().toSkuDescription().tier().compareToIgnoreCase("dynamic") == 0) {
                    continue;
                }
                binderAppServicePlan.add(asp);
                comboAppServicePlan.add(asp.name());
            }
        }
        
        if (comboAppServicePlan.getItemCount() > 0) {
            comboAppServicePlan.select(0);
        }
    }
    
    protected void fillAppServicePlansDetails() {
        int i = comboAppServicePlan.getSelectionIndex();
        if (i < 0) {
            lblAppSevicePlanLocation.setText(textNotAvailable);
            lblAppServicePlanPricingTier.setText(textNotAvailable);
        } else {
            AppServicePlan asp = binderAppServicePlan.get(i);
            lblAppSevicePlanLocation.setText(asp.region().label());
            lblAppServicePlanPricingTier.setText(asp.pricingTier().toString());
        }
    }
    
    protected void fillAppServicePlanLocations() {
        int i = comboSubscription.getSelectionIndex();
        if (i < 0) { // empty
            System.out.println("No subscription is selected");
            return;
        }
        
        comboAppServicePlanLocation.removeAll();
        binderAppServicePlanLocation = new ArrayList<Location>();
        //List<Location> locl = AzureModel.getInstance().getSubscriptionToLocationMap().get(binderSubscriptionDetails.get(i));
        Map<SubscriptionDetail, List<Location>> sdlocMap = AzureModel.getInstance().getSubscriptionToLocationMap();
        SubscriptionDetail sd = binderSubscriptionDetails.get(i);
        List<Location> locl = sdlocMap.get(sd);
       
        comboAppServicePlanLocation.add("<select location>");
        binderAppServicePlanLocation.add(null);
        
        for (Location loc : locl) {
            comboAppServicePlanLocation.add(loc.displayName());
            binderAppServicePlanLocation.add(loc);
        }
        
        if (comboAppServicePlanLocation.getItemCount() > 0)  {
            comboAppServicePlanLocation.select(0);
        }
    }
    
    protected void fillAppServicePlanPricingTiers() {
        try {
            comboAppServicePlanPricingTier.removeAll();
            binderAppServicePlanPricingTier = new ArrayList<PricingTier>();
            List<PricingTier> l = createListFromClassFields(PricingTier.class);
            for (PricingTier aspt : l) {
                comboAppServicePlanPricingTier.add(aspt.toString());
                binderAppServicePlanPricingTier.add(aspt);
            }
            if (comboAppServicePlanPricingTier.getItemCount() > 0) {
                comboAppServicePlanPricingTier.select(0);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "fillAppServicePlanPricingTiers@AppServiceCreateDialog", ex));
        }
    }
    
    protected void fill3PartyJdk() {
        comboJdk3Party.removeAll();
        binderJdk3Party = new ArrayList<AzulZuluModel>();
        for (AzulZuluModel jdk : AzulZuluModel.values()) {
            if (jdk.isDeprecated()) continue;
            binderJdk3Party.add(jdk);
            comboJdk3Party.add(jdk.getName());
        }
        if (comboJdk3Party.getItemCount() > 0) {
            comboJdk3Party.select(0);
        }
    }
    
    @Override
    protected void okPressed() {
        if (validated()) {
            String errTitle = "Create App Service Error";
            try {
                ProgressDialog.get(this.getShell(), "Create App Service Progress").run(true, true, new IRunnableWithProgress() {
                    @Override
                    public void run(IProgressMonitor monitor) {
                           monitor.beginTask("Creating App Service....", IProgressMonitor.UNKNOWN);
                        if (monitor.isCanceled()) {
                            AzureModel.getInstance().setResourceGroupToWebAppMap(null);
                            Display.getDefault().asyncExec(new Runnable() {
                                @Override
                                public void run() {
                                    AppServiceCreateDialog.super.cancelPressed();
                                }
                            });
                        }
                        
                        try {
                            webApp = WebAppUtils.createAppService(new UpdateProgressIndicator(monitor), model);
                            Display.getDefault().asyncExec(new Runnable() {
                                @Override
                                public void run() {
                                    AppServiceCreateDialog.super.okPressed();
                                };
                            });                    
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "run@ProgressDialog@okPressed@AppServiceCreateDialog", ex));
                            Display.getDefault().asyncExec(new Runnable() {
                                @Override
                                public void run() {
                                    ErrorWindow.go(getShell(), ex.getMessage(), errTitle);;
                                }
                            });

                        }
                    }
                });
            } catch (InvocationTargetException | InterruptedException ex) {
                ex.printStackTrace();
                LOG.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "okPressed@AppServiceCreateDialog", ex));
                ErrorWindow.go(getShell(), ex.getMessage(), errTitle);;
            }
        }
    }
    
    private List<ControlDecoration> decorations = new LinkedList<ControlDecoration>();
    
    protected ControlDecoration decorateContorolAndRegister(Control c) {
        ControlDecoration d = new ControlDecoration(c, SWT.TOP|SWT.LEFT);
        FieldDecoration fieldDecoration = FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_ERROR);
        Image img = fieldDecoration.getImage();
        d.setImage(img);
        d.hide();
        decorations.add(d);
        return d;
    }
    
    protected void setError(ControlDecoration d, String message) {
        d.setDescriptionText(message);
        setErrorMessage("Form validation error.");
        d.show();
    }
    
    protected void cleanError() {
        for (ControlDecoration d: decorations) {
            d.hide();
        }
        setErrorMessage(null);
    }
     
    protected boolean validated() {
        cleanError();
        model.collectData();
        String webappName = model.webAppName;
        if (webappName.length() > 60 || !webappName.matches("^[A-Za-z0-9][A-Za-z0-9-]*[A-Za-z0-9]$")) {
            StringBuilder builder = new StringBuilder();
            builder.append("The name can contain letters, numbers and hyphens but the first and last characters must be a letter or number. ");
            builder.append("The length can be between 2 and 60 characters. ");
            setError(dec_textAppName, builder.toString());
            return false;
        } else {
            for (List<WebApp> wal : AzureModel.getInstance().getResourceGroupToWebAppMap().values()) {
                for (WebApp wa : wal) {
                    if (wa.name().toLowerCase().equals(webappName.toLowerCase())) {
                        setError(dec_textAppName,"The name is already taken");
                        return false;
                    }
                }
            }
        }
        
        if (model.webContainer == null) {
            setError(dec_comboWebContainer,"Select a valid web container.");
            return false;
        }
        if (model.subscriptionDetail == null) {
            setError(dec_comboSubscription,"Select a valid subscription.");
            return false;
        }

        if (model.isAppServicePlanCreateNew) {
            if (model.appServicePlanNameCreateNew.isEmpty()) {
                setError(dec_textAppSevicePlanName, "Enter a valid App Service Plan name.");
                return false;
            } else {
                if (!model.appServicePlanNameCreateNew.matches("^[A-Za-z0-9-]*[A-Za-z0-9-]$")) {
                    setError(dec_textAppSevicePlanName, "App Service Plan name can only include alphanumeric characters and hyphens.");
                    return false;
                }
                // App service plan name must be unique in each subscription
                SubscriptionDetail sd = model.subscriptionDetail;
                List<ResourceGroup> rgl = AzureModel.getInstance().getSubscriptionToResourceGroupMap().get(sd);
                for (ResourceGroup rg : rgl ) {
                    List<AppServicePlan> aspl = AzureModel.getInstance().getResourceGroupToAppServicePlanMap().get(rg);
                    for (AppServicePlan asp : aspl) {
                        if (asp.name().toLowerCase().equals(model.appServicePlanNameCreateNew.toLowerCase())) {
                            setError(dec_textAppSevicePlanName, "App service plan name must be unuque in each subscription.");
                            return false;
                        }
                    }
                }
            }
            if (model.appServicePlanLocationCreateNew == null) {
                setError(dec_comboAppServicePlanLocation, "Select a location.");
                return false;
            }
        } else {
            if (model.appServicePlan == null ) {
                setError(dec_comboAppServicePlan,"Select a valid App Service Plan.");
                return false;
            }
        }
        
        if (model.isResourceGroupCreateNew) {
            if (model.resourceGroupNameCreateNew.isEmpty()) {
                setError(dec_textNewResGrName,"Enter a valid resource group name");
                return false;
            } else {
                if (!model.resourceGroupNameCreateNew.matches("^[A-Za-z0-9-_()\\.]*[A-Za-z0-9-_()]$")) {
                    setError(dec_textNewResGrName,"Resounce group name can only include alphanumeric characters, periods, underscores, hyphens, and parenthesis and can't end in a period.");
                    return false;
                }

                for (List<ResourceGroup> rgl : AzureModel.getInstance().getSubscriptionToResourceGroupMap().values()) {
                    for (ResourceGroup rg : rgl) {
                        if (rg.name().toLowerCase().equals(model.resourceGroupNameCreateNew.toLowerCase())) {
                            setError(dec_textNewResGrName,"The name is already taken");
                            return false;
                        }
                    }
                }
            }
        } else {
            if (model.resourceGroup == null ) {
                setError(dec_comboSelectResGr, "Select a valid resource group.");
                return false;
            }
        }

        return volidatedJdkTab();
    }
    
    protected boolean volidatedJdkTab() {
        try {
            switch (model.jdkTab) {
                case Default:
                    // do nothing
                    model.jdkDownloadUrl = null;
                    break;
                case ThirdParty:
                    if (!WebAppUtils.isUrlAccessible(model.jdk3PartyUrl)) {
                        setError(dec_comboJdk3Party, "Please check the URL is valid.");
                        return false;
                    }
                    model.jdkDownloadUrl = model.jdk3PartyUrl;
                    break;
                case Own:
                    if (model.jdkOwnUrl.isEmpty()) {
                        setError(dec_textJdkOwnUrl, "Enter a valid URL.");
                        return false;
                    } else {
                        // first check the link is accessible as it is
                        if (!WebAppUtils.isUrlAccessible(model.jdkOwnUrl)) {
                            // create shared access signature url and check its accessibility
                            String sasUrl = StorageAccoutUtils.getBlobSasUri(model.jdkOwnUrl, model.storageAccountKey);
                            if (!WebAppUtils.isUrlAccessible(sasUrl)) {
                                setError(dec_textJdkOwnUrl,"Please check the storage account key and/or URL is valid.");
                                return false;
                            } else {
                                model.jdkDownloadUrl = sasUrl;
                            }
                        } else {
                            model.jdkDownloadUrl = model.jdkOwnUrl;
                        }
                    }
                    // link to a ZIP file
                    // consider it's a SAS link
                    String urlPath = new URI(model.jdkOwnUrl).getPath();
                    if (!urlPath.endsWith(".zip")) {
                        setError(dec_textJdkOwnUrl,"link to a zip file is expected.");
                        return false;
                    }
                    break;
                default:
                    throw new Exception("Unknown JDK tab");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            setError(dec_textJdkOwnUrl,"Url validation exception:" + ex.getMessage());
            return false;
        }
        
        return true;
    }
    
    protected class Model extends WebAppUtils.CreateAppServiceModel {
        @Override
        public void collectData() {
          webAppName = textAppName.getText().trim();
          
          int index = comboWebContainer.getSelectionIndex();
          webContainer = index < 0 ? null : binderWebConteiners.get(index).toWebContainer();
          
          index = comboSubscription.getSelectionIndex();
          subscriptionDetail = index < 0 ? null : binderSubscriptionDetails.get(index);


          //isResourceGroupCreateNew = tabFolderResourceGroup.getSelection()[0] == tabItemResGrCreateNew;
          isResourceGroupCreateNew = btnResourceGroupCreateNew.getSelection();
          index = comboResourceGroup.getSelectionIndex();
          resourceGroup = index < 0 ? null : binderResourceGroup.get(index);
          resourceGroupNameCreateNew = textResourceGroupName.getText().trim();

          //isAppServicePlanCreateNew = tabFolderAppServicePlan.getSelection()[0] == tabItemAppServicePlanCreateNew;
          isAppServicePlanCreateNew = btnAppServiceCreateNew.getSelection();
          index = comboAppServicePlan.getSelectionIndex();
          appServicePlan = index < 0 ? null : binderAppServicePlan.get(index);

          appServicePlanNameCreateNew = textAppSevicePlanName.getText().trim();

          index = comboAppServicePlanPricingTier.getSelectionIndex();
          appServicePricingTierCreateNew = index < 0 ? null : binderAppServicePlanPricingTier.get(index);

          index = comboAppServicePlanLocation.getSelectionIndex();
          appServicePlanLocationCreateNew = index < 0 ? null : binderAppServicePlanLocation.get(index);

          //TabItem selectedJdkPanel = tabFolderJdk.getSelection()[0];
          jdkTab = (btnJdkDefault.getSelection())
              ? JdkTab.Default
              : (btnJdk3rdParty.getSelection())
                  ? JdkTab.ThirdParty
                  : (btnJdkOwnDownloadUrl.getSelection())
                      ? JdkTab.Own
                      : null;

          index = comboJdk3Party.getSelectionIndex();
          AzulZuluModel jdk3Party = index < 0 ? null : binderJdk3Party.get(index);
          jdk3PartyUrl = jdk3Party == null ? null : jdk3Party.getDownloadUrl();
          jdkOwnUrl = textJdkOwnUrl.getText().trim();
          storageAccountKey = textJdkOwnStorageAccountKey.getText().trim();
          jdkDownloadUrl = null; // get the value in the validate phase
            
        }
    }
    
    protected WebAppUtils.CreateAppServiceModel model = new Model();

}


