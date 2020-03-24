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

package com.microsoft.azure.hdinsight.sdk.storage.adls;

import com.microsoft.aad.adal4j.*;
import com.microsoft.azure.datalake.store.ADLException;
import com.microsoft.azure.datalake.store.ADLStoreClient;
import com.microsoft.azure.datalake.store.IfExists;
import com.microsoft.azure.hdinsight.common.HDInsightLoader;
import com.microsoft.azure.hdinsight.sdk.common.HDIException;
import com.microsoft.azure.hdinsight.sdk.storage.ADLSCertificateInfo;
import com.microsoft.azure.hdinsight.sdk.storage.ADLSStorageAccount;
import com.microsoft.azure.hdinsight.sdk.storage.IHDIStorageAccount;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class WebHDFSUtils {
    private static ExecutorService service = null;

    private static String getUserAgent() {
        final String installID = HDInsightLoader.getHDInsightHelper().getInstallationId();
        final String userAgentSource = WebHDFSUtils.class.getClassLoader().getClass().getName().toLowerCase().contains("intellij")
                ? "Azure Toolkit for IntelliJ" : "Azure Toolkit for Eclipse";
        return userAgentSource + installID;
    }

    private static String getAccessTokenFromCertificate(@NotNull ADLSStorageAccount storageAccount) throws ExecutionException, InterruptedException, MalformedURLException, HDIException {
        if (service == null) {
            synchronized (WebHDFSUtils.class) {
                if (service == null) {
                    service = Executors.newFixedThreadPool(5);
                }
            }
        }

        final ADLSCertificateInfo certificateInfo = storageAccount.getCertificateInfo();
        AuthenticationContext ctx = new AuthenticationContext(certificateInfo.getAadTenantId(), true, service);
        AsymmetricKeyCredential asymmetricKeyCredential = AsymmetricKeyCredential.create(certificateInfo.getClientId(), certificateInfo.getKey(), certificateInfo.getCertificate());
        final Future<AuthenticationResult> result = ctx.acquireToken(certificateInfo.getResourceUri(), asymmetricKeyCredential , null);
        final AuthenticationResult ar = result.get();
        return ar.getAccessToken();
    }

    public static void uploadFileToADLS(@NotNull IHDIStorageAccount storageAccount, @NotNull File localFile, @NotNull String remotePath, boolean overWrite) throws Exception {
        if (!(storageAccount instanceof ADLSStorageAccount)) {
            throw new HDIException("the storage type should be ADLS");
        }

        ADLSStorageAccount adlsStorageAccount = (ADLSStorageAccount)storageAccount;
        String accessToken = getAccessTokenFromCertificate(adlsStorageAccount);
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
            // 403 error can be expected in:
            //      1. In interactive login model
            //          login user have no write permission to attached adls storage
            //      2. In Service Principle login model
            //          the adls was attached to HDInsight by Service Principle (hdi sp).
            //          Currently we don't have a better way to use hdi sp to grant write access to ADLS, so we just
            //          try to write adls directly use the login sp account(may have no access to target ADLS)
            if (e.httpResponseCode == 403) {
                throw new HDIException("Forbidden: " + e.httpResponseMessage + "\n" +
                        "This problem could be: " +
                        "1. Attached Azure DataLake Store is not supported in Automated login model. Please logout first and try Interactive login model" +
                        "2. Login account have no write permission on attached ADLS storage. " +
                            "Please grant write access from storage account admin(or other roles who have permission to do it)", 403);
            }
        } finally {
            IOUtils.closeQuietly(stream);
        }
    }
}
