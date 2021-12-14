/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.eclipse.function.wizard;

import com.microsoft.azure.toolkit.ide.appservice.function.AzureFunctionsUtils;
import com.microsoft.azure.toolkit.ide.appservice.model.FunctionArtifactModel;
import com.microsoft.azure.toolkit.ide.appservice.model.FunctionProjectModel;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.internal.wizards.datatransfer.SmartImportJob;

import java.io.File;
import java.lang.reflect.Field;
import java.util.HashSet;

public class CreateFunctionProjectWizard extends Wizard implements INewWizard {
    protected FunctionProjectPage projectPage;
    protected ProjectArtifactPage artifactPage;

    public CreateFunctionProjectWizard() {
        setNeedsProgressMonitor(true);
    }

    @Override
    public String getWindowTitle() {
        return "New Azure Function Project";
    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        // initialize wizard with workbench and the current selection
    }

    @Override
    public void addPages() {
        projectPage = new FunctionProjectPage();
        artifactPage = new ProjectArtifactPage();
        addPage(projectPage);
        addPage(artifactPage);
    }

    public boolean canFinish() {
        if (projectPage == null || !projectPage.isPageComplete()) {
            return false;
        }
        if (artifactPage == null || !artifactPage.isPageComplete()) {
            return false;
        }
        return super.canFinish();
    }

    @Override
    public boolean performFinish() {
        final FunctionProjectModel projectModel = projectPage.getValue();
        final FunctionArtifactModel model = artifactPage.getValue();
        if (StringUtils.isBlank(model.getArtifactId())) {
            model.setArtifactId(projectModel.getProjectName());
        }
        final FunctionArtifactModel defaultModel = FunctionArtifactModel.getDefaultFunctionProjectConfig();

        try {
            mergeObjects(model, defaultModel);
        } catch (IllegalAccessException e) {
            // ignore
        }

        AzureFunctionsUtils.createAzureFunctionProject(projectModel.getLocation(),
                model.getGroupId()
                , model.getArtifactId(), model.getVersion(), "maven", projectModel.getTriggers().toArray(new String[0]), model.getPackageName());
        SmartImportJob job = new SmartImportJob(new File(projectModel.getLocation()), new HashSet<>(), true, true);
        job.schedule();
        job.addJobChangeListener(new JobChangeAdapter() {
            @Override
            public void done(IJobChangeEvent event) {
                final IStatus result = event.getResult();
                if (!result.isOK()) {
                    AzureTaskManager.getInstance().runLater(() ->
                            AzureMessager.getMessager().error(result.getMessage(), "Failed to create Azure Function project"));
                }
            }

        });

        return true;
    }

    private static <T> void mergeObjects(T to, T from) throws IllegalAccessException {
        for (Field field : FieldUtils.getAllFields(from.getClass())) {
            final Object originValue = FieldUtils.readField(field, to, true);
            if (originValue == null || (originValue instanceof String && StringUtils.isBlank((String) originValue))) {
                final Object value = FieldUtils.readField(field, from, true);
                if (value != null) {
                    FieldUtils.writeField(field, to, value, true);
                }
            }
        }
    }
}