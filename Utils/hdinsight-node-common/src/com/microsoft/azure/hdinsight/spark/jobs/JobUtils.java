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

package com.microsoft.azure.hdinsight.spark.jobs;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.Cache;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.jcraft.jsch.*;
import com.microsoft.azure.datalake.store.ADLStoreClient;
import com.microsoft.azure.datalake.store.IfExists;
import com.microsoft.azure.hdinsight.common.ClusterManagerEx;
import com.microsoft.azure.hdinsight.common.HDInsightLoader;
import com.microsoft.azure.hdinsight.common.MessageInfoType;
import com.microsoft.azure.hdinsight.common.StreamUtil;
import com.microsoft.azure.hdinsight.sdk.cluster.EmulatorClusterDetail;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.sdk.cluster.LivyCluster;
import com.microsoft.azure.hdinsight.sdk.cluster.MfaEspCluster;
import com.microsoft.azure.hdinsight.sdk.common.AuthenticationException;
import com.microsoft.azure.hdinsight.sdk.common.HDIException;
import com.microsoft.azure.hdinsight.sdk.common.HttpObservable;
import com.microsoft.azure.hdinsight.sdk.common.livy.interactive.SparkSession;
import com.microsoft.azure.hdinsight.sdk.io.spark.ClusterFileBase64BufferedOutputStream;
import com.microsoft.azure.hdinsight.sdk.rest.yarn.rm.App;
import com.microsoft.azure.hdinsight.sdk.rest.yarn.rm.ApplicationMasterLogs;
import com.microsoft.azure.hdinsight.sdk.storage.HDStorageAccount;
import com.microsoft.azure.hdinsight.sdk.storage.IHDIStorageAccount;
import com.microsoft.azure.hdinsight.sdk.storage.StorageAccountType;
import com.microsoft.azure.hdinsight.sdk.storage.webhdfs.WebHdfsParamsBuilder;
import com.microsoft.azure.hdinsight.spark.common.SparkBatchEspMfaSubmission;
import com.microsoft.azure.hdinsight.spark.common.SparkBatchSubmission;
import com.microsoft.azure.hdinsight.spark.common.log.SparkBatchJobLogLine;
import com.microsoft.azure.hdinsight.spark.jobs.livy.LivyBatchesInformation;
import com.microsoft.azure.hdinsight.spark.jobs.livy.LivySession;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.azurecommons.helpers.StringHelper;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.helpers.CallableSingleArg;
import com.microsoft.tooling.msservices.helpers.azure.sdk.StorageClientSDKManager;
import com.microsoft.tooling.msservices.model.storage.BlobContainer;
import com.microsoft.tooling.msservices.model.storage.ClientStorageAccount;
import com.sun.net.httpserver.HttpExchange;
import org.apache.commons.codec.binary.Base64OutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.*;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Observer;
import rx.Single;
import rx.Subscription;
import rx.schedulers.Schedulers;

import java.awt.*;
import java.io.*;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownServiceException;
import java.util.*;
import java.util.List;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.microsoft.azure.hdinsight.common.MessageInfoType.Info;
import static com.microsoft.azure.hdinsight.spark.common.log.SparkBatchJobLogSource.Tool;
import static org.apache.http.HttpHeaders.AUTHORIZATION;
import static rx.exceptions.Exceptions.propagate;

public class JobUtils {
    private static Logger LOGGER = LoggerFactory.getLogger(JobUtils.class);
    private static final String JobLogFolderName = "SparkJobLog";
    private static String yarnUIHisotryFormat = "%s/yarnui/hn/cluster/app/%s";

    private static String sparkUIHistoryFormat = "%s/sparkhistory/history/%s/%s/jobs";

    private static CredentialsProvider provider = new BasicCredentialsProvider();

    public static void setResponse(@NotNull HttpExchange httpExchange, @NotNull String message) {
        setResponse(httpExchange, message, 200);
    }

    public static void setResponse(@NotNull HttpExchange httpExchange, @NotNull String message, @NotNull int code) {
        try {
            httpExchange.sendResponseHeaders(code, message.length());
            OutputStream stream = httpExchange.getResponseBody();
            stream.write(message.getBytes());
            stream.flush();
            httpExchange.close();
        } catch (IOException e) {
            LOGGER.error("JobUtils set Response error", e);
        }
    }

    public static URI getLivyLogPath(@NotNull String rootPath, @NotNull String applicationId) {
        String path = StringHelper.concat(rootPath, File.separator, JobLogFolderName, File.separator, applicationId);
        File file = new File(path);
        if(!file.exists()) {
            file.mkdirs();
        }
        return file.toURI();
    }

