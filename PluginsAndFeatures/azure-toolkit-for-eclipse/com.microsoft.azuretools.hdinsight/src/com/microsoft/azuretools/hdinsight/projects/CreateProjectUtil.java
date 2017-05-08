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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.StringHelper;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.microsoft.azuretools.hdinsight.util.Messages;

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

	public static void copyFileTo(@NotNull String[] resources, @NotNull String toPath) {
		for (int i = 0; i < resources.length; ++i) {
			InputStream inputStream = CreateProjectUtil.class.getResourceAsStream("/resources" + resources[i]);
			String toFilePath = StringHelper.concat(toPath, getNameFromPath(resources[i]));
			try {
				File toFile = new File(toFilePath);

				FileUtils.copyInputStreamToFile(inputStream, new File(toFilePath));

				// refresh after copy
				convert(toFile).refreshLocal(IResource.DEPTH_ONE, null);
			} catch (IOException | CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public static void createSampleFile(@NotNull String id, @NotNull String projectName) throws CoreException {
		final IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		final IFolder sourceRootFolder = project.getFolder("src");

		if (!sourceRootFolder.exists()) {
			sourceRootFolder.create(false, true, null);
		}
		final String rootPath = sourceRootFolder.getLocation().toFile().getAbsolutePath();

		switch (id) {
		case "com.microsoft.azure.hdinsight.local-scala.projwizard":
			createResourceStructForLocalRunScalaProject(sourceRootFolder, rootPath, project);
			AppInsightsClient.create(Messages.SparkProjectSystemScalaCreation, null);
			break;
		case "com.microsoft.azure.hdinsight.local-java.projwizard":
			AppInsightsClient.create(Messages.SparkProjectSystemJavaSampleCreation, null);
			copyFileTo(Java_Local_RunSample, rootPath);
			break;
		case "com.microsoft.azure.hdinsight.cluster-scala.projwizard":
			AppInsightsClient.create(Messages.SparkProjectSystemScalaSampleCreation, null);
			copyFileTo(Scala_Cluster_Run_Sample, rootPath);
			break;
		default:
			break;
		}
	}

	@NotNull
	private static IFile convert(@NotNull File file) {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IPath location = Path.fromOSString(file.getAbsolutePath());
		IFile ifile = workspace.getRoot().getFileForLocation(location);
		return ifile;
	}

	private static void createResourceStructForLocalRunScalaProject(IFolder sourceRootFolder, String rootPath,
			IProject project) throws CoreException {
		copyFileTo(Scala_Local_Run_Sample, rootPath);
		final IFolder dataFolder = sourceRootFolder.getParent().getFolder(new Path("data"));
		if (!dataFolder.exists()) {
			dataFolder.create(false, true, null);
		}
		copyFileTo(Scala_Local_Run_Sample_Data, dataFolder.getLocation().toFile().getAbsolutePath());

		IJavaProject javaProject = JavaCore.create(project);
		IClasspathEntry[] entries = javaProject.getRawClasspath();

		IClasspathEntry[] newEntries = new IClasspathEntry[entries.length + 1];
		System.arraycopy(entries, 0, newEntries, 0, entries.length);

		IPath dataPath = javaProject.getPath().append("data");
		IClasspathEntry dataEntry = JavaCore.newSourceEntry(dataPath, null);

		newEntries[entries.length] = JavaCore.newSourceEntry(dataEntry.getPath());
		javaProject.setRawClasspath(newEntries, null);
	}

	@NotNull
	private static String getNameFromPath(@NotNull String path) {
		int index = path.lastIndexOf('/');
		return path.substring(index);
	}
}
