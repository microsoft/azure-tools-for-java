/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.hdinsight.projects;

import java.awt.Dialog;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;

import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.internal.ui.wizards.JavaProjectWizard;
import org.eclipse.jdt.ui.wizards.NewJavaProjectWizardPageOne;
import org.eclipse.jdt.ui.wizards.NewJavaProjectWizardPageTwo;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.azure.hdinsight.projects.SparkVersion;
import com.microsoft.azuretools.core.utils.PluginUtil;
import com.microsoft.azuretools.hdinsight.Activator;

public class HDInsightsScalaProjectWizard extends JavaProjectWizard implements IExecutableExtension {
    private SparkVersion sparkVersion;
    private boolean isUsingMaven = true;
    private String id;
    public static NewJavaProjectWizardPageOne hdInsightScalaPageOne;
    public NewJavaProjectWizardPageTwo hdInsightScalaPageTwo;

    public HDInsightsScalaProjectWizard() {
        this(
                PluginUtil.forceInstallPluginUsingMarketPlaceAsync(
                        PluginUtil.scalaPluginSymbolicName,
                        PluginUtil.scalaPluginMarketplaceURL,
                        PluginUtil.scalaPluginManualInstallURL),
                setFocusToInstallationWindow(),
                hdInsightScalaPageOne = createHDInsightScalaPageOne(),
                createHDInsightScalaPageTwo(hdInsightScalaPageOne)
                );
    }

    public HDInsightsScalaProjectWizard(boolean checkScalaPlugin, boolean setFocusToInstallationWindow, NewJavaProjectWizardPageOne hdInsightScalaPageOne, NewJavaProjectWizardPageTwo hdInsightScalaPageTwo) {
        this(hdInsightScalaPageOne, hdInsightScalaPageTwo);
    }

    public HDInsightsScalaProjectWizard(NewJavaProjectWizardPageOne page1, NewJavaProjectWizardPageTwo page2) {
        super(page1, page2);
        hdInsightScalaPageOne = page1;
        hdInsightScalaPageTwo = page2;
        setWindowTitle("New HDInsight Scala Project");

        page2.setTitle("HDInsight Spark Project Library Settings");
        page2.setDescription("Define the project build settings.");
    }

    public void setUsingMaven(boolean val) {
        isUsingMaven = val;
    }

    public boolean getUsingMaven() {
        return isUsingMaven;
    }

    public void setSparkVersion(SparkVersion val) {
        sparkVersion = val;
    }

    public Optional<String> getScalaVersion() {
        return Optional.ofNullable(sparkVersion).map(SparkVersion::getScalaVersion);
    }

    private static boolean setFocusToInstallationWindow() {
        // Unfortunately, the marketplace client windows can not get the focus so do the trick here since the parent is the project wizard
        if (PluginUtil.checkPlugInInstallation(PluginUtil.scalaPluginSymbolicName) != true) {
            Shell parent = PluginUtil.getParentShell();

            int isModeless = parent.getStyle() & SWT.MODELESS;
            if (isModeless == 0) {
                parent.close();
            }
        }

        return true;
    }

    private static NewJavaProjectWizardPageOne createHDInsightScalaPageOne() {
        Class<?> classHDInsightScalaPageOne;
        Constructor<?> ctorHDInsightScalaPageOne;
        NewJavaProjectWizardPageOne result = null;

        try {
            classHDInsightScalaPageOne = Class.forName("com.microsoft.azuretools.hdinsight.projects.HDInsightScalaPageOne");
            ctorHDInsightScalaPageOne =  classHDInsightScalaPageOne.getConstructor();

            result = (NewJavaProjectWizardPageOne)ctorHDInsightScalaPageOne.newInstance();
        } catch (Exception ignore) {

        }

        return result;
    }

    private static NewJavaProjectWizardPageTwo createHDInsightScalaPageTwo(Object objHDInsightScalaPageOne) {
        Class<?> classHDInsightScalaPageTwo;
        Class<?> classHDInsightScalaPageOne;
        Constructor<?> ctorHDInsightScalaPageTwo;
        NewJavaProjectWizardPageTwo result = null;

        try {
            classHDInsightScalaPageOne = Class.forName("com.microsoft.azuretools.hdinsight.projects.HDInsightScalaPageOne");
            classHDInsightScalaPageTwo = Class.forName("com.microsoft.azuretools.hdinsight.projects.HDInsightScalaPageTwo");
            ctorHDInsightScalaPageTwo = classHDInsightScalaPageTwo.getConstructor(classHDInsightScalaPageOne);

            result = (NewJavaProjectWizardPageTwo) ctorHDInsightScalaPageTwo.newInstance(objHDInsightScalaPageOne);
        } catch (Exception ignore) {

        }

        return result;
    }

    @Override
    public void setInitializationData(IConfigurationElement parameter, String arg1, Object arg2) {
        super.setInitializationData(parameter, arg1, arg2);
        this.id = parameter.getAttribute("id");

    }

    @Override
    public boolean canFinish() {
        return CreateProjectUtil.checkHDInsightProjectNature(hdInsightScalaPageTwo) && super.canFinish();
    }

    @Override
    public boolean performFinish() {
        try {
            CreateProjectUtil.createSampleFile(this.id, this.hdInsightScalaPageOne.getProjectName(), this.isUsingMaven, this.sparkVersion);
        } catch (CoreException e) {
            Activator.getDefault().log("Create HDInsight project error", e);
        }

        // Configure Java project first and then enable Maven nature, otherwise the classpath will be overwritten
        boolean result = super.performFinish();
        if (isUsingMaven) {
            try {
                ProgressMonitorDialog dialog = new ProgressMonitorDialog(getShell());

                dialog.run(true,  true,  new IRunnableWithProgress() {
                    @Override
                    public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                        try {
                            MavenPlugin.getProjectConfigurationManager().enableMavenNature(hdInsightScalaPageTwo.getJavaProject().getProject(),
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
}
