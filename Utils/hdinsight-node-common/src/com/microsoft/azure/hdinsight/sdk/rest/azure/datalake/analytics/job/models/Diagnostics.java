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

package com.microsoft.azure.hdinsight.sdk.rest.azure.datalake.analytics.job.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Error diagnostic information for failed jobs.
 */
public class Diagnostics {
    /**
     * the column where the error occured.
     */
    @JsonProperty(value = "columnNumber", access = JsonProperty.Access.WRITE_ONLY)
    private Integer columnNumber;

    /**
     * the ending index of the error.
     */
    @JsonProperty(value = "end", access = JsonProperty.Access.WRITE_ONLY)
    private Integer end;

    /**
     * the line number the error occured on.
     */
    @JsonProperty(value = "lineNumber", access = JsonProperty.Access.WRITE_ONLY)
    private Integer lineNumber;

    /**
     * the error message.
     */
    @JsonProperty(value = "message", access = JsonProperty.Access.WRITE_ONLY)
    private String message;

    /**
     * the severity of the error. Possible values include: 'Warning', 'Error', 'Info', 'SevereWarning', 'Deprecated',
     * 'UserWarning'.
     */
    @JsonProperty(value = "severity", access = JsonProperty.Access.WRITE_ONLY)
    private SeverityTypes severity;

    /**
     * the starting index of the error.
     */
    @JsonProperty(value = "start", access = JsonProperty.Access.WRITE_ONLY)
    private Integer start;

    /**
     * Get the columnNumber value.
     *
     * @return the columnNumber value
     */
    public Integer columnNumber() {
        return this.columnNumber;
    }

    /**
     * Get the end value.
     *
     * @return the end value
     */
    public Integer end() {
        return this.end;
    }

    /**
     * Get the lineNumber value.
     *
     * @return the lineNumber value
     */
    public Integer lineNumber() {
        return this.lineNumber;
    }

    /**
     * Get the message value.
     *
     * @return the message value
     */
    public String message() {
        return this.message;
    }

    /**
     * Get the severity value.
     *
     * @return the severity value
     */
    public SeverityTypes severity() {
        return this.severity;
    }

    /**
     * Get the start value.
     *
     * @return the start value
     */
    public Integer start() {
        return this.start;
    }

}
