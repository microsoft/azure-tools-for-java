/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.core.ui.commoncontrols;


import org.eclipse.osgi.util.NLS;

public final class Messages extends NLS {
    private static final String BUNDLE_NAME =
            "com.microsoft.azuretools.core.ui.commoncontrols.messages";

    /* New Certificate Dialog messages - start*/
    public static String newCertDlgCertTxt;
    public static String newCertDlgCertMsg;
    public static String newCertDlgPwdLbl;
    public static String newCertDlgCNNameLbl;
    public static String newCertDlgCnfPwdLbl;
    public static String newCertDlgGrpLbl;
    public static String newCertDlgPFXLbl;
    public static String newCertDlgBrwsBtn;
    public static String newCertDlgCertLbl;
    public static String newCertDlgBrwFldr;
    public static String newCertDlgCrtErTtl;
    public static String newCertDlgCNNull;
    public static String newCertDlgPwNul;
    public static String newCertDlgCfPwNul;
    public static String newCertDlgPwdWrng;
    public static String newCertDlgPwNtCor;
    public static String newCertDlgPwNtMtch;
    public static String newCerDlgPwNtMsg;
    public static String newCertDlgCerNul;
    public static String newCertDlgPFXNull;
    public static String newCerDlgInvldPth;
    public static String newCerDlgInvdFlExt;
    public static String newCertDlgAlias;
    public static String newCerDlgCrtCerEr;
    public static String newCertDlgImg;
    public static String imgErr;
    public static String newCertDlgPwLength;
    public static String newCertMsg;
    /* New Certificate Dialog messages - end*/
    public static String impSubDlgTtl;
    public static String dwnlPubSetFile;
    public static String pathLbl;
    public static String mgmtPortalShell;
    /* New resource group dialog messages - end*/
    public static String newResGrpTtl;
    public static String newResGrpMsg;
    public static String name;
    public static String sub;
    public static String location;
    public static String noSubErrMsg;
    public static String newResErrMsg;
    public static String getValuesErrMsg;
    public static String timeOutErr;
    public static String loadingCred;
    public static String error;
    public static String credentialsExist;
    public static String importDlgMsg;
    public static String importDlgTitle;
    public static String failedToParse;
    public static String importDlgMsgJavaVersion;
    public static String loadingAccountError;
    public static String azureSamplesPageLinkMS;
    public static String azureSamplesDlgErMsg;
    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
        super();
    }
}
