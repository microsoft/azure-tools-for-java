package org.jetbrains.plugins.azure.cloudshell.terminal

import com.intellij.openapi.vfs.VirtualFile
import com.jediterm.terminal.ProcessTtyConnector
import org.jetbrains.plugins.terminal.cloud.CloudTerminalProcess
import java.io.IOException
import java.nio.charset.Charset

abstract class AzureCloudProcessTtyConnector(process: CloudTerminalProcess)
    : ProcessTtyConnector(process, Charset.defaultCharset()) {

    abstract fun uploadFile(fileName: String, file: VirtualFile)

    override fun read(buf: CharArray?, offset: Int, length: Int): Int {
        try {
            return super.read(buf, offset, length)
        } catch (e: IOException) {
            if (shouldRethrowIOException(e)) {
                throw e
            }
        }

        return -1
    }

    override fun write(bytes: ByteArray?) {
        try {
            super.write(bytes)
        } catch (e: IOException) {
            if (shouldRethrowIOException(e)) {
                throw e
            }
        }
    }

    override fun write(string: String?) {
        try {
            super.write(string)
        } catch (e: IOException) {
            if (shouldRethrowIOException(e)) {
                throw e
            }
        }
    }

    private fun shouldRethrowIOException(exception: IOException): Boolean =
            exception.message == null || !exception.message!!.contains("pipe closed", true)

    override fun isConnected(): Boolean {
        return true
    }
}