    public static String getJobInformation(@NotNull String allBatchesInformation, @NotNull String applicationId) {
        Gson gson = new GsonBuilder().enableComplexMapKeySerialization().create();
        Type subscriptionsType = new TypeToken<LivyBatchesInformation>() {
        }.getType();

        LivyBatchesInformation information = gson.fromJson(allBatchesInformation, subscriptionsType);
        LivySession session =  information.getSession(applicationId);
        String result = "Cannot get the Livy Log";

        if(session != null) {
            String livyLog = session.getFormatLog();
            result = StringHelper.isNullOrWhiteSpace(livyLog) ? "No Livy Log" : livyLog;
        }
        return result;
    }

    public static void openYarnUIHistory(@NotNull String clusterConnectString, @NotNull String applicationId) {
        String yarnHistoryUrl = String.format(yarnUIHisotryFormat, clusterConnectString, applicationId);;
        try {
            openDefaultBrowser(yarnHistoryUrl);
        } catch (Exception e) {
            DefaultLoader.getUIHelper().showError(e.getMessage(), "open Yarn UI");
        }
    }

    public static void openSparkUIHistory(@NotNull String clusterConnectString, @NotNull String applicationId, @NotNull int attemptId) {
        String sparkHistoryUrl = String.format(sparkUIHistoryFormat, clusterConnectString, applicationId, attemptId);
        try {
            openDefaultBrowser(sparkHistoryUrl);
        } catch (IOException e) {
            DefaultLoader.getUIHelper().showError(e.getMessage(), "open Spark UI");
        }
    }

    public static void openDefaultBrowser(@NotNull final String url) throws IOException {
        final URI uri = URI.create(url);
        openDefaultBrowser(uri);
    }

    public static void openDefaultBrowser(@NotNull final URI uri) throws IOException {
        if (Desktop.isDesktopSupported()) {
            final String scheme = uri.getScheme();
            if (scheme.equalsIgnoreCase("https") || scheme.equalsIgnoreCase("http")) {
                Desktop.getDesktop().browse(uri);
            } else if (scheme.equalsIgnoreCase("file")) {
                Desktop.getDesktop().open(new File(uri.getPath()));
            }
        }
    }

    public static void openFileExplorer(@NotNull final File file) throws IOException {
        openDefaultBrowser(file.toURI());
    }

    public static void openLivyLog(String applicationId) {
        final URI livyUri = getLivyLogPath(HDInsightLoader.getHDInsightHelper().getPluginRootPath(), applicationId);
        try {
            openDefaultBrowser(livyUri);
        } catch (IOException e) {
            DefaultLoader.getUIHelper().showError(e.getMessage(), "Open Livy Logs");
        }
    }

    private static final Cache globalCache = new Cache();

    private static final String DRIVER_LOG_INFO_URL = "%s/yarnui/jobhistory/logs/%s/port/%s/%s/%s/livy";

    public static ApplicationMasterLogs getYarnLogs(@NotNull ApplicationKey key) throws IOException, ExecutionException, HDIException {
        App app = JobViewCacheManager.getYarnApp(key);

        // amHostHttpAddress example: 10.0.0.4:30060
        final String amHostHttpAddress = app.getAmHostHttpAddress();
        String [] addressAndPort = amHostHttpAddress.split(":");
        if(addressAndPort.length != 2) {
            throw new HDIException("Yarn Application Master Host parse error");
        }
        String address = addressAndPort[0];
        // TODO: Set Node Manager port to default value(30050) temporarily, we need a better way to detect it
        String nodeManagerPort = "30050";
        final String amContainerLogPath = app.getAmContainerLogs();
        String amContainerId = getContainerIdFromAmContainerLogPath(amContainerLogPath);
        String url = String.format(DRIVER_LOG_INFO_URL, key.getClusterConnString(), address, nodeManagerPort, amContainerId, amContainerId);
        IClusterDetail clusterDetail = key.getClusterDetails();
        return getYarnLogsFromWebClient(clusterDetail, url);
    }

    private static String getContainerIdFromAmContainerLogPath(@NotNull String amContainerLogPath) {
        // AM container Logs path example: http://10.0.0.4:30060/node/containerlogs/container_1488459864280_0006_01_000001/livy
        String [] res = amContainerLogPath.split("/");
        assert res.length == 7;
        String amContainerId = res[res.length - 2];
        assert amContainerId.startsWith("container_");
        return amContainerId;
    }

