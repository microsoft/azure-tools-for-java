package com.microsoft.azuretools.hdinsight.projects;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.BuildPathsBlock;
import org.eclipse.jdt.ui.wizards.JavaCapabilityConfigurationPage;
import org.eclipse.jdt.ui.wizards.NewJavaProjectWizardPageOne;
import org.eclipse.jdt.ui.wizards.NewJavaProjectWizardPageTwo;
import org.scalaide.core.SdtConstants;

public class HDInsightScalaPageTwo extends NewJavaProjectWizardPageTwo {
	private HDInsightsScalaProjectWizard parent = null;
	public HDInsightScalaPageTwo(HDInsightScalaPageOne hdInsightScalaPageOne) {
		super(hdInsightScalaPageOne);
	}
	
	public void setParent(HDInsightsScalaProjectWizard parent) {
		this.parent = parent;
	}

	public void configureJavaProject(String newProjectCompliance, IProgressMonitor monitor)
			throws CoreException, InterruptedException {
		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}
		byte nSteps = 6;
		((IProgressMonitor) monitor).beginTask(NewWizardMessages.JavaCapabilityConfigurationPage_op_desc_java,
				nSteps);
		try {
			IProject project = addHDInsightNature(monitor);
			Method method = JavaCapabilityConfigurationPage.class.getDeclaredMethod("getBuildPathsBlock");
			method.setAccessible(true);
			Object r = method.invoke(this);
			((BuildPathsBlock) r).configureJavaProject(newProjectCompliance,
					new SubProgressMonitor((IProgressMonitor) monitor, 5));
			parent.canFinish = true;
		} catch (OperationCanceledException | NoSuchMethodException | SecurityException | IllegalAccessException
				| IllegalArgumentException | InvocationTargetException ex) {
			throw new InterruptedException();
		} finally {
			((IProgressMonitor) monitor).done();
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
			String[] newNatures = new String[natures.length + 3];
			System.arraycopy(natures, 0, newNatures, 0, natures.length);
			newNatures[natures.length] = SdtConstants.NatureId();
			newNatures[natures.length + 1] = JavaCore.NATURE_ID;
			newNatures[natures.length + 2] = HDInsightProjectNature.NATURE_ID;
			description.setNatureIds(newNatures);
			project.setDescription(description, null);
		} else {
			monitor.worked(1);
		}
		return project;
	}

}