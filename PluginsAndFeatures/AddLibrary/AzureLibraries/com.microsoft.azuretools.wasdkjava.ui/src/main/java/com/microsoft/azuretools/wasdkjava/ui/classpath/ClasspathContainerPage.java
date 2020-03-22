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
package com.microsoft.azuretools.wasdkjava.ui.classpath;

import java.io.File;
import java.net.URL;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.ui.wizards.IClasspathContainerPage;
import org.eclipse.jdt.ui.wizards.IClasspathContainerPageExtension;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.Bundle;

import com.microsoft.azuretools.core.utils.PluginUtil;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.microsoft.azuretools.wasdkjava.ui.Activator;

/**
 * Classpath container page.
 *
 */
public class ClasspathContainerPage extends WizardPage
        implements IClasspathContainerPage, IClasspathContainerPageExtension {

    private IPath entry;
    private Combo combo;
    private String curVersion;
    private boolean finishVal = true;
    private String libLocation = Messages.notFound;
    private Label location;
    private Button depCheck;

    /**
     * Default constructor.
     */
    public ClasspathContainerPage() {
        super(Messages.title);
        // set title for page
        setTitle(Messages.title);
        // set description for page
        setDescription(Messages.desc);
        entry = new Path(Messages.containerID);
    }

    /**
     * Draws controls for page.
     *
     * @param parent.
     */
    @Override
    public void createControl(Composite parent) {
        Composite container = new Composite(parent, SWT.None);
        GridLayout gridLayout = new GridLayout(2, false);
        container.setLayout(gridLayout);

        // Label for version
        Label lblVersion = new Label(container, SWT.None);
        lblVersion.setText(Messages.lblVersion);
        // Combo to hold the versions
        combo = new Combo(container, SWT.READ_ONLY);
        combo.addSelectionListener(new ComboSelectionListener());
        // get bundles for the sdk
        Bundle[] bundles = Platform.getBundles(Messages.sdkID, null);
        if (bundles == null || bundles.length == 0) {
            // if bundles is null set an error msg
            setErrorMessage(Messages.libNotAvail);
            finishVal = false;
        } else {
            try {
                // populate the combo box with available versions of sdk
                populateComboBox(bundles);
                Bundle bundle = bundles[combo.getSelectionIndex()];

                if (bundle == null) {
                    finishVal = false;
                    // if bundle is null set an error msg
                    setErrorMessage(Messages.verNotAvail);
                } else {
                    // locate sdk jar in bundle
                    URL url = FileLocator.find(bundle, new Path(Messages.sdkJar), null);
                    if (url == null) {
                        finishVal = false;
                        setErrorMessage(Messages.verNotAvail);
                    } else {
                        // if jar is found then resolve url and get the location
                        url = FileLocator.resolve(url);
                        File loc = new File(url.getPath());
                        libLocation = loc.getAbsolutePath();
                        setErrorMessage(null);
                    }
                }
            } catch (Exception e) {
                // if exception is thrown set an error msg.
                setErrorMessage(Messages.verNotAvail);
                finishVal = false;
            }
        }
        // label for location
        Label lblLoc = new Label(container, SWT.None);
        GridData gridData = new GridData();
        gridData.verticalSpan = 4;
        gridData.verticalAlignment = GridData.BEGINNING;
        lblLoc.setLayoutData(gridData);
        lblLoc.setText(Messages.lblLocation);
        // label to hold the sdk jar path
        location = new Label(container, SWT.WRAP);
        gridData = new GridData();
        gridData.verticalSpan = 4;
        gridData.widthHint = 400;
        gridData.verticalAlignment = GridData.BEGINNING;
        location.setLayoutData(gridData);
        location.setText(libLocation);

        createDepCheckBox(container);

        setControl(container);
    }

    /**
     * Populates the combo with available versions of sdk.
     */
    private void populateComboBox(Bundle[] bundles) {
        // iterate over bundles and add an entry to combo
        for (Bundle bundle : bundles) {
            combo.add(String.format(Messages.version1, Integer.toString(bundle.getVersion().getMajor()),
                    Integer.toString(bundle.getVersion().getMinor()),
                    Integer.toString(bundle.getVersion().getMicro())));
        }
        // if current version can not be determined
        // then set the first entry as selected.
        if (curVersion == null || curVersion.isEmpty()) {
            combo.select(0);
        } else {
            combo.setText(String.format(Messages.version2, curVersion));
        }
    }

    @Override
    public void initialize(IJavaProject proj, IClasspathEntry[] classpathEntry) {
    }

    /**
     * Handles finish click.
     */
    @Override
    public boolean finish() {
        if (finishVal) {
            // append the version to sdk container id.
            entry = entry.append(getSelVersion());
            /*
             * Save state of Deployment Assembly check box every time, as user may say immediate edit more than once.
             */
            Activator.setDeploymentAssemblyState(depCheck.getSelection());
            if (isEdit()) {
                // edit scenario.
                configureClasspathEntries();
            } else {
                // Azure libraries getting added for the first time for specific project
                Bundle bundle = Activator.getDefault().getBundle();
                if (bundle != null) {
                    PluginUtil.showBusy(true, getShell());
                    AppInsightsClient.create("Azure Libraries", bundle.getVersion().toString());
                    PluginUtil.showBusy(false, getShell());
                }
            }
        }
        return finishVal;
    }

    /**
     * Returns the version of selected entry in combo box.
     *
     * @return version.
     */
    private String getSelVersion() {
        Bundle[] bundles = Platform.getBundles(Messages.sdkID, null);
        Bundle bundle = null;
        String version = "";
        if (bundles != null) {
            bundle = bundles[combo.getSelectionIndex()];
            version = String.format("%s.%s.%s", Integer.toString(bundle.getVersion().getMajor()),
                    Integer.toString(bundle.getVersion().getMinor()), Integer.toString(bundle.getVersion().getMicro()));
        }
        return version;
    }

    @Override
    public IClasspathEntry getSelection() {
        IClasspathEntry classPathEntry = null;
        if (depCheck.getSelection()) {
            IClasspathAttribute[] attr = new IClasspathAttribute[1];
            attr[0] = JavaCore.newClasspathAttribute(Messages.jstDep, "/WEB-INF/lib");
            classPathEntry = JavaCore.newContainerEntry(entry, null, attr, true);
        } else {
            classPathEntry = JavaCore.newContainerEntry(entry);
        }
        return classPathEntry;
    }

    @Override
    public void setSelection(IClasspathEntry selEntry) {
        if (selEntry != null) {
            curVersion = selEntry.getPath().segment(1);
        }
    }

    /**
     * Listener class for Combo box.
     *
     */
    private class ComboSelectionListener implements SelectionListener {

        @Override
        public void widgetDefaultSelected(SelectionEvent arg0) {

        }

        @Override
        public void widgetSelected(SelectionEvent arg0) {
            if (arg0.getSource() instanceof Combo) {
                try {
                    Bundle[] bundles = Platform.getBundles(Messages.sdkID, null);

                    Bundle bundle = bundles[combo.getSelectionIndex()];
                    // locate the sdk jar in bundle
                    URL url = FileLocator.find(bundle, new Path(Messages.sdkJar), null);
                    // if sdk jar not found then set an error msg
                    if (url == null) {
                        libLocation = Messages.notFound;
                        location.setText(libLocation);
                        setErrorMessage(Messages.verNotAvail);
                        finishVal = false;
                    } else {
                        // if jar is found then resolve url and get the location
                        url = FileLocator.resolve(url);
                        File loc = new File(url.getPath());
                        libLocation = loc.getAbsolutePath();
                        location.setText(libLocation);
                        setErrorMessage(null);
                        finishVal = true;
                    }
                } catch (Exception ex) {
                    setErrorMessage(Messages.verNotAvail);
                    finishVal = false;
                }
            }
        }

    }

    /**
     * Method creates Include in the project deployment assembly check box.
     */
    private void createDepCheckBox(Composite container) {
        boolean containerIdPresent = false;
        depCheck = new Button(container, SWT.CHECK);
        GridData gridData = new GridData();
        gridData.grabExcessHorizontalSpace = true;
        gridData.horizontalIndent = 10;
        gridData.verticalIndent = 10;
        gridData.horizontalSpan = 2;
        gridData.horizontalAlignment = SWT.FILL;
        depCheck.setText(Messages.depChkBox);
        depCheck.setLayoutData(gridData);
        if (isEdit()) {
            // Edit library scenario
            try {
                IJavaProject proj1 = JavaCore.create(getSelectedProject());
                // Iterate over class path entries.
                for (int i = 0; i < proj1.getRawClasspath().length; i++) {
                    /*
                     * check if class path contains our library's container Id If contains then it's not a case of
                     * immediate edit
                     */
                    if (proj1.getRawClasspath()[i].toString().contains(Messages.containerID)) {
                        containerIdPresent = true;
                        for (int j = 0; j < proj1.getRawClasspath()[i].getExtraAttributes().length; j++) {
                            if (proj1.getRawClasspath()[i].getExtraAttributes()[j].getName()
                                    .equalsIgnoreCase(Messages.jstDep)) {
                                depCheck.setSelection(true);
                                break;
                            }
                            depCheck.setSelection(false);
                        }
                        if (proj1.getRawClasspath()[i].getExtraAttributes().length == 0) {
                            depCheck.setSelection(false);
                        }
                    }
                }
                /*
                 * If contains then it's not a case of immediate edit retrieve state of deployment assembly check box
                 * which was saved when user clicks on Finish button.
                 */
                if (!containerIdPresent) {
                    depCheck.setSelection(Activator.geteploymentAssemblyState());
                }
            } catch (Exception e) {
                Activator.getDefault().log(e.getMessage(), e);
            }

        } else {
            // Add library scenario
            depCheck.setSelection(true);
        }
    }

    /**
     * @return current window is edit or not.
     */
    private boolean isEdit() {
        return getWizard().getWindowTitle().equals(Messages.edtLbrTtl);
    }

    /**
     * returns currently selected project in workspace.
     *
     * @return IProject
     */
    private IProject getSelectedProject() {
        IWorkbench workbench = PlatformUI.getWorkbench();
        IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
        ISelectionService service = window.getSelectionService();
        ISelection selection = service.getSelection();
        Object element = null;
        IResource resource;
        IProject selProject = null;
        if (selection instanceof IStructuredSelection) {
            IStructuredSelection structuredSel = (IStructuredSelection) selection;
            element = structuredSel.getFirstElement();
        }
        if (element instanceof IProject) {
            resource = (IResource) element;
            selProject = resource.getProject();
        } else if (element instanceof IJavaProject) {
            IJavaProject proj = ((IJavaElement) element).getJavaProject();
            selProject = proj.getProject();
        }
        return selProject;
    }

    /**
     * Method adds entries into .classpath file.
     */
    private void configureClasspathEntries() {
        IJavaProject proj1 = JavaCore.create(getSelectedProject());
        IClasspathEntry[] entries;
        try {
            entries = proj1.getRawClasspath();
            IClasspathEntry[] newentries = new IClasspathEntry[entries.length];

            for (int i = 0; i < entries.length; i++) {
                if (entries[i].toString().contains(Messages.containerID)) {
                    if (depCheck.getSelection()) {
                        IClasspathAttribute[] attr = new IClasspathAttribute[1];
                        attr[0] = JavaCore.newClasspathAttribute(Messages.jstDep, "/WEB-INF/lib");
                        newentries[i] = JavaCore.newContainerEntry(entry, null, attr, true);
                    } else {
                        newentries[i] = JavaCore.newContainerEntry(entry);
                    }
                } else {
                    newentries[i] = entries[i];
                }
            }
            proj1.setRawClasspath(newentries, null);
        } catch (Exception e) {
            Activator.getDefault().log(e.getMessage(), e);
        }
    }
}
