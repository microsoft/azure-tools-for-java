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
package org.jetbrains.plugins.azure.util

import com.intellij.openapi.diagnostic.Logger
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class PublicIpAddressProvider {
    companion object {
        private const val userAgentHeaderName: String = "User-Agent"
        private const val userAgentHeaderValue: String = "Azure Tookit for JetBrains Rider"

        private val logger = Logger.getInstance(PublicIpAddressProvider::class.java)

        private val plainTextProviderUrls = listOf(
                "http://ipv4bot.whatismyipaddress.com",
                "http://ip4.seeip.org",
                "http://v4.ident.me"
        )

        fun retrieveCurrentPublicIpAddress() : Result {
            val httpClient = OkHttpClient()
                    .newBuilder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build()

            for (plainTextProviderUrl in plainTextProviderUrls.shuffled()) {
                val request = Request.Builder()
                        .url(plainTextProviderUrl)
                        .header(userAgentHeaderName, userAgentHeaderValue)
                        .build()

                try {
                    val response = httpClient.newCall(request).execute()

                    if (response.isSuccessful && response.body() != null) {
                        logger.info("Public IP address retrieved succesfully from $plainTextProviderUrl")

                        val ipAddress = response.body()!!.string()

                        return Result(ipAddress.trim())
                    }
                } catch (e: Exception) {
                    logger.error("Error retrieving public IP address from $plainTextProviderUrl", e)
                }
            }

            logger.warn("Public IP address could not be retrieved from any provider.")

            return Result.none
        }
    }

    class Result(val ipv4address: String) {
        companion object {
            val none = Result("0.0.0.0")
        }
    }
}