/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.core.applicationinsights;

import org.eclipse.osgi.util.NLS;

public final class Messages extends NLS {
    private static final String BUNDLE_NAME = "com.microsoft.azuretools.core.applicationinsights.messages";
    public static String natJavaEMF;
    public static String natMdCore;
    public static String natFctCore;
    public static String natJava;
    public static String natJs;
    public static String natMaven;
    public static String unknown;
    public static String aiXMLPath;
    public static String aiXMLPathMaven;
    public static String aiParseErrMsg;
    public static String fileErrMsg;
    public static String exprConst;
    public static String filterMapTag;
    public static String filterEle;
    public static String aiWebfilter;
    public static String urlPatternTag;
    public static String filterTag;
    public static String aiWebFilterClassName;
    public static String exprFltMapping;
    public static String aiRemoveErr;
    public static String saveErrMsg;
    public static String aiListErr;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
        super();
    }
}
