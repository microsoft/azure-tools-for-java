/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.core.utils;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IProject;

import com.microsoft.azuretools.core.Activator;

public class ProjectTypeTester extends PropertyTester {
    @Override
    public boolean test(Object object, String property, Object[] args, Object value) {
        boolean retVal = false;
        try {
            if (property.equalsIgnoreCase(Messages.propSpProj) && object instanceof IProject) {
                IProject project = (IProject)object;
                retVal = WAPropertyTester.isWebProj(project) || MavenUtils.isMavenProject(project);
            }
        } catch (Exception ex) {
            Activator.getDefault().log(Messages.propErr, ex);
        }
        return retVal;
    }

}
