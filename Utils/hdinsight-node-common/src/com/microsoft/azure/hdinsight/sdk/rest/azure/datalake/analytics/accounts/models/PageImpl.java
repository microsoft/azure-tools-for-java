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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.microsoft.azure.Page;
import java.util.List;

/**
 * An instance of this class defines a page of Azure resources and a link to
 * get the next page of resources, if any.
 *
 * @param <T> type of Azure resource
 */
public class PageImpl<T> implements Page<T> {
    /**
     * The link to the next page.
     */
    @JsonProperty("nextLink")
    private String nextPageLink;

    /**
     * The list of items.
     */
    @JsonProperty("value")
    private List<T> items;

    /**
     * Gets the link to the next page.
     *
     * @return the link to the next page.
     */
    @Override
    public String nextPageLink() {
        return this.nextPageLink;
    }

    /**
     * Gets the list of items.
     *
     * @return the list of items in {@link List}.
     */
    @Override
    public List<T> items() {
        return items;
    }

    /**
     * Sets the link to the next page.
     *
     * @param nextPageLink the link to the next page.
     * @return this Page object itself.
     */
    public PageImpl<T> setNextPageLink(String nextPageLink) {
        this.nextPageLink = nextPageLink;
        return this;
    }

    /**
     * Sets the list of items.
     *
     * @param items the list of items in {@link List}.
     * @return this Page object itself.
     */
    public PageImpl<T> setItems(List<T> items) {
        this.items = items;
        return this;
    }
}
