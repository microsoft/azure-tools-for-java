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
