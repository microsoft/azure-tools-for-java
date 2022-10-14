/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.springcloud.portforwarder;

import com.microsoft.azure.toolkit.ide.common.portforwarder.AbstractPortForwarder;
import com.microsoft.azure.toolkit.ide.common.portforwarder.PortForwarderWebSocketListener;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import okhttp3.WebSocket;
import okio.ByteString;

import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

public class SpringPortForwarderWebSocketListener extends PortForwarderWebSocketListener {
    private int messagesRead = 0;

    public SpringPortForwarderWebSocketListener(ReadableByteChannel in, WritableByteChannel out, AbstractPortForwarder forwarder) {
        super(in, out, forwarder);
    }

    @Override
    protected void writeMessage(WebSocket webSocket, ByteString bytes) {
        ++this.messagesRead;
        final ByteBuffer buffer = bytes.asByteBuffer();
        if (this.messagesRead <= 2) {
            this.request();
            return;
        }
        if (!buffer.hasRemaining()) {
            this.closeWebSocket(webSocket, 1002, "Protocol error");
            throw new AzureToolkitRuntimeException("Received an empty message.");
        }
        final byte channel = buffer.get();
        if (channel < 0 || channel > 1) {
            this.closeWebSocket(webSocket, 1002, "Protocol error");
            throw new AzureToolkitRuntimeException("Received a wrong channel from the remote socket.");
        }
        if (channel == 1) {
            this.closeForwarder();
            throw new AzureToolkitRuntimeException("Received an error from the remote socket.");
        }
        super.writeMessage(webSocket, ByteString.of(buffer));
    }
}
