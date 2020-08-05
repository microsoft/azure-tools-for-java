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

package com.microsoft.tooling.msservices.helpers.azure.sdk;

import com.google.common.base.Strings;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.*;
import com.microsoft.azure.storage.core.Base64;
import com.microsoft.azure.storage.core.Utility;
import com.microsoft.azure.storage.queue.CloudQueue;
import com.microsoft.azure.storage.queue.CloudQueueClient;
import com.microsoft.azure.storage.queue.CloudQueueMessage;
import com.microsoft.azure.storage.queue.QueueListingDetails;
import com.microsoft.azure.storage.table.*;
import com.microsoft.tooling.msservices.helpers.CallableSingleArg;
import com.microsoft.tooling.msservices.model.storage.BlobContainer;
import com.microsoft.tooling.msservices.model.storage.BlobDirectory;
import com.microsoft.tooling.msservices.model.storage.BlobFile;
import com.microsoft.tooling.msservices.model.storage.BlobItem;
import com.microsoft.tooling.msservices.model.storage.ClientStorageAccount;
import com.microsoft.tooling.msservices.model.storage.Queue;
import com.microsoft.tooling.msservices.model.storage.QueueMessage;
import com.microsoft.tooling.msservices.model.storage.Table;
import com.microsoft.tooling.msservices.model.storage.TableEntity;
import com.microsoft.tooling.msservices.model.storage.TableEntity.Property;
import com.microsoft.azure.management.storage.StorageAccount;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.utils.StorageAccoutUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.*;
import java.util.Map.Entry;

public class StorageClientSDKManager {
    private static StorageClientSDKManager apiManager;

    private StorageClientSDKManager() {
    }

    @NotNull
    public static StorageClientSDKManager getManager() {
        if (apiManager == null) {
            apiManager = new StorageClientSDKManager();
        }

        return apiManager;
    }

    @NotNull
    public ClientStorageAccount getStorageAccount(@NotNull String connectionString) {
        HashMap<String, String> settings = Utility.parseAccountString(connectionString);

        String name = settings.containsKey(ClientStorageAccount.ACCOUNT_NAME_KEY) ?
                settings.get(ClientStorageAccount.ACCOUNT_NAME_KEY) : "";

        ClientStorageAccount storageAccount = new ClientStorageAccount(name);

        if (settings.containsKey(ClientStorageAccount.ACCOUNT_KEY_KEY)) {
            storageAccount.setPrimaryKey(settings.get(ClientStorageAccount.ACCOUNT_KEY_KEY));
        }

        if (settings.containsKey(ClientStorageAccount.DEFAULT_ENDPOINTS_PROTOCOL_KEY)) {
            storageAccount.setUseCustomEndpoints(false);
            storageAccount.setProtocol(settings.get(ClientStorageAccount.DEFAULT_ENDPOINTS_PROTOCOL_KEY));
        } else {
            storageAccount.setUseCustomEndpoints(true);

            if (settings.containsKey(ClientStorageAccount.BLOB_ENDPOINT_KEY)) {
                storageAccount.setBlobsUri(settings.get(ClientStorageAccount.BLOB_ENDPOINT_KEY));
            }

            if (settings.containsKey(ClientStorageAccount.QUEUE_ENDPOINT_KEY)) {
                storageAccount.setQueuesUri(settings.get(ClientStorageAccount.QUEUE_ENDPOINT_KEY));
            }

            if (settings.containsKey(ClientStorageAccount.TABLE_ENDPOINT_KEY)) {
                storageAccount.setTablesUri(settings.get(ClientStorageAccount.TABLE_ENDPOINT_KEY));
            }
        }

        return storageAccount;
    }

    @NotNull
    public List<BlobContainer> getBlobContainers(@NotNull String connectionString)
            throws AzureCmdException {
        return getBlobContainers(connectionString, null);
    }

