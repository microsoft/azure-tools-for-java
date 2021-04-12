/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
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
