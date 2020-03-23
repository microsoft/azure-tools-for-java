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
/**
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files
 * (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR
 * ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH
 * THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
//package com.microsoft.azuretools.hdinsight.projects;
//
//import org.eclipse.core.internal.resources.Project;
//import org.eclipse.core.resources.IProject;
//import org.eclipse.core.resources.IProjectDescription;
//import org.eclipse.core.resources.ResourcesPlugin;
//import org.eclipse.core.runtime.CoreException;
//import org.eclipse.core.runtime.IConfigurationElement;
//import org.eclipse.core.runtime.IExecutableExtension;
//import org.eclipse.core.runtime.IProgressMonitor;
//import org.eclipse.jdt.core.IJavaElement;
//import org.eclipse.jdt.internal.ui.packageview.PackageExplorerPart;
//import org.eclipse.jdt.internal.ui.wizards.NewElementWizard;
//import org.eclipse.jdt.ui.IPackagesViewPart;
//import org.eclipse.jdt.ui.wizards.NewJavaProjectWizardPageTwo;
//import org.eclipse.jface.dialogs.IDialogSettings;
//import org.eclipse.jface.dialogs.MessageDialog;
//import org.eclipse.jface.viewers.IStructuredSelection;
//import org.eclipse.jface.wizard.IWizardContainer;
//import org.eclipse.jface.wizard.IWizardPage;
//import org.eclipse.jface.wizard.Wizard;
//import org.eclipse.swt.graphics.Image;
//import org.eclipse.swt.graphics.RGB;
//import org.eclipse.swt.widgets.Composite;
//import org.eclipse.swt.widgets.Display;
//import org.eclipse.ui.INewWizard;
//import org.eclipse.ui.IWorkbench;
//import org.eclipse.ui.IWorkbenchPage;
//import org.eclipse.ui.IWorkbenchPart;
//import org.eclipse.ui.IWorkbenchWindow;
//import org.eclipse.ui.IWorkingSet;
//import org.eclipse.ui.PlatformUI;
//import org.eclipse.ui.wizards.newresource.BasicNewProjectResourceWizard;
//
//import com.microsoft.azuretools.hdinsight.Activator;
//
//public class HDInsightsProjectWizard extends NewElementWizard implements INewWizard, IExecutableExtension {
//    private SparkLibraryWizardPage sparkLibraryWizardPage;
//    private NewJavaProjectWizardPageTwo javaPageTwo;
//
//    private String id;
//    private IConfigurationElement fConfigElement;
//
//    public HDInsightsProjectWizard() {
//
//    }
//
//    @Override
//    public void init(IWorkbench arg0, IStructuredSelection arg1) {
//        setWindowTitle("New HDInsights Project");
//    }
//
//    @Override
//    public void addPages() {
//        sparkLibraryWizardPage = new SparkLibraryWizardPage("Library Settings");
//        javaPageTwo = new NewJavaProjectWizardPageTwo(sparkLibraryWizardPage);
//
//        addPage(sparkLibraryWizardPage);
//        addPage(javaPageTwo);
//    }
//
//    @Override
//    public boolean performFinish() {
//        boolean res= super.performFinish();
//        if (res) {
//            final IJavaElement newElement= getCreatedElement();
//
//            IWorkingSet[] workingSets= sparkLibraryWizardPage.getWorkingSets();
//            if (workingSets.length > 0) {
//                PlatformUI.getWorkbench().getWorkingSetManager().addToWorkingSets(newElement, workingSets);
//            }
//
//            BasicNewProjectResourceWizard.updatePerspective(fConfigElement);
////            selectAndReveal(javaPageTwo.getJavaProject().getProject());
//
////            Display.getDefault().asyncExec(new Runnable() {
////                @Override
////                public void run() {
////                    IWorkbenchPart activePart= getActivePart();
////                    if (activePart instanceof IPackagesViewPart) {
////                        PackageExplorerPart view= PackageExplorerPart.openInActivePerspective();
////                        view.tryToReveal(newElement);
////                    }
////                }
////            });
//
//            String projectName= sparkLibraryWizardPage.getProjectName();
//            try {
//            IProject project = getCreatedElement().getJavaProject().getProject(); //ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
//            IProjectDescription description = project.getDescription();
//            String[] natures = description.getNatureIds();
//            String[] newNatures = new String[natures.length + 1];
//            System.arraycopy(natures, 0, newNatures, 0, natures.length);
//            newNatures[natures.length] = HDInsightProjectNature.NATURE_ID;
//            description.setNatureIds(newNatures);
//            project.setDescription(description, null);
//            } catch (Exception ex) {
//                Display.getDefault().syncExec(new Runnable() {
//                    public void run() {
//                        MessageDialog.openError(null,"Error", "Error creating project");
//                    }
//                });
//                Activator.getDefault().log("Error cretaing project", ex);
//            }
//        }
//        return res;
//    }
//
//    private IWorkbenchPart getActivePart() {
//        IWorkbenchWindow activeWindow= getWorkbench().getActiveWorkbenchWindow();
//        if (activeWindow != null) {
//            IWorkbenchPage activePage= activeWindow.getActivePage();
//            if (activePage != null) {
//                return activePage.getActivePart();
//            }
//        }
//        return null;
//    }
//
//    @Override
//    public void setInitializationData(IConfigurationElement parameter, String arg1, Object arg2) throws CoreException {
//        this.id = parameter.getAttribute("id");
//        fConfigElement = parameter;
//    }
//
//    @Override
//    protected void finishPage(IProgressMonitor monitor) throws InterruptedException, CoreException {
//        javaPageTwo.performFinish(monitor);
//
//    }
//
//    @Override
//    public IJavaElement getCreatedElement() {
//        return javaPageTwo.getJavaProject();
//    }
//}
