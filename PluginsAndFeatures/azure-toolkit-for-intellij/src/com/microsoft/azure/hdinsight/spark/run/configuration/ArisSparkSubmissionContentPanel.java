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

package com.microsoft.azure.hdinsight.spark.run.configuration;

import com.google.common.collect.ImmutableSortedSet;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.HideableTitledPanel;
import com.microsoft.azure.hdinsight.common.ClusterManagerEx;
import com.microsoft.azure.hdinsight.metadata.ClusterMetaDataService;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.spark.ui.*;
import com.microsoft.azure.sqlbigdata.sdk.cluster.SqlBigDataLivyLinkClusterDetail;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import org.jetbrains.annotations.Nullable;
import rx.Observable;

import javax.swing.*;
import javax.swing.event.ListDataListener;
import java.awt.*;
import java.util.stream.Collectors;

public class ArisSparkSubmissionContentPanel extends SparkSubmissionContentPanelConfigurable {
    @NotNull
    private final Project myProject;

    public ArisSparkSubmissionContentPanel(@NotNull Project project) {
        super(project);

        this.myProject = project;
        registerCtrlListeners();
        this.jobUploadStorageCtrl = new SparkSubmissionJobUploadStorageCtrl(getStorageWithUploadPathPanel()) {
            @Nullable
            @Override
            public String getClusterName() {
                IClusterDetail clusterDetail = getSelectedClusterDetail();
                return clusterDetail == null ? null : clusterDetail.getName();
            }

            @Nullable
            @Override
            public IClusterDetail getClusterDetail() {
                return getClusterDetails()
                        .stream()
                        .filter(clusterDetail -> clusterDetail.getName().equals(getClusterName()))
                        .findFirst()
                        .orElse(null);
            }
        };
    }

    @Override
    @NotNull
    protected ImmutableSortedSet<? extends IClusterDetail> getClusterDetails() {
        return ImmutableSortedSet.copyOf((x, y) -> x.getTitle().compareToIgnoreCase(y.getTitle()),
                ClusterMetaDataService.getInstance().getCachedClusterDetails().stream()
                        .filter(cluster -> cluster instanceof SqlBigDataLivyLinkClusterDetail)
                        .collect(Collectors.toList()));
    }

    @NotNull
    @Override
    protected Observable<ImmutableSortedSet<? extends IClusterDetail>> getClusterDetailsWithRefresh() {
        return Observable.fromCallable(() -> ClusterManagerEx.getInstance().getClusterDetails()
                .stream()
                .filter(cluster -> cluster instanceof SqlBigDataLivyLinkClusterDetail)
                .collect(Collectors.toList()))
                .map(list -> ImmutableSortedSet.copyOf((x, y) -> x.getTitle().compareToIgnoreCase(y.getTitle()), list));
    }
}
