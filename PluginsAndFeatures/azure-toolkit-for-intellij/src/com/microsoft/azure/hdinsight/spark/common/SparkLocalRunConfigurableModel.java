/*
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
 */

package com.microsoft.azure.hdinsight.spark.common;

import com.intellij.execution.CommonJavaRunConfigurationParameters;
import com.intellij.execution.ExternalizablePath;
import com.intellij.openapi.project.Project;
import com.intellij.util.PathUtil;
import com.intellij.util.xmlb.XmlSerializer;
import com.intellij.util.xmlb.annotations.MapAnnotation;
import com.intellij.util.xmlb.annotations.Tag;
import com.intellij.util.xmlb.annotations.Transient;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

@Tag("spark-local-run-configurable-model")
public class SparkLocalRunConfigurableModel implements CommonJavaRunConfigurationParameters {
    @Tag(value = "is-parallel-execution", textIfEmpty = "false")
    private boolean isParallelExecution;
    @Tag(value = "is-pass-parent-envs", textIfEmpty = "true")
    private boolean isPassParentEnvs = true;
    @Transient
    @NotNull
    private Project project;
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
    @Tag("data-root")
    @NotNull
    private String dataRootDirectory;

    public SparkLocalRunConfigurableModel(@NotNull Project project) {
        this.project = project;
        this.setWorkingDirectory(PathUtil.getLocalPath(project.getBaseDir().getPath()));
        this.dataRootDirectory = "";
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
        return dataRootDirectory;
    }

    public void setDataRootDirectory(@NotNull String dataRootDirectory) {
        this.dataRootDirectory = dataRootDirectory;
    }

    @Transient
    @Nullable
    @Override
    public String getPackage() {
        return null;
    }

    public Element exportToElement() {
        return XmlSerializer.serialize(this);
    }

    public void applyFromElement(Element element) {
        XmlSerializer.deserializeInto(this, element);
    }
}