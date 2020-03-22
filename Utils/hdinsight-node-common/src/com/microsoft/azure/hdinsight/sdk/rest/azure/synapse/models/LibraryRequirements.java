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
 * Spark pool library version requirements.
 * Library requirements for a Big Data pool powered by Apache Spark.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class LibraryRequirements {
    /**
     * The last update time of the library requirements file.
     */
    @JsonProperty(value = "time", access = JsonProperty.Access.WRITE_ONLY)
    private String time;

    /**
     * The library requirements.
     */
    @JsonProperty(value = "content")
    private String content;

    /**
     * The filename of the library requirements file.
     */
    @JsonProperty(value = "filename")
    private String filename;

    /**
     * Get the last update time of the library requirements file.
     *
     * @return the time value
     */
    public String time() {
        return this.time;
    }

    /**
     * Get the library requirements.
     *
     * @return the content value
     */
    public String content() {
        return this.content;
    }

    /**
     * Set the library requirements.
     *
     * @param content the content value to set
     * @return the LibraryRequirements object itself.
     */
    public LibraryRequirements withContent(String content) {
        this.content = content;
        return this;
    }

    /**
     * Get the filename of the library requirements file.
     *
     * @return the filename value
     */
    public String filename() {
        return this.filename;
    }

    /**
     * Set the filename of the library requirements file.
     *
     * @param filename the filename value to set
     * @return the LibraryRequirements object itself.
     */
    public LibraryRequirements withFilename(String filename) {
        this.filename = filename;
        return this;
    }

}
