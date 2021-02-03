/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.util;

import java.io.File;

/**
 * This Class is a utility class for handling application parameters
 * while adding a component.
 */
public class AppCmpntParam {
    private String impSrc;
    private String impAs;
    private String impMethod;
    private static final String BASE_PATH = "${basedir}" + File.separator + "..";

    /**
     * Method returns import source of application.
     *
     * @return
     */
    public String getImpSrc() {
        return impSrc;
    }

    /**
     * Method sets parameterized value of
     * import source to application.
     *
     * @param impSrc
     */
    public void setImpSrc(String impSrc) {
        String pathLoc = impSrc;
//        IWorkspace workspace = ResourcesPlugin.getWorkspace();
//        IWorkspaceRoot root = workspace.getRoot();
//        if (pathLoc.contains(root.getLocation().toOSString())) {
//            String wrkSpcPath = root.getLocation().toOSString();
//            String replaceString = pathLoc;
//            String subString = impSrc.substring(pathLoc.indexOf(wrkSpcPath), wrkSpcPath.length());
//            pathLoc = replaceString.replace(subString, BASE_PATH);
//            this.impSrc = pathLoc;
//        } else {
        this.impSrc = impSrc;
//        }
    }

    /**
     * Method returns import as of application.
     *
     * @return
     */
    public String getImpAs() {
        return impAs;
    }

    /**
     * Method sets parameterized value of
     * import as to application.
     *
     * @param impAs
     */
    public void setImpAs(String impAs) {
        this.impAs = impAs;
    }

    /**
     * Method returns import method of application.
     *
     * @return
     */
    public String getImpMethod() {
        return impMethod;
    }

    /**
     * Method sets parameterized value of
     * import method to application.
     *
     * @param impMethod
     */
    public void setImpMethod(String impMethod) {
        this.impMethod = impMethod;
    }
}
