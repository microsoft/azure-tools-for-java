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
//import org.eclipse.core.resources.IWorkspace;
//import org.eclipse.core.resources.IWorkspaceRoot;
//import org.eclipse.core.resources.ResourcesPlugin;
//import org.eclipse.core.runtime.CoreException;
//import org.eclipse.core.runtime.IProgressMonitor;
//import org.eclipse.jdt.ui.wizards.JavaCapabilityConfigurationPage;
//import org.eclipse.jdt.ui.wizards.NewJavaProjectWizardPageOne;
//import org.eclipse.jface.wizard.WizardPage;
//import org.eclipse.swt.SWT;
//import org.eclipse.swt.layout.GridData;
//import org.eclipse.swt.layout.GridLayout;
//import org.eclipse.swt.widgets.Composite;
//import org.eclipse.swt.widgets.Label;
//import org.eclipse.swt.widgets.Text;
//
//public class SparkLibraryWizardPage extends NewJavaProjectWizardPageOne /*WizardPage*/ {
//    private SparkLibraryOptionsPanel sparkLibraryOptionsPanel;
//
//    protected SparkLibraryWizardPage(String pageName) {
//        super();
//        setTitle("Libraries Settings");
//        setDescription("Libraries Settings");
//        setPageComplete(true);
//    }
//
//    @Override
//    public void createControl(Composite parent) {
//        final IWorkspace workspace = ResourcesPlugin.getWorkspace();
//        final IWorkspaceRoot root = workspace.getRoot();
//
//        //display help contents
////        PlatformUI.getWorkbench().getHelpSystem().setHelp(parent.getShell(),
////                "com.persistent.winazure.eclipseplugin." +
////                "windows_azure_project");
//
////        GridLayout gridLayout = new GridLayout();
////        gridLayout.numColumns = 2;
////        GridData gridData = new GridData();
////        gridData.grabExcessHorizontalSpace = true;
////        Composite container = new Composite(parent, SWT.NONE);
////
////        container.setLayout(gridLayout);
////        container.setLayoutData(gridData);
//
//        super.createControl(parent);
//        Composite container = (Composite) getControl();
//        Composite composite = new Composite(container, SWT.NONE);
//        GridLayout gridLayout = new GridLayout();
//        gridLayout.numColumns = 2;
//        GridData gridData = new GridData();
//        gridData.grabExcessHorizontalSpace = true;
//        composite.setLayout(gridLayout);
//        composite.setLayoutData(gridData);
//        Label lblProjName = new Label(composite, SWT.LEFT | SWT.TOP);
//        lblProjName.setText("Spark SDK:");
//
//        sparkLibraryOptionsPanel = new SparkLibraryOptionsPanel(composite, SWT.NONE);
////        Text textProjName = new Text(container, SWT.SINGLE | SWT.BORDER);
////        GridData gridData = new GridData();
////        gridData.widthHint = 330;
////        gridData.horizontalAlignment = SWT.FILL;
////        gridData.grabExcessHorizontalSpace = true;
////        textProjName.setLayoutData(gridData);
//
//        setControl(container);
//
//    }
//
////    @Override
////    public void performFinish(IProgressMonitor monitor) throws CoreException, InterruptedException {
////        try {
////            monitor.beginTask(NewWizardMessages.NewJavaProjectWizardPageTwo_operation_create, 3);
////            if(this.fCurrProject == null) {
////                this.updateProject(new SubProgressMonitor(monitor, 1));
////            }
////
////            String newProjectCompliance = this.fKeepContent?null:this.fFirstPage.getCompilerCompliance();
////            this.configureJavaProject(newProjectCompliance, new SubProgressMonitor(monitor, 2));
////        } finally {
////            monitor.done();
////            this.fCurrProject = null;
////            if(this.fIsAutobuild != null) {
////                CoreUtility.setAutoBuilding(this.fIsAutobuild.booleanValue());
////                this.fIsAutobuild = null;
////            }
////
////        }
////
////    }
//
//}
