/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.hdinsight.projects;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;

public class HDInsightProjectNature implements IProjectNature {
    public static final String NATURE_ID = "hdinsight.projectnature";
    private IProject project;

    @Override
    public void configure() throws CoreException {

    }

    @Override
    public void deconfigure() throws CoreException {

    }

    @Override
    public IProject getProject() {
        return project;
    }

    @Override
    public void setProject(IProject project) {
        this.project = project;
    }
}
