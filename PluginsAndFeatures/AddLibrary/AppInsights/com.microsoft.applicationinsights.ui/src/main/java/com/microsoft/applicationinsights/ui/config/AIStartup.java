/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.applicationinsights.ui.config;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.ui.IStartup;

import com.microsoft.applicationinsights.ui.activator.Activator;
import com.microsoft.azuretools.core.applicationinsights.ApplicationInsightsPreferences;

public class AIStartup implements IStartup {

    @Override
    public void earlyStartup() {
        try {
            IWorkspace workspace = ResourcesPlugin.getWorkspace();
            IWorkspaceRoot root = workspace.getRoot();
            // register resource change listener
            AIResourceChangeListener listener = new AIResourceChangeListener();
            workspace.addResourceChangeListener(listener, IResourceChangeEvent.POST_CHANGE);
            // load application insights preferences
            ApplicationInsightsPreferences.load();
            for (IProject iproject : root.getProjects()) {
                AIResourceChangeListener.initializeAIRegistry(iproject);
            }
            ApplicationInsightsPreferences.save();
        } catch (Exception ex) {
            Activator.getDefault().log(Messages.startUpErr, ex);
        }
    }
}
