/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.eclipse.function.launch.local;

import com.microsoft.azure.toolkit.eclipse.common.launch.LaunchConfigurationUtils;
import com.microsoft.azure.toolkit.eclipse.function.launch.model.FunctionLocalRunConfiguration;
import com.microsoft.azure.toolkit.eclipse.function.tester.AzureFunctionTypeTester;
import com.microsoft.azure.toolkit.eclipse.function.utils.FunctionUtils;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.*;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.ILaunchShortcut2;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaLaunchShortcut;
import org.eclipse.jdt.internal.debug.ui.JDIDebugUIPlugin;
import org.eclipse.jdt.internal.debug.ui.launcher.LauncherMessages;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableContext;

import java.io.IOException;
import java.util.*;

public class FunctionLocalRunShortcut extends JavaLaunchShortcut implements ILaunchShortcut2 {
    @Override
    protected ILaunchConfigurationType getConfigurationType() {
        return getLaunchManager().getLaunchConfigurationType(getLaunchConfigurationTypeId());
    }

    @Override
    protected void launch(IType type, String mode) {
        List<ILaunchConfiguration> configs = getCandidates(type, getConfigurationType());
        if (configs != null) {
            ILaunchConfiguration config = null;
            int count = configs.size();
            if (count == 1) {
                config = configs.get(0);
            } else if (count > 1) {
                config = chooseConfiguration(configs);
                if (config == null) {
                    return;
                }
            }
            if (config == null) {
                config = createConfiguration(type);
            }
            if (config != null) {
                DebugUITools.launch(config, mode);
            }
        }
    }

    List<ILaunchConfiguration> getCandidates(IType type, ILaunchConfigurationType ctype) {
        List<ILaunchConfiguration> candidateConfigs = Collections.EMPTY_LIST;
        try {
            ILaunchConfiguration[] configs = DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurations(ctype);
            candidateConfigs = new ArrayList<>(configs.length);
            for (int i = 0; i < configs.length; i++) {
                ILaunchConfiguration config = configs[i];
                if (config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, "").equals(type.getJavaProject().getElementName())) { //$NON-NLS-1$
                    candidateConfigs.add(config);
                }
            }
        } catch (CoreException e) {
            JDIDebugUIPlugin.log(e);
        }
        return candidateConfigs;
    }

    @Override
    protected ILaunchConfiguration findLaunchConfiguration(IType type, ILaunchConfigurationType configType) {
        List<ILaunchConfiguration> configs = getCandidates(type, configType);
        int count = configs.size();
        if (count >= 1) {
            return configs.get(0);
        }
        return null;
    }

    @Override
    protected ILaunchConfiguration createConfiguration(IType type) {
        ILaunchConfiguration config = null;
        ILaunchConfigurationWorkingCopy wc;
        try {
            ILaunchConfigurationType configType = getConfigurationType();
            wc = configType.newInstance(null, getLaunchManager().generateLaunchConfigurationName(type.getTypeQualifiedName('.')));
            wc.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, type.getJavaProject().getElementName());
            final FunctionLocalRunConfiguration localRunConfiguration = new FunctionLocalRunConfiguration();
            localRunConfiguration.setProjectName(type.getJavaProject().getElementName());
            final IFile localSettingsFile = type.getJavaProject().getProject().getFile("local.settings.json");

            if (localSettingsFile.exists()) {
                localRunConfiguration.setLocalSettingsJsonPath(localSettingsFile.getLocation().toOSString());
            }
            try {
                final String funcPath = FunctionUtils.getFuncPath();
                localRunConfiguration.setFunctionCliPath(funcPath);
            } catch (IOException | InterruptedException ex) {
                throw new AzureToolkitRuntimeException("Cannot find Azure Functions Core Tools. Please go to https://aka.ms/azfunc-install to install Azure Functions Core Tools.");
            }
            LaunchConfigurationUtils.saveToConfiguration(localRunConfiguration, wc);
            wc.setMappedResources(new IResource[]{type.getUnderlyingResource()});
            config = wc.doSave();
        } catch (CoreException exception) {
            MessageDialog.openError(JDIDebugUIPlugin.getActiveWorkbenchShell(), LauncherMessages.JavaLaunchShortcut_3, exception.getStatus().getMessage());
        }
        return config;
    }

    @Override
    protected IType[] findTypes(Object[] elements, IRunnableContext context) {
        List<IType> list = new ArrayList<>();
        for (IJavaElement element : getJavaElements(elements)) {
            IType typeFromJavaElement = AzureFunctionTypeTester.getTypeFromJavaElement(element);
            if (typeFromJavaElement != null) {
                list.add(typeFromJavaElement);
            }
        }
        return list.toArray(new IType[0]);
    }

    @Override
    protected String getTypeSelectionTitle() {
        return "Select Function local run configurations";
    }

    @Override
    protected String getEditorEmptyMessage() {
        return "Editor does not contain an Azure Function class";
    }

    @Override
    protected String getSelectionEmptyMessage() {
        return "Selection does not contain an Azure Function class";
    }

    protected IJavaElement[] getJavaElements(Object[] objects) {
        List<IJavaElement> list = new ArrayList<>(objects.length);
        for (Object object : objects) {
            if (object instanceof IAdaptable) {
                IJavaElement element = ((IAdaptable) object).getAdapter(IJavaElement.class);
                if (element != null) {
                    if (element instanceof IMember) {
                        // Use the declaring type if available
                        IJavaElement type = ((IMember) element).getDeclaringType();
                        if (type != null) {
                            element = type;
                        }
                    }
                    list.add(element);
                }
            }
        }
        return list.toArray(new IJavaElement[0]);
    }

    protected String getLaunchConfigurationTypeId() {
        return "com.microsoft.azure.toolkit.eclipse.function.localRunConfigurationType";
    }

    private ILaunchManager getLaunchManager() {
        return DebugPlugin.getDefault().getLaunchManager();
    }
}
