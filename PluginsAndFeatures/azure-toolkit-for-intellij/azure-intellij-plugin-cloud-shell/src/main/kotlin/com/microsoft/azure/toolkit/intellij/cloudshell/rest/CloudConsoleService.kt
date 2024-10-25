/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.cloudshell.rest

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.io.move
import com.microsoft.azure.toolkit.intellij.cloudshell.CloudShellAccessTokenService
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import java.nio.file.Path
import kotlin.io.path.appendBytes
import kotlin.io.path.createTempFile

@Service(Service.Level.PROJECT)
class CloudConsoleService : Disposable {
    companion object {
        fun getInstance(project: Project) = project.service<CloudConsoleService>()
    }

    private val contentDispositionHeaderRegex = "(?<=filename=\").*?(?=\")".toRegex()

    private var tenantId: String? = null
    private val bearerTokenStorage = mutableListOf<BearerTokens>()

    @OptIn(ExperimentalSerializationApi::class)
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                explicitNulls = false
                ignoreUnknownKeys = true
            })
        }
        install(Auth) {
            bearer {
                loadTokens {
                    bearerTokenStorage.lastOrNull()
                }
                refreshTokens {
                    val accessToken = tenantId?.let { CloudShellAccessTokenService.getInstance().getAccessToken(it) }
                    if (accessToken == null) return@refreshTokens null

                    bearerTokenStorage.add(BearerTokens(accessToken, accessToken))
                    bearerTokenStorage.last()
                }
            }
        }
    }

    fun setAccessToken(accessToken: String, tenantId: String) {
        bearerTokenStorage.add(BearerTokens(accessToken, accessToken))
        this.tenantId = tenantId
    }

    suspend fun getUserSettings(
        resourceManagerEndpoint: String,
    ): CloudConsoleUserSettings? {
        val response =
            client.get("${resourceManagerEndpoint}providers/Microsoft.Portal/userSettings/cloudconsole?api-version=2023-02-01-preview")
        if (!response.status.isSuccess()) return null

        return response.body<CloudConsoleUserSettings>()
    }

    suspend fun provision(
        resourceManagerEndpoint: String,
        preferredLocation: String?,
        parameters: CloudConsoleProvisionParameters
    ): CloudConsoleProvisionResult? {
        val response =
            client.put("${resourceManagerEndpoint}providers/Microsoft.Portal/consoles/default?api-version=2023-02-01-preview") {
                headers {
                    preferredLocation?.let { append("x-ms-console-preferred-location", it) }
                }
                contentType(ContentType.Application.Json)
                setBody(parameters)
            }
        if (!response.status.isSuccess()) return null

        return response.body<CloudConsoleProvisionResult>()
    }

    suspend fun provisionTerminal(
        url: String,
        columns: Int,
        rows: Int,
        referrer: String,
        provisionParameters: CloudConsoleProvisionTerminalParameters,
    ): CloudConsoleProvisionTerminalResult? {
        val response = client.post(url) {
            url {
                parameters.append("cols", columns.toString())
                parameters.append("rows", rows.toString())
                parameters.append("shell", "bash")
            }
            headers {
                append(HttpHeaders.Referrer, referrer)
            }
            contentType(ContentType.Application.Json)
            setBody(provisionParameters)
        }
        if (!response.status.isSuccess()) return null

        return response.body<CloudConsoleProvisionTerminalResult>()
    }

    suspend fun resizeTerminal(url: String, columns: Int, rows: Int): Boolean {
        val response = client.post(url) {
            url {
                parameters.append("cols", columns.toString())
                parameters.append("rows", rows.toString())
            }
            contentType(ContentType.Application.Json)
            setBody("{}")
        }

        return response.status.isSuccess()
    }

    suspend fun openPreviewPort(url: String): PreviewPortResult? {
        val response = client.post(url) {
            contentType(ContentType.Application.Json)
            setBody("{}")
        }
        if (!response.status.isSuccess()) return null

        return response.body<PreviewPortResult>()
    }

    suspend fun closePreviewPort(url: String): PreviewPortResult? {
        val response = client.post(url) {
            contentType(ContentType.Application.Json)
            setBody("{}")
        }
        if (!response.status.isSuccess()) return null

        return response.body<PreviewPortResult>()
    }

    suspend fun uploadFileToTerminal(url: String, fileName: String, file: VirtualFile): Boolean {
        val response = client.post(url) {
            setBody(MultiPartFormDataContent(
                formData {
                    append("uploading-file", file.contentsToByteArray(), Headers.build {
                        append(HttpHeaders.ContentType, ContentType.Application.OctetStream)
                        append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                    })
                }
            ))
        }

        return response.status.isSuccess()
    }

    suspend fun downloadFileFromTerminal(url: String): Path {
        val file = createTempFile("cloudshell-")
        var fileNameFromHeaders: String? = null

        client.prepareGet(url).execute { httpResponse ->
            val contentDisposition = httpResponse.headers["Content-Disposition"]
            if (contentDisposition != null) {
                val match = contentDispositionHeaderRegex.find(contentDisposition)
                if (match != null) {
                    fileNameFromHeaders = match.value
                        .replace('/', '-')
                        .replace('\\', '-')
                }
            }

            val channel: ByteReadChannel = httpResponse.body()
            while (!channel.isClosedForRead) {
                val packet = channel.readRemaining(DEFAULT_BUFFER_SIZE.toLong())
                while (!packet.isEmpty) {
                    val bytes = packet.readBytes()
                    file.appendBytes(bytes)
                }
            }
        }

        return fileNameFromHeaders?.let {
            val newPath = file.parent.resolve(it)
            file.move(newPath)
            newPath
        } ?: file
    }

    override fun dispose() = client.close()
}