    public List<BlobContainer> getBlobContainers(@NotNull String connectionString, @Nullable BlobRequestOptions options)
            throws AzureCmdException {
        List<BlobContainer> bcList = new ArrayList<BlobContainer>();

        try {
            CloudBlobClient client = getCloudBlobClient(connectionString);
            for (CloudBlobContainer container : client.listContainers(null, ContainerListingDetails.ALL, options, null)) {
                String uri = container.getUri() != null ? container.getUri().toString() : "";
                String eTag = "";
                Calendar lastModified = new GregorianCalendar();
                BlobContainerProperties properties = container.getProperties();

                if (properties != null) {
                    eTag = Strings.nullToEmpty(properties.getEtag());

                    if (properties.getLastModified() != null) {
                        lastModified.setTime(properties.getLastModified());
                    }
                }

                String publicReadAccessType = "";
                BlobContainerPermissions blobContainerPermissions = container.downloadPermissions();

                if (blobContainerPermissions != null && blobContainerPermissions.getPublicAccess() != null) {
                    publicReadAccessType = blobContainerPermissions.getPublicAccess().toString();
                }

                bcList.add(new BlobContainer(Strings.nullToEmpty(container.getName()),
                        uri,
                        eTag,
                        lastModified,
                        publicReadAccessType));
            }

            return bcList;
        } catch (Throwable t) {
            throw new AzureCmdException("Error retrieving the Blob Container list", t);
        }

    }

    @NotNull
    public BlobContainer createBlobContainer(@NotNull String connectionString,
                                             @NotNull BlobContainer blobContainer)
            throws AzureCmdException {
        try {
            CloudBlobClient client = getCloudBlobClient(connectionString);

            CloudBlobContainer container = client.getContainerReference(blobContainer.getName());
            container.createIfNotExists();
            container.downloadAttributes();

            String uri = container.getUri() != null ? container.getUri().toString() : "";
            String eTag = "";
            Calendar lastModified = new GregorianCalendar();
            BlobContainerProperties properties = container.getProperties();

            if (properties != null) {
                eTag = Strings.nullToEmpty(properties.getEtag());

                if (properties.getLastModified() != null) {
                    lastModified.setTime(properties.getLastModified());
                }
            }

            String publicReadAccessType = "";
            BlobContainerPermissions blobContainerPermissions = container.downloadPermissions();

            if (blobContainerPermissions != null && blobContainerPermissions.getPublicAccess() != null) {
                publicReadAccessType = blobContainerPermissions.getPublicAccess().toString();
            }

            blobContainer.setUri(uri);
            blobContainer.setETag(eTag);
            blobContainer.setLastModified(lastModified);
            blobContainer.setPublicReadAccessType(publicReadAccessType);

            return blobContainer;
        } catch (Throwable t) {
            throw new AzureCmdException("Error creating the Blob Container", t);
        }
    }

    public void deleteBlobContainer(@NotNull StorageAccount storageAccount, @NotNull BlobContainer blobContainer)
            throws AzureCmdException {
        try {
            CloudBlobClient client = getCloudBlobClient(storageAccount);

            CloudBlobContainer container = client.getContainerReference(blobContainer.getName());
            container.deleteIfExists();
        } catch (Throwable t) {
            throw new AzureCmdException("Error deleting the Blob Container", t);
        }
    }

    public void deleteBlobContainer(@NotNull ClientStorageAccount clientStorageAccount, @NotNull BlobContainer blobContainer)
            throws AzureCmdException {
        try {
            CloudBlobClient client = getCloudBlobClient(clientStorageAccount.getConnectionString());

            CloudBlobContainer container = client.getContainerReference(blobContainer.getName());
            container.deleteIfExists();
        } catch (Throwable t) {
            throw new AzureCmdException("Error deleting the Blob Container", t);
        }
    }

    @NotNull
    public BlobDirectory getRootDirectory(@NotNull String connectionString, @NotNull BlobContainer blobContainer)
            throws AzureCmdException {
        try {
            CloudBlobClient client = getCloudBlobClient(connectionString);

            CloudBlobContainer container = client.getContainerReference(blobContainer.getName());
            CloudBlobDirectory directory = container.getDirectoryReference("");

            String uri = directory.getUri() != null ? directory.getUri().toString() : "";

            return new BlobDirectory("", uri, blobContainer.getName(), "");
        } catch (Throwable t) {
            throw new AzureCmdException("Error retrieving the root Blob Directory", t);
        }
    }

