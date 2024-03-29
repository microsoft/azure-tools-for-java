/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.common;

import com.microsoft.azure.hdinsight.common.CommonConst;
import com.microsoft.azure.hdinsight.common.StreamUtil;
import com.microsoft.azure.hdinsight.common.logger.ILogger;
import com.microsoft.azure.hdinsight.sdk.common.errorresponse.*;
import com.microsoft.azure.hdinsight.sdk.rest.ObjectConvertUtils;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.service.ServiceManager;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.*;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.HeaderGroup;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.apache.http.util.EntityUtils;
import rx.Observable;
import rx.exceptions.Exceptions;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.UnknownServiceException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static rx.exceptions.Exceptions.propagate;

public class HttpObservable implements ILogger {
    @NotNull
    private RequestConfig defaultRequestConfig;

    @NotNull
    private String userAgentPrefix;

    @Nullable
    private String userAgent = null;

    @NotNull
    private HeaderGroup defaultHeaders;

    @NotNull
    private CookieStore cookieStore;

    @NotNull
    private HttpContext httpContext;

    @NotNull
    private CloseableHttpClient httpClient;

    @NotNull
    private List<NameValuePair> defaultParameters = new ArrayList<>();


    /*
     * Constructors
     */

    public HttpObservable() {
        this.defaultHeaders = new HeaderGroup();

        String loadingClass = this.getClass().getClassLoader().getClass().getName().toLowerCase();
        this.userAgentPrefix = loadingClass.contains("intellij") ? "Azure Toolkit for IntelliJ" :
                (loadingClass.contains("eclipse") ? "Azure Toolkit for Eclipse" : "Azure HDInsight SDK HTTP RxJava client");
        this.userAgent = userAgentPrefix;

        // set default headers
        this.defaultHeaders.setHeaders(new Header[] {
                new BasicHeader("Content-Type", "application/json"),
                new BasicHeader("User-Agent", userAgent),
                new BasicHeader("X-Requested-By", "ambari")
        });

        this.cookieStore = new BasicCookieStore();
        this.httpContext = new BasicHttpContext();
        this.httpContext.setAttribute(HttpClientContext.COOKIE_STORE, cookieStore);

        // Create global request configuration
        this.defaultRequestConfig = RequestConfig.custom()
                .setCookieSpec(CookieSpecs.DEFAULT)
                .setTargetPreferredAuthSchemes(Arrays.asList(AuthSchemes.KERBEROS, AuthSchemes.DIGEST, AuthSchemes.BASIC))
                .setProxyPreferredAuthSchemes(Collections.singletonList(AuthSchemes.BASIC))
                .build();

        this.httpClient = HttpClients.custom()
                .useSystemProperties()
                .setDefaultCookieStore(getCookieStore())
                .setDefaultRequestConfig(getDefaultRequestConfig())
                .setSSLSocketFactory(createSSLSocketFactory())
                .build();
    }

    /**
     * Constructor with basic authentication
     *
     * @param username Basic authentication user name
     * @param password Basic authentication password
     */
    public HttpObservable(@NotNull final String username, @NotNull final String password) {
        this();

        if (StringUtils.isNotBlank(username)) {
            String auth = username + ":" + password;
            final byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(StandardCharsets.ISO_8859_1));
            setDefaultHeader(new BasicHeader(
                    HttpHeaders.AUTHORIZATION, String.format("%s %s", "Basic", new String(encodedAuth))));
        }

