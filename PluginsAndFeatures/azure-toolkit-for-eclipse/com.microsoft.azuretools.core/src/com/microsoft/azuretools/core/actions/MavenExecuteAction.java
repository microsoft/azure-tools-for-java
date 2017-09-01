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
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.RefreshTab;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.window.Window;
import org.eclipse.m2e.actions.MavenLaunchConstants;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.internal.IMavenConstants;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.internal.launch.LaunchingUtils;
import org.eclipse.m2e.internal.launch.Messages;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

import java.util.ArrayList;
import java.util.concurrent.Callable;

public class MavenExecuteAction {

    private String goalName = null;

    public MavenExecuteAction(String goal) {
        this.goalName = goal;
    }

    public void launch(IContainer basecon, String mode, Callable<?> callback) throws CoreException {
        if (basecon == null) {
            return;
        }

        ILaunchConfiguration launchConfiguration = getLaunchConfiguration(basecon, mode);
        if (launchConfiguration == null) {
            return;
        }

        DebugPlugin.getDefault().addDebugEventListener(new IDebugEventSetListener() {
            @Override
            public void handleDebugEvents(DebugEvent[] events) {
                for (int i = 0; i < events.length; i++) {
                    DebugEvent event = events[i];
                    if (event.getSource() instanceof IProcess && event.getKind() == DebugEvent.TERMINATE) {
                        IProcess process = (IProcess) event.getSource();
                        if (launchConfiguration == process.getLaunch().getLaunchConfiguration()) {
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

        DebugUITools.launch(launchConfiguration, mode);
    }

    private ILaunchConfiguration createLaunchConfiguration(IContainer basedir, String goal) {
        try {
            ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
            ILaunchConfigurationType launchConfigurationType = launchManager
                    .getLaunchConfigurationType(MavenLaunchConstants.LAUNCH_CONFIGURATION_TYPE_ID);

            String launchSafeGoalName = goal.replace(':', '-');

            ILaunchConfigurationWorkingCopy workingCopy = launchConfigurationType.newInstance(null,
                    NLS.bind(Messages.ExecutePomAction_executing, launchSafeGoalName,
                            basedir.getLocation().toString().replace('/', '-')));
            workingCopy.setAttribute(MavenLaunchConstants.ATTR_POM_DIR, basedir.getLocation().toOSString());
            workingCopy.setAttribute(MavenLaunchConstants.ATTR_GOALS, goal);
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

    // IJavaProjects
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

    private ILaunchConfiguration getLaunchConfiguration(IContainer basedir, String mode) {
        if (goalName != null) {
            return createLaunchConfiguration(basedir, goalName);
        }

        ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
        ILaunchConfigurationType launchConfigurationType = launchManager
                .getLaunchConfigurationType(MavenLaunchConstants.LAUNCH_CONFIGURATION_TYPE_ID);

        // scan existing launch configurations
        IPath basedirLocation = basedir.getLocation();
        try {
            ILaunchConfiguration[] launchConfigurations = launchManager
                    .getLaunchConfigurations(launchConfigurationType);
            ArrayList<ILaunchConfiguration> matchingConfigs = new ArrayList<ILaunchConfiguration>();
            for (ILaunchConfiguration configuration : launchConfigurations) {
                try {
                    // substitute variables (may throw exceptions)
                    String workDir = LaunchingUtils.substituteVar(
                            configuration.getAttribute(MavenLaunchConstants.ATTR_POM_DIR, (String) null));
                    if (workDir == null) {
                        continue;
                    }
                    IPath workPath = new Path(workDir);
                    if (basedirLocation.equals(workPath)) {
                        matchingConfigs.add(configuration);
                    }
                } catch (CoreException e) {
                }

            }

            if (matchingConfigs.size() == 1) {
                return matchingConfigs.get(0);
            } else if (matchingConfigs.size() > 1) {
                final IDebugModelPresentation labelProvider = DebugUITools.newDebugModelPresentation();
                ElementListSelectionDialog dialog = new ElementListSelectionDialog(getShell(), //
                        new ILabelProvider() {
                            @Override
                            public Image getImage(Object element) {
                                return labelProvider.getImage(element);
                            }

                            @Override
                            public String getText(Object element) {
                                if (element instanceof ILaunchConfiguration) {
                                    ILaunchConfiguration configuration = (ILaunchConfiguration) element;
                                    try {
                                        return labelProvider.getText(element) + " : "
                                                + configuration.getAttribute(MavenLaunchConstants.ATTR_GOALS, "");
                                    } catch (CoreException ex) {
                                        // ignore
                                    }
                                }
                                return labelProvider.getText(element);
                            }

                            @Override
                            public boolean isLabelProperty(Object element, String property) {
                                return labelProvider.isLabelProperty(element, property);
                            }

                            @Override
                            public void addListener(ILabelProviderListener listener) {
                                labelProvider.addListener(listener);
                            }

                            @Override
                            public void removeListener(ILabelProviderListener listener) {
                                labelProvider.removeListener(listener);
                            }

                            @Override
                            public void dispose() {
                                labelProvider.dispose();
                            }
                        });
                dialog.setElements(matchingConfigs.toArray(new ILaunchConfiguration[matchingConfigs.size()]));
                dialog.setTitle(Messages.ExecutePomAction_dialog_title);
                if (mode.equals(ILaunchManager.DEBUG_MODE)) {
                    dialog.setMessage(Messages.ExecutePomAction_dialog_debug_message);
                } else {
                    dialog.setMessage(Messages.ExecutePomAction_dialog_run_message);
                }
                dialog.setMultipleSelection(false);
                int result = dialog.open();
                labelProvider.dispose();
                return result == Window.OK ? (ILaunchConfiguration) dialog.getFirstResult() : null;
            }

        } catch (CoreException ex) {
        }

        String newName = launchManager.generateLaunchConfigurationName(basedirLocation.lastSegment());
        try {
            ILaunchConfigurationWorkingCopy workingCopy = launchConfigurationType.newInstance(null, newName);
            workingCopy.setAttribute(MavenLaunchConstants.ATTR_POM_DIR, basedirLocation.toString());

            setProjectConfiguration(workingCopy, basedir);

            return workingCopy.doSave();
        } catch (Exception ex) {
        }
        return null;
    }

    private Shell getShell() {
        return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
    }

}
