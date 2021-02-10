/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.common.livy;

import com.microsoft.azuretools.azurecommons.helpers.Nullable;

import java.util.regex.Pattern;

public class MemorySize {
    private static final Pattern memorySizeRegex = Pattern.compile("\\d+(.\\d+)?[gGmM]");

    public enum Unit {
        MEGABYTES("M"),
        GIGABYTES("G");

        private final String present;

        Unit(final String present) {
            this.present = present;
        }


        @Override
        public String toString() {
            return this.present;
        }
    }

    final String value;

    /**
     * Constructor with memory size string with unit.
     *
     * @param memorySize memory size string with unit, such like 1G, 800m or 1.5g
     */
    public MemorySize(final String memorySize) {
        if (!memorySizeRegex.matcher(memorySize).matches()) {
            throw new IllegalArgumentException("Unsupported memory size format: " + memorySize
                    + " , which should be like 1G, 800m or 1.5g");
        }

        this.value = memorySize;
    }

    public MemorySize(final float value, final Unit unit) {
        this.value = ((value == (int) value) ? Integer.toString((int) value) : value) + unit.toString();
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public boolean equals(final @Nullable Object obj) {
        if (!(obj instanceof MemorySize)) {
            return false;
        }

        return value.equals(obj);
    }
}
