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

package com.microsoft.azure.hdinsight.sdk.rest.arcadia.models;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The error response.
 */
public class ErrorResponse {
    /**
     * The error code.
     */
    @JsonProperty(value = "code")
    private String code;

    /**
     * The error message.
     */
    @JsonProperty(value = "message")
    private String message;

    /**
     * Property name.
     */
    @JsonProperty(value = "target")
    private String target;

    /**
     * The list of invalid fields sent in request, in case of validation error.
     */
    @JsonProperty(value = "details")
    private List<ErrorFieldContract> details;

    /**
     * Get the error code.
     *
     * @return the code value
     */
    public String code() {
        return this.code;
    }

    /**
     * Set the error code.
     *
     * @param code the code value to set
     * @return the ErrorResponse object itself.
     */
    public ErrorResponse withCode(String code) {
        this.code = code;
        return this;
    }

    /**
     * Get the error message.
     *
     * @return the message value
     */
    public String message() {
        return this.message;
    }

    /**
     * Set the error message.
     *
     * @param message the message value to set
     * @return the ErrorResponse object itself.
     */
    public ErrorResponse withMessage(String message) {
        this.message = message;
        return this;
    }

    /**
     * Get property name.
     *
     * @return the target value
     */
    public String target() {
        return this.target;
    }

    /**
     * Set property name.
     *
     * @param target the target value to set
     * @return the ErrorResponse object itself.
     */
    public ErrorResponse withTarget(String target) {
        this.target = target;
        return this;
    }

    /**
     * Get the list of invalid fields sent in request, in case of validation error.
     *
     * @return the details value
     */
    public List<ErrorFieldContract> details() {
        return this.details;
    }

    /**
     * Set the list of invalid fields sent in request, in case of validation error.
     *
     * @param details the details value to set
     * @return the ErrorResponse object itself.
     */
    public ErrorResponse withDetails(List<ErrorFieldContract> details) {
        this.details = details;
        return this;
    }

}
