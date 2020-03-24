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

import static com.microsoft.azuretools.telemetry.TelemetryConstants.HDINSIGHT;

import com.microsoft.azuretools.telemetrywrapper.EventType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import com.microsoft.azuretools.hdinsight.Activator;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.corext.util.JavaModelUtil;
import org.eclipse.jdt.ui.wizards.NewJavaProjectWizardPageTwo;

import com.microsoft.azure.hdinsight.projects.SparkVersion;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.StringHelper;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.microsoft.azuretools.core.utils.Messages;

public class CreateProjectUtil {

    // sample file path should be start with "/"

    private static final String[] Java_Local_RunSample = new String[] { "/hdinsight/templates/java/JavaSparkPi.java" };
    private static final String[] Scala_Cluster_Run_Sample = new String[] {
            "/hdinsight/templates/scala/scala_cluster_run/SparkCore_WasbIOTest.scala",
            "/hdinsight/templates/scala/scala_cluster_run/SparkStreaming_HdfsWordCount.scala",
            "/hdinsight/templates/scala/scala_cluster_run/SparkSQL_RDDRelation.scala" };
    private static final String[] Scala_Local_Run_Sample = new String[] {
            "/hdinsight/templates/scala/scala_local_run/LogQuery.scala",
            "/hdinsight/templates/scala/scala_local_run/SparkML_RankingMetricsExample.scala" };

    private static final String[] Scala_Local_Run_Sample_Data = new String[] {
            "/hdinsight/templates/scala/scala_local_run/data/sample_movielens_data.txt" };

    private static final String SourcePomFileRelativePath = "/hdinsight/templates/pom/spark_";

    private static final String JavaMavenProjectFolder = "/main/java";

    private static final String ScalaMavenProjectFolder = "/main/scala";

    public static void copyFileTo(@NotNull String[] resources, @NotNull String toPath) {
        for (int i = 0; i < resources.length; ++i) {
            String toName = getNameFromPath(resources[i]);

            copyFileTo(resources[i], toPath, toName);
        }
    }

    public static void copyFileTo(@NotNull String resource, @NotNull String toPath, @NotNull String toName) {
        InputStream inputStream = CreateProjectUtil.class.getResourceAsStream("/resources" + resource);
        String toFilePath = StringHelper.concat(toPath, toName);
        try {
            File toFile = new File(toFilePath);

            FileUtils.copyInputStreamToFile(inputStream, new File(toFilePath));

            // refresh after copy
            convert(toFile).refreshLocal(IResource.DEPTH_ONE, null);
        } catch (IOException | CoreException e) {
            Activator.getDefault().log("Copy file error", e);
        }
    }

    public static void createSampleFile(@NotNull String id, @NotNull String projectName, boolean useMaven, @NotNull SparkVersion sparkVersion) throws CoreException {
        final IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
        final IFolder sourceRootFolder = project.getFolder("src");

        if (!sourceRootFolder.exists()) {
            sourceRootFolder.create(false, true, null);
        }
        String rootPath = sourceRootFolder.getLocation().toFile().getAbsolutePath();

        switch (id) {
        case "com.microsoft.azure.hdinsight.local-scala.projwizard":
            if (useMaven) {
                rootPath += ScalaMavenProjectFolder;
            }

            createResourceStructForLocalRunScalaProject(sourceRootFolder, rootPath, project, useMaven);
            AppInsightsClient.create(Messages.SparkProjectSystemScalaCreation, null);
            break;
        case "com.microsoft.azure.hdinsight.local-java.projwizard":
            if (useMaven) {
                rootPath += JavaMavenProjectFolder;
            }

            copyFileTo(Java_Local_RunSample, rootPath);
            AppInsightsClient.create(Messages.SparkProjectSystemJavaSampleCreation, null);
            EventUtil.logEvent(EventType.info, HDINSIGHT, Messages.SparkProjectSystemJavaSampleCreation, null);
            break;
        case "com.microsoft.azure.hdinsight.cluster-scala.projwizard":
            if (useMaven) rootPath += ScalaMavenProjectFolder;
            copyFileTo(Scala_Cluster_Run_Sample, rootPath);
            AppInsightsClient.create(Messages.SparkProjectSystemScalaSampleCreation, null);
            EventUtil.logEvent(EventType.info, HDINSIGHT, Messages.SparkProjectSystemScalaSampleCreation, null);
            break;
        case "com.microsoft.azure.hdinsight.scala.projwizard":
            AppInsightsClient.create(Messages.SparkProjectSystemScalaCreation, null);
            EventUtil.logEvent(EventType.info, HDINSIGHT, Messages.SparkProjectSystemScalaCreation, null);
            break;
        case "com.microsoft.azure.hdinsight.java.projwizard":
            AppInsightsClient.create(Messages.SparkProjectSystemJavaCreation, null);
            EventUtil.logEvent(EventType.info, HDINSIGHT, Messages.SparkProjectSystemJavaCreation, null);
            break;
        default:
            AppInsightsClient.create(Messages.SparkProjectSystemOtherCreation, null);
            EventUtil.logEvent(EventType.info, HDINSIGHT, Messages.SparkProjectSystemOtherCreation, null);
            break;
        }

        if (useMaven) {
            CreateProjectUtil.copyPomFile(sparkVersion, projectName);
        }
    }

    public static void copyPomFile(@NotNull SparkVersion sparkVersion, @NotNull String projectName) {
        final IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
        final String rootPath = project.getLocation().toFile().getAbsolutePath();

        final String pomFileName = SourcePomFileRelativePath + sparkVersion.getSparkVersioninDashFormat() + "pom.xml";

        copyFileTo(pomFileName, rootPath, "/pom.xml");
    }

