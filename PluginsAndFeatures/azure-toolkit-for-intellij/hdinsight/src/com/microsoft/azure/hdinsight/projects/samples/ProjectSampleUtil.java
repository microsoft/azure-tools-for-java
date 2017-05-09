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
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azure.hdinsight.common.StreamUtil;
import com.microsoft.azure.hdinsight.projects.HDInsightTemplatesType;
import com.microsoft.azuretools.azurecommons.helpers.StringHelper;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class ProjectSampleUtil {

    //sample file path should be start with "/"

    private static final String[] Java_Local_RunSample = new String[]{"/hdinsight/templates/java/JavaSparkPi.java"};
    private static final String[] Scala_Cluster_Run_Sample = new String[]{
            "/hdinsight/templates/scala/scala_cluster_run/SparkCore_WasbIOTest.scala",
            "/hdinsight/templates/scala/scala_cluster_run/SparkStreaming_HdfsWordCount.scala",
            "/hdinsight/templates/scala/scala_cluster_run/SparkSQL_RDDRelation.scala"
    };
    private static final String[] Scala_Local_Run_Sample = new String[]{
            "/hdinsight/templates/scala/scala_local_run/LogQuery.scala",
            "/hdinsight/templates/scala/scala_local_run/SparkML_RankingMetricsExample.scala"
    };
    private static final String[] Scala_Local_Run_Sample_Data = new String[]{"/hdinsight/templates/scala/scala_local_run/data/sample_movielens_data.txt"};

    public static void copyFileToPath(Module module, HDInsightTemplatesType templatesType) {
        String sourcePath = getRootOrSourceFolder(module, true);
        String rootPath = getRootOrSourceFolder(module, false);

        if (StringHelper.isNullOrWhiteSpace(sourcePath) || StringHelper.isNullOrWhiteSpace(rootPath)) {
            DefaultLoader.getUIHelper().showError("Failed get root or resource folder of current module", "Create Sample Project");
        }else {
            try {
                if (templatesType == HDInsightTemplatesType.ScalaLocalSample) {
                    copyFileToPath(Scala_Local_Run_Sample, sourcePath);
                    copyFileToPath(Scala_Local_Run_Sample_Data, StringHelper.concat(rootPath, File.separator, "data"));
                } else if (templatesType == HDInsightTemplatesType.ScalaClusterSample) {
                    copyFileToPath(Scala_Cluster_Run_Sample, sourcePath);
                } else if(templatesType == HDInsightTemplatesType.JavaLocalSample) {
                    copyFileToPath(Java_Local_RunSample, sourcePath);
                }
            } catch (Exception e) {
                DefaultLoader.getUIHelper().showError("Failed to get the sample resource folder for project", "Create Sample Project");
            }
        }
    }

    public static String getRootOrSourceFolder(Module module, boolean isSourceFolder) {
        ModuleRootManager moduleRootManager = module.getComponent(ModuleRootManager.class);
        if(module == null) {
            return null;
        }
        VirtualFile[] files = isSourceFolder ? moduleRootManager.getSourceRoots() : moduleRootManager.getContentRoots();

        if(files.length == 0) {
            DefaultLoader.getUIHelper().showError("Source Root should be created if you want to create a new sample project", "Create Sample Project");
            return null;
        }
        return files[0].getPath();
    }

    @NotNull
    private static String getNameFromPath(@NotNull String path) {
        int index = path.lastIndexOf('/');
        return path.substring(index);
    }

    private static void copyFileToPath(String[] resources, String toPath) throws Exception {
        for (int i = 0; i < resources.length; ++i) {
            File file = StreamUtil.getResourceFile(resources[i]);

            if (file == null) {
                DefaultLoader.getUIHelper().showError("Failed to get the sample resource folder for project", "Create Sample Project");
            } else {
                String toFilePath = StringHelper.concat(toPath, getNameFromPath(resources[i]));
                FileUtil.copy(file, new File(toFilePath));
            }
        }
    }}
