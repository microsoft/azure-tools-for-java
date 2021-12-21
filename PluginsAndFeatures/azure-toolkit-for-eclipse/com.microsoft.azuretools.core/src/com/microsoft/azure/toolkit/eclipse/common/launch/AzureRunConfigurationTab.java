/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.eclipse.common.launch;

import com.microsoft.azure.toolkit.eclipse.common.form.AzureFormPanel;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import java.util.List;
import java.util.function.Function;

public class AzureRunConfigurationTab<T extends BaseRunConfiguration> extends AbstractLaunchConfigurationTab {
    private final Class<T> configurationModelClass;
    private final Function<Composite, AzureFormPanel<T>> panelSupplier;
    private AzureFormPanel<T> panel;
    private String name;

    public AzureRunConfigurationTab(Function<Composite, AzureFormPanel<T>> panelSupplier, String name, Class<T> clz) {
        this.configurationModelClass = clz;
        this.panelSupplier = panelSupplier;
        this.name = name;
    }

    @Override
    public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
    }

    @Override
    public boolean isValid(ILaunchConfiguration configuration) {
        final List<AzureValidationInfo> infos = panel.getAllValidationInfos(true);
        return infos.stream().allMatch(AzureValidationInfo::isValid);
    }

    @Override
    public void createControl(Composite parent) {
        panel = panelSupplier.apply(parent);
        setControl((Control) panel);
        for (AzureFormInput<?> input : panel.getInputs()) {
            input.addValueChangedListener(v -> {
                markDirty();
            });
        }

    }

    @Override
    public void initializeFrom(ILaunchConfiguration configuration) {
        T origin = LaunchConfigurationUtils.getFromConfiguration(configuration, configurationModelClass);
        panel.setValue(origin);
    }

    @Override
    public void performApply(ILaunchConfigurationWorkingCopy configuration) {
        T origin = LaunchConfigurationUtils.getFromConfiguration(configuration, configurationModelClass);
        T newConfig = panel.getValue();
        try {

            Utils.mergeObjects(newConfig, origin);
        } catch (IllegalAccessException e) {
            // ignore
        }
        LaunchConfigurationUtils.saveToConfiguration(newConfig, configuration);
        configuration.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, newConfig.getProjectName());
    }

    @Override
    public String getName() {
        return name;
    }

    private void markDirty() {
        AzureTaskManager.getInstance().runLater(() -> {
            if (!this.getControl().isDisposed()) {
                setDirty(true);
                updateLaunchConfigurationDialog();
            }
        });

    }
}
