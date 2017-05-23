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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.microsoft.azure.hdinsight.common.HDInsightLoader;
import com.microsoft.azure.hdinsight.common.JobViewManager;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.sdk.common.HDIException;
import com.microsoft.azure.hdinsight.spark.jobs.framework.RequestDetail;
import com.microsoft.azure.hdinsight.spark.jobs.livy.LivyBatchesInformation;
import com.microsoft.azure.hdinsight.spark.jobs.livy.LivySession;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.azurecommons.helpers.StringHelper;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import javafx.application.Application;
import javafx.stage.Stage;
import org.apache.commons.io.FileUtils;
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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/*
    this class is pass to Web Page when load html from WebEngin
    non-related web page action should not be here.
 */
public class JobUtils {
    private static String defaultYarnUIHistoryFormat = "https://%s.azurehdinsight.net/yarnui/hn/cluster";
    private static String yarnUIHisotryFormat = "https://%s.azurehdinsight.net/yarnui/hn/cluster/app/%s";

    private static String sparkUIHistoryFormat = "https://%s.azurehdinsight.net/sparkhistory/history/%s/jobs";
    private static String defaultSparkUIHistoryFormat = "https://%s.azurehdinsight.net/sparkhistory";
    private Map<String, String> webPageMaps = new HashMap<>();

    private static final String JobLogFolderName = "SparkJobLog";
    private static final String SPARK_EVENT_LOG_FOLDER_NAME = "SparkEventLog";
    private static CredentialsProvider provider = new BasicCredentialsProvider();

    public static URI getLivyLogPath(@NotNull String rootPath, @NotNull String applicationId) {
        String path = StringHelper.concat(rootPath, File.separator, JobLogFolderName, File.separator, applicationId);
        File file = new File(path);
        if(!file.exists()) {
            file.mkdirs();
        }
        return file.toURI();
    }

    public String getItem(@Nullable String key) {
        return webPageMaps.get(key);
    }
    public void setItem(@NotNull String key, @Nullable String value) {
        webPageMaps.put(key, value);
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

    public void openYarnUIHistory(String applicationId) {
        RequestDetail requestDetail = JobHttpHandler.getCurrentRequestDetail();
        String yarnHistoryUrl = null;
        if(StringHelper.isNullOrWhiteSpace(applicationId)) {
            yarnHistoryUrl = String.format(defaultYarnUIHistoryFormat, requestDetail.getClusterDetail().getName());
        } else {
            yarnHistoryUrl = String.format(yarnUIHisotryFormat, requestDetail.getClusterDetail().getName(), applicationId);
        }
        openDefaultBrowser(yarnHistoryUrl);
    }

    public void openSparkUIHistory(String applicationId) {
        RequestDetail requestDetail = JobHttpHandler.getCurrentRequestDetail();
        String sparkHistoryUrl = null;
        if(StringHelper.isNullOrWhiteSpace(applicationId)) {
            sparkHistoryUrl = String.format(defaultSparkUIHistoryFormat, requestDetail.getClusterDetail().getName());
        } else {
            sparkHistoryUrl = String.format(sparkUIHistoryFormat, requestDetail.getClusterDetail().getName(), applicationId);
        }

        openDefaultBrowser(sparkHistoryUrl);
    }

    public void openDefaultBrowser(@NotNull final String url) {
        Application application = new Application() {
            @Override
            public void start(Stage primaryStage) throws Exception {
                getHostServices().showDocument(url);
            }
        };

        try {
            application.start(null);
        }catch (Exception e) {
            DefaultLoader.getUIHelper().showError("Failed to open browser", "Open browser Error");
        }
    }

    public void openFileExplorer(URI uri) {
        openDefaultBrowser(uri.toString());
    }

    public void openLivyLog(String applicationId) {
        openFileExplorer(getLivyLogPath(HDInsightLoader.getHDInsightHelper().getPluginRootPath(), applicationId));
    }

    private static final String EVENT_LOG_REST_API = "applications/%s/logs";
    private static final String Event_LOG_FILE_NAME = "eventLogs.zip";

    public void openSparkEventLog(String uuid, String applicationId) {
        IClusterDetail clusterDetail = JobViewManager.getCluster(uuid);
        String path = StringHelper.concat(HDInsightLoader.getHDInsightHelper().getPluginRootPath(), File.separator, SPARK_EVENT_LOG_FOLDER_NAME, File.separator, applicationId);
        File downloadFile = new File(path, Event_LOG_FILE_NAME);
        File file = new File(path);
        if(!file.exists() || !downloadFile.exists()) {
            if(!file.exists()) {
                file.mkdirs();
            }
            String restApi = String.format(EVENT_LOG_REST_API, applicationId);

            try {
                HttpEntity entity = getEntity(clusterDetail,restApi);
                FileUtils.copyInputStreamToFile(entity.getContent(), downloadFile);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        openFileExplorer(file.toURI());
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
