/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.oidc.common.timestamp.impl;

import com.microsoft.azure.oidc.common.timestamp.TimeStamp;
import com.microsoft.azure.oidc.exception.PreconditionException;

final class SimpleTimeStamp implements TimeStamp {
    private final Long time;

    public SimpleTimeStamp(final Long time) {
        if(time == null) {
            throw new PreconditionException("Required parameter is null");
        }
        this.time = time;
    }

    @Override
    public Long getTime() {
        return time;
    }

    @Override
    public String toString() {
        return time.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((time == null) ? 0 : time.hashCode());
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
        SimpleTimeStamp other = (SimpleTimeStamp) obj;
        if (time == null) {
            if (other.time != null)
                return false;
        } else if (!time.equals(other.time))
            return false;
        return true;
    }

    @Override
    public int compareTo(TimeStamp o) {
        if(o == null) {
            throw new NullPointerException();
        }
        return this.getTime().compareTo(o.getTime());
    }
}