    @NotNull
    public List<BlobItem> getBlobItems(@NotNull String connectionString, @NotNull BlobDirectory blobDirectory)
            throws AzureCmdException {
        List<BlobItem> biList = new ArrayList<BlobItem>();

        try {
            CloudBlobClient client = getCloudBlobClient(connectionString);
            String containerName = blobDirectory.getContainerName();
            String delimiter = client.getDirectoryDelimiter();

            CloudBlobContainer container = client.getContainerReference(containerName);
            CloudBlobDirectory directory = container.getDirectoryReference(blobDirectory.getPath());

            for (ListBlobItem item : directory.listBlobs()) {
                String uri = item.getUri() != null ? item.getUri().toString() : "";

                if (item instanceof CloudBlobDirectory) {
                    CloudBlobDirectory subDirectory = (CloudBlobDirectory) item;

                    String name = extractBlobItemName(subDirectory.getPrefix(), delimiter);
                    String path = Strings.nullToEmpty(subDirectory.getPrefix());

                    biList.add(new BlobDirectory(name, uri, containerName, path));
                } else if (item instanceof CloudBlob) {
                    CloudBlob blob = (CloudBlob) item;

                    String name = extractBlobItemName(blob.getName(), delimiter);
                    String path = Strings.nullToEmpty(blob.getName());
                    String type = "";
                    String cacheControlHeader = "";
                    String contentEncoding = "";
                    String contentLanguage = "";
                    String contentType = "";
                    String contentMD5Header = "";
                    String eTag = "";
                    Calendar lastModified = new GregorianCalendar();
                    long size = 0;

                    BlobProperties properties = blob.getProperties();

                    if (properties != null) {
                        if (properties.getBlobType() != null) {
                            type = properties.getBlobType().toString();
                        }

                        cacheControlHeader = Strings.nullToEmpty(properties.getCacheControl());
                        contentEncoding = Strings.nullToEmpty(properties.getContentEncoding());
                        contentLanguage = Strings.nullToEmpty(properties.getContentLanguage());
                        contentType = Strings.nullToEmpty(properties.getContentType());
                        contentMD5Header = Strings.nullToEmpty(properties.getContentMD5());
                        eTag = Strings.nullToEmpty(properties.getEtag());

                        if (properties.getLastModified() != null) {
                            lastModified.setTime(properties.getLastModified());
                        }

                        size = properties.getLength();
                    }

                    biList.add(new BlobFile(name, uri, containerName, path, type, cacheControlHeader, contentEncoding,
                            contentLanguage, contentType, contentMD5Header, eTag, lastModified, size));
                }
            }

            return biList;
        } catch (Throwable t) {
            throw new AzureCmdException("Error retrieving the Blob Item list", t);
        }
    }

    @NotNull
    public BlobDirectory createBlobDirectory(@NotNull StorageAccount storageAccount,
                                             @NotNull BlobDirectory parentBlobDirectory,
                                             @NotNull BlobDirectory blobDirectory)
            throws AzureCmdException {
        try {
            CloudBlobClient client = getCloudBlobClient(storageAccount);
            String containerName = parentBlobDirectory.getContainerName();

            CloudBlobContainer container = client.getContainerReference(containerName);
            CloudBlobDirectory parentDirectory = container.getDirectoryReference(parentBlobDirectory.getPath());
            CloudBlobDirectory directory = parentDirectory.getDirectoryReference(blobDirectory.getName());

            String uri = directory.getUri() != null ? directory.getUri().toString() : "";
            String path = Strings.nullToEmpty(directory.getPrefix());

            blobDirectory.setUri(uri);
            blobDirectory.setContainerName(containerName);
            blobDirectory.setPath(path);

            return blobDirectory;
        } catch (Throwable t) {
            throw new AzureCmdException("Error creating the Blob Directory", t);
        }
    }

    @NotNull
    public BlobFile createBlobFile(@NotNull ClientStorageAccount storageAccount,
                                   @NotNull BlobDirectory parentBlobDirectory,
                                   @NotNull BlobFile blobFile)
            throws AzureCmdException {
        try {
            CloudBlobClient client = getCloudBlobClient(storageAccount);
            String containerName = parentBlobDirectory.getContainerName();

            CloudBlobContainer container = client.getContainerReference(containerName);
            CloudBlobDirectory parentDirectory = container.getDirectoryReference(parentBlobDirectory.getPath());

            CloudBlob blob = getCloudBlob(parentDirectory, blobFile);

            blob.upload(new ByteArrayInputStream(new byte[0]), 0);

            return reloadBlob(blob, containerName, blobFile);
        } catch (Throwable t) {
            throw new AzureCmdException("Error creating the Blob File", t);
        }
    }

    public void deleteBlobFile(@NotNull String connectionString, @NotNull BlobFile blobFile)
            throws AzureCmdException {
        try {
            CloudBlobClient client = getCloudBlobClient(connectionString);
            String containerName = blobFile.getContainerName();

            CloudBlobContainer container = client.getContainerReference(containerName);

            CloudBlob blob = getCloudBlob(container, blobFile);

            blob.deleteIfExists();
        } catch (Throwable t) {
            throw new AzureCmdException("Error deleting the Blob File", t);
        }
    }

