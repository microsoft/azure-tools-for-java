/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.hdinsight.projects;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.scalaide.ui.ScalaImages;
import org.scalaide.ui.internal.wizards.NewScalaProjectWizardPageOne;

import com.microsoft.azure.hdinsight.projects.SparkVersion;
import com.microsoft.tooling.msservices.components.DefaultLoader;

class HDInsightScalaPageOne extends NewScalaProjectWizardPageOne {

    private SparkLibraryOptionsPanel sparkLibraryOptionsPanel;
    private HDInsightsScalaProjectWizard parent;

    public HDInsightScalaPageOne() {
        super();

    }

    public void setParent(HDInsightsScalaProjectWizard parent) {
        this.parent = parent;
    }

    @Override
    public boolean canFlipToNextPage() {
        if (!sparkLibraryOptionsPanel.getUsingMaven()) {
            final String jarPathString = sparkLibraryOptionsPanel.getSparkLibraryPath();
            if(StringUtils.isEmpty(jarPathString)) {
                return false;
            }
        }

        if (parent != null && sparkLibraryOptionsPanel != null) {
            parent.setUsingMaven(sparkLibraryOptionsPanel.getUsingMaven());
            parent.setSparkVersion(sparkLibraryOptionsPanel.getSparkVersion());
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
                if (sparkLibraryOptionsPanel != null && !sparkLibraryOptionsPanel.getUsingMaven()) {
                    final String jarPathString = sparkLibraryOptionsPanel.getSparkLibraryPath();
                    if (StringUtils.isEmpty(jarPathString)) {
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
        setImageDescriptor(ScalaImages.SCALA_PROJECT_WIZARD());
        setTitle("Create a Scala project");
        setDescription("Create a Scala project in the workspace or in an external location.");

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

        this.parent = (HDInsightsScalaProjectWizard)this.getWizard();
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
