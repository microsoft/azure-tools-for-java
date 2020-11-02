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

package com.microsoft.azure.toolkit.lib.appservice.file;

import okhttp3.ResponseBody;
import retrofit2.http.*;
import rx.Observable;

import java.util.List;

public interface KuduFileClient extends AppServiceFileClient {
    @Headers({
                 "Content-Type: application/json; charset=utf-8",
                 "x-ms-logging-context: com.microsoft.azure.management.appservice.WebApps getFile"
             })
    @GET("api/vfs/{path}")
    @Streaming
    Observable<ResponseBody> getFileContent(@Path("path") String path);

    @Headers({
                 "Content-Type: application/json; charset=utf-8",
                 "x-ms-logging-context: com.microsoft.azure.management.appservice.WebApps getFilesInDirectory"
             })
    @GET("api/vfs/{path}/")
    Observable<List<AppServiceFile>> getFilesInDirectory(@Path("path") String path);

    @Headers({
                 "Content-Type: application/json; charset=utf-8",
                 "x-ms-logging-context: com.microsoft.azure.management.appservice.WebApps saveFile"
             })
    @PUT("api/vfs/{path}")
    Observable<ResponseBody> saveFile(@Path("path") String path);

    @Headers({
                 "Content-Type: application/json; charset=utf-8",
                 "x-ms-logging-context: com.microsoft.azure.management.appservice.WebApps createDirectory"
             })
    @PUT("api/vfs/{path}/")
    Observable<ResponseBody> createDirectory(@Path("path") String path);

    @Headers({
                 "Content-Type: application/json; charset=utf-8",
                 "x-ms-logging-context: com.microsoft.azure.management.appservice.WebApps deleteFile"
             })
    @DELETE("api/vfs/{path}")
    Observable<ResponseBody> deleteFile(@Path("path") String path);
}
