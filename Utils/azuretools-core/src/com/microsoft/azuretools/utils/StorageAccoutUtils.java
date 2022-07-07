/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.utils;

import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.BlobContainerPermissions;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.SharedAccessBlobPermissions;
import com.microsoft.azure.storage.blob.SharedAccessBlobPolicy;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureCloud;
import com.microsoft.azuretools.authmanage.IdeAzureAccount;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * Created by vlashch on 1/19/17.
 */
public class StorageAccoutUtils {
    public static final String DEFAULT_PROTOCOL = "https";
    public static final String DEFAULT_ENDPOINTS_PROTOCOL_KEY = "DefaultEndpointsProtocol";
    public static final String ACCOUNT_NAME_KEY = "AccountName";
    public static final String ACCOUNT_KEY_KEY = "AccountKey";
    public static final String ENDPOINT_SUFFIX_KEY = "EndpointSuffix";
    public static final String BLOB_ENDPOINT_KEY = "BlobEndpoint";
    public static final String QUEUE_ENDPOINT_KEY = "QueueEndpoint";
    public static final String TABLE_ENDPOINT_KEY = "TableEndpoint";
    public static final String DEFAULT_CONN_STR_TEMPLATE = DEFAULT_ENDPOINTS_PROTOCOL_KEY + "=%s;" +
            ACCOUNT_NAME_KEY + "=%s;" +
            ACCOUNT_KEY_KEY + "=%s;" +
            ENDPOINT_SUFFIX_KEY + "=%s";
    public static final String CUSTOM_CONN_STR_TEMPLATE = BLOB_ENDPOINT_KEY + "=%s;" +
            QUEUE_ENDPOINT_KEY + "=%s;" +
            TABLE_ENDPOINT_KEY + "=%s;" +
            ACCOUNT_NAME_KEY + "=%s;" +
            ACCOUNT_KEY_KEY + "=%s";

    private static CloudStorageAccount getCloudStorageAccount(String blobLink, String saKey) throws MalformedURLException, URISyntaxException, InvalidKeyException {
        if (blobLink == null || blobLink.isEmpty()) {
            throw new IllegalArgumentException("Invalid blob link, it's null or empty: " + blobLink);
        }
        if (saKey == null || saKey.isEmpty()) {
            throw new IllegalArgumentException("Invalid storage account key, it's null or empty: " + saKey);
        }
        // check the link is valic
        URI blobUri = new URL(blobLink).toURI();
        String host =  blobUri.getHost();
        if (host == null) {
            throw new IllegalArgumentException("Invalid blobLink, can't find host: " + blobLink);
        }
        String storageAccountName = host.substring(0, host.indexOf("."));
        String storageConnectionString = getConnectionString(storageAccountName, saKey);
        CloudStorageAccount cloudStorageAccount = CloudStorageAccount.parse(storageConnectionString);
        return cloudStorageAccount;
    }

    public static String  getBlobSasUri(String blobLink, String saKey) throws URISyntaxException, StorageException, InvalidKeyException, MalformedURLException {
        CloudStorageAccount cloudStorageAccount = getCloudStorageAccount(blobLink, saKey);
        // Create the blob client.
        CloudBlobClient blobClient = cloudStorageAccount.createCloudBlobClient();
        // Get container and blob name from the link
        String path = new URI(blobLink).getPath();
        if (path == null) {
            throw new IllegalArgumentException("Invalid blobLink: " + blobLink);
        }
        int containerNameEndIndex = path.indexOf("/", 1);
        String containerName = path.substring(1, containerNameEndIndex);
        if (containerName == null || containerName.isEmpty()) {
            throw new IllegalArgumentException("Invalid blobLink, can't find container name: " + blobLink);
        }
        String blobName = path.substring(path.indexOf("/", containerNameEndIndex)+1);
        if (blobName == null || blobName.isEmpty()) {
            throw new IllegalArgumentException("Invalid blobLink, can't find blob name: " + blobLink);
        }
        // Retrieve reference to a previously created container.
        CloudBlobContainer container = blobClient.getContainerReference(containerName);

        //CloudBlockBlob blob = container.getBlockBlobReference(blobName);
        SharedAccessBlobPolicy sharedAccessBlobPolicy = new SharedAccessBlobPolicy();
        GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        calendar.setTime(new Date());
        sharedAccessBlobPolicy.setSharedAccessStartTime(calendar.getTime());
        calendar.add(Calendar.HOUR, 23);
        sharedAccessBlobPolicy.setSharedAccessExpiryTime(calendar.getTime());
        sharedAccessBlobPolicy.setPermissions(EnumSet.of(SharedAccessBlobPermissions.READ));
        BlobContainerPermissions containerPermissions = new BlobContainerPermissions();
        container.uploadPermissions(containerPermissions);
        String signature = container.generateSharedAccessSignature(sharedAccessBlobPolicy, null);
        return blobLink + "?" + signature;
    }


    public static String getEndpointSuffix() {
        String endpointSuffix;
        try {
            if (IdeAzureAccount.getInstance().isLoggedIn()) {
                endpointSuffix = Azure.az(AzureCloud.class).getOrDefault().getStorageEndpointSuffix();
            } else {
                endpointSuffix = AzureEnvironment.AZURE.storageEndpointSuffix();
            }
        } catch (Exception ex) {
            endpointSuffix = AzureEnvironment.AZURE.storageEndpointSuffix();
        }
        return endpointSuffix.substring(1);
    }

    @NotNull
    public static String getConnectionString(String accountName, String key) {
        return String.format(DEFAULT_CONN_STR_TEMPLATE,
                        DEFAULT_PROTOCOL,
                        accountName,
                        key,
                        getEndpointSuffix());
    }


}
