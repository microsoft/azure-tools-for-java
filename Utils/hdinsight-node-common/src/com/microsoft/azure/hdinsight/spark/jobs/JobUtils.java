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
package com.microsoft.azure.hdinsight.spark.jobs;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.DomNodeList;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.microsoft.azure.hdinsight.common.HDInsightLoader;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.sdk.common.HDIException;
import com.microsoft.azure.hdinsight.sdk.rest.yarn.rm.App;
import com.microsoft.azure.hdinsight.sdk.rest.yarn.rm.ApplicationMasterLogs;
import com.microsoft.azure.hdinsight.spark.jobs.livy.LivyBatchesInformation;
import com.microsoft.azure.hdinsight.spark.jobs.livy.LivySession;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.StringHelper;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.sun.net.httpserver.HttpExchange;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.concurrent.ExecutionException;

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

    private static final WebClient HTTP_WEB_CLIENT = new WebClient();

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
        final CredentialsProvider credentialsProvider  =  new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(clusterDetail.getHttpUserName(), clusterDetail.getHttpPassword()));
        HTTP_WEB_CLIENT.setCredentialsProvider(credentialsProvider);

        String standerr = getInformationFromYarnLogDom(url + "/stderr/?start=0");
        String standout = getInformationFromYarnLogDom(url + "/stout/?start=0");
        String directoryInfo = getInformationFromYarnLogDom(url + "/directory.info/?start=0");
        return new ApplicationMasterLogs(standout, standerr, directoryInfo);
    }

    private static String getInformationFromYarnLogDom(@NotNull String url) {
        try {
            HtmlPage htmlPage = HTTP_WEB_CLIENT.getPage(url);
            // parse pre tag from html response
            // there's only one 'pre' in response
            DomNodeList<DomElement> preTagElements = htmlPage.getElementsByTagName("pre");
            if (preTagElements.size() != 0) {
                return preTagElements.get(0).asText();
            }
        } catch (IOException e) {
            LOGGER.error("get Driver Log Error", e);
        }
        return "";
    }

    public static HttpEntity getEntity(@NotNull final IClusterDetail clusterDetail, @NotNull final String url) throws IOException, HDIException {
        provider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(clusterDetail.getHttpUserName(), clusterDetail.getHttpPassword()));
        final HttpClient client = HttpClients.custom().setDefaultCredentialsProvider(provider).build();

        final HttpGet get = new HttpGet(url);
        final HttpResponse response = client.execute(get);
        int code = response.getStatusLine().getStatusCode();
        if (code == HttpStatus.SC_OK || code == HttpStatus.SC_CREATED) {
            return response.getEntity();
        } else {
            throw new HDIException(response.getStatusLine().getReasonPhrase(), response.getStatusLine().getStatusCode());
        }
    }
}
