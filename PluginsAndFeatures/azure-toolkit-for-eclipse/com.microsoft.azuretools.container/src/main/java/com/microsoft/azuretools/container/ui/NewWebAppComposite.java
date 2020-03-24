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

package com.microsoft.azuretools.container.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ExpandBar;
import org.eclipse.swt.widgets.ExpandItem;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

class NewWebAppComposite extends Composite {
    Text txtAppName;
    Text txtAppServicePlanName;
    Combo cbLocation;
    Combo cbPricingTier;
    Combo cbExistingAppServicePlan;
    Combo cbSubscription;
    Text txtNewResourceGroupName;
    Combo cbExistingResourceGroup;
    Label lblLocationValue;
    Label lblPricingTierValue;
    Button rdoNewAppServicePlan;
    Button rdoExistingAppServicePlan;
    Button rdoExistingResourceGroup;
    Button rdoNewResourceGroup;
    private ExpandItem appServicePlanHolder;
    private ExpandItem resourceGroupHolder;
    ExpandBar expandBar;

    /**
     * Create the composite.
     */
    public NewWebAppComposite(Composite parent, int style) {
        super(parent, style);
        setLayout(new GridLayout(1, false));

        Composite cpCommonInfo = new Composite(this, SWT.NONE);
        cpCommonInfo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        cpCommonInfo.setLayout(new GridLayout(3, false));

        Label lblAppName = new Label(cpCommonInfo, SWT.NONE);
        lblAppName.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        lblAppName.setText("App name");

        txtAppName = new Text(cpCommonInfo, SWT.BORDER);
        txtAppName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

        Label lblAzureHostname = new Label(cpCommonInfo, SWT.NONE);
        lblAzureHostname.setText(".azurewebsites.net");

        Label lblSubscription = new Label(cpCommonInfo, SWT.NONE);
        lblSubscription.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        lblSubscription.setText("Subscription");

        cbSubscription = new Combo(cpCommonInfo, SWT.NONE | SWT.READ_ONLY);
        cbSubscription.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

        expandBar = new ExpandBar(this, SWT.NONE);
        expandBar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        expandBar.setBackground(parent.getBackground());

        resourceGroupHolder = new ExpandItem(expandBar, SWT.NONE);
        resourceGroupHolder.setExpanded(true);
        resourceGroupHolder.setText("Resource Group");

        Composite composite = new Composite(expandBar, SWT.BORDER);
        resourceGroupHolder.setControl(composite);
        composite.setLayout(new GridLayout(2, false));

        rdoExistingResourceGroup = new Button(composite, SWT.RADIO);
        rdoExistingResourceGroup.setSelection(true);
        rdoExistingResourceGroup.setText("Use Existing");

        cbExistingResourceGroup = new Combo(composite, SWT.READ_ONLY);
        cbExistingResourceGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

        rdoNewResourceGroup = new Button(composite, SWT.RADIO);
        rdoNewResourceGroup.setText("Create New");

        appServicePlanHolder = new ExpandItem(expandBar, SWT.NONE);
        appServicePlanHolder.setExpanded(true);
        appServicePlanHolder.setText("App Service Plan");

        Composite composite1 = new Composite(expandBar, SWT.BORDER);
        appServicePlanHolder.setControl(composite1);
        composite1.setLayout(new GridLayout(2, false));

        rdoExistingAppServicePlan = new Button(composite1, SWT.RADIO);
        rdoExistingAppServicePlan.setSelection(true);
        rdoExistingAppServicePlan.setText("Use Existing");

        cbExistingAppServicePlan = new Combo(composite1, SWT.READ_ONLY);
        cbExistingAppServicePlan.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

        Label lblLocation = new Label(composite1, SWT.NONE);
        lblLocation.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        lblLocation.setText("Location");

        lblLocationValue = new Label(composite1, SWT.NONE);
        lblLocationValue.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        lblLocationValue.setText("N/A");

        Label lblPricingTier = new Label(composite1, SWT.NONE);
        lblPricingTier.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        lblPricingTier.setText("Pricing Tier");

        lblPricingTierValue = new Label(composite1, SWT.NONE);
        lblPricingTierValue.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        lblPricingTierValue.setText("N/A");

        rdoNewAppServicePlan = new Button(composite1, SWT.RADIO);
        rdoNewAppServicePlan.setText("Create New");

        txtAppServicePlanName = new Text(composite1, SWT.BORDER);
        txtAppServicePlanName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

        Label lblNewLabel2 = new Label(composite1, SWT.NONE);
        lblNewLabel2.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        lblNewLabel2.setText("Location");

        cbLocation = new Combo(composite1, SWT.READ_ONLY);
        cbLocation.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

        Label lblNewLabel3 = new Label(composite1, SWT.NONE);
        lblNewLabel3.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        lblNewLabel3.setText("Pricing Tier");

        cbPricingTier = new Combo(composite1, SWT.READ_ONLY);
        cbPricingTier.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

        txtNewResourceGroupName = new Text(composite, SWT.BORDER);
        txtNewResourceGroupName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

        resizeExpandItems();
    }

    void resizeExpandItems() {
        resourceGroupHolder.setHeight(resourceGroupHolder.getControl().computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
        appServicePlanHolder.setHeight(appServicePlanHolder.getControl().computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
    }

    @Override
    protected void checkSubclass() {
        // Disable the check that prevents subclassing of SWT components
    }
}
