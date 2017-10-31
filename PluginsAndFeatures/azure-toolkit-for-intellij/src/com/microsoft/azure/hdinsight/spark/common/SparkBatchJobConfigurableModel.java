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

import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializer;
import com.intellij.util.xmlb.annotations.Tag;
import com.intellij.util.xmlb.annotations.Transient;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;


@Tag("spark-job-configuration")
public class SparkBatchJobConfigurableModel {
    @Tag("local-run")
    @NotNull
    private SparkLocalRunConfigurableModel localRunConfigurableModel;
    @Transient
    @NotNull
    private SparkSubmitModel submitModel;

    public SparkBatchJobConfigurableModel(@NotNull Project project) {
        localRunConfigurableModel = new SparkLocalRunConfigurableModel(project);
        submitModel = new SparkSubmitModel(project);
    }

    @Transient
    @NotNull
    public SparkLocalRunConfigurableModel getLocalRunConfigurableModel() {
        return localRunConfigurableModel;
    }

    public void setLocalRunConfigurableModel(final SparkLocalRunConfigurableModel localRunConfigurableModel) {
        this.localRunConfigurableModel = localRunConfigurableModel;
    }

    @Transient
    @NotNull
    public SparkSubmitModel getSubmitModel() {
        return submitModel;
    }

    public void setSubmitModel(SparkSubmitModel submitModel) {
        this.submitModel = submitModel;
    }

    public Element exportToElement() {
        Element jobConfElement = XmlSerializer.serialize(this);

        jobConfElement.addContent(getSubmitModel().exportToElement());

        return jobConfElement;
    }

    public void applyFromElement(Element element) {
        Element root = element.getChild("spark-job-configuration");

        if (root != null) {
            Optional.ofNullable(root.getChild("local-run"))
                    .map(elem -> (Element) elem.getContent(0))
                    .ifPresent(elem -> getLocalRunConfigurableModel().applyFromElement(elem));

            Optional.ofNullable(root.getChild("spark_submission"))
                    .map(elem -> SparkSubmitModel.factoryFromElement(getSubmitModel().getProject(), elem))
                    .ifPresent(this::setSubmitModel);
        }
    }
}