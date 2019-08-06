/**
 * Copyright (c) 2018 JetBrains s.r.o.
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
package org.jetbrains.plugins.azure.cloudshell.rest

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface CloudConsoleService {
    @GET("providers/Microsoft.Portal/userSettings/cloudconsole?api-version=2017-12-01-preview")
    @Headers("Accept: application/json")
    fun userSettings(): Call<CloudConsoleUserSettings>

    @PUT("providers/Microsoft.Portal/consoles/default?api-version=2017-12-01-preview")
    @Headers("Accept: application/json")
    fun provision(@Body cloudConsoleProvisionParameters: CloudConsoleProvisionParameters): Call<CloudConsoleProvisionResult>

    @POST
    @Headers("Accept: application/json")
    fun provisionTerminal(@Url shellUrl: String, @Query("cols") columns: Int, @Query("rows") rows: Int, @Body cloudConsoleProvisionTerminalParameters: CloudConsoleProvisionTerminalParameters): Call<CloudConsoleProvisionTerminalResult>

    @POST
    @Headers(*arrayOf("Accept: application/json", "Content-type: application/json"))
    fun resizeTerminal(@Url shellUrl: String, @Query("cols") columns: Int, @Query("rows") rows: Int, @Query("version") version: String = "2018-06-01"): Call<Void>

    @POST
    @Headers(*arrayOf("Accept: application/json", "Content-type: application/json"))
    fun openPreviewPort(@Url portUrl: String): Call<PreviewPortResult>

    @POST
    @Headers(*arrayOf("Accept: application/json", "Content-type: application/json"))
    fun closePreviewPort(@Url portUrl: String): Call<PreviewPortResult>

    @POST
    @Multipart
    fun uploadFileToTerminal(@Url shellUrl: String, @Part file: MultipartBody.Part, @Query("version") version: String = "2018-06-01"): Call<Void>

    @GET
    @Streaming
    fun downloadFileFromTerminal(@Url fileUrl: String): Call<ResponseBody>
}
