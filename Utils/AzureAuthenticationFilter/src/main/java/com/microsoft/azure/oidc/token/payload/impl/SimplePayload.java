/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.oidc.token.payload.impl;

import com.microsoft.azure.oidc.exception.PreconditionException;
import com.microsoft.azure.oidc.token.payload.Payload;

final class SimplePayload implements Payload {
    private final String header;
    private final String body;

    public SimplePayload(final String header, final String body) {
        if (header == null || body == null) {
            throw new PreconditionException("Required parameter is null");
        }
        this.header = header;
        this.body = body;
    }

    @Override
    public String getValue() {
        final StringBuffer buffer = new StringBuffer();
        buffer.append(header);
        buffer.append(".");
        buffer.append(body);
        return buffer.toString();
    }

    @Override
    public String toString() {
        final StringBuffer buffer = new StringBuffer();
        buffer.append(header);
        buffer.append(".");
        buffer.append(body);
        return buffer.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((body == null) ? 0 : body.hashCode());
        result = prime * result + ((header == null) ? 0 : header.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SimplePayload other = (SimplePayload) obj;
        if (body == null) {
            if (other.body != null)
                return false;
        } else if (!body.equals(other.body))
            return false;
        if (header == null) {
            if (other.header != null)
                return false;
        } else if (!header.equals(other.header))
            return false;
        return true;
    }
}
