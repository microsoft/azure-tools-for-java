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
 * SOFTWARE.
 */

package com.microsoft.azure.hdinsight.serverexplore;

import com.microsoft.azure.hdinsight.common.ClusterManagerEx;
import com.microsoft.azure.hdinsight.common.mvc.IdeSchedulers;
import com.microsoft.azure.hdinsight.common.mvc.SettableControl;
import com.microsoft.azure.hdinsight.sdk.cluster.HDInsightAdditionalClusterDetail;
import com.microsoft.azure.hdinsight.sdk.cluster.HDInsightLivyLinkClusterDetail;
import com.microsoft.azure.hdinsight.sdk.cluster.LivyCluster;
import com.microsoft.azure.hdinsight.sdk.common.AuthenticationException;
import com.microsoft.azure.hdinsight.sdk.storage.HDStorageAccount;
import com.microsoft.azure.hdinsight.spark.jobs.JobUtils;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.StringHelper;
import com.microsoft.tooling.msservices.helpers.azure.sdk.StorageClientSDKManager;
import com.microsoft.tooling.msservices.model.storage.BlobContainer;
import com.microsoft.tooling.msservices.model.storage.ClientStorageAccount;
import org.apache.commons.lang3.StringUtils;
import rx.Observable;

import java.net.URI;
import java.util.Optional;
import java.util.stream.Collectors;

public class AddNewClusterCtrlProvider {
    private static final String URL_PREFIX = "https://";

    @NotNull
    private SettableControl<AddNewClusterModel> controllableView;

    @NotNull
    private IdeSchedulers ideSchedulers;

    public AddNewClusterCtrlProvider(@NotNull SettableControl<AddNewClusterModel> controllableView,
                                     @NotNull IdeSchedulers ideSchedulers) {
        this.controllableView = controllableView;
        this.ideSchedulers = ideSchedulers;
    }

    //format input string
    public static String getClusterName(String userNameOrUrl) {
        if (userNameOrUrl.startsWith(URL_PREFIX)) {
            return StringHelper.getClusterNameFromEndPoint(userNameOrUrl);
        } else {
            return userNameOrUrl;
        }
    }

