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
package org.jetbrains.plugins.azure.cloudshell.controlchannel

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.annotations.SerializedName
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.jetbrains.plugins.azure.cloudshell.rest.CloudConsoleService
import java.net.URI

class CloudConsoleControlChannelWebSocket(private val project: Project,
                                          serverURI: URI,
                                          private val cloudConsoleService: CloudConsoleService,
                                          private val cloudConsoleBaseUrl: String)
    : WebSocketClient(serverURI) {

    companion object {
        private val logger = Logger.getInstance(CloudConsoleControlChannelWebSocket::class.java)
    }

    private val gson = Gson()

    override fun onOpen(handshakedata: ServerHandshake?) { }

    override fun onMessage(message: String?) {
        if (message != null) {
            try {
                val controlMessage = gson.fromJson(message, ControlMessage::class.java)
                when {
                    controlMessage.audience == "download" ->
                        DownloadControlMessageHandler(gson, project, cloudConsoleService, cloudConsoleBaseUrl)
                            .handle(message)
                    controlMessage.audience == "url" ->
                        UrlControlMessageHandler(gson, project)
                            .handle(message)
                }
            } catch (e: JsonSyntaxException) {
                logger.error("Error on receiving message from cloud shell terminal: $e")
            }
        }
    }

    override fun onError(ex: Exception?) { }

    override fun onClose(code: Int, reason: String?, remote: Boolean) { }

    private data class ControlMessage(
            @SerializedName("audience")
            val audience : String
    )
}