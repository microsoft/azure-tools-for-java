/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.eclipse.common.launch;

import com.microsoft.azure.toolkit.eclipse.common.form.AzureFormPanel;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.swt.widgets.Composite;

import java.util.function.Function;

public class AzureLaunchConfigurationTabGroup extends AbstractLaunchConfigurationTabGroup {
    private final Class<?> modelClass;
    private String configurationName;
    private Function<Composite, AzureFormPanel<?>> panelCreator;

    public AzureLaunchConfigurationTabGroup(String configurationName, Function<Composite, AzureFormPanel<?>> panelCreator, Class<?> modelClass) {
        this.configurationName = configurationName;
        this.panelCreator = panelCreator;
        this.modelClass = modelClass;
    }

    @Override
    public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
        setTabs(new AzureRunConfigurationTab(panelCreator, configurationName, modelClass), new CommonTab());
    }
}
