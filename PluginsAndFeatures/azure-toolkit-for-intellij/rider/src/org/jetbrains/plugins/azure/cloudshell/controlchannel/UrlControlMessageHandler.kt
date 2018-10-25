package org.jetbrains.plugins.azure.cloudshell.controlchannel

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.diagnostic.Logger

class UrlControlMessageHandler(
        private val gson: Gson)
    : ControlMessageHandler {

    companion object {
        private val logger = Logger.getInstance(UrlControlMessageHandler::class.java)
    }

    override fun handle(jsonControlMessage: String) {
        val message = gson.fromJson(jsonControlMessage, UrlControlMessage::class.java)

        if (!message.url.isEmpty()) {
            logger.info("Opening browser for URL: {message.url}")

            // Azure needs some time to get ready to serve the URL, otherwise we will open a 404/500 error page
            Thread.sleep(2000)

            BrowserUtil.browse(message.url)
        }
    }

    private data class UrlControlMessage(
            @SerializedName("audience")
            val audience : String,

            @SerializedName("url")
            val url : String
    )
}