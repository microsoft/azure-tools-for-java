/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.core.mvp.ui.rediscache;

import java.util.List;

import redis.clients.jedis.ScanResult;

public class RedisScanResult {

    private List<String> keys;
    private String nextCursor;


    public RedisScanResult(ScanResult<String> result) {
        this.keys = result.getResult();
        this.nextCursor = result.getStringCursor();
    }

    public String getNextCursor() {
        return nextCursor;
    }

    public List<String> getKeys() {
        return keys;
    }
}