    public static void removeSourceFolderfromClassPath(IJavaProject javaProject, String folderUnderRoot) throws JavaModelException {
        IClasspathEntry[] entries = javaProject.getRawClasspath();

        IPath targetPath = javaProject.getProject().getFullPath().append(new Path(folderUnderRoot));
        IClasspathEntry[] newEntries = Arrays.stream(entries)
                .filter(e -> e.getEntryKind() != IClasspathEntry.CPE_SOURCE
                            || !e.getPath().equals(targetPath))
                .toArray(IClasspathEntry[]::new);

        javaProject.setRawClasspath(newEntries, new NullProgressMonitor());
    }

    public static void addSourceFoldertoClassPath(IJavaProject javaProject, String folderUnderRoot) throws JavaModelException {
        IProject project = javaProject.getProject();

        IFolder currFolder = createFolderUnderRoot(project, folderUnderRoot);

        IClasspathEntry[] entries = javaProject.getRawClasspath();

        IClasspathEntry newSrcFolder = JavaCore.newSourceEntry(currFolder.getFullPath());

        IClasspathEntry[] newEntries;
        if (addExclusiontoClassPath(newSrcFolder, entries) == true) {
            newEntries = new IClasspathEntry[entries.length + 1];

            newEntries[entries.length] = newSrcFolder;
        } else {
            newEntries = new IClasspathEntry[entries.length];
        }

        System.arraycopy(entries, 0, newEntries, 0, entries.length);

        javaProject.setRawClasspath(newEntries, new NullProgressMonitor());
    }

    public static boolean checkHDInsightProjectNature(NewJavaProjectWizardPageTwo pageTwo) {
        if (pageTwo != null && pageTwo.getJavaProject() != null && pageTwo.getJavaProject().getProject() != null) {
            try {
                return pageTwo.getJavaProject().getProject().hasNature(HDInsightProjectNature.NATURE_ID);
            } catch (CoreException e) {
                Activator.getDefault().log("Fail to get project nature", e);
            }
        }

        return false;
    }

    private static boolean addExclusiontoClassPath(IClasspathEntry newEntry, IClasspathEntry[] entries) {
        IPath newEntryPath = newEntry.getPath();
        for (int i = 0; i < entries.length; i++) {
            if (entries[i].getEntryKind() == IClasspathEntry.CPE_SOURCE) {
                IPath currPath = entries[i].getPath();
                if (currPath.equals(newEntryPath)) {
                    return false;
                } else {
                    if (currPath.isPrefixOf(newEntryPath)) {
                        IPath[] exclusionFilters = (IPath[])entries[i].getExclusionPatterns();
                        if (!JavaModelUtil.isExcludedPath(currPath, exclusionFilters)) {
                            IPath pathToExclude = newEntryPath.removeFirstSegments(currPath.segmentCount()).addTrailingSeparator();
                            IPath[] newExclusionFilters = new IPath[exclusionFilters.length + 1];
                            System.arraycopy(exclusionFilters, 0, newExclusionFilters, 0, exclusionFilters.length);
                            newExclusionFilters[exclusionFilters.length] = pathToExclude;
                            entries[i] = JavaCore.newSourceEntry(currPath,
                                    entries[i].getInclusionPatterns(),
                                    newExclusionFilters,
                                    entries[i].getOutputLocation(),
                                    entries[i].getExtraAttributes());
                        }
                    }
                }
            }
        }

        return true;
    }

    @NotNull
    private static IFile convert(@NotNull File file) {
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IPath location = Path.fromOSString(file.getAbsolutePath());
        IFile ifile = workspace.getRoot().getFileForLocation(location);
        return ifile;
    }

    private static void createResourceStructForLocalRunScalaProject(IFolder sourceRootFolder, String rootPath,
            IProject project, boolean useMaven) throws CoreException {
        // Copy code
        copyFileTo(Scala_Local_Run_Sample, rootPath);

        // Copy data
        String strDataFolder;
        // Putting data to "src/main/scala/resources/data" seems like not working
        //if (useMaven) {
        //    strDataFolder = "src/main/scala/resources/data";
        //} else {
            strDataFolder = "data";
        //}

        IFolder dataFolder = createFolderUnderRoot(project, strDataFolder);

        copyFileTo(Scala_Local_Run_Sample_Data, dataFolder.getLocation().toFile().getAbsolutePath());

        IJavaProject javaProject = JavaCore.create(project);

        addSourceFoldertoClassPath(javaProject, strDataFolder);
    }

    private static IFolder createFolderUnderRoot(@NotNull IProject project, @NotNull String relativeFolder) {
        return createFolderUnderSpecifiedRoot(project, relativeFolder, null);
    }

    private static IFolder createFolderUnderSpecifiedRoot(@NotNull IProject project, String relativeFolder, String rootFolder ) {
        String[] srcFolders = relativeFolder.split("/");
        if (srcFolders.length == 0) {
            return null;
        }

        IFolder[] parentFolder = new IFolder[1];
        int startIndex = 0;
        if (rootFolder != null && rootFolder.length() >= 0) {
            parentFolder[0] = project.getFolder(rootFolder);
        } else {
            parentFolder[0] = project.getFolder(new Path(srcFolders[0]));
            startIndex = 1;
        }

        for (int i = startIndex; i < srcFolders.length; i++) {
            String srcFolder = srcFolders[i];

            IFolder currFolder = parentFolder[0].getFolder(srcFolder);
            if (!currFolder.exists()) {
                try {
                    currFolder.create(false, true, null);
                } catch (CoreException e) {
                    Activator.getDefault().log("Create project folder error", e);
                }
            }
            parentFolder[0] = currFolder;
        }

        return parentFolder[0];
    }

    @NotNull
    private static String getNameFromPath(@NotNull String path) {
        int index = path.lastIndexOf('/');
        return path.substring(index);
    }
}
