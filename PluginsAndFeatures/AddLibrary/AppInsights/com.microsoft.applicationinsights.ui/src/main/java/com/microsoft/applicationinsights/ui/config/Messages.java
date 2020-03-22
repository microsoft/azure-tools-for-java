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
package com.microsoft.applicationinsights.ui.config;

import org.eclipse.osgi.util.NLS;

public final class Messages extends NLS {
    private static final String BUNDLE_NAME = "com.microsoft.applicationinsights.ui.config.messages";
    public static String OK;
    public static String aiConfigWiz;
    public static String aiTxt;
    public static String aiMsg;
    public static String lblInstrumentationKey;
    public static String propWebProj;
    public static String propErr;
    public static String aiErrTitle;
    public static String aiInstrumentationKeyNull;
    public static String azureSDKcontainerID;
    public static String aiCheckBoxTxt;
    public static String aiXMLPath;
    public static String aiConfFileName;
    public static String aiConfRelDirLoc;
    public static String aiConfResFileLoc;
    public static String aiConfigError;
    public static String aiParseError;
    public static String lnkInstrumentationKey;
    public static String lnkAIPrivacy;
    public static String aiConfigRemoveError;
    public static String containerID;
    public static String containerDesc;
    public static String desc;
    public static String excp;
    public static String lblLocation;
    public static String lblVersion;
    public static String libNotAvail;
    public static String notFound;
    public static String sdkContainer;
    public static String sdkID;
    public static String title;
    public static String verNotAvail;
    public static String version1;
    public static String version2;
    public static String depLocation;
    public static String depChkBox;
    public static String jstDep;
    public static String edtLbrTtl;
    public static String src;
    public static String appInsightsLibGrpTxt;
    public static String aiInstrumentationKeyLbl;
    public static String aiWebFilterCheck;
    public static String aiInstrumentationKeyTxtNull;
    public static String depDirLoc;
    public static String resFileLoc;
    public static String depFileName;
    public static String webxmlPath;
    public static String depDescTtl;
    public static String depDescMsg;
    public static String aiErrMsg;
    public static String fileErrMsg;
    public static String saveErrMsg;
    public static String aiParseErrMsg;
    public static String exprConst;
    public static String exprFltMapping;
    public static String filterTag;
    public static String filterMapTag;
    public static String urlPatternTag;
    public static String filterEle;
    public static String aiWebfilter;
    public static String aiWebFilterClassName;
    public static String filterJarLocation;
    public static String aiRemoveErr;
    public static String webxmlPathMaven;
    public static String aiXMLPathMaven;
    public static String natMaven;
    public static String depDirLocMaven;
    public static String aiConfRelDirLocMaven;
    public static String resChangeErr;
    public static String refreshJobName;
    public static String aiID;
    public static String genKeyListErr;
    public static String startUpErr;
    public static String dlgImgPath;
    public static String sdkJar;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
        super();
    }
}
