package org.jetbrains.plugins.azure.cloudshell.controlchannel

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.annotations.SerializedName
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

    private val gson = Gson()

    override fun onOpen(handshakedata: ServerHandshake?) { }

    override fun onMessage(message: String?) {
        if (message != null) {
            try {
                val controlMessage = gson.fromJson(message, ControlMessage::class.java)
                if (controlMessage.audience == "download") {
                    DownloadControlMessageHandler(gson, project, cloudConsoleService, cloudConsoleBaseUrl)
                            .handle(message)
                }
            } catch (e: JsonSyntaxException) {
                // TODO
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