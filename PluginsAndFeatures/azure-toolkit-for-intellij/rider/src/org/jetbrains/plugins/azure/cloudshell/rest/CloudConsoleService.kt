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
    @Multipart
    fun uploadFileToTerminal(@Url shellUrl: String, @Part file: MultipartBody.Part, @Query("version") version: String = "2018-06-01"): Call<Void>

    @GET
    @Streaming
    fun downloadFileFromTerminal(@Url fileUrl: String): Call<ResponseBody>
}