    public void uploadBlobFileContent(@NotNull String connectionString,
                                      @NotNull BlobContainer blobContainer,
                                      @NotNull String filePath,
                                      @NotNull InputStream content,
                                      CallableSingleArg<Void, Long> processBlock,
                                      long maxBlockSize,
                                      long length)
            throws AzureCmdException {
        try {
            CloudBlobClient client = getCloudBlobClient(connectionString);
            String containerName = blobContainer.getName();

            CloudBlobContainer container = client.getContainerReference(containerName);
            final CloudBlockBlob blob = container.getBlockBlobReference(filePath);
            long uploadedBytes = 0;

            ArrayList<BlockEntry> blockEntries = new ArrayList<BlockEntry>();

            while (uploadedBytes < length) {
                String blockId = Base64.encode(UUID.randomUUID().toString().getBytes());
                BlockEntry entry = new BlockEntry(blockId, BlockSearchMode.UNCOMMITTED);

                long blockSize = maxBlockSize;
                if (length - uploadedBytes <= maxBlockSize) {
                    blockSize = length - uploadedBytes;
                }

                if (processBlock != null) {
                    processBlock.call(uploadedBytes);
                }

                entry.setSize(blockSize);

                blockEntries.add(entry);
                blob.uploadBlock(entry.getId(), content, blockSize);
                uploadedBytes += blockSize;
            }

            blob.commitBlockList(blockEntries);

        } catch (Throwable t) {
            throw new AzureCmdException("Error uploading the Blob File content", t);
        }
    }

    public void downloadBlobFileContent(@NotNull String connectionString,
                                        @NotNull BlobFile blobFile,
                                        @NotNull OutputStream content)
            throws AzureCmdException {
        try {
            CloudBlobClient client = getCloudBlobClient(connectionString);
            String containerName = blobFile.getContainerName();

            CloudBlobContainer container = client.getContainerReference(containerName);

            CloudBlob blob = getCloudBlob(container, blobFile);

            blob.download(content);
        } catch (Throwable t) {
            throw new AzureCmdException("Error downloading the Blob File content", t);
        }
    }

    @NotNull
    public List<Queue> getQueues(@NotNull StorageAccount storageAccount)
            throws AzureCmdException {
        List<Queue> qList = new ArrayList<Queue>();

        try {
            CloudQueueClient client = getCloudQueueClient(storageAccount);

            for (CloudQueue cloudQueue : client.listQueues(null, QueueListingDetails.ALL, null, null)) {
                String uri = cloudQueue.getUri() != null ? cloudQueue.getUri().toString() : "";

                qList.add(new Queue(Strings.nullToEmpty(cloudQueue.getName()),
                        uri,
                        cloudQueue.getApproximateMessageCount()));
            }

            return qList;
        } catch (Throwable t) {
            throw new AzureCmdException("Error retrieving the Queue list", t);
        }
    }

    @NotNull
    public Queue createQueue(@NotNull StorageAccount storageAccount,
                             @NotNull Queue queue)
            throws AzureCmdException {
        try {
            CloudQueueClient client = getCloudQueueClient(storageAccount);

            CloudQueue cloudQueue = client.getQueueReference(queue.getName());
            cloudQueue.createIfNotExists();
            cloudQueue.downloadAttributes();

            String uri = cloudQueue.getUri() != null ? cloudQueue.getUri().toString() : "";
            long approximateMessageCount = cloudQueue.getApproximateMessageCount();

            queue.setUri(uri);
            queue.setApproximateMessageCount(approximateMessageCount);

            return queue;
        } catch (Throwable t) {
            throw new AzureCmdException("Error creating the Queue", t);
        }
    }

    public void deleteQueue(@NotNull StorageAccount storageAccount, @NotNull Queue queue)
            throws AzureCmdException {
        try {
            CloudQueueClient client = getCloudQueueClient(storageAccount);

            CloudQueue cloudQueue = client.getQueueReference(queue.getName());
            cloudQueue.deleteIfExists();
        } catch (Throwable t) {
            throw new AzureCmdException("Error deleting the Queue", t);
        }
    }

