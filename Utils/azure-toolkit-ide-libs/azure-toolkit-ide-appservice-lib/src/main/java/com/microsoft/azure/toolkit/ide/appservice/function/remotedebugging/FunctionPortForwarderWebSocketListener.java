/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.appservice.function.remotedebugging;

import com.microsoft.azure.toolkit.ide.common.portforwarder.AbstractPortForwarder;
import com.microsoft.azure.toolkit.ide.common.portforwarder.PortForwarderWebSocketListener;
import okhttp3.WebSocket;
import okio.ByteString;
import org.apache.commons.lang3.ArrayUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.function.BooleanSupplier;

public class FunctionPortForwarderWebSocketListener extends PortForwarderWebSocketListener {
    public FunctionPortForwarderWebSocketListener(ReadableByteChannel in, WritableByteChannel out, AbstractPortForwarder forwarder) {
        super(in, out, forwarder);
    }

    protected void pipe(ReadableByteChannel in, WebSocket webSocket, BooleanSupplier isAlive) throws IOException, InterruptedException {
        final ByteBuffer buffer = ByteBuffer.allocate(4096);
        int read;
        do {
            buffer.clear();
            buffer.put((byte) 0);
            read = in.read(buffer);
            if (read > 0) {
                buffer.flip();
                final byte[] array = buffer.array();
                final byte[] fixedArray = ArrayUtils.subarray(array, array[0] == 0 ? 1 : 0, buffer.limit());
                webSocket.send(ByteString.of(fixedArray));
            } else if (read == 0) {
                Thread.sleep(50L);
            }
        } while (isAlive.getAsBoolean() && read >= 0);
    }
}
