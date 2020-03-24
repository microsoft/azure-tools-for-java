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