    @NotNull
    public List<QueueMessage> getQueueMessages(@NotNull StorageAccount storageAccount, @NotNull Queue queue)
            throws AzureCmdException {
        List<QueueMessage> qmList = new ArrayList<QueueMessage>();

        try {
            CloudQueueClient client = getCloudQueueClient(storageAccount);
            String queueName = queue.getName();

            CloudQueue cloudQueue = client.getQueueReference(queueName);

            for (CloudQueueMessage cqm : cloudQueue.peekMessages(32)) {
                String id = Strings.nullToEmpty(cqm.getId());
                String content = Strings.nullToEmpty(cqm.getMessageContentAsString());

                Calendar insertionTime = new GregorianCalendar();

                if (cqm.getInsertionTime() != null) {
                    insertionTime.setTime(cqm.getInsertionTime());
                }

                Calendar expirationTime = new GregorianCalendar();

                if (cqm.getExpirationTime() != null) {
                    expirationTime.setTime(cqm.getExpirationTime());
                }

                int dequeueCount = cqm.getDequeueCount();

                qmList.add(new QueueMessage(id, queueName, content, insertionTime, expirationTime, dequeueCount));
            }

            return qmList;
        } catch (Throwable t) {
            throw new AzureCmdException("Error retrieving the Queue Message list", t);
        }
    }

    public void clearQueue(@NotNull StorageAccount storageAccount, @NotNull Queue queue)
            throws AzureCmdException {
        try {
            CloudQueueClient client = getCloudQueueClient(storageAccount);

            CloudQueue cloudQueue = client.getQueueReference(queue.getName());
            cloudQueue.clear();
        } catch (Throwable t) {
            throw new AzureCmdException("Error clearing the Queue", t);
        }
    }

    public void createQueueMessage(@NotNull StorageAccount storageAccount,
                                   @NotNull QueueMessage queueMessage,
                                   int timeToLiveInSeconds)
            throws AzureCmdException {
        try {
            CloudQueueClient client = getCloudQueueClient(storageAccount);

            CloudQueue cloudQueue = client.getQueueReference(queueMessage.getQueueName());
            cloudQueue.addMessage(new CloudQueueMessage(queueMessage.getContent()), timeToLiveInSeconds, 0, null, null);
        } catch (Throwable t) {
            throw new AzureCmdException("Error creating the Queue Message", t);
        }
    }

    @NotNull
    public QueueMessage dequeueFirstQueueMessage(@NotNull StorageAccount storageAccount, @NotNull Queue queue)
            throws AzureCmdException {
        try {
            CloudQueueClient client = getCloudQueueClient(storageAccount);
            String queueName = queue.getName();

            CloudQueue cloudQueue = client.getQueueReference(queueName);
            CloudQueueMessage cqm = cloudQueue.retrieveMessage();

            String id = "";
            String content = "";
            Calendar insertionTime = new GregorianCalendar();
            Calendar expirationTime = new GregorianCalendar();
            int dequeueCount = 0;

            if (cqm != null) {
                id = Strings.nullToEmpty(cqm.getId());
                content = Strings.nullToEmpty(cqm.getMessageContentAsString());

                if (cqm.getInsertionTime() != null) {
                    insertionTime.setTime(cqm.getInsertionTime());
                }

                if (cqm.getExpirationTime() != null) {
                    expirationTime.setTime(cqm.getExpirationTime());
                }

                dequeueCount = cqm.getDequeueCount();
            }

            QueueMessage queueMessage = new QueueMessage(id, queueName, content, insertionTime, expirationTime, dequeueCount);

            if (cqm != null) {
                cloudQueue.deleteMessage(cqm);
            }

            return queueMessage;
        } catch (Throwable t) {
            throw new AzureCmdException("Error dequeuing the first Queue Message", t);
        }
    }

    @NotNull
    public List<Table> getTables(@NotNull StorageAccount storageAccount)
            throws AzureCmdException {
        List<Table> tList = new ArrayList<Table>();

        try {
            CloudTableClient client = getCloudTableClient(storageAccount);

            for (String tableName : client.listTables()) {
                CloudTable cloudTable = client.getTableReference(tableName);

                String uri = cloudTable.getUri() != null ? cloudTable.getUri().toString() : "";

                tList.add(new Table(tableName, uri));
            }

            return tList;
        } catch (Throwable t) {
            throw new AzureCmdException("Error retrieving the Table list", t);
        }
    }

    @NotNull
    public Table createTable(@NotNull StorageAccount storageAccount,
                             @NotNull Table table)
            throws AzureCmdException {
        try {
            CloudTableClient client = getCloudTableClient(storageAccount);

            CloudTable cloudTable = client.getTableReference(table.getName());
            cloudTable.createIfNotExists();

            String uri = cloudTable.getUri() != null ? cloudTable.getUri().toString() : "";

            table.setUri(uri);

            return table;
        } catch (Throwable t) {
            throw new AzureCmdException("Error creating the Table", t);
        }
    }

