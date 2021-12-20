/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.eclipse.function.wizard;

import com.microsoft.azuretools.appservice.Activator;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.internal.ui.wizards.NewElementWizard;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.wizards.newresource.BasicNewResourceWizard;

public class NewFunctionClassCreationWizard extends NewElementWizard implements INewWizard {
    private IWorkbench workbench;
    private IStructuredSelection selection;
    private NewFunctionClassWizardPage page;

    public NewFunctionClassCreationWizard() {
        this.setWindowTitle("Create Azure function class");
        setDefaultPageImageDescriptor(Activator.getImageDescriptor("icons/functionapp.png"));
    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection currentSelection) {
        this.workbench = workbench;
        selection = currentSelection;
    }

    public IStructuredSelection getSelection() {
        return selection;
    }

    public IWorkbench getWorkbench() {
        return workbench;
    }

    protected void selectAndReveal(IResource newResource) {
        BasicNewResourceWizard.selectAndReveal(newResource, workbench.getActiveWorkbenchWindow());
    }

    /*
     * @see Wizard#createPages
     */
    @Override
    public void addPages() {
        super.addPages();
        if (page == null) {
            page = new NewFunctionClassWizardPage();
            page.setWizard(this);
            page.init(getSelection());
        }
        addPage(page);
    }

    public IJavaElement getCreatedElement() {
        return page.getCreatedType();
    }

    @Override
    public boolean performFinish() {
        warnAboutTypeCommentDeprecation();
        boolean res = super.performFinish();
        if (res) {
            IResource resource = page.getModifiedResource();
            if (resource != null) {
                selectAndReveal(resource);
                openResource((IFile) resource);
            }
        }
        return res;
    }

    protected void finishPage(IProgressMonitor monitor) throws InterruptedException, CoreException {
        page.createType(monitor); // use the full progress monitor
    }
}