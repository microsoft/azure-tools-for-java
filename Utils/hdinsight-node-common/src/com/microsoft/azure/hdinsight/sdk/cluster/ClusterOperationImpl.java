/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.cluster;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.microsoft.azure.hdinsight.sdk.common.AuthenticationErrorHandler;
import com.microsoft.azure.hdinsight.sdk.common.RequestCallback;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureCloud;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azuretools.authmanage.IdeAzureAccount;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.tooling.msservices.helpers.azure.rest.AzureAADHelper;
import com.microsoft.tooling.msservices.helpers.azure.rest.RestServiceManager;
import com.microsoft.tooling.msservices.helpers.azure.rest.RestServiceManagerBaseImpl;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

public class ClusterOperationImpl implements IClusterOperation {
     private static final String VERSION = "2015-03-01-preview";

     /**
      * list hdinsight cluster
      *
      * @param subscription
      * @return cluster raw data info
      * @throws IOException
      */
     public List<ClusterRawInfo> listCluster(final Subscription subscription) throws AzureCmdException {
          try {
               String response = requestWithToken(subscription.getTenantId(), (accessToken) -> {
                    String managementUrl = Azure.az(AzureCloud.class).getOrDefault().getResourceManagerEndpoint();
                    return AzureAADHelper.executeRequest(managementUrl,
                            String.format("/subscriptions/%s/providers/Microsoft.HDInsight/clusters?api-version=%s", subscription.getId(), VERSION),
                            RestServiceManager.ContentType.Json,
                            "GET",
                            null,
                            accessToken,
                            new RestServiceManagerBaseImpl());
                      });

               return new AuthenticationErrorHandler<List<ClusterRawInfo>>() {
                    @Override
                    public List<ClusterRawInfo> execute(String response) {
                         Type clustersInfoType = new TypeToken<ClustersCollection>() {}.getType();

                        ClustersCollection clustersCollection= new Gson().fromJson(response, clustersInfoType);
                         return clustersCollection.getValue();
                    }
               }.run(response);
          } catch (Throwable th) {
               throw new AzureCmdException("Error listing HDInsight clusters", th);
          }
     }

     /**
      * get cluster configuration including http username, password, storage and additional storage account
      *
      * @param subscription
      * @param clusterId
      * @return cluster configuration info
      * @throws IOException
      */
     public ClusterConfiguration getClusterConfiguration(final Subscription subscription, final String clusterId) throws AzureCmdException {
          try {
               String response = requestWithToken(subscription.getTenantId(), new RequestCallback<String>() {
                    @Override
                    public String execute(String accessToken) throws Throwable {
                         String managementURI = Azure.az(AzureCloud.class).getOrDefault().getResourceManagerEndpoint();
                         return AzureAADHelper.executeRequest(managementURI,
                                 String.format("%s/configurations?api-version=%s", clusterId.replaceAll("/+$", ""), VERSION),
                                 RestServiceManager.ContentType.Json,
                                 "GET",
                                 null,
                                 accessToken,
                                 new RestServiceManagerBaseImpl());
                    }
               });
               return new AuthenticationErrorHandler<ClusterConfiguration>() {
                    @Override
                    public ClusterConfiguration execute(String response) {
                         Type listType = new TypeToken<ClusterConfiguration>() {
                         }.getType();
                         ClusterConfiguration clusterConfiguration = new Gson().fromJson(response, listType);

                         if (clusterConfiguration == null || clusterConfiguration.getConfigurations() == null) {
                              return null;
                         }

                         return clusterConfiguration;
                    }
               }.run(response);
          } catch (Throwable th) {
               throw new AzureCmdException("Error getting cluster configuration", th);
          }
     }

     @NotNull
     public <T> T requestWithToken(@NotNull String tenantId, @NotNull final RequestCallback<T> requestCallback)
             throws Throwable {
          String accessToken = IdeAzureAccount.getInstance().getAccessTokenForTrack1(tenantId);
          return requestCallback.execute(accessToken);
     }
}
