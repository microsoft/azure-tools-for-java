/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.applicationinsights.util;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IProject;

import com.microsoft.applicationinsights.ui.activator.Activator;
import com.microsoft.applicationinsights.ui.config.Messages;
import com.microsoft.azuretools.core.applicationinsights.WebPropertyTester;

public class WAPropertyTester extends PropertyTester {

    @Override
    public boolean test(Object object, String property, Object[] args, Object value) {
        boolean retVal = false;
        try {
            if (property.equalsIgnoreCase(Messages.propWebProj) && object instanceof IProject) {
                retVal = WebPropertyTester.isWebProj(object);
            }
        } catch (Exception ex) {
            // As this is not an user initiated method,
            // only logging the exception and not showing an error dialog.
            Activator.getDefault().log(Messages.propErr, ex);
        }
        return retVal;
    }
}
