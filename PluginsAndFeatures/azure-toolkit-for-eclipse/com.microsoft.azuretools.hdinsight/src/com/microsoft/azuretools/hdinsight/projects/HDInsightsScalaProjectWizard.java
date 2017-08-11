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
package com.microsoft.azuretools.hdinsight.projects;

import java.awt.Dialog;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.jdt.internal.ui.wizards.JavaProjectWizard;
import org.eclipse.jdt.ui.wizards.NewJavaProjectWizardPageOne;
import org.eclipse.jdt.ui.wizards.NewJavaProjectWizardPageTwo;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.azuretools.core.utils.PluginUtil;
import com.microsoft.azuretools.hdinsight.Activator;

public class HDInsightsScalaProjectWizard extends JavaProjectWizard implements IExecutableExtension {
	private String id;
	private Composite sparkLibraryOptionsPanel;
	private HDInsightScalaPageOne pageOne;
	private HDInsightScalaPageTwo pageTwo;
	public static boolean canFinish = false;
	
	public HDInsightsScalaProjectWizard() {
		this(
				PluginUtil.forceInstallPluginUsingMarketplace(
						PluginUtil.scalaPluginSymbolicName, 
						PluginUtil.scalaPluginMarketplaceURL),
				setFocusToInstallationWindow(),
				new HDInsightScalaPageOne()
				);
	}

	public HDInsightsScalaProjectWizard(boolean checkScalaPlugin, boolean setFocusToInstallationWindow, HDInsightScalaPageOne hdInsightScalaPageOne) {
		this(hdInsightScalaPageOne, new HDInsightScalaPageTwo(hdInsightScalaPageOne));
	}

	public HDInsightsScalaProjectWizard(HDInsightScalaPageOne page1, HDInsightScalaPageTwo page2) {
		super((NewJavaProjectWizardPageOne)page1, (NewJavaProjectWizardPageTwo)page2);
		canFinish = false;
		pageOne = page1;
		pageTwo = page2;
		setWindowTitle("New HDInsight Scala Project");

		page2.setTitle("HDInsight Spark Project Library Settings");
		page2.setDescription("Define the project build settings.");
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

	@Override
	public void setInitializationData(IConfigurationElement parameter, String arg1, Object arg2) {
		super.setInitializationData(parameter, arg1, arg2);
		this.id = parameter.getAttribute("id");

	}
	
	@Override
	public boolean canFinish() {
		return canFinish && super.canFinish();
	}

	@Override
	public boolean performFinish() {
		try {
			CreateProjectUtil.createSampleFile(this.id, this.pageOne.getProjectName());
		} catch (CoreException e) {
			Activator.getDefault().log("Create HDInsight project error", e);
		}

		return super.performFinish();
	}
}
