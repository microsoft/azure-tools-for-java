/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.core.storageregistry;
import org.eclipse.osgi.util.NLS;

public final class Messages extends NLS {
    private static final String BUNDLE_NAME =
            "com.microsoft.azuretools.core.storageregistry.messages";

    public static String http;
    public static String https;

    static {
            // initialize resource bundle
            NLS.initializeMessages(BUNDLE_NAME, Messages.class);
        }

        private Messages() {
            super();
        }
}