    public void deleteTable(@NotNull StorageAccount storageAccount, @NotNull Table table)
            throws AzureCmdException {
        try {
            CloudTableClient client = getCloudTableClient(storageAccount);

            CloudTable cloudTable = client.getTableReference(table.getName());
            cloudTable.deleteIfExists();
        } catch (Throwable t) {
            throw new AzureCmdException("Error deleting the Table", t);
        }
    }

    @NotNull
    public List<TableEntity> getTableEntities(@NotNull StorageAccount storageAccount, @NotNull Table table,
                                              @NotNull String filter)
            throws AzureCmdException {
        List<TableEntity> teList = new ArrayList<TableEntity>();

        try {
            CloudTableClient client = getCloudTableClient(storageAccount);
            String tableName = table.getName();
            CloudTable cloudTable = client.getTableReference(tableName);

            TableQuery<DynamicTableEntity> tableQuery = TableQuery.from(DynamicTableEntity.class);

            if (!filter.isEmpty()) {
                tableQuery.where(filter);
            }

            TableRequestOptions tro = new TableRequestOptions();
            tro.setTablePayloadFormat(TablePayloadFormat.JsonFullMetadata);

            for (DynamicTableEntity dte : cloudTable.execute(tableQuery, tro, null)) {
                teList.add(getTableEntity(tableName, dte));
            }

            return teList;
        } catch (Throwable t) {
            throw new AzureCmdException("Error retrieving the Table Entity list", t);
        }
    }

    @NotNull
    public TableEntity createTableEntity(@NotNull StorageAccount storageAccount, @NotNull String tableName,
                                         @NotNull String partitionKey, @NotNull String rowKey,
                                         @NotNull Map<String, Property> properties)
            throws AzureCmdException {
        try {
            CloudTableClient client = getCloudTableClient(storageAccount);
            CloudTable cloudTable = client.getTableReference(tableName);

            DynamicTableEntity entity = getDynamicTableEntity(partitionKey, rowKey, properties);

            TableRequestOptions tro = new TableRequestOptions();
            tro.setTablePayloadFormat(TablePayloadFormat.JsonFullMetadata);

            TableResult result = cloudTable.execute(TableOperation.insert(entity, true), tro, null);
            DynamicTableEntity resultEntity = result.getResultAsType();

            return getTableEntity(tableName, resultEntity);
        } catch (Throwable t) {
            throw new AzureCmdException("Error creating the Table Entity", t);
        }
    }

    @NotNull
    public TableEntity updateTableEntity(@NotNull StorageAccount storageAccount, @NotNull TableEntity tableEntity)
            throws AzureCmdException {
        try {
            CloudTableClient client = getCloudTableClient(storageAccount);
            CloudTable cloudTable = client.getTableReference(tableEntity.getTableName());

            DynamicTableEntity entity = getDynamicTableEntity(tableEntity);

            TableRequestOptions tro = new TableRequestOptions();
            tro.setTablePayloadFormat(TablePayloadFormat.JsonFullMetadata);

            TableResult result = cloudTable.execute(TableOperation.replace(entity), tro, null);
            DynamicTableEntity resultEntity = result.getResultAsType();

            return getTableEntity(tableEntity.getTableName(), resultEntity);
        } catch (Throwable t) {
            throw new AzureCmdException("Error updating the Table Entity", t);
        }
    }

    public void deleteTableEntity(@NotNull StorageAccount storageAccount, @NotNull TableEntity tableEntity)
            throws AzureCmdException {
        try {
            CloudTableClient client = getCloudTableClient(storageAccount);
            CloudTable cloudTable = client.getTableReference(tableEntity.getTableName());

            DynamicTableEntity entity = getDynamicTableEntity(tableEntity);

            TableRequestOptions tro = new TableRequestOptions();
            tro.setTablePayloadFormat(TablePayloadFormat.JsonFullMetadata);

            cloudTable.execute(TableOperation.delete(entity), tro, null);
        } catch (Throwable t) {
            throw new AzureCmdException("Error deleting the Table Entity", t);
        }
    }

    @NotNull
    public static String getConnectionString(StorageAccount storageAccount) {
        String accountName = storageAccount.name();
        String key = storageAccount.getKeys().get(0).value();
        return StorageAccoutUtils.getConnectionString(accountName, key);
    }

    public static String getEndpointSuffix() {
        return StorageAccoutUtils.getEndpointSuffix();
    }

    @NotNull
    public static CloudStorageAccount getCloudStorageAccount(@NotNull String connectionString) throws URISyntaxException, InvalidKeyException {
        return CloudStorageAccount.parse(connectionString);
    }

