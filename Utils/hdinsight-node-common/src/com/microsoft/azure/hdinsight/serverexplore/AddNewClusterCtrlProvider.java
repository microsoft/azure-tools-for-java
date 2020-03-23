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

package com.microsoft.azure.hdinsight.serverexplore;

import com.google.common.collect.ImmutableList;
import com.microsoft.azure.hdinsight.common.ClusterManagerEx;
import com.microsoft.azure.hdinsight.common.mvc.IdeSchedulers;
import com.microsoft.azure.hdinsight.common.mvc.SettableControl;
import com.microsoft.azure.hdinsight.sdk.cluster.*;
import com.microsoft.azure.hdinsight.sdk.common.AuthType;
import com.microsoft.azure.hdinsight.sdk.common.AuthenticationException;
import com.microsoft.azure.hdinsight.sdk.storage.HDStorageAccount;
import com.microsoft.azure.hdinsight.spark.jobs.JobUtils;
import com.microsoft.azure.sqlbigdata.sdk.cluster.SqlBigDataLivyLinkClusterDetail;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.StringHelper;
import com.microsoft.tooling.msservices.helpers.azure.sdk.StorageClientSDKManager;
import com.microsoft.tooling.msservices.model.storage.BlobContainer;
import com.microsoft.tooling.msservices.model.storage.ClientStorageAccount;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.client.utils.URIUtils;
import rx.Observable;

import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLPeerUnverifiedException;
import java.net.URI;
import java.security.cert.CertificateException;
import java.util.Optional;
import java.util.stream.Collectors;

public class AddNewClusterCtrlProvider {
    private static final String URL_PREFIX = "https://";
    private static final String UserRejectCAErrorMsg =
            "You have rejected the untrusted servers'certificate.\r\n" +
            "Please click'OK',then accept the untrusted certificate \r\n"+
            "if you want to link to this cluster.Or you can update the \r\n" +
            "host to link to a different cluster.";
    private static final String InvalidCertificateErrorMsg =
            "The cluster certificate is invalid.";
    private static final String InvalidCertificateInfoMsg =
            "If you want to bypass certificate verification for SQL Server Big Data clusters, " +
            "check the option in menu option: \"Tools | Azure | Validate Spark Cluster SSL Certificate | Disable\"";

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
            URI uri = URI.create(url);
            if (URIUtils.extractHost(uri) == null) {
                return false;
            } else {
                return true;
            }
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * Check if cluster name exists in:
     * 1. clusters under user's azure account subscription
     * 2. linked HDI cluster by user
     * 3. linked livy cluster by user
     * 4. Emulator cluster
     * @param clusterName
     * @return whether cluster name exists or not
     */
    public boolean doesClusterNameExistInAllHDInsightClusters(@NotNull String clusterName) {
        return ClusterManagerEx.getInstance().getCachedClusters().stream()
                .filter(ClusterManagerEx.getInstance().getHDInsightClusterFilterPredicate())
                .anyMatch(clusterDetail -> clusterDetail.getName().equals(clusterName));
    }

    /**
     * Check if cluster name exists in:
     * 1. linked HDI cluster by user
     * 2. linked livy cluster by user
     * @param clusterName
     * @return whether cluster name exists or not
     */
    public boolean doesClusterNameExistInLinkedHDInsightClusters(@NotNull String clusterName) {
        return ClusterManagerEx.getInstance().getAdditionalClusterDetails().stream()
                .filter(clusterDetail -> clusterDetail instanceof HDInsightAdditionalClusterDetail ||
                        clusterDetail instanceof HDInsightLivyLinkClusterDetail)
                .anyMatch(clusterDetail -> clusterDetail.getName().equals(clusterName));
    }

    /**
     * Check if livy endpoint exists in:
     * 1. clusters under user's azure account subscription
     * 2. linked HDI cluster by user
     * 3. linked livy cluster by user
     * @param livyEndpoint
     * @return whether livy endpoint exists or not
     */
    public boolean doesClusterLivyEndpointExistInAllHDInsightClusters(@NotNull String livyEndpoint) {
        return ClusterManagerEx.getInstance().getCachedClusters().stream()
                .filter(clusterDetail -> clusterDetail instanceof ClusterDetail ||
                        clusterDetail instanceof HDInsightAdditionalClusterDetail ||
                        clusterDetail instanceof HDInsightLivyLinkClusterDetail)
                .anyMatch(clusterDetail ->
                        URI.create(((LivyCluster) clusterDetail).getLivyConnectionUrl()).getHost()
                                .equals(URI.create(livyEndpoint).getHost()));
    }

    /**
     * Check if cluster name exists in:
     * 1. Livy Linked SQL Big Data clusters
     * @param clusterName
     * @return whether cluster name exists or not
     */
    public boolean doesClusterNameExistInSqlBigDataClusters(@NotNull String clusterName) {
        return ClusterManagerEx.getInstance().getAdditionalClusterDetails().stream()
                .filter(clusterDetail -> clusterDetail instanceof SqlBigDataLivyLinkClusterDetail)
                .anyMatch(clusterDetail -> clusterDetail.getName().equals(clusterName));
    }

