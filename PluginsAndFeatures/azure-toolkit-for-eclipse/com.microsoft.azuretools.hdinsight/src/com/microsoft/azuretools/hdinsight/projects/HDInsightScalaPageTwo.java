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
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.ui.wizards.NewWizardMessages;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.BuildPathsBlock;
import org.eclipse.jdt.ui.wizards.JavaCapabilityConfigurationPage;
import org.eclipse.jdt.ui.wizards.NewJavaProjectWizardPageTwo;
import org.eclipse.swt.widgets.Display;
import org.scalaide.core.IScalaInstallation;
import org.scalaide.core.SdtConstants;
import org.scalaide.core.internal.ScalaPlugin;
import org.scalaide.core.internal.jdt.util.ClasspathContainerSetter;
import org.scalaide.core.internal.project.ScalaInstallationChoice;
import org.scalaide.core.internal.project.ScalaProject;
import org.scalaide.util.internal.SettingConverterUtil;

import scala.Option;
import scala.tools.nsc.settings.ScalaVersion;


public class HDInsightScalaPageTwo extends NewJavaProjectWizardPageTwo {
    private HDInsightsScalaProjectWizard parent = null;
    private boolean hasConfiguredScalaClasspathContainer = false;
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

            addMoreSourcetoClassPath();

            if (parent == null) {
                parent = (HDInsightsScalaProjectWizard) this.getWizard();
            }

            if (hasConfiguredScalaClasspathContainer == false) {
                hasConfiguredScalaClasspathContainer = true;
                Display.getDefault().syncExec(() -> {
                        IJavaProject javaProject = getJavaProject();
                        IClasspathEntry[] entries = null;
                        try {
                            entries = javaProject.getRawClasspath();
                            IClasspathEntry scalaClasspathContainerEntry = null;
                            int scalaClasspathContainerEntryIndex = -1;
                            for (int i = 0; i < entries.length; i++) {
                                String entryName = entries[i].getPath().toPortableString().toLowerCase();
                                if (entryName != null && entryName.contains(SdtConstants.ScalaLibContId().toLowerCase())) {
                                    scalaClasspathContainerEntry = entries[i];
                                    scalaClasspathContainerEntryIndex = i;
                                    break;
                                }
                            }

                            if (scalaClasspathContainerEntry != null) {
                                ScalaVersion scalaVersion = parent.getScalaVersion().map(ver -> ScalaVersion.apply(ver)).orElseGet(ScalaVersion::current);
                                ClasspathContainerSetter setter = new ClasspathContainerSetter(javaProject);
                                Option<IScalaInstallation> scalaInstallation = setter.bestScalaBundleForVersion(scalaVersion);
                                if (!scalaInstallation.isEmpty()) {
                                    setter.updateBundleFromScalaInstallation(new Path(SdtConstants.ScalaLibContId()), scalaInstallation.get());

                                    ScalaPlugin plugin = ScalaPlugin.apply();
                                    Option<ScalaProject> scalaPrj = plugin.asScalaProject(javaProject.getProject());

                                    if (!scalaPrj.isEmpty()) {
                                        scalaPrj.get().projectSpecificStorage().setValue(
                                                SettingConverterUtil.SCALA_DESIRED_INSTALLATION(),
                                                ScalaInstallationChoice.apply(scalaVersion).toString());
                                    }
                                }
                            }
                        } catch (JavaModelException ignore) {

                        }
                });
            }
        } catch (OperationCanceledException | NoSuchMethodException | SecurityException | IllegalAccessException
                | IllegalArgumentException | InvocationTargetException ex) {
            throw new InterruptedException();
        } finally {
            ((IProgressMonitor) monitor).done();
        }
    }

    private void addMoreSourcetoClassPath() throws JavaModelException {
        if (parent == null) {
            parent = (HDInsightsScalaProjectWizard) this.getWizard();
        }

        if (parent.getUsingMaven()) {
            CreateProjectUtil.removeSourceFolderfromClassPath(this.getJavaProject(), "src");
            CreateProjectUtil.addSourceFoldertoClassPath(this.getJavaProject(), "src/main/scala");
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
