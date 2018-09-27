package org.jetbrains.plugins.azure.cloudshell.rest

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
    }

    override fun onClose(code: Int, reason: String?, remote: Boolean) {
        inputStream.close()
        socketReceiver.close()

        outputStream.close()
        socketSender.close()
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