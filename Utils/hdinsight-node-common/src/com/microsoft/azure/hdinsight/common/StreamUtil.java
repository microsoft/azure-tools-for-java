/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.common;

import com.microsoft.azure.hdinsight.sdk.common.HttpResponse;
import com.microsoft.azure.hdinsight.sdk.storage.IHDIStorageAccount;
import com.microsoft.azure.hdinsight.sdk.storage.adls.WebHDFSUtils;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.StringEntity;

import java.io.*;
import java.net.URL;
import java.util.*;

public class StreamUtil {

    public static String getResultFromInputStream(InputStream inputStream) throws IOException {
//      change string buffer to string builder for thread-safe
        StringBuilder result = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
        }

        return result.toString();
    }

    public static HttpResponse getResultFromHttpResponse(CloseableHttpResponse response) throws IOException {
        int code = response.getStatusLine().getStatusCode();
        String reason = response.getStatusLine().getReasonPhrase();
        // Entity for HEAD is empty
        HttpEntity entity = Optional.ofNullable(response.getEntity())
                .orElse(new StringEntity(""));
        try (InputStream inputStream = entity.getContent()) {
            String response_content = getResultFromInputStream(inputStream);
            return new HttpResponse(code, response_content, response.getAllHeaders(), reason);
        }
    }

    public static File getResourceFile(String resource) throws IOException {
        File file = null;
        URL res = streamUtil.getClass().getResource(resource);

        if (res.toString().startsWith("jar:")) {
            InputStream input = null;
            OutputStream out = null;

            try {
                input = streamUtil.getClass().getResourceAsStream(resource);
                file = File.createTempFile(String.valueOf(new Date().getTime()), ".tmp");
                out = new FileOutputStream(file);

                int read;
                byte[] bytes = new byte[1024];

                while ((read = input.read(bytes)) != -1) {
                    out.write(bytes, 0, read);
                }
            } finally {
                if (input != null) {
                    input.close();
                }

                if (out != null) {
                    out.flush();
                    out.close();
                }

                if (file != null) {
                    file.deleteOnExit();
                }
            }

        } else {
            file = new File(res.getFile());
        }

        return file;
    }

    private static StreamUtil streamUtil = new StreamUtil();
    private static final String SPARK_SUBMISSION_FOLDER = "SparkSubmission";

    public static String uploadArtifactToADLS(@NotNull File localFile, IHDIStorageAccount storageAccount, @NotNull String uploadFolderPath) throws Exception {
        String rootPath = storageAccount.getDefaultContainerOrRootPath();
        if(rootPath.startsWith("/")) {
            rootPath = rootPath.substring(1);
        }

        final String remoteFilePath = String.format("%s%s/%s/%s", rootPath, SPARK_SUBMISSION_FOLDER, uploadFolderPath, localFile.getName());
        WebHDFSUtils.uploadFileToADLS(storageAccount, localFile, remoteFilePath, true);
        return String.format("adl://%s.azuredatalakestore.net/%s", storageAccount.getName(), remoteFilePath);
    }
}