    private static ApplicationMasterLogs getYarnLogsFromWebClient(@NotNull final IClusterDetail clusterDetail, @NotNull final String url) throws HDIException, IOException {
        final String authCode = SparkBatchSubmission.getClusterSubmission(clusterDetail).getAuthCode();

        String standerr = getInformationFromYarnLogDom(authCode, url, "stderr", 0, 0);
        String standout = getInformationFromYarnLogDom(authCode, url, "stdout", 0, 0);
        String directoryInfo = getInformationFromYarnLogDom(authCode, url, "directory.info", 0, 0);

        return new ApplicationMasterLogs(standout, standerr, directoryInfo);
    }

    public static String getInformationFromYarnLogDom(@Nullable String authCode,
                                                      @NotNull String baseUrl,
                                                      @NotNull String type,
                                                      long start,
                                                      int size) {
        final WebClient HTTP_WEB_CLIENT = new WebClient(BrowserVersion.CHROME);
        HTTP_WEB_CLIENT.getOptions().setUseInsecureSSL(HttpObservable.isSSLCertificateValidationDisabled());
        HTTP_WEB_CLIENT.setCache(globalCache);

        if (authCode != null) {
            HTTP_WEB_CLIENT.addRequestHeader(AUTHORIZATION, authCode);
        }

        return getInformationFromYarnLogDom(HTTP_WEB_CLIENT, baseUrl, type, start, size);
    }

    private static String getInformationFromYarnLogDom(@NotNull WebClient client,
                                                      @NotNull String baseUrl,
                                                      @NotNull String type,
                                                      long start,
                                                      int size) {
        URI url = null;

        try {
            url = new URI(baseUrl + "/").resolve(
                    String.format("%s?start=%d", type, start) +
                            (size <= 0 ? "" : String.format("&&end=%d", start + size)));
            HtmlPage htmlPage = client.getPage(url.toString());

            Iterator<DomElement> iterator = htmlPage.getElementById("navcell").getNextElementSibling().getChildElements().iterator();

            HashMap<String, String> logTypeMap = new HashMap<>();
            final AtomicReference<String> logType = new AtomicReference<>();
            String logs = "";

            while (iterator.hasNext()) {
                DomElement node = iterator.next();

                if (node instanceof HtmlParagraph) {
                    // In history server, need to read log type paragraph in page
                    final Pattern logTypePattern = Pattern.compile("Log Type:\\s+(\\S+)");

                    Optional.ofNullable(node.getFirstChild())
                            .map(DomNode::getTextContent)
                            .map(String::trim)
                            .map(logTypePattern::matcher)
                            .filter(Matcher::matches)
                            .map(matcher -> matcher.group(1))
                            .ifPresent(logType::set);
                } else if (node instanceof HtmlPreformattedText) {
                    // In running, no log type paragraph in page
                    logs = Optional.ofNullable(node.getFirstChild())
                                .map(DomNode::getTextContent)
                                .orElse("");

                    if (logType.get() != null) {
                        // Only get the first <pre>...</pre>
                        logTypeMap.put(logType.get(), logs);

                        logType.set(null);
                    }
                }
            }

            return logTypeMap.getOrDefault(type, logs);
        } catch (FailingHttpStatusCodeException httpError) {
            // If the URL is wrong, will get 200 response with content:
            //      Unable to locate 'xxx' log for container
            //  OR
            //      Logs not available for <user>. Aggregation may not be complete, Check back later or try the nodemanager at...
            //  OR
            //      Cannot get container logs without ...
            //
            // if fetching Yarn log hits the gap between the job running and stop, will get the status 403
            // the log is moving to job history server, just wait and retry.
            if (httpError.getStatusCode() != HttpStatus.SC_FORBIDDEN) {
                LOGGER.warn("The GET request to " + url + " responded error: " + httpError.getMessage());
            }
        } catch (URISyntaxException e) {
            LOGGER.error("baseUrl has syntax error: " + baseUrl);
        } catch (Exception e) {
            LOGGER.warn("get Spark job log Error", e);
        }
        return "";
    }

