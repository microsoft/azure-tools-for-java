/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.tooling.msservices.model.storage;

import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.tooling.msservices.model.ServiceTreeItem;

import java.util.Calendar;
import java.util.Map;
import java.util.UUID;

public class TableEntity implements ServiceTreeItem {
    public enum PropertyType {
        Boolean,
        DateTime,
        Double,
        Uuid,
        Integer,
        Long,
        String,
    }

    public static class Property {
        private Object value;
        private PropertyType type;

        public Property(@NotNull Boolean value) {
            this.value = value;
            this.type = PropertyType.Boolean;
        }

        public Property(@NotNull Calendar value) {
            this.value = value;
            this.type = PropertyType.DateTime;
        }

        public Property(@NotNull Double value) {
            this.value = value;
            this.type = PropertyType.Double;
        }

        public Property(@NotNull UUID value) {
            this.value = value;
            this.type = PropertyType.Uuid;
        }

        public Property(@NotNull Integer value) {
            this.value = value;
            this.type = PropertyType.Integer;
        }

        public Property(@NotNull Long value) {
            this.value = value;
            this.type = PropertyType.Long;
        }

        public Property(@NotNull String value) {
            this.value = value;
            this.type = PropertyType.String;
        }

        @NotNull
        public Boolean getValueAsBoolean() throws AzureCmdException {
            if (type.equals(PropertyType.Boolean) && this.value instanceof Boolean) {
                return (Boolean) this.value;
            } else {
                throw new AzureCmdException("Property value is not of Boolean type", "");
            }
        }

        @NotNull
        public Calendar getValueAsCalendar() throws AzureCmdException {
            if (type.equals(PropertyType.DateTime) && this.value instanceof Calendar) {
                return (Calendar) this.value;
            } else {
                throw new AzureCmdException("Property value is not of Calendar type", "");
            }
        }

        @NotNull
        public Double getValueAsDouble() throws AzureCmdException {
            if (type.equals(PropertyType.Double) && this.value instanceof Double) {
                return (Double) this.value;
            } else {
                throw new AzureCmdException("Property value is not of Double type", "");
            }
        }

        @NotNull
        public UUID getValueAsUuid() throws AzureCmdException {
            if (type.equals(PropertyType.Uuid) && this.value instanceof UUID) {
                return (UUID) this.value;
            } else {
                throw new AzureCmdException("Property value is not of UUID type", "");
            }
        }

        @NotNull
        public Integer getValueAsInteger() throws AzureCmdException {
            if (type.equals(PropertyType.Integer) && this.value instanceof Integer) {
                return (Integer) this.value;
            } else {
                throw new AzureCmdException("Property value is not of Integer type", "");
            }
        }

        @NotNull
        public Long getValueAsLong() throws AzureCmdException {
            if (type.equals(PropertyType.Long) && this.value instanceof Long) {
                return (Long) this.value;
            } else {
                throw new AzureCmdException("Property value is not of Long type", "");
            }
        }

        @NotNull
        public String getValueAsString() throws AzureCmdException {
            if (type.equals(PropertyType.String) && this.value instanceof String) {
                return (String) this.value;
            } else {
                throw new AzureCmdException("Property value is not of String type", "");
            }
        }

        @NotNull
        public PropertyType getType() {
            return type;
        }
    }

    private boolean loading;
    private String partitionKey;
    private String rowKey;
    private String tableName;
    private String eTag;
    private Calendar timestamp;
    private Map<String, Property> properties;

    public TableEntity(@NotNull String partitionKey,
                       @NotNull String rowKey,
                       @NotNull String tableName,
                       @NotNull String eTag,
                       @NotNull Calendar timestamp,
                       @NotNull Map<String, Property> properties) {
        this.partitionKey = partitionKey;
        this.rowKey = rowKey;
        this.tableName = tableName;
        this.eTag = eTag;
        this.timestamp = timestamp;
        this.properties = properties;
    }

    @Override
    public boolean isLoading() {
        return loading;
    }

    @Override
    public void setLoading(boolean loading) {
        this.loading = loading;
    }

    @NotNull
    public String getPartitionKey() {
        return partitionKey;
    }

    @NotNull
    public String getRowKey() {
        return rowKey;
    }

    @NotNull
    public String getTableName() {
        return tableName;
    }

    public void setTableName(@NotNull String tableName) {
        this.tableName = tableName;
    }

    @NotNull
    public String getETag() {
        return eTag;
    }

    public void setETag(@NotNull String eTag) {
        this.eTag = eTag;
    }

    @NotNull
    public Calendar getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(@NotNull Calendar timestamp) {
        this.timestamp = timestamp;
    }

    @NotNull
    public Map<String, Property> getProperties() {
        return properties;
    }

    @Override
    public String toString() {
        return partitionKey + " - " + rowKey + (loading ? " (loading...)" : "");
    }
}
