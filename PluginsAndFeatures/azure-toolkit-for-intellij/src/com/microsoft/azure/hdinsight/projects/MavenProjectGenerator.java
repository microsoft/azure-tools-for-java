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

package com.microsoft.azure.hdinsight.projects;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azure.hdinsight.common.StreamUtil;
import com.intellij.openapi.util.io.FileUtil;
import com.microsoft.azure.hdinsight.projects.util.ProjectSampleUtil;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

public class MavenProjectGenerator {
    private Module module;
    private HDInsightTemplatesType templatesType;
    private SparkVersion sparkVersion;

    public MavenProjectGenerator(@NotNull Module module,
                                 @NotNull HDInsightTemplatesType templatesType,
                                 @NotNull SparkVersion sparkVersion) {
        this.module = module;
        this.templatesType = templatesType;
        this.sparkVersion = sparkVersion;
    }

    public void generate() {
        String root = ProjectSampleUtil.getRootOrSourceFolder(this.module, false);

        try {
            createDirectories(root);
            createPom(root);
            copySamples(root);
            importMavenProject();
        } catch (Exception e) {
            DefaultLoader.getUIHelper().showError("Failed to create project", "Create Sample Project");
        }
    }

    private void createDirectories(String root) throws IOException {
        switch (this.templatesType) {
            case Java:
                VfsUtil.createDirectories(root + "/src/main/java/sample");
                VfsUtil.createDirectories(root + "/src/main/resources");
                VfsUtil.createDirectories(root + "/src/test/java");
                break;
            case Scala:
            case ScalaClusterSample:
                VfsUtil.createDirectories(root + "/src/main/scala/sample");
                VfsUtil.createDirectories(root + "/src/main/resources");
                VfsUtil.createDirectories(root + "/src/test/scala");
                break;
        }
    }

    private void createPom(String root) throws Exception {
        File file = null;
        switch (this.sparkVersion) {
            case SPARK_1_5_2:
                file = StreamUtil.getResourceFile("/hdinsight/templates/pom/spark_1_5_2_pom.xml");
                break;
            case SPARK_1_6_2:
                file = StreamUtil.getResourceFile("/hdinsight/templates/pom/spark_1_6_2_pom.xml");
                break;
            case SPARK_1_6_3:
                file = StreamUtil.getResourceFile("/hdinsight/templates/pom/spark_1_6_3_pom.xml");
                break;
            case SPARK_2_0_2:
                file = StreamUtil.getResourceFile("/hdinsight/templates/pom/spark_2_0_2_pom.xml");
                break;
            case SPARK_2_1_0:
                file = StreamUtil.getResourceFile("/hdinsight/templates/pom/spark_2_1_0_pom.xml");
                break;
        }

        if (null == file) {
            DefaultLoader.getUIHelper().showError("Failed to get the sample resource folder for project", "Create Sample Project");
        } else {
            FileUtil.copy(file, new File(root + "/pom.xml"));
        }
    }

    private void copySamples(String root) throws Exception {
        switch (this.templatesType) {
            case ScalaClusterSample:
                ProjectSampleUtil.copyFileToPath(new String[]{
                        "/hdinsight/templates/java/JavaSparkPi.java"
                }, root + "/src/main/java/sample");

                ProjectSampleUtil.copyFileToPath(new String[]{
                        "/hdinsight/templates/scala/scala_cluster_run/SparkCore_WasbIOTest.scala",
                        "/hdinsight/templates/scala/scala_cluster_run/SparkStreaming_HdfsWordCount.scala",
                        "/hdinsight/templates/scala/scala_cluster_run/SparkSQL_RDDRelation.scala"
                }, root + "/src/main/scala/sample");

                ProjectSampleUtil.copyFileToPath(new String[]{
                        "/hdinsight/templates/scala/scala_local_run/LogQuery.scala",
                        "/hdinsight/templates/scala/scala_local_run/SparkML_RankingMetricsExample.scala"
                }, root + "/src/main/scala/sample");

                ProjectSampleUtil.copyFileToPath(new String[]{
                        "/hdinsight/templates/scala/scala_local_run/data/data/sample_movielens_data.txt"
                }, root + "/data/__default__/data/");

                ProjectSampleUtil.copyFileToPath(new String[]{
                        "/hdinsight/templates/scala/scala_local_run/data/HdiSamples/HdiSamples/FoodInspectionData/README"
                }, root + "/data/__default__/HdiSamples/HdiSamples/FoodInspectionData/");

                ProjectSampleUtil.copyFileToPath(new String[]{
                        "/hdinsight/templates/scala/scala_local_run/data/HdiSamples/HdiSamples/SensorSampleData/hvac/HVAC.csv"
                }, root + "/data/__default__/HdiSamples/HdiSamples/SensorSampleData/hvac/");

                // Falling through
            case Scala:
            case Java:
                new File(root, "data/__default__/user/current/").mkdirs();

                ProjectSampleUtil.copyFileToPath(new String[]{
                        "/hdinsight/templates/log4j.properties"
                }, root + "/src/main/resources");
                break;
        }
    }

    private void importMavenProject() throws ConfigurationException {
        Project project = this.module.getProject();
        String baseDirPath = project.getBasePath();
        MavenProjectsManager manager = MavenProjectsManager.getInstance(project);

        File pomFile = new File(baseDirPath + File.separator + "pom.xml");
        VirtualFile pom = VfsUtil.findFileByIoFile(pomFile, true);

        manager.addManagedFiles(Collections.singletonList(pom));
        manager.scheduleImportAndResolve();
    }
}