    /**
     * To create an Observable for specified Yarn container log type
     *
     * @param authCode the authCode in request's Authorization header
     * @param stop the stop observable to cancel the log fetch, refer to Observable.window() operation
     * @param containerLogUrl the contaniner log url
     * @param type the log type
     * @param blockSize the block size for one fetch, the value 0 for as many as possible
     * @return the log Observable
     */
    public static Observable<String> createYarnLogObservable(@Nullable final String authCode,
                                                             @Nullable final Observable<Object> stop,
                                                             @NotNull final String containerLogUrl,
                                                             @NotNull final String type,
                                                             final int blockSize) {
        int retryIntervalMs = 1000;

        if (blockSize <= 0)
            return Observable.empty();

        return Observable.create((Observable.OnSubscribe<String>) ob -> {
            long nextStart = 0;
            String remainedLine = "";
            String logs;
            Thread currentThread = Thread.currentThread();

            // Refer to the Observable.window() operation:
            //    http://reactivex.io/documentation/operators/window.html
            // The event from `stop` observable will stop the log fetch
            Optional<Subscription> stopSubscriptionOptional = Optional.ofNullable(stop).map(stopOb ->
                    stopOb.subscribe(any -> currentThread.interrupt()));

            try {
                while (!ob.isUnsubscribed()) {
                    logs = JobUtils.getInformationFromYarnLogDom(
                            authCode, containerLogUrl, type, nextStart, blockSize);
                    int lastLineBreak = logs.lastIndexOf('\n');

                    if (lastLineBreak < 0) {
                        // No linebreak found
                        if (logs.isEmpty()) {
                            // Remained line is a full line since the backend producing logs line by line
                            ob.onNext(remainedLine);
                            remainedLine = "";
                        } else {
                            remainedLine += logs;
                            nextStart += logs.length();
                        }
                    } else {
                        long handledLength = new BufferedReader(new StringReader(
                                                remainedLine + logs.substring(0, lastLineBreak)))
                                .lines()
                                .map(line -> {
                                    ob.onNext(line);

                                    // Count the line length with linebreak
                                    // We need to handle this since the web client may convert the LF to CRLF
                                    return (line.length() + 1);
                                })
                                .reduce((x, y) -> x + y)
                                .orElse(0);

                        nextStart += handledLength - remainedLine.length();
                        remainedLine = "";
                    }

                    Thread.sleep(retryIntervalMs);
                }
            } catch (InterruptedException ignore) {
            } finally {
                // Get the rest logs from history server
                // Don't worry about the log is moved to history server, the YarnUI can do URL redirect by itself
                logs = JobUtils.getInformationFromYarnLogDom(authCode, containerLogUrl, type, nextStart, 0);

                new BufferedReader(new StringReader(remainedLine + logs)).lines().forEach(ob::onNext);
            }

            ob.onCompleted();
            stopSubscriptionOptional.ifPresent(Subscription::unsubscribe);
        }).subscribeOn(Schedulers.io());
    }

    public static HttpEntity getEntity(@NotNull final IClusterDetail clusterDetail, @NotNull final String url) throws IOException, HDIException {
        final HttpClient client;
        if (clusterDetail instanceof MfaEspCluster) {
            final String tenantId = ((MfaEspCluster) clusterDetail).getTenantId();

            if (tenantId == null) {
                throw new UnknownServiceException("Can't get HIB cluster Tenant ID");
            }

            client = new SparkBatchEspMfaSubmission(tenantId, clusterDetail.getName()).getHttpClient();
        } else {
            provider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(clusterDetail.getHttpUserName(), clusterDetail.getHttpPassword()));
            client = HttpClients.custom()
                    .useSystemProperties()
                    .setDefaultCredentialsProvider(provider).build();
        }

