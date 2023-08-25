/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.eclipse.functionapp.creation;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.azure.toolkit.eclipse.appservice.AppServiceCreationComposite;
import com.microsoft.azure.toolkit.eclipse.common.component.AzureDialog;
import com.microsoft.azure.toolkit.ide.appservice.function.FunctionAppConfig;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.appservice.model.JavaVersion;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.appservice.model.WebContainer;

public class CreateFunctionAppDialog extends AzureDialog<FunctionAppConfig> {
    // workaround to fix the java runtime
    public static final Runtime FUNCTION_WINDOWS_JAVA8 = new Runtime(OperatingSystem.WINDOWS, WebContainer.JAVA_OFF,
            JavaVersion.JAVA_8);
    public static final Runtime FUNCTION_WINDOWS_JAVA11 = new Runtime(OperatingSystem.WINDOWS, WebContainer.JAVA_OFF,
            JavaVersion.JAVA_11);
    public static final Runtime FUNCTION_WINDOWS_JAVA17 = new Runtime(OperatingSystem.WINDOWS, WebContainer.JAVA_OFF,
            JavaVersion.JAVA_17);
    public static final Runtime FUNCTION_LINUX_JAVA8 = new Runtime(OperatingSystem.LINUX, WebContainer.JAVA_OFF,
            JavaVersion.JAVA_8);
    public static final Runtime FUNCTION_LINUX_JAVA11 = new Runtime(OperatingSystem.LINUX, WebContainer.JAVA_OFF,
            JavaVersion.JAVA_11);
    public static final Runtime FUNCTION_LINUX_JAVA17 = new Runtime(OperatingSystem.LINUX, WebContainer.JAVA_OFF,
            JavaVersion.JAVA_17);
    public static final List<Runtime> FUNCTION_APP_RUNTIME = Collections.unmodifiableList(Arrays.asList(FUNCTION_LINUX_JAVA8, FUNCTION_LINUX_JAVA11, FUNCTION_LINUX_JAVA17,
            FUNCTION_WINDOWS_JAVA8, FUNCTION_WINDOWS_JAVA11, FUNCTION_WINDOWS_JAVA17));
    
    private FunctionAppConfig config;
    private AppServiceCreationComposite<FunctionAppConfig> composite;

    /**
     * Create the dialog.
     *
     * @param parentShell
     */
    public CreateFunctionAppDialog(Shell parentShell, FunctionAppConfig config) {
        super(parentShell);
        this.config = config;
        setShellStyle(SWT.SHELL_TRIM);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite container = (Composite) super.createDialogArea(parent);
        this.composite = new AppServiceCreationComposite<FunctionAppConfig>(container, SWT.NONE, FunctionAppConfig::new);
        this.composite.setRuntime(FUNCTION_APP_RUNTIME);
        this.composite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false, 1, 1));
        return container;
    }

    @Override
    protected String getDialogTitle() {
        return "Create Azure Function App";
    }

    @Override
    public AzureForm<FunctionAppConfig> getForm() {
        return this.composite;
    }

    @Override
    public int open() {
        Optional.ofNullable(config)
                .ifPresent(config -> AzureTaskManager.getInstance().runLater(() -> composite.setValue(config)));
        return super.open();
    }

}
