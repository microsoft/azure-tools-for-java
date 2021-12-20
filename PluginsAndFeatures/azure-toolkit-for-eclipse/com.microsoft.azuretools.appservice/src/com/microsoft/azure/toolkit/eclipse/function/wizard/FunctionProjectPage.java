/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.eclipse.function.wizard;

import com.microsoft.azure.toolkit.eclipse.common.component.AzWizardPageWrapper;
import com.microsoft.azure.toolkit.eclipse.common.component.AzureTextInput;
import com.microsoft.azure.toolkit.ide.appservice.model.FunctionProjectModel;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import org.apache.commons.compress.utils.FileNameUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FunctionProjectPage extends AzWizardPageWrapper implements AzureForm<FunctionProjectModel> {
    public static final String HTTP_TRIGGER = "HttpTrigger";
    public static final String BLOB_TRIGGER = "BlobTrigger";
    public static final String QUEUE_TRIGGER = "QueueTrigger";
    public static final String TIMER_TRIGGER = "TimerTrigger";
    public static final String EVENT_HUB_TRIGGER = "EventHubTrigger";
    private Text location;
    private Composite container;
    private Label lblLocation;
    private Button btnBrowse;
    private Button btnUseDefaultLocation;
    private Button checkHttpTrigger;
    private Label lblTriggers;
    private Button checkBlobTrigger;
    private Button checkQueueTrigger;
    private Button checkTimerTrigger;
    private Button checkEventHubTrigger;
    private Label lblProjectName;
    private AzureTextInput txtProjectName;

    public FunctionProjectPage() {
        super("New Azure Function Project");
        setTitle("New Azure Function Project");
        setDescription("Create an Azure Function Project");
    }

    @Override
    public void createControl(Composite parent) {
        container = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        container.setLayout(layout);
        layout.numColumns = 3;
        // required to avoid an error in the system
        setControl(container);

        lblProjectName = new Label(container, SWT.NONE);
        lblProjectName.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        lblProjectName.setText("Project name:");

        txtProjectName = new AzureTextInput(container);
        txtProjectName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

        btnUseDefaultLocation = new Button(container, SWT.CHECK);
        btnUseDefaultLocation.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
        btnUseDefaultLocation.setText("Use default location");
        new Label(container, SWT.NONE);

        lblLocation = new Label(container, SWT.NONE);
        lblLocation.setText("Location:");

        location = new Text(container, SWT.BORDER | SWT.SINGLE);
        location.setText("");
        location.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        btnBrowse = new Button(container, SWT.NONE);
        btnBrowse.setText("Browse...");

        lblTriggers = new Label(container, SWT.NONE);
        lblTriggers.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
        lblTriggers.setText("Choose Function Triggers:");
        new Label(container, SWT.NONE);

        checkHttpTrigger = new Button(container, SWT.CHECK);
        GridData gd_checkHttpTrigger = new GridData(SWT.LEFT, SWT.CENTER, false, false, 3, 1);
        gd_checkHttpTrigger.horizontalIndent = 5;
        checkHttpTrigger.setLayoutData(gd_checkHttpTrigger);
        checkHttpTrigger.setText(HTTP_TRIGGER);

        checkBlobTrigger = new Button(container, SWT.CHECK);
        GridData gd_checkBlobTrigger = new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1);
        gd_checkBlobTrigger.horizontalIndent = 5;
        checkBlobTrigger.setLayoutData(gd_checkBlobTrigger);
        checkBlobTrigger.setText(BLOB_TRIGGER);
        new Label(container, SWT.NONE);

        checkQueueTrigger = new Button(container, SWT.CHECK);
        GridData gd_checkQueueTrigger = new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1);
        gd_checkQueueTrigger.horizontalIndent = 5;
        checkQueueTrigger.setLayoutData(gd_checkQueueTrigger);
        checkQueueTrigger.setText(QUEUE_TRIGGER);
        new Label(container, SWT.NONE);

        checkTimerTrigger = new Button(container, SWT.CHECK);
        GridData gd_checkTimerTrigger = new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1);
        gd_checkTimerTrigger.horizontalIndent = 5;
        checkTimerTrigger.setLayoutData(gd_checkTimerTrigger);
        checkTimerTrigger.setText(TIMER_TRIGGER);
        new Label(container, SWT.NONE);

        checkEventHubTrigger = new Button(container, SWT.CHECK);
        GridData gd_checkEventHubTrigger = new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1);
        gd_checkEventHubTrigger.horizontalIndent = 5;
        checkEventHubTrigger.setLayoutData(gd_checkEventHubTrigger);
        checkEventHubTrigger.setText(EVENT_HUB_TRIGGER);
        new Label(container, SWT.NONE);
        initListeners();
    }

    private void initListeners() {
        btnUseDefaultLocation.setSelection(true);
        checkHttpTrigger.setSelection(true);
        updateLocation();
        btnBrowse.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
            DirectoryDialog dialog = new DirectoryDialog(getShell());
            dialog.setText("Select Location");

            String path = location.getText();
            if (path.length() == 0) {
                path = ResourcesPlugin.getWorkspace().getRoot().getLocation().toPortableString();
            }
            dialog.setFilterPath(path);

            String selectedDir = dialog.open();
            if (selectedDir != null) {
                location.setText(selectedDir);
                btnUseDefaultLocation.setSelection(false);
            }
            if (StringUtils.isBlank(txtProjectName.getValue())) {
                txtProjectName.setValue(FileNameUtils.getBaseName(selectedDir));
            }
            this.doValidateAllSync();
        }));
        txtProjectName.addValidator(() -> {
            final IWorkspace workspace = JavaPlugin.getWorkspace();
            final IStatus nameStatus = workspace.validateName(txtProjectName.getValue(), IResource.PROJECT);
            AzureValidationInfo errorInfo = null;
            AzureValidationInfo.AzureValidationInfoBuilder partialErrorBuilder = AzureValidationInfo.builder().input(txtProjectName).type(AzureValidationInfo.Type.ERROR);
            if (StringUtils.isBlank(txtProjectName.getValue())) {
                errorInfo = partialErrorBuilder.message("Project name is required.").build();
            } else if (!nameStatus.isOK()) {
                errorInfo = partialErrorBuilder.message(nameStatus.getMessage()).build();
            } else {
                final IProject project = workspace.getRoot().getProject(txtProjectName.getValue());
                if (project.exists()) {
                    errorInfo = partialErrorBuilder.message("A project with this name already exists.").build();
                }
            }
            if (errorInfo != null) {
                return errorInfo;
            }
            return AzureValidationInfo.success(txtProjectName);

        });
        txtProjectName.addValueChangedListener(value -> {
            AzureTaskManager.getInstance().runLater(() -> location.setText(getProjectPath()));
        });
        btnUseDefaultLocation.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                super.widgetSelected(e);
                boolean selection = btnUseDefaultLocation.getSelection();
                updateLocation();
                if (!selection) {
                    location.setText("");
                } else {
                    location.setText(getProjectPath());
                }
            }
        });
    }

    private String getProjectPath() {
        return getLocationPath().toOSString();
    }

    private void updateLocation() {
        boolean inWorkspace = isInWorkspace();
        location.setEnabled(!inWorkspace);
        lblLocation.setEnabled(!inWorkspace);
        location.setText(getProjectPath());
        btnBrowse.setEnabled(!inWorkspace);
    }

    public IPath getLocationPath() {
        if (isInWorkspace()) {
            return ResourcesPlugin.getWorkspace().getRoot().getLocation().append(txtProjectName.getText().trim());
        }
        return Path.fromOSString(location.getText().trim());
    }

    public String getProjectName() {
        return txtProjectName.getText().trim();
    }

    private boolean isInWorkspace() {
        return btnUseDefaultLocation.getSelection();
    }

    @Override
    public AzureForm<FunctionProjectModel> getForm() {
        return this;
    }

    @Override
    public FunctionProjectModel getValue() {
        FunctionProjectModel model = new FunctionProjectModel();
        model.setProjectName(txtProjectName.getText().trim());
        model.setLocation(getProjectPath());
        model.setTriggers(getTriggers());
        return model;
    }

    @Override
    public void setValue(@Nonnull FunctionProjectModel model) {
        this.txtProjectName.setText(model.getProjectName());
        this.btnUseDefaultLocation.setSelection(false);
        this.location.setText(model.getLocation());
        checkHttpTrigger.setSelection(model.getTriggers().contains(HTTP_TRIGGER));
        checkBlobTrigger.setSelection(model.getTriggers().contains(BLOB_TRIGGER));
        checkEventHubTrigger.setSelection(model.getTriggers().contains(EVENT_HUB_TRIGGER));
        checkQueueTrigger.setSelection(model.getTriggers().contains(QUEUE_TRIGGER));
        checkTimerTrigger.setSelection(model.getTriggers().contains(TIMER_TRIGGER));
    }

    private List<String> getTriggers() {
        List<String> list = new ArrayList<>();
        if (this.checkHttpTrigger.getSelection()) {
            list.add(HTTP_TRIGGER);
        }

        if (this.checkBlobTrigger.getSelection()) {
            list.add(BLOB_TRIGGER);
        }

        if (this.checkEventHubTrigger.getSelection()) {
            list.add(EVENT_HUB_TRIGGER);
        }

        if (this.checkQueueTrigger.getSelection()) {
            list.add(QUEUE_TRIGGER);
        }

        if (this.checkTimerTrigger.getSelection()) {
            list.add(TIMER_TRIGGER);
        }

        return list;
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Arrays.asList(txtProjectName);
    }
}