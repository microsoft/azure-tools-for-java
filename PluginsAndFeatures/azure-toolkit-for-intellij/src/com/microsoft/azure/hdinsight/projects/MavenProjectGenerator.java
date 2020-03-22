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
package com.microsoft.azure.hdinsight.projects;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azure.hdinsight.common.StreamUtil;
import com.microsoft.azure.hdinsight.projects.util.ProjectSampleUtil;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import org.jetbrains.concurrency.Promise;
import org.jetbrains.concurrency.Promises;
import org.jetbrains.idea.maven.project.MavenProject;
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

    public Promise<MavenProject> generate() {
        String root = ProjectSampleUtil.getRootOrSourceFolder(this.module, false);

        try {
            createDirectories(root);
            createPom(root);
            copySamples(root);
            return importMavenProject();
        } catch (Exception e) {
            DefaultLoader.getUIHelper().showError("Failed to create project: " + e.getMessage(), "Create Sample Project");

            return Promises.rejectedPromise(e);
        }
    }

    private void createDirectories(String root) throws IOException {
        switch (this.templatesType) {
            case ScalaFailureTaskDebugSample:
                VfsUtil.createDirectories(root + "/lib");
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
                file = this.templatesType != HDInsightTemplatesType.ScalaFailureTaskDebugSample ?
                        StreamUtil.getResourceFile("/hdinsight/templates/pom/spark_2_1_0_pom.xml") :
                        StreamUtil.getResourceFile("/hdinsight/templates/pom/spark_2_1_0_failure_task_debug_pom.xml");
                break;
            case SPARK_2_2_0:
                file = StreamUtil.getResourceFile("/hdinsight/templates/pom/spark_2_2_0_pom.xml");
                break;
            case SPARK_2_3_0:
                file = this.templatesType != HDInsightTemplatesType.ScalaFailureTaskDebugSample ?
                        StreamUtil.getResourceFile("/hdinsight/templates/pom/spark_2_3_0_pom.xml") :
                        StreamUtil.getResourceFile("/hdinsight/templates/pom/spark_2_3_0_failure_task_debug_pom.xml");
                break;
            case SPARK_2_3_2:
                file = this.templatesType != HDInsightTemplatesType.ScalaFailureTaskDebugSample ?
                        StreamUtil.getResourceFile("/hdinsight/templates/pom/spark_2_3_2_pom.xml") :
                        StreamUtil.getResourceFile("/hdinsight/templates/pom/spark_2_3_2_failure_task_debug_pom.xml");
                break;
            case SPARK_2_4_0:
                file = StreamUtil.getResourceFile("/hdinsight/templates/pom/spark_2_4_0_pom.xml");
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

                if (SparkVersion.sparkVersionComparator.compare(this.sparkVersion, SparkVersion.SPARK_2_1_0) >= 0) {
                    // sample code
                    ProjectSampleUtil.copyFileToPath(new String[]{
                            "/hdinsight/templates/scala/sparksql/SparkSQLExample.scala"
                    }, root + "/src/main/scala/sample");

                    // sample data
                    ProjectSampleUtil.copyFileToPath(new String[]{
                            "/hdinsight/templates/scala/scala_local_run/data/example/data/people.json"
                    }, root + "/data/__default__/example/data/");
                }

                // Falling through
            case Scala:
            case Java:
                new File(root, "data/__default__/user/current/").mkdirs();

                ProjectSampleUtil.copyFileToPath(new String[]{
                        "/hdinsight/templates/log4j.properties"
                }, root + "/src/main/resources");
                break;
            case ScalaFailureTaskDebugSample:
                ProjectSampleUtil.copyFileToPath(new String[]{
                        "/hdinsight/templates/scala/sample/AgeMean_Div0.scala"
                }, root + "/src/main/scala/sample");

                new File(root, "data/__default__/user/current/").mkdirs();

                ProjectSampleUtil.copyFileToPath(new String[]{
                        "/hdinsight/templates/log4j.properties"
                }, root + "/src/main/resources");

                ProjectSampleUtil.copyFileToPath(new String[]{
                        "/spark/" + SparkToolsLib.INSTANCE.getJarFileName(this.sparkVersion)
                }, root + "/lib");

                break;
        }
    }

    private Promise<MavenProject> importMavenProject() {
        Project project = this.module.getProject();
        String baseDirPath = project.getBasePath();
        MavenProjectsManager manager = MavenProjectsManager.getInstance(project);

        File pomFile = new File(baseDirPath + File.separator + "pom.xml");
        VirtualFile pom = VfsUtil.findFileByIoFile(pomFile, true);

        if (pom == null) {
            return Promises.rejectedPromise("Can't find Maven pom.xml file to import into IDEA");
        }

        manager.addManagedFiles(Collections.singletonList(pom));

        return manager.scheduleImportAndResolve()
                .then(modules -> manager.findProject(module));
    }
}
