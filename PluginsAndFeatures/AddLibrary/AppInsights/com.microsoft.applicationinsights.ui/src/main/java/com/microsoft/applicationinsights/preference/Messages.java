/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.applicationinsights.preference;

import org.eclipse.osgi.util.NLS;

public final class Messages extends NLS {
    private static final String BUNDLE_NAME = "com.microsoft.applicationinsights.preference.messages";
    public static String resrcName;
    public static String instrKey;
    public static String btnNewLbl;
    public static String btnAddLbl;
    public static String btnDtlsLbl;
    public static String btnRmvLbl;
    public static String name;
    public static String sub;
    public static String resGrp;
    public static String region;
    public static String appTtl;
    public static String unknown;
    public static String addKeyTtl;
    public static String addKeyMsg;
    public static String newKeyTtl;
    public static String newKeyMsg;
    public static String key;
    public static String sameKeyErrMsg;
    public static String sameNameErrMsg;
    public static String rsrcRmvMsg;
    public static String resCreateErrMsg;
    public static String noSubErrMsg;
    public static String noResGrpErrMsg;
    public static String getSubIdErrMsg;
    public static String getValuesErrMsg;
    public static String keyErrMsg;
    public static String importErrMsg;
    public static String loadErrMsg;
    public static String saveErrMsg;
    public static String rsrcUseMsg;
    public static String signInErr;
    public static String timeOutErr;
    public static String callBackErr;
    public static String noAuthErr;
    public static String timeOutErr1;
    public static String err;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
        super();
    }
}