        final HttpGet get = new HttpGet(url);
        final HttpResponse response = client.execute(get);
        int code = response.getStatusLine().getStatusCode();
        if (code == HttpStatus.SC_OK || code == HttpStatus.SC_CREATED) {
            return response.getEntity();
        } else {
            throw new HDIException(response.getStatusLine().getReasonPhrase(), response.getStatusLine().getStatusCode());
        }
    }

    @Nullable
    private static BlobContainer getSparkClusterContainer(ClientStorageAccount storageAccount, String dealtContainerName) throws AzureCmdException {
        List<BlobContainer> containerList = StorageClientSDKManager.getManager().getBlobContainers(storageAccount.getConnectionString());
        for (BlobContainer container : containerList) {
            if (container.getName().toLowerCase().equals(dealtContainerName.toLowerCase())) {
                return container;
            }
        }

        return null;
    }

    public static void ctrlInfo(@Nullable Observer<SimpleImmutableEntry<MessageInfoType, String>> legacyLogSubject,
                                @Nullable Observer<SparkBatchJobLogLine> newLogSubject,
                                String message) {
        assert legacyLogSubject != null || newLogSubject != null : "At least one logSubject is not null";
        assert legacyLogSubject == null || newLogSubject == null : "At least one logSubject should be null";

        if (legacyLogSubject != null) {
            legacyLogSubject.onNext(new SimpleImmutableEntry<>(Info, message));
        } else {
            newLogSubject.onNext(new SparkBatchJobLogLine(Tool, Info, message));
        }
    }

    public static void ctrlError(@Nullable Observer<SimpleImmutableEntry<MessageInfoType, String>> legacyLogSubject,
                                 @Nullable Observer<SparkBatchJobLogLine> newLogSubject,
                                 Throwable error) {
        assert legacyLogSubject != null || newLogSubject != null : "At least one logSubject is not null";
        assert legacyLogSubject == null || newLogSubject == null : "At least one logSubject should be null";

        if (legacyLogSubject != null) {
            legacyLogSubject.onError(error);
        } else {
            newLogSubject.onError(error);
        }
    }

    public static void ctrlComplete(@Nullable Observer<SimpleImmutableEntry<MessageInfoType, String>> legacyLogSubject,
                                    @Nullable Observer<SparkBatchJobLogLine> newLogSubject) {
        assert legacyLogSubject != null || newLogSubject != null : "At least one logSubject is not null";
        assert legacyLogSubject == null || newLogSubject == null : "At least one logSubject should be null";

        if (legacyLogSubject != null) {
            legacyLogSubject.onCompleted();
        } else {
            newLogSubject.onCompleted();
        }
    }

    @Deprecated
    public static String uploadFileToAzureBase(File file,
                                               IHDIStorageAccount storageAccount,
                                               String containerName,
                                               String uploadFolderPath,
                                               @Nullable Observer<SimpleImmutableEntry<MessageInfoType, String>> legacyLogSubject,
                                               @Nullable Observer<SparkBatchJobLogLine> newLogSubject,
                                               @Nullable CallableSingleArg<Void, Long> uploadInProcessCallback) throws Exception {
        if(storageAccount.getAccountType() == StorageAccountType.BLOB) {
            try (FileInputStream fileInputStream = new FileInputStream(file)) {
                try (BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream)) {
                    HDStorageAccount blobStorageAccount = (HDStorageAccount) storageAccount;
                    BlobContainer container = getSparkClusterContainer(blobStorageAccount, containerName);
                    if (container == null) {
                        throw new IllegalArgumentException("Can't get the valid container.");
                    }

                    String path = String.format("SparkSubmission/%s/%s", uploadFolderPath, file.getName());
                    String uploadedPath = String.format("wasbs://%s@%s/%s", containerName, blobStorageAccount.getFullStorageBlobName(), path);

                    ctrlInfo(legacyLogSubject, newLogSubject,
                             String.format("Begin uploading file %s to Azure Blob Storage Account %s ...",
                                           file.getPath(), uploadedPath));

                    StorageClientSDKManager.getManager().uploadBlobFileContent(
                            blobStorageAccount.getConnectionString(),
                            container,
                            path,
                            bufferedInputStream,
                            uploadInProcessCallback,
                            1024 * 1024,
                            file.length());

                    ctrlInfo(legacyLogSubject, newLogSubject,
                             String.format("Submit file to azure blob '%s' successfully.", uploadedPath));

                    return uploadedPath;
                }
            }
        } else if(storageAccount.getAccountType() == StorageAccountType.ADLS) {
            String uploadPath = String.format("adl://%s.azuredatalakestore.net%s%s", storageAccount.getName(), storageAccount.getDefaultContainerOrRootPath(), "SparkSubmission");
            ctrlInfo(legacyLogSubject, newLogSubject,
                     String.format("Begin uploading file %s to Azure Datalake store %s ...",
                                   file.getPath(), uploadPath));

            String uploadedPath = StreamUtil.uploadArtifactToADLS(file, storageAccount, uploadFolderPath);

            ctrlInfo(legacyLogSubject, newLogSubject,
                     String.format("Submit file to Azure Datalake store '%s' successfully.", uploadedPath));

            return uploadedPath;
        } else {
            throw new UnsupportedOperationException("unknown storage account type");
        }
    }

    @Deprecated
    public static String uploadFileToAzureNew(File file,
                                              IHDIStorageAccount storageAccount,
                                              String containerName,
                                              String uploadFolderPath,
                                              Observer<SparkBatchJobLogLine> logSubject,
                                              @Nullable CallableSingleArg<Void, Long> uploadInProcessCallback)
            throws Exception {
        return uploadFileToAzureBase(file, storageAccount, containerName, uploadFolderPath, null, logSubject,
                                     uploadInProcessCallback);
    }

    @Deprecated
    public static String uploadFileToAzure(File file,
                                           IHDIStorageAccount storageAccount,
                                           String containerName,
                                           String uploadFolderPath,
                                           Observer<SimpleImmutableEntry<MessageInfoType, String>> logSubject,
                                           @Nullable CallableSingleArg<Void, Long> uploadInProcessCallback) throws Exception {
        return uploadFileToAzureBase(file, storageAccount, containerName, uploadFolderPath, logSubject, null,
                                     uploadInProcessCallback);
    }

    public static String sftpFileToEmulator(String localFile, String folderPath, IClusterDetail clusterDetail)
                                           throws  IOException,HDIException, JSchException, SftpException {
        EmulatorClusterDetail emulatorClusterDetail = (EmulatorClusterDetail) clusterDetail;
        final File file = new File(localFile);
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            try (BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream)) {
                String sshEndpoint = emulatorClusterDetail.getSSHEndpoint();
                URL url = new URL(sshEndpoint);
                String host = url.getHost();
                int port = url.getPort();

                JSch jsch = new JSch();
                Session session = jsch.getSession(emulatorClusterDetail.getHttpUserName(), host, port);
                session.setPassword(emulatorClusterDetail.getHttpPassword());

                java.util.Properties config = new java.util.Properties();
                config.put("StrictHostKeyChecking", "no");
                session.setConfig(config);

                session.connect();
                ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
                channel.connect();

                String[] folders = folderPath.split( "/" );
                for ( String folder : folders ) {
                    if (folder.length() > 0) {
                        try {
                            channel.cd(folder);
                        } catch (SftpException e) {
                            channel.mkdir(folder);
                            channel.cd(folder);
                        }
                    }
                }

                channel.put(bufferedInputStream, file.getName());
                channel.disconnect();
                session.disconnect();
                return file.getName();
            }
        }
    }

    public static String getFormatPathByDate() {
        int year = Calendar.getInstance(TimeZone.getTimeZone("UTC")).get(Calendar.YEAR);
        int month = Calendar.getInstance(TimeZone.getTimeZone("UTC")).get(Calendar.MONTH) + 1;
        int day = Calendar.getInstance(TimeZone.getTimeZone("UTC")).get(Calendar.DAY_OF_MONTH);

        String uniqueFolderId = UUID.randomUUID().toString();

        return String.format("%04d/%02d/%02d/%s", year, month, day, uniqueFolderId);
    }


    public static String uploadFileToEmulator(@NotNull IClusterDetail selectedClusterDetail,
                                              @NotNull String buildJarPath,
                                              @NotNull Observer<SparkBatchJobLogLine> logSubject) throws Exception {
        logSubject.onNext(new SparkBatchJobLogLine(Tool, Info, String.format("Get target jar from %s.",
                                                                             buildJarPath)));
        String uniqueFolderId = UUID.randomUUID().toString();
        String folderPath = String.format("../opt/livy/SparkSubmission/%s", uniqueFolderId);
        return String.format("/opt/livy/SparkSubmission/%s/%s",
                uniqueFolderId, sftpFileToEmulator(buildJarPath, folderPath, selectedClusterDetail));
    }

    public static String uploadFileToHDFSBase(IClusterDetail selectedClusterDetail,
                                              String buildJarPath,
                                              @Nullable Observer<SimpleImmutableEntry<MessageInfoType, String>> legacyLogSubject,
                                              @Nullable Observer<SparkBatchJobLogLine> newLogSubject)
            throws HDIException {
        ctrlInfo(legacyLogSubject, newLogSubject, String.format("Get target jar from %s.", buildJarPath));

        File srcJarFile = new File(buildJarPath);
        URI destUri = URI.create(String.format("/SparkSubmission/%s/%s", getFormatPathByDate(), srcJarFile.getName()));

        String username = selectedClusterDetail.getHttpUserName();
        String password = selectedClusterDetail.getHttpPassword();
        String sessionName = "Helper session to upload " + destUri.toString();

        URI livyUri = selectedClusterDetail instanceof LivyCluster ?
                      URI.create(((LivyCluster) selectedClusterDetail).getLivyConnectionUrl()) :
                      URI.create(selectedClusterDetail.getConnectionUrl());

        ctrlInfo(legacyLogSubject, newLogSubject, "Create Spark helper interactive session...");

        try {
            return Observable.using(() -> new SparkSession(sessionName, livyUri, username, password),
                                    SparkSession::create,
                                    SparkSession::close)
                             .map(sparkSession -> {
                                 sparkSession.getCtrlSubject()
                                             .subscribe(logLine -> ctrlInfo(legacyLogSubject, newLogSubject,
                                                                            logLine.getRawLog()),
                                                        err -> ctrlError(legacyLogSubject, newLogSubject, err),
                                                        () -> ctrlComplete(legacyLogSubject, newLogSubject));

                                 ClusterFileBase64BufferedOutputStream clusterFileBase64Out =
                                         new ClusterFileBase64BufferedOutputStream(sparkSession, destUri);
                                 Base64OutputStream base64Enc = new Base64OutputStream(clusterFileBase64Out, true);
                                 InputStream inFile;

                                 try {
                                     inFile = new BufferedInputStream(new FileInputStream(srcJarFile));

                                     ctrlInfo(legacyLogSubject, newLogSubject, String.format("Uploading %s...",
                                                                                             srcJarFile));
                                     IOUtils.copy(inFile, base64Enc);

                                     inFile.close();
                                     base64Enc.close();
                                 } catch (FileNotFoundException fnfEx) {
                                     throw propagate(new HDIException(String.format("Source file %s not found.",
                                                                                    srcJarFile), fnfEx));
                                 } catch (IOException ioEx) {
                                     throw propagate(new HDIException(String.format("Failed to upload file %s.",
                                                                                    destUri), ioEx));
                                 }
                                 ctrlInfo(legacyLogSubject, newLogSubject, String.format("Uploaded to %s.", destUri));

                                 return destUri.toString();
                             })
                             .toBlocking()
                             .single();
        } catch (NoSuchElementException ignored) {
            // The cause exception will be thrown inside
            throw new HDIException("Failed to upload file to HDFS (Should Not Reach).");
        }
    }

    public static String uploadFileToHDFSNew(IClusterDetail selectedClusterDetail,
                                          String buildJarPath,
                                          Observer<SparkBatchJobLogLine> logSubject) throws HDIException {
        return uploadFileToHDFSBase(selectedClusterDetail, buildJarPath, null, logSubject);
    }


    public static String uploadFileToHDFS(IClusterDetail selectedClusterDetail,
                                          String buildJarPath,
                                          Observer<SimpleImmutableEntry<MessageInfoType, String>> logSubject) throws HDIException {
        return uploadFileToHDFSBase(selectedClusterDetail, buildJarPath, logSubject, null);
    }

    public static String uploadFileToCluster(@NotNull final IClusterDetail selectedClusterDetail,
                                    @NotNull final String buildJarPath,
                                    @NotNull Observer<SparkBatchJobLogLine> logSubject) throws Exception {

        return selectedClusterDetail.isEmulator() ?
                JobUtils.uploadFileToEmulator(selectedClusterDetail, buildJarPath, logSubject) :
                (selectedClusterDetail.getStorageAccount() == null ?
                        JobUtils.uploadFileToHDFSNew(selectedClusterDetail, buildJarPath, logSubject):
                        JobUtils.uploadFileToAzureNew(
                                new File(buildJarPath),
                                selectedClusterDetail.getStorageAccount(),
                                selectedClusterDetail.getStorageAccount().getDefaultContainerOrRootPath(),
                                getFormatPathByDate(),
                                logSubject,
                                null));
    }

    // Have to catch IOException in subscribe
    @NotNull
    public static Observable<String> deployArtifactToADLS(@NotNull String artifactLocalPath,
                                                 @NotNull String adlRootPath,
                                                 @NotNull String accessToken) {
        return Observable.fromCallable(() -> {
            File localFile = new File(artifactLocalPath);

            URI remote = URI.create(adlRootPath)
                    .resolve("SparkSubmission/")
                    .resolve(getFormatPathByDate() + "/")
                    .resolve(localFile.getName());

            ADLStoreClient storeClient = ADLStoreClient.createClient(remote.getHost(), accessToken);

            try (OutputStream adlsOutputStream = storeClient.createFile(remote.getPath(), IfExists.OVERWRITE, "755", true)) {
                long size = IOUtils.copyLarge(new FileInputStream(localFile), adlsOutputStream);

                adlsOutputStream.flush();
                adlsOutputStream.close();

                return remote.toString();
            }
        });
    }

    public static Observable<String> deployArtifact(@NotNull String artifactLocalPath,
                                                    @NotNull final IHDIStorageAccount storageAccount,
                                                    @NotNull Observer<SparkBatchJobLogLine> logSubject) {
        return Observable.fromCallable(() -> JobUtils.uploadFileToAzureNew(
                new File(artifactLocalPath),
                storageAccount,
                storageAccount.getDefaultContainerOrRootPath(),
                getFormatPathByDate(),
                logSubject,
                null));
    }

    public static Single<SimpleImmutableEntry<IClusterDetail, String>> deployArtifact(@NotNull String artifactLocalPath,
                                                        @NotNull String clusterName,
                                                        @NotNull Observer<SparkBatchJobLogLine> logSubject) {
        return Single.create(ob -> {
            try {
                IClusterDetail clusterDetail = ClusterManagerEx.getInstance()
                        .getClusterDetailByName(clusterName)
                        .orElseThrow(() -> new HDIException("No cluster name matched selection: " + clusterName));

                String jobArtifactUri = JobUtils.uploadFileToCluster(
                        clusterDetail,
                        artifactLocalPath,
                        logSubject);


                ob.onSuccess(new SimpleImmutableEntry<>(clusterDetail, jobArtifactUri));
            } catch (Exception e) {
                ob.onError(e);
            }
        });
    }

    public static Observable<String> deployArtifact(@NotNull SparkBatchSubmission submission,
                                                    @NotNull String destinationRootPath,
                                                    @NotNull String artifactPath) {
        return Observable.fromCallable(() -> {
            File file = new File(artifactPath);
            String webHdfsUploadPath = destinationRootPath.concat(file.getName());
            String redirectUri = null;

            List<NameValuePair> params = new WebHdfsParamsBuilder("CREATE")
                    .setOverwrite("true")
                    .build();

            URIBuilder uriBuilder = new URIBuilder(webHdfsUploadPath);
            uriBuilder.addParameters(params);

            HttpUriRequest req = RequestBuilder
                    .put(uriBuilder.build())
                    .build();

            CloseableHttpClient httpclient = submission.getHttpClient();
            try (CloseableHttpResponse response = httpclient.execute(req)) {
                //two steps to upload via webhdfs
                // 1.put request the get 307 redirect uri from response
                // 2.put redirect request with file content as setEntity
                redirectUri = response
                        .getFirstHeader("Location")
                        .getValue();

                if (StringUtils.isBlank(redirectUri)) {
                    throw new UnknownServiceException("can not get valid redirect uri using webhdfs");
                }
            } catch (Exception ex) {
                throw new UnknownServiceException("using webhdfs encounter problem:".concat(ex.toString()));
            }

            InputStreamEntity reqEntity = new InputStreamEntity(
                    new FileInputStream(file),
                    -1,
                    ContentType.APPLICATION_OCTET_STREAM);
            reqEntity.setChunked(true);
            BufferedHttpEntity reqEntityBuf = new BufferedHttpEntity(reqEntity);

            //setup url with redirect url and entity ,config 100 continue to header
            req = RequestBuilder
                    .put(redirectUri)
                    .setEntity(reqEntityBuf)
                    .setConfig(RequestConfig.custom().setExpectContinueEnabled(true).build())
                    .build();

            // execute put request
            try (CloseableHttpResponse putResp = httpclient.execute(req)) {
                params = new WebHdfsParamsBuilder("OPEN")
                        .build();
                uriBuilder = new URIBuilder(webHdfsUploadPath);
                uriBuilder.addParameters(params);

                //return get file uri
                return uriBuilder.build().toString();
            }
        });
    }

    public static Cache getGlobalCache() {
        return globalCache;
    }

    public static AbstractMap.SimpleImmutableEntry<Integer, List<Header>>
    authenticate(IClusterDetail clusterDetail) throws HDIException, IOException {
        SparkBatchSubmission submission = SparkBatchSubmission.getInstance();
        String livyUrl = clusterDetail instanceof LivyCluster ? ((LivyCluster) clusterDetail).getLivyBatchUrl() : null;
        if (livyUrl == null) {
            throw new IOException("Can't get livy connection Url");
        }

        if (!StringUtils.isEmpty(clusterDetail.getHttpUserName()) && !StringUtils.isEmpty(clusterDetail.getHttpPassword())) {
            submission.setUsernamePasswordCredential(clusterDetail.getHttpUserName(), clusterDetail.getHttpPassword());
        }

        com.microsoft.azure.hdinsight.sdk.common.HttpResponse response = clusterDetail instanceof MfaEspCluster
                ? submission.negotiateAuthMethodWithResp(livyUrl)
                : submission.getHttpResponseViaHead(livyUrl);

        int statusCode = response.getCode();
        if (statusCode >= 200 && statusCode <= 302) {
            return new AbstractMap.SimpleImmutableEntry<>(statusCode, response.getHeaders());
        }

        throw new AuthenticationException(response.getContent(), statusCode);
    }
}