        this.httpClient = HttpClients.custom()
                .useSystemProperties()
                .setDefaultCookieStore(getCookieStore())
                .setDefaultRequestConfig(getDefaultRequestConfig())
                .setSSLSocketFactory(createSSLSocketFactory())
                .build();
    }

    /*
     * Getter / Setter
     */

    @NotNull
    public String getUserAgentPrefix() {
        return userAgentPrefix;
    }

    @NotNull
    public RequestConfig getDefaultRequestConfig() {
        return defaultRequestConfig;
    }

    public HttpObservable setDefaultRequestConfig(@NotNull RequestConfig defaultRequestConfig) {
        this.defaultRequestConfig = defaultRequestConfig;

        return this;
    }

    @NotNull
    public CookieStore getCookieStore() {
        return cookieStore;
    }

    public HttpObservable setCookieStore(@NotNull CookieStore cookieStore) {
        this.cookieStore = cookieStore;

        return this;
    }

    @NotNull
    public CloseableHttpClient getHttpClient() {
        return httpClient;
    }

    public HttpObservable setHttpClient(@NotNull CloseableHttpClient httpClient) {
        this.httpClient = httpClient;

        return this;
    }

    public Header[] getDefaultHeaders() throws IOException {
        return defaultHeaders.getAllHeaders();
    }

    public HttpObservable setDefaultHeader(@Nullable Header defaultHeader) {
        this.defaultHeaders.updateHeader(defaultHeader);
        return this;
    }

    @Nullable
    public HeaderGroup getDefaultHeaderGroup()  {
        return defaultHeaders;
    }

    public HttpObservable setDefaultHeaderGroup(@Nullable HeaderGroup defaultHeaders) {
        this.defaultHeaders = defaultHeaders;
        return this;
    }

    public HttpObservable setContentType(@NotNull String type) {
        this.defaultHeaders.updateHeader(new BasicHeader("Content-Type", type));
        return this;
    }

    @NotNull
    public HttpContext getHttpContext() {
        return httpContext;
    }

    @Nullable
    public String getUserAgent() {
        return userAgent;
    }

    public HttpObservable setUserAgent(@Nullable String userAgent) {
        this.userAgent = userAgent;

        // Update the default headers
        return setDefaultHeader(new BasicHeader("User-Agent", userAgent));
    }


    @NotNull
    public List<NameValuePair> getDefaultParameters() {
        return defaultParameters;
    }

    /*
     * Helper functions
     */

    public static boolean isSSLCertificateValidationDisabled() {
        try {
            return DefaultLoader.getIdeHelper().isApplicationPropertySet(CommonConst.DISABLE_SSL_CERTIFICATE_VALIDATION) &&
                    Boolean.valueOf(DefaultLoader.getIdeHelper().getApplicationProperty(CommonConst.DISABLE_SSL_CERTIFICATE_VALIDATION));
        } catch (Exception ex) {
            // To fix exception in unit test
            return false;
        }
    }

    private SSLConnectionSocketFactory createSSLSocketFactory() {
        TrustStrategy ts = ServiceManager.getServiceProvider(TrustStrategy.class);
        SSLConnectionSocketFactory sslSocketFactory = null;

        if (ts != null) {
            try {
                SSLContext sslContext = new SSLContextBuilder()
                        .loadTrustMaterial(ts)
                        .build();

                sslSocketFactory = new SSLConnectionSocketFactory(
                        sslContext,
                        new String[] { "TLSv1.2", "TLSv1.3" },
                        null,
                        HttpObservable.isSSLCertificateValidationDisabled()
                                ? NoopHostnameVerifier.INSTANCE
                                : new DefaultHostnameVerifier());

            } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
                log().error("Prepare SSL Context for HTTPS failure. " + ExceptionUtils.getStackTrace(e));
            }
        }
        return sslSocketFactory;
    }

    @NotNull
    public static HttpErrorStatus classifyHttpError(@NotNull CloseableHttpResponse httpResponse) throws IOException {
        StatusLine status = httpResponse.getStatusLine();
        int statusCode = status.getStatusCode();
        HttpEntity httpEntity = httpResponse.getEntity();
        String message = EntityUtils.toString(httpEntity);
        Header[] headers = httpResponse.getAllHeaders();
        if (statusCode == 400) {
            return new BadRequestHttpErrorStatus(message, headers, httpEntity);
        } else if (statusCode == 401) {
            return new UnauthorizedHttpErrorStatus(message, headers, httpEntity);
        } else if (statusCode == 403) {
            return new ForbiddenHttpErrorStatus(message, headers, httpEntity);
        } else if (statusCode == 404) {
            return new NotFoundHttpErrorStatus(message, headers, httpEntity);
        } else if (statusCode == 405) {
            return new MethodNotAllowedHttpErrorStatus(message, headers, httpEntity);
        } else if (statusCode == 500) {
            return new InternalServerErrorHttpErrorStatus(message, headers, httpEntity);
        } else if (statusCode == 504) {
            return new GatewayTimeoutErrorStatus(message, headers, httpEntity);
        } else {
            return new UnclassifiedHttpErrorStatus(statusCode, message, headers, httpEntity);
        }
    }

    /**
     * Helper to convert the closeable stream good Http response (2xx) to String result.
     * If the response is bad, propagate a HttpResponseException
     *
     * @param closeableHttpResponse the source closeable stream
     * @return Http Response as String
     */
    public static Observable<HttpResponse> toStringOnlyOkResponse(CloseableHttpResponse closeableHttpResponse) {
        return Observable.using(
                // Resource factory
                () -> closeableHttpResponse,
                // Observable factory
                streamResp -> {
                    try {
                        StatusLine status = streamResp.getStatusLine();

                        if (status.getStatusCode() >= 300) {
                            return Observable.error(classifyHttpError(streamResp));
                        }

                        return Observable.just(StreamUtil.getResultFromHttpResponse(streamResp));
                    } catch (IOException e) {
                        return Observable.error(e);
                    }
                },
                // Resource dispose
                streamResp -> {
                    try {
                        streamResp.close();
                    } catch (IOException ignore) {
                        // The connection will be closed automatically after timeout,
                        // the exception in closing can be ignored.
                    }
                });
    }

    /**
     * Helper to convert the http response to a specified type
     *
     * @param resp HTTP response, consumed as String content
     * @param clazz the target type to convert
     * @param <T> the target type
     * @return the specified type class instance
     */
    @NotNull
    public <T> T convertJsonResponseToObject(@NotNull final HttpResponse resp, @NotNull final Class<T> clazz) {
        try {
            return ObjectConvertUtils.convertJsonToObject(resp.getMessage(), clazz)
                    .orElseThrow(() -> propagate(
                            new HDIException("Unknown HTTP server response: " + resp.getMessage())));
        } catch (IOException e) {
            throw propagate(e);
        }
    }

    /*
     * Core request
     */
    public Observable<CloseableHttpResponse> request(@NotNull final HttpRequestBase httpRequest,
                                                     @Nullable final HttpEntity entity,
                                                     final List<NameValuePair> parameters,
                                                     final List<Header> addOrReplaceHeaders) {
        return Observable.fromCallable(() -> {
            URIBuilder builder = new URIBuilder(httpRequest.getURI());

            // Add parameters
            builder.setParameters(getDefaultParameters());

            Optional.ofNullable(parameters)
                    .filter(pairs -> !pairs.isEmpty())
                    .ifPresent(builder::addParameters);

            httpRequest.setURI(builder.build());

            // Set the default headers and update Headers
            httpRequest.setHeaders(getDefaultHeaders());
            Optional.ofNullable(addOrReplaceHeaders)
                    .ifPresent(headers -> headers.forEach(httpRequest::setHeader));

            // Set entity for non-entity
            if (httpRequest instanceof HttpEntityEnclosingRequestBase && entity != null) {
                ((HttpEntityEnclosingRequestBase)httpRequest).setEntity(entity);

                // Update the content type by entity
                httpRequest.setHeader(entity.getContentType());
            }

            return getHttpClient().execute(httpRequest, getHttpContext());
        });
    }

    /*
     * RESTful API operations with response conversion for specified type
     */
    public Observable<HttpResponse> requestWithHttpResponse(@NotNull final HttpRequestBase httpRequest,
                                                            @Nullable final HttpEntity entity,
                                                            final List<NameValuePair> parameters,
                                                            final List<Header> addOrReplaceHeaders) {
        return request(httpRequest, entity, parameters, addOrReplaceHeaders)
                .flatMap(HttpObservable::toStringOnlyOkResponse);
    }

    public Observable<HttpResponse> head(@NotNull final String uri,
                                         @NotNull final List<NameValuePair> parameters,
                                         @NotNull final List<Header> addOrReplaceHeaders) {
        return requestWithHttpResponse(new HttpHead(uri), null, parameters, addOrReplaceHeaders);
    }

    public <T> Observable<T> get(@NotNull final String uri,
                                 final List<NameValuePair> parameters,
                                 final List<Header> addOrReplaceHeaders,
                                 @NotNull final Class<T> clazz) {
        return requestWithHttpResponse(new HttpGet(uri), null, parameters, addOrReplaceHeaders)
                .map(resp -> this.convertJsonResponseToObject(resp, clazz));
    }

    public <T> Observable<T> put(@NotNull final String uri,
                                 @Nullable final HttpEntity entity,
                                 final List<NameValuePair> parameters,
                                 final List<Header> addOrReplaceHeaders,
                                 @NotNull final Class<T> clazz) {
        return requestWithHttpResponse(new HttpPut(uri), entity, parameters, addOrReplaceHeaders)
                .map(resp -> this.convertJsonResponseToObject(resp, clazz));
    }

    public <T> Observable<T> post(@NotNull final String uri,
                                  @Nullable final HttpEntity entity,
                                  final List<NameValuePair> parameters,
                                  final List<Header> addOrReplaceHeaders,
                                  @NotNull final Class<T> clazz) {
        return requestWithHttpResponse(new HttpPost(uri), entity, parameters, addOrReplaceHeaders)
                .map(resp -> this.convertJsonResponseToObject(resp, clazz));
    }

    public Observable<HttpResponse> delete(@NotNull final String uri,
                                           final List<NameValuePair> parameters,
                                           final List<Header> addOrReplaceHeaders) {
        return requestWithHttpResponse(new HttpDelete(uri), null, parameters, addOrReplaceHeaders);
    }

    public <T> Observable<T> patch(@NotNull final String uri,
                                   @Nullable final HttpEntity entity,
                                   final List<NameValuePair> parameters,
                                   final List<Header> addOrReplaceHeaders,
                                   @NotNull final Class<T> clazz) {
        return requestWithHttpResponse(new HttpPatch(uri), entity, parameters, addOrReplaceHeaders)
                .map(resp -> this.convertJsonResponseToObject(resp, clazz));
    }

    public Observable<CloseableHttpResponse> executeReqAndCheckStatus(HttpRequestBase httpRequest,
                                                                      @Nullable HttpEntity entity,
                                                                      List<NameValuePair> parameters,
                                                                      List<Header> addOrReplaceHeaders,
                                                                      int validStatueCode) {
        return request(httpRequest, entity, parameters, addOrReplaceHeaders)
                .doOnNext(resp -> {
                    int statusCode = resp.getStatusLine().getStatusCode();
                    if (statusCode != validStatueCode) {
                        Exceptions.propagate(new UnknownServiceException(
                                String.format("Exceute request with unexpected code %s and resp %s", statusCode, resp)
                        ));
                    }
                });
    }
}