    public boolean isURLValid(@NotNull String url) {
        try {
            URI.create(url);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    public boolean doesClusterNameExist(@NotNull String clusterName) {
        if (ClusterManagerEx.getInstance().getHdinsightAdditionalClusterDetails().stream().anyMatch(clusterDetail ->
                clusterDetail.getName().equals(clusterName))) {
            return true;
        }
        return false;
    }

    public boolean doesClusterLivyEndpointExist(@NotNull String livyEndpoint) {
        if (ClusterManagerEx.getInstance().getHdinsightAdditionalClusterDetails().stream()
                .filter(cluster -> cluster instanceof LivyCluster)
                .anyMatch(clusterDetail ->
                        URI.create(((LivyCluster) clusterDetail).getLivyConnectionUrl()).resolve("/").toString()
                                .equals(URI.create(livyEndpoint).resolve("/").toString()))) {
            return true;
        }
        return false;
    }

    public Observable<AddNewClusterModel> refreshContainers() {
        return Observable.just(new AddNewClusterModel())
                .doOnNext(controllableView::getData)
                .observeOn(ideSchedulers.processBarVisibleAsync("Getting storage account containers..."))
                .map(toUpdate -> {
                    toUpdate.setContainers(null);

                    if (StringUtils.isNotBlank(toUpdate.getStorageName()) && StringUtils.isNotBlank(toUpdate.getStorageKey())) {
                        try {
                            ClientStorageAccount storageAccount = new ClientStorageAccount(toUpdate.getStorageName());
                            storageAccount.setPrimaryKey(toUpdate.getStorageKey());

                            toUpdate.setContainers(
                                    StorageClientSDKManager
                                            .getManager()
                                            .getBlobContainers(storageAccount.getConnectionString())
                                            .stream()
                                            .map(BlobContainer::getName)
                                            .collect(Collectors.toList()));
                        } catch (Exception ex) {
                            return toUpdate.setErrorMessage("Can't get storage containers, check if the key matches");
                        }
                    }

                    return toUpdate.setErrorMessage(null);
                })
                .observeOn(ideSchedulers.dispatchUIThread())
                .doOnNext(controllableView::setData);
    }


    public Observable<AddNewClusterModel> validateAndAdd() {
        return Observable.just(new AddNewClusterModel())
                .doOnNext(controllableView::getData)
                .observeOn(ideSchedulers.processBarVisibleAsync("Validating the cluster settings..."))
                .map(toUpdate -> {
                    String clusterNameOrUrl = toUpdate.getClusterName();
                    String userName = toUpdate.getUserName();
                    String storageName = toUpdate.getStorageName();
                    String storageKey = toUpdate.getStorageKey();
                    String password = toUpdate.getPassword();
                    URI livyEndpoint = toUpdate.getLivyEndpoint();
                    URI yarnEndpoint = toUpdate.getYarnEndpoint();
                    Boolean isHDInsightClusterSelected = toUpdate.getHDInsightClusterSelected();
                    int selectedContainerIndex = toUpdate.getSelectedContainerIndex();

                    // These validation check are redundant for intelliJ sicne intellij does full check at view level
                    // but necessary for Eclipse

                    // Incomplete data check
                    // link through livy don't need to verify empty username and password
                    if (livyEndpoint == null) {
                        if (StringUtils.containsWhitespace(clusterNameOrUrl) ||
                                StringUtils.containsWhitespace(userName) ||
                                StringUtils.containsWhitespace(password)) {
                            String highlightPrefix = "* ";

                            if (!toUpdate.getClusterNameLabelTitle().startsWith(highlightPrefix)) {
                                toUpdate.setClusterNameLabelTitle(highlightPrefix + toUpdate.getClusterNameLabelTitle());
                            }

                            if (!toUpdate.getUserNameLabelTitle().startsWith(highlightPrefix)) {
                                toUpdate.setUserNameLabelTitle(highlightPrefix + toUpdate.getUserNameLabelTitle());
                            }

                            if (!toUpdate.getPasswordLabelTitle().startsWith(highlightPrefix)) {
                                toUpdate.setPasswordLabelTitle(highlightPrefix + toUpdate.getPasswordLabelTitle());
                            }

                            return toUpdate.setErrorMessage("All (*) fields are required.");
                        }
                    }

                    String clusterName = getClusterName(clusterNameOrUrl);

                    // Cluster name check
                    if (clusterName == null) {
                        return toUpdate.setErrorMessage("Wrong cluster name or endpoint");
                    }

                    // Duplication check
                    if (ClusterManagerEx.getInstance().getHdinsightAdditionalClusterDetails().stream().anyMatch(clusterDetail ->
                            clusterDetail.getName().equals(clusterName))) {
                        return toUpdate.setErrorMessage("Cluster already exists in linked list");
                    }

                    // Storage access check
                    HDStorageAccount storageAccount = null;
                    if (StringUtils.isNotEmpty(storageName)) {
                        ClientStorageAccount storageAccountClient = new ClientStorageAccount(storageName);
                        storageAccountClient.setPrimaryKey(storageKey);

                        // Storage Key check
                        try {
                            StorageClientSDKManager.getCloudStorageAccount(storageAccountClient.getConnectionString());
                        } catch (Exception ex) {
                            return toUpdate.setErrorMessage("Storage key doesn't match the account.");
                        }

                        // Containers selection check
                        if (selectedContainerIndex < 0 ||
                                selectedContainerIndex >= toUpdate.getContainers().size()) {
                            return toUpdate.setErrorMessage("The storage container isn't selected");
                        }

                        storageAccount = new HDStorageAccount(
                                null,
                                ClusterManagerEx.getInstance().getBlobFullName(storageName),
                                storageKey,
                                false,
                                toUpdate.getContainers().get(selectedContainerIndex));
                    }


                    HDInsightAdditionalClusterDetail hdInsightAdditionalClusterDetail = isHDInsightClusterSelected ?
                            new HDInsightAdditionalClusterDetail(clusterName, userName, password, storageAccount) :
                            new HDInsightLivyLinkClusterDetail(livyEndpoint, yarnEndpoint, clusterName, userName, password);

                    // Account certificate check
                    try {
                        JobUtils.authenticate(hdInsightAdditionalClusterDetail);
                    } catch (AuthenticationException authErr) {
                        return toUpdate.setErrorMessage("Authentication Error: " + Optional.ofNullable(authErr.getMessage())
                                .filter(msg -> !msg.isEmpty())
                                .orElse("Wrong username/password") +
                                " (" + authErr.getErrorCode() + ")");
                    } catch (Exception ex) {
                        return toUpdate.setErrorMessage("Authentication Error: " + ex.getMessage());
                    }

                    // No issue
                    ClusterManagerEx.getInstance().addHDInsightAdditionalCluster(hdInsightAdditionalClusterDetail);

                    return toUpdate.setErrorMessage(null);
                })
                .observeOn(ideSchedulers.dispatchUIThread())     // UI operation needs to be in dispatch thread
                .doOnNext(controllableView::setData)
                .filter(data -> StringUtils.isEmpty(data.getErrorMessage()));
    }
}
