/**
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.azure.hdinsight.projects.samples;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azure.hdinsight.common.StreamUtil;
import com.microsoft.azure.hdinsight.projects.HDInsightTemplatesType;
import com.microsoft.azure.hdinsight.projects.SparkVersion;
import com.intellij.openapi.util.io.FileUtil;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

public class MavenSampleUtil {
    public static void generateMavenSample(@NotNull Module module, HDInsightTemplatesType templatesType, SparkVersion version) {
        String root = ProjectSampleUtil.getRootOrSourceFolder(module, false);

        try {
            createDirectories(root, templatesType);
            createPom(root, version);
            copySamples(root, templatesType);
            importMavenProject(module.getProject());
        } catch (Exception e) {
            DefaultLoader.getUIHelper().showError("Failed to create project", "Create Sample Project");
            e.printStackTrace();
        }
    }

    private static void createDirectories(String root, HDInsightTemplatesType templatesType) throws IOException {
        switch (templatesType) {
            case JavaLocalSample:
            case Java:
                VfsUtil.createDirectories(root + "/src/main/java/sample");
                VfsUtil.createDirectories(root + "/src/main/resources");
                VfsUtil.createDirectories(root + "/src/test/java");
                break;
            case Scala:
            case ScalaLocalSample:
            case ScalaClusterSample:
                VfsUtil.createDirectories(root + "/src/main/scala/sample");
                VfsUtil.createDirectories(root + "/src/main/resources");
                VfsUtil.createDirectories(root + "/src/test/scala");
                break;
        }
    }

    private static void createPom(String root, SparkVersion version) throws Exception {
        File file = null;
        switch (version) {
            case SPARK_1_6:
                file = StreamUtil.getResourceFile("/hdinsight/templates/pom/spark_1_6_pom.xml");
                break;
            case SPARK_2_0:
                file = StreamUtil.getResourceFile("/hdinsight/templates/pom/spark_2_0_pom.xml");
                break;
        }

        if (null == file) {
            DefaultLoader.getUIHelper().showError("Failed to get the sample resource folder for project", "Create Sample Project");
        } else {
            FileUtil.copy(file, new File(root + "/pom.xml"));
        }
    }

    private static void copySamples(String root, HDInsightTemplatesType templatesType) throws Exception {
        switch (templatesType) {
            case JavaLocalSample:
                ProjectSampleUtil.copyFileToPath(new String[]{
                        "/hdinsight/templates/java/JavaSparkPi.java"
                }, root + "/src/main/java/sample");
                break;
            case ScalaClusterSample:
                ProjectSampleUtil.copyFileToPath(new String[]{
                        "/hdinsight/templates/scala/scala_cluster_run/SparkCore_WasbIOTest.scala",
                        "/hdinsight/templates/scala/scala_cluster_run/SparkStreaming_HdfsWordCount.scala",
                        "/hdinsight/templates/scala/scala_cluster_run/SparkSQL_RDDRelation.scala"
                }, root + "/src/main/scala/sample");
                break;
            case ScalaLocalSample:
                ProjectSampleUtil.copyFileToPath(new String[]{
                        "/hdinsight/templates/scala/scala_local_run/LogQuery.scala",
                        "/hdinsight/templates/scala/scala_local_run/SparkML_RankingMetricsExample.scala"
                }, root + "/src/main/scala/sample");

                ProjectSampleUtil.copyFileToPath(new String[]{
                        "/hdinsight/templates/scala/scala_local_run/data/sample_movielens_data.txt"
                }, root + "/data");
                break;
        }
    }

    private static void importMavenProject(@NotNull Project project) throws ConfigurationException {
        String baseDirPath = project.getBasePath();
        MavenProjectsManager manager = MavenProjectsManager.getInstance(project);

        File pomFile = new File(baseDirPath + File.separator + "pom.xml");
        VirtualFile pom = VfsUtil.findFileByIoFile(pomFile, true);

        manager.addManagedFiles(Collections.singletonList(pom));
        manager.scheduleImportAndResolve();
    }
}
