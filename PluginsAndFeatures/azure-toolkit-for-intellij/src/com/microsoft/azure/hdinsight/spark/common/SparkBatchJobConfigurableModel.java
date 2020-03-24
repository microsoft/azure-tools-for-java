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

import com.intellij.openapi.command.impl.DummyProject;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.util.xmlb.XmlSerializer;
import com.intellij.util.xmlb.annotations.Tag;
import com.intellij.util.xmlb.annotations.Transient;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import org.jdom.Element;

import java.util.Optional;


@Tag("spark-job-configuration")
public class SparkBatchJobConfigurableModel {
    @Transient
    private Project project;

    @Tag("local-run")
    @NotNull
    private SparkLocalRunConfigurableModel localRunConfigurableModel;
    @Transient
    @NotNull
    private SparkSubmitModel submitModel;
    @Tag("focused-tab-index")
    private int focusedTabIndex = 0;
    @Transient
    private boolean isLocalRunConfigEnabled = true;
    @Transient
    private boolean isClusterSelectionEnabled = true;

    public SparkBatchJobConfigurableModel() {
        this(DummyProject.getInstance());
    }

    public SparkBatchJobConfigurableModel(@NotNull Project project) {
        this.project = project;
        localRunConfigurableModel = new SparkLocalRunConfigurableModel(project);
        submitModel = new SparkSubmitModel(project);
    }

    @Transient
    @NotNull
    public SparkLocalRunConfigurableModel getLocalRunConfigurableModel() {
        return localRunConfigurableModel;
    }

    public void setLocalRunConfigurableModel(@NotNull final SparkLocalRunConfigurableModel localRunConfigurableModel) {
        this.localRunConfigurableModel = localRunConfigurableModel;
    }

    @Transient
    @NotNull
    public SparkSubmitModel getSubmitModel() {
        return submitModel;
    }

    public void setSubmitModel(@NotNull SparkSubmitModel submitModel) {
        this.submitModel = submitModel;
    }

    public Element exportToElement() throws WriteExternalException {
        Element jobConfElement = XmlSerializer.serialize(this);

        jobConfElement.addContent(getSubmitModel().exportToElement());

        return jobConfElement;
    }

    public void applyFromElement(Element element) {
        Element root = element.getChild("spark-job-configuration");

        if (root != null) {
            XmlSerializer.deserializeInto(this, root);

            // Transient fields
            this.localRunConfigurableModel.setProject(project);

            Optional.ofNullable(root.getChild("spark_submission"))
                    .ifPresent(elem -> getSubmitModel().applyFromElement(elem));
        }
    }

    public int getFocusedTabIndex() {
        return focusedTabIndex;
    }

    public void setFocusedTabIndex(int focusedTabIndex) {
        this.focusedTabIndex = focusedTabIndex;
    }

    @Transient
    public boolean isLocalRunConfigEnabled() {
        return isLocalRunConfigEnabled;
    }

    public void setLocalRunConfigEnabled(boolean localRunConfigEnabled) {
        isLocalRunConfigEnabled = localRunConfigEnabled;
    }

    @Transient
    public boolean isClusterSelectionEnabled() {
        return isClusterSelectionEnabled;
    }

    public void setClusterSelectionEnabled(boolean clusterSelectionEnabled) {
        this.isClusterSelectionEnabled = clusterSelectionEnabled;
    }
}
