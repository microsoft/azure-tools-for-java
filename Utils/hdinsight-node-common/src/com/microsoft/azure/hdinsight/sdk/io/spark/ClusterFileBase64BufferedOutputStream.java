/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.io.spark;

import com.microsoft.azure.hdinsight.sdk.common.livy.interactive.Session;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import org.apache.commons.codec.binary.Base64;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.ByteBuffer;

public class ClusterFileBase64BufferedOutputStream extends OutputStream {
    private static final int DEFAULT_BLOCK_SIZE_KB = 32;      // 32KB block size

    @NotNull
    private final Session session;

    @NotNull
    private final ByteBuffer buf;

    private final String preloadedCodes = String.join("\n",
            "import java.io._",
            "import java.util.Base64",
            "",
            "val jarOutput = \"%s\"",
            "val fs = org.apache.hadoop.fs.FileSystem.get(sc.hadoopConfiguration)",
            "val jarFileOutput = fs.create(new org.apache.hadoop.fs.Path(jarOutput), true)",
            "val out = new DataOutputStream(new BufferedOutputStream(jarFileOutput))",
            "",
            "def writePage(encodedBase64: String) = {",
            "    val pageBytes = Base64.getDecoder.decode(encodedBase64)",
            "",
            "    out.write(pageBytes, 0, pageBytes.size)",
            "}");


    public ClusterFileBase64BufferedOutputStream(@NotNull Session session, @NotNull URI destination, final int blockSizeKB) {
        this.session = session;
        this.buf = ByteBuffer.allocate(blockSizeKB * 1024); // Due to BASE64 requirement, the block size
                                                            // must be aligned to 4 bytes

        // Pre-load
        session.runCodes(String.format(preloadedCodes, destination.toString()))
                .toBlocking()
                .singleOrDefault(null);
    }

    public ClusterFileBase64BufferedOutputStream(@NotNull Session session, @NotNull URI destination) {
        this(session, destination, DEFAULT_BLOCK_SIZE_KB);
    }

    @Override
    public void close() throws IOException {
        flush();

        session.runCodes("out.close()")
                .toBlocking()
                .singleOrDefault(null);

        session.close();
        super.close();
    }

    @Override
    public void write(int b) throws IOException {
        if (!buf.hasRemaining()) {
            flush();
        }

        if (Base64.isBase64((byte) b)) {
            buf.put((byte) b);
        }
    }

    @Override
    public void flush() throws IOException {
        if (buf.position() > 0) {
            String codesPage = new String(buf.array(), 0, buf.position());

            buf.clear();
            session.runCodes(String.format("writePage(\"%s\")", codesPage))
                    .toBlocking()
                    .singleOrDefault(null);
        }

        super.flush();
    }
}
