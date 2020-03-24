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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.hdinsight.common.HttpFutureCallback;
import com.microsoft.azure.hdinsight.common.MultiHttpFutureCallback;
import com.microsoft.azure.hdinsight.common.task.*;
import com.microsoft.azure.hdinsight.spark.jobs.framework.RequestDetail;
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail;
import com.microsoft.azure.hdinsight.spark.jobs.framework.HttpRequestType;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class JobViewDummyHttpServer {
    private static RequestDetail requestDetail;
    public static final int PORT = 39128;
    private static HttpServer server;
    private static final int NO_OF_THREADS = 10;
    private static ExecutorService executorService;
    private static boolean isEnabled = false;

    public static RequestDetail getCurrentRequestDetail() {
        return requestDetail;
    }

    public synchronized static boolean isEnabled() {
        return isEnabled;
    }

    public synchronized static void close() {
        if (server != null) {
            server.stop(0);
        }
        if (executorService != null) {
            executorService.shutdown();
            try {
                executorService.awaitTermination(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
            }
        }
        isEnabled = false;
    }


    public synchronized static void initlize() {
        if (isEnabled) {
            return;
        }
        try {
            server = HttpServer.create(new InetSocketAddress(PORT), 10);

            server.createContext("/clusters/", new HttpHandler() {
                @Override
                public void handle(final HttpExchange httpExchange) throws IOException {
                    requestDetail = RequestDetail.getRequestDetail(httpExchange.getRequestURI());
                    if (requestDetail == null) {
                        return;
                    }

                    IClusterDetail clusterDetail = requestDetail.getClusterDetail();
                    final String queryUrl = requestDetail.getQueryUrl();
                    if (requestDetail.getApiType() == HttpRequestType.YarnHistory) {
                        TaskExecutor.submit(new YarnHistoryTask(clusterDetail, queryUrl, new HttpFutureCallback(httpExchange) {
                            @Override
                            public void onSuccess(String str) {
                                httpExchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                                try {
                                    // work around of get job result
                                    //TODO: get job result by REST API
                                    Document doc = Jsoup.parse(str);
                                    Elements contentElements = doc.getElementsByClass("content");
                                    if (contentElements.size() == 1) {
                                        Elements elements = contentElements.get(0).getElementsByTag("pre");
                                        if (elements.size() == 1) {
                                            str = elements.get(0).html();
                                        }
                                    }

                                    httpExchange.sendResponseHeaders(200, str.length());
                                    OutputStream stream = httpExchange.getResponseBody();
                                    stream.write(str.getBytes());
                                    stream.close();
                                } catch (IOException e) {
                                    int a = 1;
//                                    LOGGER.error("Get job history error", e);
                                }
                            }

                        }));
                    } else if (requestDetail.getApiType() == HttpRequestType.LivyBatchesRest) {
                        TaskExecutor.submit(new LivyTask(clusterDetail, queryUrl, new HttpFutureCallback(httpExchange) {
                            @Override
                            public void onSuccess(String str) {
                                httpExchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                                try {
                                    String applicationId = requestDetail.getProperty("applicationId");
                                    if (applicationId != null) {
                                        str = JobUtils.getJobInformation(str, applicationId);
                                    }

                                    httpExchange.sendResponseHeaders(200, str.length());
                                    OutputStream stream = httpExchange.getResponseBody();
                                    stream.write(str.getBytes());
                                    stream.close();
                                } catch (IOException e) {
//                                    LOGGER.error("Get job history error", e);
                                }
                            }
                        }));
                    } else if(requestDetail.getApiType() == HttpRequestType.MultiTask){
                        TaskExecutor.submit(new MultiRestTask(clusterDetail, requestDetail.getQueryUrls(), new MultiHttpFutureCallback(httpExchange){
                            public void onSuccess(List<String> strs) {
                                httpExchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                                try {
                                    String str = tasksDetailsConvert(strs);
                                    httpExchange.sendResponseHeaders(200, str.length());
                                    OutputStream stream = httpExchange.getResponseBody();
                                    stream.write(str.getBytes());
                                    stream.close();
                                } catch (IOException e) {
//                                    LOGGER.error("Get job history error", e);
                                }
                            }
                        }));
                    } else {
                        TaskExecutor.submit(new RestTask(clusterDetail, queryUrl, new HttpFutureCallback(httpExchange) {
                            @Override
                            public void onSuccess(@NotNull String str) {
                                httpExchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
                                try {
                                    httpExchange.sendResponseHeaders(200, str.length());
                                    OutputStream stream = httpExchange.getResponseBody();
                                    stream.write(str.getBytes());
                                    stream.close();
                                } catch (IOException e) {
//                                    LOGGER.error("Get job history error", e);
                                }
                            }
                        }));
                    }
                }
            });
            executorService = Executors.newFixedThreadPool(NO_OF_THREADS);
            server.setExecutor(executorService);
            server.start();
            isEnabled = true;
        } catch (IOException e) {
//            LOGGER.error("Get job history error", e);
//            DefaultLoader.getUIHelper().showError(e.getClass().getName(), e.getMessage());
        }
    }
    private static ObjectMapper mapper = new ObjectMapper();

    private static String tasksDetailsConvert(List<String> strs) throws IOException {
        List results = new ArrayList();
        for(String str : strs) {
            List taskList = taskConvert(str);
            if(taskList != null) {
                results.addAll(taskList);
            }
        }
        return mapper.writeValueAsString(results);
    }

    private static List taskConvert(String json) throws IOException {
        List names = mapper.readValue(json, List.class);
        LinkedHashMap map = (LinkedHashMap) names.get(0);
        if(map == null) {
            return null;
        }

        LinkedHashMap tasks = (LinkedHashMap) map.get("tasks");
        if(tasks == null) {
            return null;
        }

        Object[] objs = tasks.entrySet().toArray();
        if(objs == null) {
            return null;
        }
        return Arrays.asList(objs);
    }
}
