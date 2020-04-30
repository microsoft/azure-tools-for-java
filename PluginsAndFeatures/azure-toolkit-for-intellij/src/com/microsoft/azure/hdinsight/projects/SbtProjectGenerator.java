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

import com.intellij.ide.actions.ImportModuleAction;
import com.intellij.ide.util.newProjectWizard.AddModuleWizard;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.externalSystem.ExternalSystemManager;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.service.project.wizard.AbstractExternalProjectImportProvider;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.projectImport.ProjectImportProvider;
import com.microsoft.azure.hdinsight.projects.util.ProjectSampleUtil;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class SbtProjectGenerator {
    private Module module;
    private HDInsightTemplatesType templatesType;
    private SparkVersion sparkVersion;
    private String sparkSimpleVersion;
    private String scalaVersion;
    private String scalaVer;
    private String sbtVersion;

    public SbtProjectGenerator(@NotNull Module module,
                               @NotNull HDInsightTemplatesType templatesType,
                               @NotNull SparkVersion sparkVersion,
                               @NotNull String sbtVersion) {
        this.module = module;
        this.templatesType = templatesType;
        this.sparkVersion = sparkVersion;
        this.sparkSimpleVersion = sparkVersion.getSparkVersion();
        this.scalaVersion = sparkVersion.getScalaVersion();
        this.scalaVer = sparkVersion.getScalaVer();
        this.sbtVersion = sbtVersion;
    }

    public void generate() {
        String root = ProjectSampleUtil.getRootOrSourceFolder(this.module, false);
        try {
            createDirectories(root);
            copySamples(root);
            generateSbt(root);
            importSbtProject(root);
        } catch (Exception e) {
            DefaultLoader.getUIHelper().showError("Failed to create project", "Create Sample Project");
            e.printStackTrace();
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
        }
    }

    private void generateSbt(String root) throws IOException {
        File sbt = new File(root + File.separator + "build.sbt");
        FileUtil.writeToFile(sbt, generateSbtFileContent());
    }

    private String generateSbtFileContent() {
        List<String> sbtLines = new ArrayList<>();

        sbtLines.add(String.format("name := \"%s\"", this.module.getName()));
        sbtLines.add("version := \"1.0\"");
        sbtLines.add(String.format("scalaVersion := \"%s\"", this.scalaVersion));
        sbtLines.add("libraryDependencies ++= Seq(");
        sbtLines.add(String.format("\"org.apache.spark\" %% \"spark-core_%s\" %% \"%s\",", this.scalaVer, this.sparkSimpleVersion));
        sbtLines.add(String.format("\"org.apache.spark\" %% \"spark-sql_%s\" %% \"%s\",", this.scalaVer, this.sparkSimpleVersion));
        sbtLines.add(String.format("\"org.apache.spark\" %% \"spark-streaming_%s\" %% \"%s\",", this.scalaVer, this.sparkSimpleVersion));
        sbtLines.add(String.format("\"org.apache.spark\" %% \"spark-mllib_%s\" %% \"%s\",", this.scalaVer, this.sparkSimpleVersion));
        sbtLines.add(String.format("\"org.jmockit\" %% \"jmockit\" %% \"%s\" %% \"%s\"", "1.34", "test"));
        sbtLines.add(")");

        return StringUtils.join(sbtLines, "\n");
    }

    private void importSbtProject(String root) {
        Project project = this.module.getProject();
        final ProjectSystemId externalSystemId = new ProjectSystemId("SBT");
        final ExternalSystemManager<?,?,?,?,?> manager = ExternalSystemApiUtil.getManager(externalSystemId);
        if (manager == null) {
            return;
        }

        ProjectImportProvider[] projectImportProviders = new ProjectImportProvider[1];
        for (ProjectImportProvider provider : ProjectImportProvider.PROJECT_IMPORT_PROVIDER.getExtensions()) {
            if (provider instanceof AbstractExternalProjectImportProvider
                    && externalSystemId.equals(((AbstractExternalProjectImportProvider)provider).getExternalSystemId()))
            {
                projectImportProviders[0] = provider;
                break;
            }
        }

        if (projectImportProviders[0] == null) {
            return;
        }

        final VirtualFile projectFile = VfsUtil.findFile(Paths.get(root, "build.sbt"), true);
        if (projectFile != null) {
            ApplicationManager.getApplication().invokeLater(() -> {
                AddModuleWizard wizard = ImportModuleAction.createImportWizard(project,
                        null,
                        projectFile,
                        projectImportProviders);

                if (wizard != null && (wizard.getStepCount() <= 0 || wizard.showAndGet())) {
                    ImportModuleAction.createFromWizard(project, wizard);
                }
            });
        }
    }
}