    @NotNull
    private static CloudBlobClient getCloudBlobClient(@NotNull ClientStorageAccount storageAccount)
            throws Exception {
        CloudStorageAccount csa = getCloudStorageAccount(storageAccount.getConnectionString());

        return csa.createCloudBlobClient();
    }

    @NotNull
    private static CloudBlobClient getCloudBlobClient(@NotNull StorageAccount storageAccount) throws Exception {
        CloudStorageAccount csa = getCloudStorageAccount(getConnectionString(storageAccount));
        return csa.createCloudBlobClient();
    }

    @NotNull
    private static CloudBlobClient getCloudBlobClient(@NotNull String connectionString) throws Exception {
        CloudStorageAccount csa = getCloudStorageAccount(connectionString);
        return csa.createCloudBlobClient();
    }

    @NotNull
    private static CloudQueueClient getCloudQueueClient(@NotNull StorageAccount storageAccount)
            throws Exception {
        CloudStorageAccount csa = getCloudStorageAccount(getConnectionString(storageAccount));

        return csa.createCloudQueueClient();
    }

    @NotNull
    private static CloudTableClient getCloudTableClient(@NotNull StorageAccount storageAccount)
            throws Exception {
        CloudStorageAccount csa = getCloudStorageAccount(getConnectionString(storageAccount));

        return csa.createCloudTableClient();
    }

    @NotNull
    private static CloudBlob getCloudBlob(@NotNull CloudBlobContainer container,
                                          @NotNull BlobFile blobFile)
            throws URISyntaxException, StorageException {
        CloudBlob blob;

        if (blobFile.getType().equals(BlobType.BLOCK_BLOB.toString())) {
            blob = container.getBlockBlobReference(blobFile.getPath());
        } else {
            blob = container.getPageBlobReference(blobFile.getPath());
        }

        return blob;
    }

    @NotNull
    private static CloudBlob getCloudBlob(@NotNull CloudBlobDirectory parentDirectory,
                                          @NotNull BlobFile blobFile)
            throws URISyntaxException, StorageException {
        CloudBlob blob;

        if (blobFile.getType().equals(BlobType.BLOCK_BLOB.toString())) {
            blob = parentDirectory.getBlockBlobReference(blobFile.getName());
        } else {
            blob = parentDirectory.getPageBlobReference(blobFile.getName());
        }
        return blob;
    }

    @NotNull
    private static BlobFile reloadBlob(@NotNull CloudBlob blob, @NotNull String containerName, @NotNull BlobFile blobFile)
            throws StorageException, URISyntaxException {
        blob.downloadAttributes();

        String uri = blob.getUri() != null ? blob.getUri().toString() : "";
        String path = Strings.nullToEmpty(blob.getName());
        String type = "";
        String cacheControlHeader = "";
        String contentEncoding = "";
        String contentLanguage = "";
        String contentType = "";
        String contentMD5Header = "";
        String eTag = "";
        Calendar lastModified = new GregorianCalendar();
        long size = 0;

        BlobProperties properties = blob.getProperties();

        if (properties != null) {
            if (properties.getBlobType() != null) {
                type = properties.getBlobType().toString();
            }

            cacheControlHeader = Strings.nullToEmpty(properties.getCacheControl());
            contentEncoding = Strings.nullToEmpty(properties.getContentEncoding());
            contentLanguage = Strings.nullToEmpty(properties.getContentLanguage());
            contentType = Strings.nullToEmpty(properties.getContentType());
            contentMD5Header = Strings.nullToEmpty(properties.getContentMD5());
            eTag = Strings.nullToEmpty(properties.getEtag());

            if (properties.getLastModified() != null) {
                lastModified.setTime(properties.getLastModified());
            }

            size = properties.getLength();
        }

        blobFile.setUri(uri);
        blobFile.setPath(path);
        blobFile.setContainerName(containerName);
        blobFile.setType(type);
        blobFile.setCacheControlHeader(cacheControlHeader);
        blobFile.setContentEncoding(contentEncoding);
        blobFile.setContentLanguage(contentLanguage);
        blobFile.setContentType(contentType);
        blobFile.setContentMD5Header(contentMD5Header);
        blobFile.setETag(eTag);
        blobFile.setLastModified(lastModified);
        blobFile.setSize(size);

        return blobFile;
    }

    @NotNull
    private static String extractBlobItemName(@Nullable String path, @Nullable String delimiter) {
        if (path == null) {
            return "";
        } else if (delimiter == null || delimiter.isEmpty()) {
            return path;
        } else {
            String[] parts = path.split(delimiter);

            if (parts.length == 0) {
                return "";
            } else {
                return parts[parts.length - 1];
            }
        }
    }

