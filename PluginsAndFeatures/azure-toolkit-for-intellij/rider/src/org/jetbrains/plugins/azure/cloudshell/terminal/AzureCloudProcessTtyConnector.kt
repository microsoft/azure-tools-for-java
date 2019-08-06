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
package org.jetbrains.plugins.azure.cloudshell.terminal

import com.intellij.openapi.vfs.VirtualFile
import com.jediterm.terminal.ProcessTtyConnector
import org.jetbrains.plugins.terminal.cloud.CloudTerminalProcess
import java.io.IOException
import java.nio.charset.Charset

abstract class AzureCloudProcessTtyConnector(process: CloudTerminalProcess)
    : ProcessTtyConnector(process, Charset.defaultCharset()) {

    abstract fun uploadFile(fileName: String, file: VirtualFile)

    val openPreviewPorts = mutableListOf<Int>()

    abstract fun openPreviewPort(port: Int, openInBrowser: Boolean)

    abstract fun closePreviewPort(port: Int)

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