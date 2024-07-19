/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:OptIn(ExperimentalSerializationApi::class)

package com.microsoft.azure.toolkit.intellij.cloudshell.controlchannel

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.serialization.kotlinx.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

class CloudConsoleControlChannelClient(
    private val controlChannelSocketUrl: String,
    private val cloudConsoleBaseUrl: String,
    private val project: Project
) : Disposable {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val client = HttpClient(CIO) {
        install(WebSockets) {
            contentConverter = KotlinxWebsocketSerializationConverter(json)
        }
    }

    fun connect(scope: CoroutineScope) {
        scope.launch {
            client.webSocket(controlChannelSocketUrl) {
                while (true) {
                    when (val message = incoming.receive()) {
                        is Frame.Text -> {
                            val controlMessage = receiveDeserialized<ControlMessage>()
                            handleControlMessage(controlMessage)
                        }

                        is Frame.Binary -> {
                            val stringMessage = message.readBytes().decodeToString()
                            handleControlMessage(json.decodeFromString(stringMessage))
                        }

                        is Frame.Close -> break
                        else -> continue
                    }
                }
            }
        }
    }

    private fun handleControlMessage(message: ControlMessage) {
        when (message.audience) {
            "download" -> {
                DownloadControlMessageHandler.getInstance(project).handle(cloudConsoleBaseUrl, message)
            }

            "url" -> {
                UrlControlMessageHandler.getInstance(project).handle(message)
            }
        }
    }

    override fun dispose() {
        client.close()
    }
}