    @NotNull
    private static TableEntity getTableEntity(@NotNull String tableName,
                                              @NotNull DynamicTableEntity dte) {
        String partitionKey = Strings.nullToEmpty(dte.getPartitionKey());
        String rowKey = Strings.nullToEmpty(dte.getRowKey());
        String eTag = Strings.nullToEmpty(dte.getEtag());

        Calendar timestamp = new GregorianCalendar();

        if (dte.getTimestamp() != null) {
            timestamp.setTime(dte.getTimestamp());
        }

        Map<String, Property> properties = new HashMap<String, Property>();

        if (dte.getProperties() != null) {
            for (Entry<String, EntityProperty> entry : dte.getProperties().entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    String key = entry.getKey();
                    Property property;

                    switch (entry.getValue().getEdmType()) {
                        case BOOLEAN:
                            property = new Property(entry.getValue().getValueAsBooleanObject());
                            break;
                        case DATE_TIME:
                            Calendar value = new GregorianCalendar();
                            value.setTime(entry.getValue().getValueAsDate());
                            property = new Property(value);
                            break;
                        case DOUBLE:
                            property = new Property(entry.getValue().getValueAsDoubleObject());
                            break;
                        case GUID:
                            property = new Property(entry.getValue().getValueAsUUID());
                            break;
                        case INT32:
                            property = new Property(entry.getValue().getValueAsIntegerObject());
                            break;
                        case INT64:
                            property = new Property(entry.getValue().getValueAsLongObject());
                            break;
                        case STRING:
                            property = new Property(entry.getValue().getValueAsString());
                            break;
                        default:
                            property = new Property(entry.getValue().getValueAsString());
                            break;
                    }

                    properties.put(key, property);
                }
            }
        }

        return new TableEntity(partitionKey, rowKey, tableName, eTag, timestamp, properties);
    }

    @NotNull
    private static DynamicTableEntity getDynamicTableEntity(@NotNull TableEntity tableEntity)
            throws AzureCmdException {
        return getDynamicTableEntity(tableEntity.getPartitionKey(), tableEntity.getRowKey(),
                tableEntity.getTimestamp(), tableEntity.getETag(), tableEntity.getProperties());

    }

    @NotNull
    private static DynamicTableEntity getDynamicTableEntity(@NotNull String partitionKey,
                                                            @NotNull String rowKey,
                                                            @NotNull Map<String, Property> properties)
            throws AzureCmdException {
        return getDynamicTableEntity(partitionKey, rowKey, null, null, properties);
    }

    @NotNull
    private static DynamicTableEntity getDynamicTableEntity(@NotNull String partitionKey,
                                                            @NotNull String rowKey,
                                                            @Nullable Calendar timestamp,
                                                            @Nullable String eTag,
                                                            @NotNull Map<String, Property> properties)
            throws AzureCmdException {
        Date ts = null;

        if (timestamp != null) {
            ts = timestamp.getTime();
        }

        HashMap<String, EntityProperty> entityProperties = getEntityProperties(properties);

        return new DynamicTableEntity(partitionKey, rowKey, eTag, entityProperties);
    }

    @NotNull
    private static HashMap<String, EntityProperty> getEntityProperties(@NotNull Map<String, Property> properties) throws AzureCmdException {
        HashMap<String, EntityProperty> entityProperties = new HashMap<String, EntityProperty>();

        for (Entry<String, Property> entry : properties.entrySet()) {
            String key = entry.getKey();
            Property property = entry.getValue();

            EntityProperty entityProperty;

            switch (property.getType()) {
                case Boolean:
                    entityProperty = new EntityProperty(property.getValueAsBoolean());
                    break;
                case DateTime:
                    entityProperty = new EntityProperty(property.getValueAsCalendar().getTime());
                    break;
                case Double:
                    entityProperty = new EntityProperty(property.getValueAsDouble());
                    break;
                case Uuid:
                    entityProperty = new EntityProperty(property.getValueAsUuid());
                    break;
                case Integer:
                    entityProperty = new EntityProperty(property.getValueAsInteger());
                    break;
                case Long:
                    entityProperty = new EntityProperty(property.getValueAsLong());
                    break;
                case String:
                    entityProperty = new EntityProperty(property.getValueAsString());
                    break;
                default:
                    entityProperty = new EntityProperty(property.getValueAsString());
                    break;
            }

            entityProperties.put(key, entityProperty);
        }
        return entityProperties;
    }
}
