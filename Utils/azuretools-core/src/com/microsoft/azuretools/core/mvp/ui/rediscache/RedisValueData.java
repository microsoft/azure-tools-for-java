/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.core.mvp.ui.rediscache;

import java.util.ArrayList;

import com.microsoft.azuretools.azurecommons.helpers.RedisKeyType;

public class RedisValueData {

    private ArrayList<String[]> rowData;
    private RedisKeyType keyType;

    /**
     * Constructor for RedisValueData class.
     *
     * @param columnName
     *            the column name array for the table widget
     * @param rowData
     *            the data for each table row
     * @param keyType
     *            the Redis Cache's key type
     */
    public RedisValueData(ArrayList<String[]> rowData, RedisKeyType keyType) {
        this.rowData = rowData;
        this.keyType = keyType;
    }

    public ArrayList<String[]> getRowData() {
        return rowData;
    }

    public RedisKeyType getKeyType() {
        return keyType;
    }
}
