/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.cloudshell.controlchannel

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.platform.ide.progress.withBackgroundProgress
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.milliseconds

@Service(Service.Level.PROJECT)
class UrlControlMessageHandler(
    private val project: Project,
    private val scope: CoroutineScope
) : Disposable {
    companion object {
        fun getInstance(project: Project): UrlControlMessageHandler = project.service()
        private val LOG = logger<UrlControlMessageHandler>()
    }

    private val client = HttpClient(CIO)

    fun handle(message: ControlMessage) = scope.launch(Dispatchers.Default) {
        if (message.url.isNullOrEmpty()) {
            LOG.warn("URL is empty in the control message")
            return@launch
        }

        withBackgroundProgress(project, "Waiting for cloud shell URL to become available...") {
            for (i in 0..<10) {
                try {
                    val response = client.request(message.url)
                    if (response.status.isSuccess()) {
                        BrowserUtil.browse(message.url)
                        break
                    } else {
                        delay(300.milliseconds)
                    }
                } catch (ce: CancellationException) {
                    throw ce
                } catch (e: Exception) {
                    //do nothing
                }
            }
        }
    }

    override fun dispose() {
        client.close()
    }
}