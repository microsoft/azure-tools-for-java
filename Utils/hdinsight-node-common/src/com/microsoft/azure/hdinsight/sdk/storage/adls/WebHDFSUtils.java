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
package com.microsoft.azure.hdinsight.sdk.storage.adls;

import com.microsoft.azure.datalake.store.ADLException;
import com.microsoft.azure.datalake.store.ADLStoreClient;
import com.microsoft.azure.datalake.store.IfExists;
import com.microsoft.azure.hdinsight.common.HDInsightLoader;
import com.microsoft.azure.hdinsight.sdk.common.HDIException;
import com.microsoft.azure.hdinsight.sdk.storage.IHDIStorageAccount;
import com.microsoft.azure.hdinsight.spark.common.SparkBatchSubmission;
import com.microsoft.azure.management.dns.HttpStatusCode;
import com.microsoft.azuretools.authmanage.AuthMethodManager;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.sdkmanage.AzureManager;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.HttpClients;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

public class WebHDFSUtils {
    private static String getUserAgent() {
        final String installID = HDInsightLoader.getHDInsightHelper().getInstallationId();
        final String userAgentSource = WebHDFSUtils.class.getClassLoader().getClass().getName().toLowerCase().contains("intellij")
                ? "Azure Toolkit for IntelliJ" : "Azure Toolkit for Eclipse";
        return userAgentSource + installID;
    }

    public static void uploadFileToADLS(@NotNull IHDIStorageAccount storageAccount, @NotNull File localFile, @NotNull String remotePath, boolean overWrite) throws Exception {
        com.microsoft.azuretools.sdkmanage.AzureManager manager = AuthMethodManager.getInstance().getAzureManager();
        String tid = manager.getSubscriptionManager().getSubscriptionTenant(storageAccount.getSubscriptionId());
        String accessToken = manager.getAccessToken(tid);

        // TODO: accountFQDN should work for Mooncake
        String storageName = storageAccount.getName();
        ADLStoreClient client = ADLStoreClient.createClient(String.format("%s.azuredatalakestore.net", storageName), accessToken);
        OutputStream stream = null;
        try {
            stream = client.createFile(remotePath, IfExists.OVERWRITE);
            IOUtils.copy(new FileInputStream(localFile), stream);
            stream.flush();
            stream.close();
        } catch (ADLException e) {
            // ADLS operation may get a 403 when 'user' didn't have access to ADLS under Service Principle model
            // currently we didn't have a good way to solve this problem
            // we just popup the exception message for customers to guide customers login under interactive model
            if (e.httpResponseCode == 403 || HttpStatusCode.valueOf(e.httpResponseMessage) == HttpStatusCode.FORBIDDEN) {
                throw new HDIException("Forbidden. Attached Azure DataLake Store is not supported in Automated login model. Please logout first and try Interactive login model", 403);
            }
        } finally {
            IOUtils.closeQuietly(stream);
        }
    }
}
