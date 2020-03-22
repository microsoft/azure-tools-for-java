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

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;

import com.microsoft.azuretools.core.Activator;


public class WAPropertyTester extends PropertyTester {
    public enum ProjExportType {
        WAR, EAR, JAR
    };

    @Override
    public boolean test(Object object, String property, Object[] args, Object value) {
        boolean retVal = false;
        try {
            if (property.equalsIgnoreCase(Messages.propWebProj) && object instanceof IProject) {
                retVal = isWebProj(object);
            }
        } catch (Exception ex) {
            // As this is not an user initiated method,
            // only logging the exception and not showing an error dialog.
            Activator.getDefault().log(Messages.propErr, ex);
        }
        return retVal;
    }

    /**
     * Determines whether a project is a dynamic web project or not.
     *
     * @param object
     *            : variable from the calling test method.
     * @return true if the project is a dynamic web project else false.
     * @throws CoreException
     * @throws WindowsAzureInvalidProjectOperationException
     */
    public static boolean isWebProj(Object object) throws CoreException {
        boolean retVal = false;
        IProject project = (IProject) object;
        if (project.isOpen()) {
            ProjExportType type = getProjectNature(project);
            if (type != null && type.equals(ProjExportType.WAR)) {
                retVal = true;
            }
        }
        return retVal;
    }

    /**
     * Method returns nature of project.
     *
     * @param proj
     * @return
     */
    public static ProjExportType getProjectNature(IProject proj) {
        ProjExportType type = null;
        try {
            if (proj.hasNature(Messages.natJavaEMF)
                    && proj.hasNature(Messages.natMdCore)
                    && proj.hasNature(Messages.natFctCore)
                    && proj.hasNature(Messages.natJava)
                    && proj.hasNature(Messages.natJs)) {
                type = ProjExportType.WAR;
            } else if (proj.hasNature(Messages.natFctCore)
                    && proj.hasNature(Messages.natMdCore)) {
                if (proj.hasNature(Messages.natJs)
                        || proj.hasNature(Messages.natJava)
                        || proj.hasNature(Messages.natJavaEMF)) {

                    type = ProjExportType.JAR;
                } else {
                    type = ProjExportType.EAR;
                }
            } else {
                type = ProjExportType.JAR;
            }
        } catch (CoreException e) {
            Activator.getDefault().log(e.getMessage(), e);
        }
        return type;
    }

}