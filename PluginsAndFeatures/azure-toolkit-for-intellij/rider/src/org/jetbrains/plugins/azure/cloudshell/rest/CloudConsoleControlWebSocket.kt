package org.jetbrains.plugins.azure.cloudshell.rest

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.annotations.SerializedName
import com.intellij.ide.BrowserUtil
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI

class CloudConsoleControlWebSocket(serverURI: URI, private val baseUrl: String)
    : WebSocketClient(serverURI) {

    companion object {
        protected val gson = Gson()
    }

    override fun onOpen(handshakedata: ServerHandshake?) { }

    override fun onMessage(message: String?) {
        if (message != null) {
            try {
                val controlMessage = gson.fromJson(message, ControlMessage::class.java)
                if (controlMessage.audience == "download") {
                    handleDownload(gson.fromJson(message, DownloadControlMessage::class.java))
                }
            } catch (e: JsonSyntaxException) {
                // TODO
            }
        }
    }

    private fun handleDownload(message: DownloadControlMessage?) {
        if (message == null) return

        BrowserUtil.browse(baseUrl + message.fileUri)
    }

    override fun onError(ex: Exception?) { }

    override fun onClose(code: Int, reason: String?, remote: Boolean) { }

    data class ControlMessage(
            @SerializedName("audience")
            val audience : String
    )

    data class DownloadControlMessage(
            @SerializedName("audience")
            val audience : String,

            @SerializedName("fileUri")
            val fileUri : String
    )
}