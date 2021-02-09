/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.core.actions;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.RefreshTab;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.m2e.actions.MavenLaunchConstants;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;
import org.eclipse.m2e.core.project.ResolverConfiguration;

import java.util.concurrent.Callable;

public class MavenExecuteAction {

    private final String goalName;

    private static final String MVN_LAUNCH_MODE = "run";

    public MavenExecuteAction(String goal) {
        this.goalName = goal;
    }

    public void launch(IContainer basecon, Callable<?> callback) throws CoreException {
        if (basecon == null) {
            return;
        }

        ILaunchConfiguration launchConfiguration = createLaunchConfiguration(basecon);
        if (launchConfiguration == null) {
            return;
        }

        ILaunch launch = launchConfiguration.launch(MVN_LAUNCH_MODE, new NullProgressMonitor(), true, true);

        DebugPlugin.getDefault().addDebugEventListener(new IDebugEventSetListener() {
            @Override
            public void handleDebugEvents(DebugEvent[] events) {
                for (int i = 0; i < events.length; i++) {
                    DebugEvent event = events[i];
                    if (event.getSource() instanceof IProcess && event.getKind() == DebugEvent.TERMINATE) {
                        IProcess process = (IProcess) event.getSource();
                        if (launch == process.getLaunch()) {
                            DebugPlugin.getDefault().removeDebugEventListener(this);
                            try {
                                if (process.getExitValue() == 0) {
                                    callback.call();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            return;
                        }
                    }
                }
            }
        });
    }

    private ILaunchConfiguration createLaunchConfiguration(IContainer basedir) {
        try {
            ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
            ILaunchConfigurationType launchConfigurationType = launchManager
                    .getLaunchConfigurationType(MavenLaunchConstants.LAUNCH_CONFIGURATION_TYPE_ID);

            String launchSafeGoalName = goalName.replace(':', '-');

            ILaunchConfigurationWorkingCopy workingCopy = launchConfigurationType.newInstance(null, launchSafeGoalName);

            workingCopy.setAttribute(MavenLaunchConstants.ATTR_POM_DIR, basedir.getLocation().toOSString());
            workingCopy.setAttribute(MavenLaunchConstants.ATTR_GOALS, goalName);
            workingCopy.setAttribute(IDebugUIConstants.ATTR_PRIVATE, true);
            workingCopy.setAttribute(RefreshTab.ATTR_REFRESH_SCOPE, "${project}");
            workingCopy.setAttribute(RefreshTab.ATTR_REFRESH_RECURSIVE, true);

            setProjectConfiguration(workingCopy, basedir);

            IPath path = getJREContainerPath(basedir);
            if (path != null) {
                workingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_JRE_CONTAINER_PATH,
                        path.toPortableString());
            }

            return workingCopy;
        } catch (CoreException ex) {
        }
        return null;
    }

    private void setProjectConfiguration(ILaunchConfigurationWorkingCopy workingCopy, IContainer basedir) {
        IMavenProjectRegistry projectManager = MavenPlugin.getMavenProjectRegistry();
        IFile pomFile = basedir.getFile(new Path(IMavenConstants.POM_FILE_NAME));
        IMavenProjectFacade projectFacade = projectManager.create(pomFile, false, new NullProgressMonitor());
        if (projectFacade != null) {
            ResolverConfiguration configuration = projectFacade.getResolverConfiguration();

            String selectedProfiles = configuration.getSelectedProfiles();
            if (selectedProfiles != null && selectedProfiles.length() > 0) {
                workingCopy.setAttribute(MavenLaunchConstants.ATTR_PROFILES, selectedProfiles);
            }
        }
    }

    private IPath getJREContainerPath(IContainer basedir) throws CoreException {
        IProject project = basedir.getProject();
        if (project != null && project.hasNature(JavaCore.NATURE_ID)) {
            IJavaProject javaProject = JavaCore.create(project);
            IClasspathEntry[] entries = javaProject.getRawClasspath();
            for (int i = 0; i < entries.length; i++) {
                IClasspathEntry entry = entries[i];
                if (JavaRuntime.JRE_CONTAINER.equals(entry.getPath().segment(0))) {
                    return entry.getPath();
                }
            }
        }
        return null;
    }
}
