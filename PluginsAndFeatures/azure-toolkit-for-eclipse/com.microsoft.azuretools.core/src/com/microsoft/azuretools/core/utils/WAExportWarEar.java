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

package com.microsoft.azuretools.core.utils;

import java.util.concurrent.ExecutionException;

import org.eclipse.jst.j2ee.application.internal.operations.EARComponentExportDataModelProvider;
import org.eclipse.jst.j2ee.datamodel.properties.IEARComponentExportDataModelProperties;
import org.eclipse.jst.j2ee.internal.web.archive.operations.WebComponentExportDataModelProvider;
import org.eclipse.jst.j2ee.web.datamodel.properties.IWebComponentExportDataModelProperties;
import org.eclipse.wst.common.frameworks.datamodel.DataModelFactory;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;

import com.microsoft.azuretools.core.Activator;


/**
 * This class Export dependent dynamic web project as war or ear.
 */
@SuppressWarnings("restriction")
public class WAExportWarEar {

    private static String errorMessage;

    /**
     * This method export dependent dynamic web project as war
     * with their dependent libraries if any.
     * @param projName
     * @param destPath
     * @throws ExecutionException
     */
    public static void exportWarComponent(String projName, String destPath)
            throws ExecutionException {
        IDataModel warModel = DataModelFactory.
                createDataModel(new WebComponentExportDataModelProvider());
        warModel.setProperty(
                IWebComponentExportDataModelProperties.PROJECT_NAME, projName);
        warModel.setProperty(
                IWebComponentExportDataModelProperties.ARCHIVE_DESTINATION, destPath);
        warModel.setProperty(IWebComponentExportDataModelProperties.
                OVERWRITE_EXISTING, true);
        try {
            warModel.getDefaultOperation().execute(null, null);
        } catch (org.eclipse.core.commands.ExecutionException e) {
            errorMessage = String.format("%s%s%s%s%s",
                    Messages.crtErrMsg, " ", "WAR", " of project: ", projName);
            Activator.getDefault().log(errorMessage, e);
        }
    }

    /**
     * This method export dependent enterprise application project as ear
     * with their dependent libraries if any.
     * @param projName
     * @param destPath
     * @throws ExecutionException
     */
    public static void exportEarComponent(String projName, String destPath)
            throws ExecutionException {
        IDataModel earModel = DataModelFactory.
                createDataModel(new EARComponentExportDataModelProvider());
        earModel.setProperty(
                IEARComponentExportDataModelProperties.PROJECT_NAME, projName);
        earModel.setProperty(
                IEARComponentExportDataModelProperties.ARCHIVE_DESTINATION, destPath);
        earModel.setProperty(IEARComponentExportDataModelProperties.
                OVERWRITE_EXISTING, true);
        try {
            earModel.getDefaultOperation().execute(null, null);
        } catch (org.eclipse.core.commands.ExecutionException e) {
            errorMessage = String.format("%s%s%s%s%s",
                    Messages.crtErrMsg, " ", "EAR", " of project: ", projName);
            Activator.getDefault().log(errorMessage, e);
        }
    }

}
