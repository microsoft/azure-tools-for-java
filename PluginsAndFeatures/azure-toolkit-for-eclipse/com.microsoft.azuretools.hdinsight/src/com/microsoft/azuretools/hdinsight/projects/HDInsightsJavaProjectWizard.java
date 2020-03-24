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

package com.microsoft.azuretools.hdinsight.projects;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.wizards.JavaProjectWizard;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.BuildPathsBlock;
import org.eclipse.jdt.ui.wizards.JavaCapabilityConfigurationPage;
import org.eclipse.jdt.ui.wizards.NewJavaProjectWizardPageOne;
import org.eclipse.jdt.ui.wizards.NewJavaProjectWizardPageTwo;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;

import com.microsoft.azuretools.adauth.StringUtils;
import com.microsoft.azuretools.hdinsight.Activator;
import com.microsoft.tooling.msservices.components.DefaultLoader;

public class HDInsightsJavaProjectWizard extends JavaProjectWizard implements IExecutableExtension {
    public HDInsightJavaPageOne pageOne;
    public HDInsightJavaPageTwo pageTwo;

    private String id;

    public HDInsightsJavaProjectWizard() {
            this(new HDInsightJavaPageOne());
        }

    public HDInsightsJavaProjectWizard(HDInsightJavaPageOne page1) {
        this(page1, new HDInsightJavaPageTwo(page1));
    }

    public HDInsightsJavaProjectWizard(HDInsightJavaPageOne page1, HDInsightJavaPageTwo page2) {
        super(page1, page2);
        this.pageOne = page1;
        this.pageTwo = page2;
    }


    @Override
    public boolean canFinish() {
        return CreateProjectUtil.checkHDInsightProjectNature(pageTwo) & super.canFinish();
    }

    @Override
    public boolean performFinish() {
        try {
            CreateProjectUtil.createSampleFile(this.id,
                    this.pageOne.getProjectName(),
                    pageOne.sparkLibraryOptionsPanel.getUsingMaven(),
                    pageOne.sparkLibraryOptionsPanel.getSparkVersion());
        } catch (CoreException e) {
            Activator.getDefault().log("Create HDInsight project error", e);
        }

        // Configure Java project first and then enable Maven nature, otherwise the classpath will be overwritten
        boolean result = super.performFinish();
        if (pageOne.sparkLibraryOptionsPanel.getUsingMaven()) {
            try {
                ProgressMonitorDialog dialog = new ProgressMonitorDialog(getShell());

                dialog.run(true, true, new IRunnableWithProgress() {
                    @Override
                    public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                        try {
                            MavenPlugin.getProjectConfigurationManager().enableMavenNature(pageTwo.getJavaProject().getProject(),
                                    new ResolverConfiguration(),
                                    monitor);
                        } catch (CoreException e) {
                            Activator.getDefault().log("Error in enabling Maven nature", e);
                        }
                    }
                });
            } catch (InvocationTargetException | InterruptedException e1) {
                Activator.getDefault().log("Fail to enable Maven feature", e1);
            }
        }

