/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.applicationinsights.ui.config;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.microsoft.applicationinsights.preference.ApplicationInsightsResource;
import com.microsoft.applicationinsights.preference.ApplicationInsightsResourceRegistry;
import com.microsoft.applicationinsights.ui.activator.Activator;
import com.microsoft.azuretools.core.applicationinsights.AILibraryHandler;
import com.microsoft.azuretools.core.applicationinsights.WebPropertyTester;

public class AIResourceChangeListener implements IResourceChangeListener {

    @Override
    public void resourceChanged(IResourceChangeEvent event) {
        IResourceDelta resourcedelta = event.getDelta();
        IResourceDeltaVisitor visitor = new IResourceDeltaVisitor() {
            @Override
            public boolean visit(IResourceDelta delta) throws CoreException {
                IProject project = delta.getResource().getProject();
                // Check if project is of required nature
                if (project != null && project.isOpen() && WebPropertyTester.isWebProj(project)) {
                    handleResourceChange(delta);
                }
                return true;
            }
        };
        try {
            resourcedelta.accept(visitor);
            WorkspaceJob job = new WorkspaceJob(Messages.refreshJobName) {
                @Override
                public IStatus runInWorkspace(IProgressMonitor monitor) throws CoreException {
                    return Status.OK_STATUS;
                }
            };
            job.schedule();
        } catch (CoreException e) {
            Activator.getDefault().log(Messages.resChangeErr, e);
        }
    }

    /**
     * Handles project import or open event.
     *
     * @param delta
     *            .
     */
    private void handleResourceChange(IResourceDelta delta) {
        IResource resource = delta.getResource();
        IProject project = resource.getProject();
        // If project gets opened
        if (resource.getType() == IResource.PROJECT && (delta.getFlags() & IResourceDelta.OPEN) != 0) {
            initializeAIRegistry(project);
        }
    }

    /**
     * initializeAIRegistry.
     */
    public static void initializeAIRegistry(IProject iproject) {
        try {
            if (iproject.isOpen() && WebPropertyTester.isWebProj(iproject)) {
                String aiXMLPath;
                if (iproject.hasNature(Messages.natMaven)) {
                    aiXMLPath = Messages.aiXMLPathMaven;
                } else {
                    aiXMLPath = Messages.aiXMLPath;
                }
                AILibraryHandler handler = new AILibraryHandler();
                /*
                 * If ApplicationInsights.xml does not exits, then application insights might not have been configured.
                 * It will thrown FNF exception in that case, just catch it and log. Note : file.exists() check is not
                 * useful in this case as during project import generally file.exists() return false.
                 */
                handler.parseAIConfXmlPath(iproject.getFile(aiXMLPath).getLocation().toOSString());
                String key = handler.getAIInstrumentationKey();
                if (key != null && !key.isEmpty()) {
                    String unknown = com.microsoft.applicationinsights.preference.Messages.unknown;
                    List<ApplicationInsightsResource> list = ApplicationInsightsResourceRegistry
                            .getAppInsightsResrcList();
                    ApplicationInsightsResource resourceToAdd = new ApplicationInsightsResource(key, key, unknown,
                            unknown, unknown, unknown, false);
                    if (!list.contains(resourceToAdd)) {
                        ApplicationInsightsResourceRegistry.getAppInsightsResrcList().add(resourceToAdd);
                    }
                }
            }
        } catch (Exception ex) {
            Activator.getDefault().log(Messages.resChangeErr);
        }
    }

    /**
     * Method scans all open Maven or Dynamic web projects form workspace and returns name of project who is using
     * specific key.
     */
    public static String getProjectNameAsPerKey(String keyToRemove) {
        String name = "";
        try {
            IWorkspace workspace = ResourcesPlugin.getWorkspace();
            IWorkspaceRoot root = workspace.getRoot();
            for (IProject iproject : root.getProjects()) {
                if (iproject.isOpen() && WebPropertyTester.isWebProj(iproject)) {
                    String aiXMLPath;
                    String webXMLPath;
                    if (iproject.hasNature(Messages.natMaven)) {
                        aiXMLPath = Messages.aiXMLPathMaven;
                        webXMLPath = Messages.webxmlPathMaven;
                    } else {
                        aiXMLPath = Messages.aiXMLPath;
                        webXMLPath = Messages.webxmlPath;
                    }
                    AILibraryHandler handler = new AILibraryHandler();
                    IFile aiFile = iproject.getFile(aiXMLPath);
                    IFile webFile = iproject.getFile(webXMLPath);
                    if (aiFile.exists() && webFile.exists()) {
                        handler.parseWebXmlPath(webFile.getLocation().toOSString());
                        handler.parseAIConfXmlPath(aiFile.getLocation().toOSString());
                        // if application insights configuration is enabled.
                        if (handler.isAIWebFilterConfigured()) {
                            String key = handler.getAIInstrumentationKey();
                            if (key != null && !key.isEmpty() && key.equals(keyToRemove)) {
                                return iproject.getName();
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            Activator.getDefault().log(Messages.genKeyListErr, ex);
        }
        return name;
    }
}
