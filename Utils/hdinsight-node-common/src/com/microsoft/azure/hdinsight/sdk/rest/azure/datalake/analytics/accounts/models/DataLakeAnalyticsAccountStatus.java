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

package com.microsoft.azure.hdinsight.sdk.rest.azure.datalake.analytics.accounts.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Defines values for DataLakeAnalyticsAccountStatus.
 */
public enum DataLakeAnalyticsAccountStatus {
    /** Enum value Failed. */
    FAILED("Failed"),

    /** Enum value Creating. */
    CREATING("Creating"),

    /** Enum value Running. */
    RUNNING("Running"),

    /** Enum value Succeeded. */
    SUCCEEDED("Succeeded"),

    /** Enum value Patching. */
    PATCHING("Patching"),

    /** Enum value Suspending. */
    SUSPENDING("Suspending"),

    /** Enum value Resuming. */
    RESUMING("Resuming"),

    /** Enum value Deleting. */
    DELETING("Deleting"),

    /** Enum value Deleted. */
    DELETED("Deleted"),

    /** Enum value Undeleting. */
    UNDELETING("Undeleting"),

    /** Enum value Canceled. */
    CANCELED("Canceled");

    /** The actual serialized value for a DataLakeAnalyticsAccountStatus instance. */
    private String value;

    DataLakeAnalyticsAccountStatus(String value) {
        this.value = value;
    }

    /**
     * Parses a serialized value to a DataLakeAnalyticsAccountStatus instance.
     *
     * @param value the serialized value to parse.
     * @return the parsed DataLakeAnalyticsAccountStatus object, or null if unable to parse.
     */
    @JsonCreator
    public static DataLakeAnalyticsAccountStatus fromString(String value) {
        DataLakeAnalyticsAccountStatus[] items = DataLakeAnalyticsAccountStatus.values();
        for (DataLakeAnalyticsAccountStatus item : items) {
            if (item.toString().equalsIgnoreCase(value)) {
                return item;
            }
        }
        return null;
    }

    @JsonValue
    @Override
    public String toString() {
        return this.value;
    }
}
