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
