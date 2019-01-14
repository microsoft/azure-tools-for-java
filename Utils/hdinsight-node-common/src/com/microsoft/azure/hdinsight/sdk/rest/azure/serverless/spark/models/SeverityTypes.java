/**
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
 *
 */

package com.microsoft.azure.hdinsight.sdk.rest.azure.serverless.spark.models;

import java.util.Collection;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.microsoft.rest.ExpandableStringEnum;

/**
 * Defines values for SeverityTypes.
 */
public final class SeverityTypes extends ExpandableStringEnum<SeverityTypes> {
    /** Static value Warning for SeverityTypes. */
    public static final SeverityTypes WARNING = fromString("Warning");

    /** Static value Error for SeverityTypes. */
    public static final SeverityTypes ERROR = fromString("Error");

    /** Static value Info for SeverityTypes. */
    public static final SeverityTypes INFO = fromString("Info");

    /** Static value SevereWarning for SeverityTypes. */
    public static final SeverityTypes SEVERE_WARNING = fromString("SevereWarning");

    /** Static value Deprecated for SeverityTypes. */
    public static final SeverityTypes DEPRECATED = fromString("Deprecated");

    /** Static value UserWarning for SeverityTypes. */
    public static final SeverityTypes USER_WARNING = fromString("UserWarning");

    /**
     * Creates or finds a SeverityTypes from its string representation.
     * @param name a name to look for
     * @return the corresponding SeverityTypes
     */
    @JsonCreator
    public static SeverityTypes fromString(String name) {
        return fromString(name, SeverityTypes.class);
    }

    /**
     * @return known SeverityTypes values
     */
    public static Collection<SeverityTypes> values() {
        return values(SeverityTypes.class);
    }
}
