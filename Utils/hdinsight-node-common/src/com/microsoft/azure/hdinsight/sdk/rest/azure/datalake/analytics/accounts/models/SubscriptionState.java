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

import java.util.Collection;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.microsoft.rest.ExpandableStringEnum;

/**
 * Defines values for SubscriptionState.
 */
public final class SubscriptionState extends ExpandableStringEnum<SubscriptionState> {
    /** Static value Registered for SubscriptionState. */
    public static final SubscriptionState REGISTERED = fromString("Registered");

    /** Static value Suspended for SubscriptionState. */
    public static final SubscriptionState SUSPENDED = fromString("Suspended");

    /** Static value Deleted for SubscriptionState. */
    public static final SubscriptionState DELETED = fromString("Deleted");

    /** Static value Unregistered for SubscriptionState. */
    public static final SubscriptionState UNREGISTERED = fromString("Unregistered");

    /** Static value Warned for SubscriptionState. */
    public static final SubscriptionState WARNED = fromString("Warned");

    /**
     * Creates or finds a SubscriptionState from its string representation.
     * @param name a name to look for
     * @return the corresponding SubscriptionState
     */
    @JsonCreator
    public static SubscriptionState fromString(String name) {
        return fromString(name, SubscriptionState.class);
    }

    /**
     * @return known SubscriptionState values
     */
    public static Collection<SubscriptionState> values() {
        return values(SubscriptionState.class);
    }
}
