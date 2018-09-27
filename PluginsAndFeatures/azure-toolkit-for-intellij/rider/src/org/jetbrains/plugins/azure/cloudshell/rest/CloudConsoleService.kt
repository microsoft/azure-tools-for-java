package org.jetbrains.plugins.azure.cloudshell.rest

import retrofit2.Call
import retrofit2.http.*

interface CloudConsoleService {
    @GET("providers/Microsoft.Portal/userSettings/cloudconsole?api-version=2017-12-01-preview")
    fun userSettings(): Call<CloudConsoleUserSettings>

    @PUT("providers/Microsoft.Portal/consoles/default?api-version=2017-12-01-preview")
    fun provision(@Body cloudConsoleProvisionParameters: CloudConsoleProvisionParameters): Call<CloudConsoleProvisionResult>

    @POST
    fun provisionTerminal(@Url shellUrl: String, @Query("cols") columns: Int, @Query("rows") rows: Int, @Body cloudConsoleProvisionTerminalParameters: CloudConsoleProvisionTerminalParameters): Call<CloudConsoleProvisionTerminalResult>
}
