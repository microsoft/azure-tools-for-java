/**
 * Copyright (c) 2019 JetBrains s.r.o.
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

package org.jetbrains.plugins.azure.functions

import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Url

interface GitHubReleasesService {
    companion object {
        fun createInstance(baseUrl: String = "https://api.github.com/"): GitHubReleasesService =
                Retrofit.Builder()
                        .baseUrl(baseUrl)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()
                        .create(GitHubReleasesService::class.java)
    }

    @GET
    @Headers("Accept: application/json")
    fun getReleases(@Url releasesUrl: String): Call<List<GitHubRelease>>
}

class GitHubRelease(val url: String?,
                    @SerializedName("assets_url") val assetsUrl: String?,
                    val name: String?,
                    val prerelease: Boolean,
                    @SerializedName("tag_name") val tagName: String?,
                    val assets: List<GitHubReleaseAsset> = mutableListOf()) {
}

class GitHubReleaseAsset(val url: String?,
                         @SerializedName("browser_download_url") val browserDownloadUrl: String?,
                         val name: String?,
                         val label: String?,
                         @SerializedName("content_type")  val contentType: String?,
                         val size: Long?)