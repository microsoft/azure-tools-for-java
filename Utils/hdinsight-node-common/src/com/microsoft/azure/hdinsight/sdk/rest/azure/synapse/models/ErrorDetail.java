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

package com.microsoft.azure.hdinsight.sdk.rest.azure.synapse.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Error details.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ErrorDetail {
    /**
     * Error message.
     */
    @JsonProperty(value = "message")
    private String message;

    /**
     * Error code.
     */
    @JsonProperty(value = "code")
    private String code;

    /**
     * Error target.
     */
    @JsonProperty(value = "target")
    private String target;

    /**
     * Get error message.
     *
     * @return the message value
     */
    public String message() {
        return this.message;
    }

    /**
     * Set error message.
     *
     * @param message the message value to set
     * @return the ErrorDetail object itself.
     */
    public ErrorDetail withMessage(String message) {
        this.message = message;
        return this;
    }

    /**
     * Get error code.
     *
     * @return the code value
     */
    public String code() {
        return this.code;
    }

    /**
     * Set error code.
     *
     * @param code the code value to set
     * @return the ErrorDetail object itself.
     */
    public ErrorDetail withCode(String code) {
        this.code = code;
        return this;
    }

    /**
     * Get error target.
     *
     * @return the target value
     */
    public String target() {
        return this.target;
    }

    /**
     * Set error target.
     *
     * @param target the target value to set
     * @return the ErrorDetail object itself.
     */
    public ErrorDetail withTarget(String target) {
        this.target = target;
        return this;
    }

}
