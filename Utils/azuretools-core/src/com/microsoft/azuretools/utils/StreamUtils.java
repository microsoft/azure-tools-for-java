/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.utils;

import org.apache.commons.lang3.StringUtils;

import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class StreamUtils {

    public static String toString(OutputStream outputStream, Charset charset) throws UnsupportedEncodingException {
        if (outputStream == null) {
            return StringUtils.EMPTY;
        }
        if (charset != null) {
            return new String(outputStream.toString().getBytes(), charset.name());
        } else {
            return new String(outputStream.toString().getBytes(), StandardCharsets.UTF_8.name());
        }
    }

    public static String toString(OutputStream outputStream) throws UnsupportedEncodingException {
        return StreamUtils.toString(outputStream, StandardCharsets.UTF_8);
    }
}
