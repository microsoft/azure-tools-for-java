/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.core.ui.views;

import org.eclipse.osgi.util.NLS;

public final class Messages extends NLS {
    private static final String BUNDLE_NAME = "com.microsoft.azuretools.core.ui.views.messages"; //$NON-NLS-1$
    public static String desc;
    public static String startTime;
    public static String progress;
    public static String status;
    public static String runStatus;
    public static String runStatusVisible;
    public static String consoleName;
    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
