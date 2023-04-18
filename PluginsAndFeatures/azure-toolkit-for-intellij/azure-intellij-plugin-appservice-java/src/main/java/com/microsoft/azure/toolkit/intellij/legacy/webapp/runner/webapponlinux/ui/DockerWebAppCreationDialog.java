/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webapponlinux.ui;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.legacy.webapp.WebAppCreationDialog;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;

public class DockerWebAppCreationDialog extends WebAppCreationDialog {
    public DockerWebAppCreationDialog(Project project) {
        super(project);
    }

    @Override
    protected void init() {
        super.init();
        this.basicForm.setFixedRuntime(Runtime.DOCKER);
        this.advancedForm.setFixedRuntime(Runtime.DOCKER);
    }
}
