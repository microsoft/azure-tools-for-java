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
package org.jetbrains.plugins.azure.cloudshell.rest

import com.intellij.openapi.application.ApplicationManager
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.net.URI

class CloudConsoleTerminalWebSocket(serverURI: URI)
    : WebSocketClient(serverURI) {

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
        // TODO
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

        override fun write(b: ByteArray?, off: Int, len: Int) {
            if (b != null) {
                socket.send(b.toString(Charsets.UTF_8))
            }
            super.write(b, off, len)
        }
    }
}