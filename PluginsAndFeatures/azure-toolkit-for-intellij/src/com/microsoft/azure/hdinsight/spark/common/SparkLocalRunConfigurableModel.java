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

package com.microsoft.azure.hdinsight.spark.common;

import com.intellij.execution.CommonJavaRunConfigurationParameters;
import com.intellij.execution.ExternalizablePath;
import com.intellij.openapi.command.impl.DummyProject;
import com.intellij.openapi.project.Project;
import com.intellij.util.PathUtil;
import com.intellij.util.xmlb.annotations.MapAnnotation;
import com.intellij.util.xmlb.annotations.Tag;
import com.intellij.util.xmlb.annotations.Transient;
import com.microsoft.azure.hdinsight.common.logger.ILogger;
import org.apache.commons.lang3.SystemUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.microsoft.azure.hdinsight.spark.ui.SparkLocalRunParamsPanel.HADOOP_HOME_ENV;
import static com.microsoft.azure.hdinsight.spark.ui.SparkLocalRunParamsPanel.WINUTILS_EXE_NAME;

@Tag("spark-local-run-configurable-model")
public class SparkLocalRunConfigurableModel implements CommonJavaRunConfigurationParameters, ILogger {
    @Tag(value = "is-parallel-execution", textIfEmpty = "false")
    private boolean isParallelExecution;
    @Tag(value = "is-pass-parent-envs", textIfEmpty = "true")
    private boolean isPassParentEnvs = true;
    @Transient
    @NotNull
    private Project project = DummyProject.getInstance();
    @Tag("program-parameters")
    @Nullable
    private String programParameters;
    @Tag("working-directory")
    @Nullable
    private String workingDirectory;
    @Tag("envs")
    @MapAnnotation(entryTagName="envs")
    @NotNull
    private Map<String, String> envs = new HashMap<>();
    @Tag("vm-parameters")
    @Nullable
    private String vmParameters;
    @Tag("main-class")
    @Nullable
    private String mainClass;
    @Tag("classpath-module")
    @Nullable
    private String classpathModule;
    @Tag("data-root")
    @Nullable
    private String dataRootDirectory;

    // For XML deserialization
    private SparkLocalRunConfigurableModel() {
    }

    public SparkLocalRunConfigurableModel(@NotNull Project project) {
        final String projectBasePath = project.getBasePath();
        this.project = project;
        this.setWorkingDirectory(PathUtil.getLocalPath(
                projectBasePath != null ? projectBasePath : Paths.get("").toAbsolutePath().toString()));
        this.setDataRootDirectory(Paths.get(this.getWorkingDirectory(), "data").toString());

        if (SystemUtils.IS_OS_WINDOWS) {
            try {
                Optional.ofNullable(System.getenv(HADOOP_HOME_ENV))
                        .map(hadoopHome -> Paths.get(hadoopHome, "bin", WINUTILS_EXE_NAME).toString())
                        .map(File::new)
                        .filter(File::exists)
                        .map(winUtilsFile -> winUtilsFile.getParentFile().getParent())
                        .ifPresent(hadoopHome -> this.envs.put(HADOOP_HOME_ENV, hadoopHome));
            } catch (Exception ex) {
                log().warn("Ignore HADOOP_HOME environment variable since an exception is thrown when finding winutils.exe: " + ex);
            }
        }
    }

    @Transient
    public boolean isIsParallelExecution() {
        return isParallelExecution;
    }

    public void setIsParallelExecution(final boolean isParallelExecution) {
        this.isParallelExecution = isParallelExecution;
    }

    @Transient
    @NotNull
    @Override
    public Project getProject() {
        return project;
    }

    public void setProject(@NotNull Project project) {
        this.project = project;
    }

    @Override
    public void setProgramParameters(@Nullable String s) {
        programParameters = s;
    }

    @Transient
    @Nullable
    @Override
    public String getProgramParameters() {
        return programParameters;
    }

    @Override
    public void setWorkingDirectory(@Nullable String s) {
        workingDirectory = ExternalizablePath.urlValue(s);
    }

    @Transient
    @NotNull
    @Override
    public String getWorkingDirectory() {
        return ExternalizablePath.localPathValue(workingDirectory);
    }

    @Override
    public void setEnvs(@NotNull Map<String, String> map) {
        if (map == envs) {
            return;
        }

        envs.clear();
        envs.putAll(map);
    }

    @Transient
    @NotNull
    @Override
    public Map<String, String> getEnvs() {
        return envs;
    }

    @Override
    public void setPassParentEnvs(boolean b) {
        isPassParentEnvs = b;
    }

    @Transient
    @Override
    public boolean isPassParentEnvs() {
        return isPassParentEnvs;
    }

    @Override
    public void setVMParameters(String s) {
        vmParameters = s;
    }

    @Transient
    @Override
    public String getVMParameters() {
        return vmParameters;
    }

    @Transient
    @Override
    public boolean isAlternativeJrePathEnabled() {
        return false;
    }

    @Override
    public void setAlternativeJrePathEnabled(boolean b) {

    }

    @Transient
    @Nullable
    @Override
    public String getAlternativeJrePath() {
        return null;
    }

    @Override
    public void setAlternativeJrePath(String s) {

    }

    @Transient
    @Nullable
    @Override
    public String getRunClass() {
        return mainClass;
    }

    public void setRunClass(String s) {
        mainClass = s;
    }

    @NotNull
    public String getDataRootDirectory() {
        return ExternalizablePath.localPathValue(dataRootDirectory);
    }

    public void setDataRootDirectory(@NotNull String dataRootDirectory) {
        this.dataRootDirectory = ExternalizablePath.urlValue(dataRootDirectory);
    }

    @Transient
    @Nullable
    @Override
    public String getPackage() {
        return null;
    }

    @Nullable
    public String getClasspathModule() {
        return classpathModule;
    }

    public void setClasspathModule(@Nullable String classpathModule) {
        this.classpathModule = classpathModule;
    }
}