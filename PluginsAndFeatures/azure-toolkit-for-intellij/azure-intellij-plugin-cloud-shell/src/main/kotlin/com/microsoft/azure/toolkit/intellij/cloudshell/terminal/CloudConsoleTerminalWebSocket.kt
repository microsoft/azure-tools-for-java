/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.cloudshell.terminal

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.util.io.toByteArray
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.net.URI
import java.nio.ByteBuffer

class CloudConsoleTerminalWebSocket(serverURI: URI): WebSocketClient(serverURI) {
    companion object {
        private val LOG = logger<CloudConsoleTerminalWebSocket>()
    }

    private val socketReceiver = PipedOutputStream()
    val inputStream = PipedInputStream()

    private val socketSender = PipedInputStream()
    val outputStream = MyPipedOutputStream(this)

    override fun onOpen(handshakedata: ServerHandshake?) {
        socketReceiver.connect(inputStream)
        outputStream.connect(socketSender)
    }

    override fun onMessage(message: String?) {
        if (message != null) {
            socketReceiver.write(message.toByteArray())
            socketReceiver.flush()
        }
    }

    override fun onError(ex: Exception?) {
        LOG.warn("Exception in the cloud console WebSocket", ex)
    }

    override fun onMessage(bytes: ByteBuffer?) {
        if (bytes != null) {
            socketReceiver.write(bytes.toByteArray())
            socketReceiver.flush()
        }
    }

    override fun onClose(code: Int, reason: String?, remote: Boolean) {
        if (remote) {
            socketReceiver.write("\r\nConnection terminated by remote host. ($code)\r\n".toByteArray())
            if (!reason.isNullOrBlank()) {
                socketReceiver.write("Reason: $reason".toByteArray())
            }
            socketReceiver.flush()
        }

        ApplicationManager.getApplication().invokeLater {
            inputStream.close()
            socketReceiver.close()

            outputStream.close()
            socketSender.close()
        }
    }

    class MyPipedOutputStream(private val socket: CloudConsoleTerminalWebSocket) : PipedOutputStream() {
        override fun write(b: Int) {
            socket.send(b.toChar().toString())
            super.write(b)
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            socket.send(b.toString(Charsets.UTF_8))
            super.write(b, off, len)
        }
    }
}