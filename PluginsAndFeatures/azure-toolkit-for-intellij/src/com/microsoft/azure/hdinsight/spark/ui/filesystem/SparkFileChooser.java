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
package com.microsoft.azure.hdinsight.spark.ui.filesystem;

import com.intellij.execution.ExecutionException;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDialog;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ArrayUtil;
import com.microsoft.azure.hdinsight.common.ClusterManagerEx;
import com.microsoft.azure.hdinsight.common.MessageInfoType;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.spark.common.Deployable;
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitModel;
import com.microsoft.azure.hdinsight.spark.run.SparkBatchJobDeployFactory;
import org.jetbrains.annotations.NotNull;
import rx.subjects.PublishSubject;

import java.awt.*;
import java.util.List;
import java.util.*;

public class SparkFileChooser {

    public static List<VirtualFile> setRoots(@NotNull SparkSubmitModel submitModel) {
        String clusterName = submitModel.getSubmissionParameter().getClusterName();
        Optional<IClusterDetail> clusterDetail = ClusterManagerEx.getInstance().getClusterDetailByName(clusterName);

        if (clusterDetail.isPresent()) {
            PublishSubject<AbstractMap.SimpleImmutableEntry<MessageInfoType, String>> ctrlSubject = PublishSubject.create();
            try {
                Deployable jobDeploy = SparkBatchJobDeployFactory.getInstance().buildSparkBatchJobDeploy(submitModel, ctrlSubject);
                String rootPath = submitModel.getJobUploadStorageModel().getUploadPath().replace("/SparkSubmission/", "");

                HashMap<String, SparkVirtualFile> parentFiles = new HashMap<>();
                SparkVirtualFile root = new SparkVirtualFile("SparkSubmission", true, rootPath);
                parentFiles.put(root.getPath(), root);

                jobDeploy.listRemoteFiles(rootPath)
                        .map(path -> new SparkVirtualFile(path.getName(), path.isDirectory(), rootPath))
                        .doOnNext(file -> {
                            if (file.isDirectory()) {
                                parentFiles.put(file.getPath(), file);
                            }

                            String parentPath = file.getPath().substring(0, file.getPath().lastIndexOf("/"));
                            if (parentFiles.keySet().contains(parentPath)) {
                                parentFiles.get(parentPath).addChildren(file);
                                file.setParent(parentFiles.get(parentPath));
                            }
                        }).toBlocking().subscribe(ob -> {
                });

                return Arrays.asList(root);
            } catch (ExecutionException e) {
                return Arrays.asList(SparkVirtualFile.empty);
            }
        }

        return Arrays.asList(SparkVirtualFile.empty);
    }

    public static VirtualFile chooseFile(FileChooserDescriptor descriptor) {
        Component parentComponent = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
        final FileChooserDialog chooser = new SparkFileChooserDialogImpl(descriptor, parentComponent, null);
        return ArrayUtil.getFirstElement(chooser.choose(null, new SparkVirtualFile[]{null}));
    }
}