        return result;
    }

    @Override
    public void setInitializationData(IConfigurationElement parameter, String arg1, Object arg2) {
        super.setInitializationData(parameter, arg1, arg2);
        this.id = parameter.getAttribute("id");
    }

    static class HDInsightJavaPageOne extends NewJavaProjectWizardPageOne {
        private SparkLibraryOptionsPanel sparkLibraryOptionsPanel;

        protected HDInsightJavaPageOne() {
            super();
//            setTitle("Libraries Settings");
//            setDescription("Libraries Settings");
//            setPageComplete(true);
        }


        @Override
        public boolean canFlipToNextPage() {
            if (!sparkLibraryOptionsPanel.getUsingMaven()) {
                final String jarPathString = sparkLibraryOptionsPanel.getSparkLibraryPath();
                if(StringUtils.isNullOrEmpty(jarPathString)) {
                    return false;
                }
            }

            return super.canFlipToNextPage();
        }

        @Override
        public IClasspathEntry[] getDefaultClasspathEntries() {
            final IClasspathEntry[] entries = super.getDefaultClasspathEntries();
            final IClasspathEntry[] newEntries = new IClasspathEntry[entries.length + 1];
            Display.getDefault().syncExec(new Runnable() {
                @Override
                public void run() {
                    if (!sparkLibraryOptionsPanel.getUsingMaven()) {
                        final String jarPathString = sparkLibraryOptionsPanel.getSparkLibraryPath();
                        if (StringUtils.isNullOrEmpty(jarPathString)) {
                            DefaultLoader.getUIHelper().showError("Spark Library Path cannot be null",
                                    "Spark Project Settings");
                        } else {
                            IPath jarPath = new Path(jarPathString);
                            IClasspathEntry sparkEntry = JavaCore.newLibraryEntry(jarPath, null, null);
                            System.arraycopy(entries, 0, newEntries, 0, entries.length);
                            newEntries[entries.length] = sparkEntry;
                        }
                    }
                }
            });
            return newEntries[0] == null ? entries : newEntries;
        }

        @Override
        public void createControl(Composite parent) {
            initializeDialogUnits(parent);

            final Composite composite= new Composite(parent, SWT.NULL);
            composite.setFont(parent.getFont());
            composite.setLayout(initGridLayout(new GridLayout(1, false), true));
            composite.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));

            // create UI elements
            Control nameControl= createNameControl(composite);
            nameControl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            Control locationControl= createLocationControl(composite);
            locationControl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            Control jreControl= createJRESelectionControl(composite);
            jreControl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            Control layoutControl= createProjectLayoutControl(composite);
            layoutControl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            Control sparkControl = createSparkControl(composite);
            sparkControl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            Control workingSetControl= createWorkingSetControl(composite);
            workingSetControl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            Control infoControl= createInfoControl(composite);
            infoControl.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            setControl(composite);
        }

        private Control createSparkControl(Composite composite) {
            Group sparkGroup = new Group(composite, SWT.NONE);
            sparkGroup.setFont(composite.getFont());
            sparkGroup.setText("Spark Library");
            sparkGroup.setLayout(new GridLayout(1, false));

            sparkLibraryOptionsPanel = new SparkLibraryOptionsPanel(this, sparkGroup, SWT.NONE);
            return sparkGroup;
        }

        private GridLayout initGridLayout(GridLayout layout, boolean margins) {
            layout.horizontalSpacing= convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
            layout.verticalSpacing= convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
            if (margins) {
                layout.marginWidth= convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
                layout.marginHeight= convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
            } else {
                layout.marginWidth= 0;
                layout.marginHeight= 0;
            }
            return layout;
        }
    }

    static class HDInsightJavaPageTwo extends NewJavaProjectWizardPageTwo {
        public HDInsightJavaPageTwo(NewJavaProjectWizardPageOne mainPage) {
            super(mainPage);
        }

        public void configureJavaProject(String newProjectCompliance, IProgressMonitor monitor) throws CoreException, InterruptedException {
            if(monitor == null) {
                monitor = new NullProgressMonitor();
            }
            byte nSteps = 6;
            ((IProgressMonitor)monitor).beginTask(NewWizardMessages.JavaCapabilityConfigurationPage_op_desc_java, nSteps);
            try {
                IProject project = addHDInsightNature(monitor);
                Method method = JavaCapabilityConfigurationPage.class.getDeclaredMethod("getBuildPathsBlock");
                method.setAccessible(true);
                Object r = method.invoke(this);
                ((BuildPathsBlock) r).configureJavaProject(newProjectCompliance, new SubProgressMonitor((IProgressMonitor)monitor, 5));

                addMoreSourcetoClassPath();

                HDInsightsJavaProjectWizard parent = (HDInsightsJavaProjectWizard)getWizard();
            } catch (OperationCanceledException | NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                throw new InterruptedException();
            } finally {
                ((IProgressMonitor)monitor).done();
            }
        }

        private void addMoreSourcetoClassPath() throws JavaModelException {
            HDInsightJavaPageOne previousPage = (HDInsightJavaPageOne)getPreviousPage();
            if (previousPage.sparkLibraryOptionsPanel.getUsingMaven()) {
                CreateProjectUtil.removeSourceFolderfromClassPath(this.getJavaProject(), "src");
                CreateProjectUtil.addSourceFoldertoClassPath(this.getJavaProject(), "src/main/java");
            }

        }

        private IProject addHDInsightNature(IProgressMonitor monitor) throws CoreException {
            if (monitor != null && monitor.isCanceled()) {
                  throw new OperationCanceledException();
            }
            IProject project = this.getJavaProject().getProject();
            if (!project.hasNature(JavaCore.NATURE_ID)) {
                IProjectDescription description = project.getDescription();
                String[] natures = description.getNatureIds();
                String[] newNatures = new String[natures.length + 2];
                System.arraycopy(natures, 0, newNatures, 0, natures.length);
                newNatures[natures.length] = HDInsightProjectNature.NATURE_ID;
                newNatures[natures.length + 1] = JavaCore.NATURE_ID;
                description.setNatureIds(newNatures);
                project.setDescription(description, null);
            } else {
                monitor.worked(1);
            }
            return project;
        }
    }
}