    public boolean doeshostExistInSqlBigDataClusters(@NotNull String host) {
        return ClusterManagerEx.getInstance().getAdditionalClusterDetails().stream()
                .filter(clusterDetail -> clusterDetail instanceof SqlBigDataLivyLinkClusterDetail)
                .anyMatch(clusterDetail -> ((SqlBigDataLivyLinkClusterDetail) clusterDetail).getHost().equals(host));
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
                    SparkClusterType sparkClusterType = toUpdate.getSparkClusterType();
                    String clusterNameOrUrl = toUpdate.getClusterName();
                    String userName = Optional.ofNullable(toUpdate.getUserName()).orElse("");
                    AuthType authType = toUpdate.getAuthType();
                    String storageName = toUpdate.getStorageName();
                    String storageKey = toUpdate.getStorageKey();
                    String password = Optional.ofNullable(toUpdate.getPassword()).orElse("");
                    URI livyEndpoint = toUpdate.getLivyEndpoint();
                    URI yarnEndpoint = toUpdate.getYarnEndpoint();
                    String host = toUpdate.getHost();
                    int knoxPort = toUpdate.getKnoxPort();
                    int selectedContainerIndex = toUpdate.getSelectedContainerIndex();


                    // For HDInsight linked cluster, only real cluster name or real cluster endpoint(pattern as https://sparkcluster.azurehdinsight.net/) are allowed to be cluster name
                    // For HDInsight livy linked or aris linked cluster, cluster name format is not restricted
                    final String clusterName = sparkClusterType == SparkClusterType.HDINSIGHT_CLUSTER
                            ? getClusterName(clusterNameOrUrl)
                            : clusterNameOrUrl;

                    // These validation check are redundant for intelliJ sicne intellij does full check at view level
                    // but necessary for Eclipse
                    HDStorageAccount storageAccount = null;
                    if (sparkClusterType == SparkClusterType.HDINSIGHT_CLUSTER) {
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

                        // Cluster name check
                        if (clusterName == null) {
                            return toUpdate.setErrorMessage("Wrong cluster name or endpoint");
                        }

                        // Duplication check
                        if (ClusterManagerEx.getInstance().getAdditionalClusterDetails().stream()
                                .filter(clusterDetail -> !(clusterDetail instanceof SqlBigDataLivyLinkClusterDetail))
                                .anyMatch(clusterDetail -> clusterDetail.getName().equals(clusterName))) {
                            return toUpdate.setErrorMessage("Cluster already exists in linked list");
                        }

                        // Storage access check
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
                    }

                    IClusterDetail additionalClusterDetail = null;
                    switch (sparkClusterType) {
                        case HDINSIGHT_CLUSTER:
                            additionalClusterDetail =
                                  authType == AuthType.BasicAuth
                                          ? new HDInsightAdditionalClusterDetail(clusterName, userName, password, storageAccount)
                                          : new MfaHdiAdditionalClusterDetail(clusterName, userName, password, storageAccount);
                            break;
                        case LIVY_LINK_CLUSTER:
                            additionalClusterDetail =
                                    new HDInsightLivyLinkClusterDetail(livyEndpoint, yarnEndpoint, clusterName, userName, password);
                            break;
                        case SQL_BIG_DATA_CLUSTER:
                            additionalClusterDetail =
                                    new SqlBigDataLivyLinkClusterDetail(host, knoxPort, clusterName, userName, password);
                    }

                    // Account certificate check
                    try {
                        JobUtils.authenticate(additionalClusterDetail);
                    } catch (AuthenticationException authErr) {
                        String errorMsg = "Authentication Error: " + Optional.ofNullable(authErr.getMessage())
                                .filter(msg -> !msg.isEmpty())
                                .orElse("Wrong username/password") +
                                " (" + authErr.getErrorCode() + ")";
                        return toUpdate
                                .setErrorMessage(errorMsg)
                                .setErrorMessageList(ImmutableList.of(Pair.of(toUpdate.ERROR_OUTPUT, errorMsg)));
                    } catch (SSLHandshakeException ex) {
                        //user rejects the ac when linking aris cluster
                        if (sparkClusterType == SparkClusterType.SQL_BIG_DATA_CLUSTER && ex.getCause() instanceof CertificateException) {
                            return toUpdate
                                    .setErrorMessage(UserRejectCAErrorMsg)
                                    .setErrorMessageList(ImmutableList.of(Pair.of(toUpdate.ERROR_OUTPUT, UserRejectCAErrorMsg)));
                        }

                        String errorMsg = "Authentication Error: " + ex.getMessage();
                        return toUpdate
                                .setErrorMessage(errorMsg)
                                .setErrorMessageList(ImmutableList.of(Pair.of(toUpdate.ERROR_OUTPUT, errorMsg)));
                    } catch (SSLPeerUnverifiedException ex) {
                        String errorMsg = "Authentication Error: " + ex.getMessage();
                        return toUpdate
                                .setErrorMessage(errorMsg)
                                .setErrorMessageList(ImmutableList.of(
                                        Pair.of(toUpdate.ERROR_OUTPUT, errorMsg),
                                        Pair.of(toUpdate.ERROR_OUTPUT, InvalidCertificateErrorMsg),
                                        Pair.of(toUpdate.NORMAL_OUTPUT, InvalidCertificateInfoMsg)));
                    } catch (Exception ex) {
                        String errorMsg = "Authentication Error: " + ex.getMessage();
                        return toUpdate
                                .setErrorMessage(errorMsg)
                                .setErrorMessageList(ImmutableList.of(Pair.of(toUpdate.ERROR_OUTPUT, errorMsg)));
                    }

                    // No issue
                    ClusterManagerEx.getInstance().addAdditionalCluster(additionalClusterDetail);

                    return toUpdate.setErrorMessage(null).setErrorMessageList(null);
                })
                .observeOn(ideSchedulers.dispatchUIThread())     // UI operation needs to be in dispatch thread
                .doOnNext(controllableView::setData)
                .filter(data -> StringUtils.isEmpty(data.getErrorMessage()));
    }
}
