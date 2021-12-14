/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.eclipse.function.tester;

import com.microsoft.azure.toolkit.eclipse.function.jdt.EclipseFunctionStagingContributor;
import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.*;

public class AzureFunctionTypeTester extends PropertyTester {
    private static final String PROPERTY_IS_FUNCTION = "isFunction";

    @Override
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        if (!(receiver instanceof IAdaptable)) {
            throw new IllegalArgumentException("Element must be of type 'IAdaptable', is " +
                    (receiver == null ? "null" : receiver.getClass().getName()));
        }

        IJavaElement element;
        if (receiver instanceof IJavaElement) {
            element = (IJavaElement) receiver;
        } else if (receiver instanceof IResource) {
            element = JavaCore.create((IResource) receiver);
            if (element == null) {
                return false;
            }
        } else { // is IAdaptable
            element = ((IAdaptable) receiver).getAdapter(IJavaElement.class);
            if (element == null) {
                IResource resource = ((IAdaptable) receiver).getAdapter(IResource.class);
                element = JavaCore.create(resource);
                if (element == null) {
                    return false;
                }
            }
        }
        if (PROPERTY_IS_FUNCTION.equals(property)) {
            return testFunction(element);
        }

        return false;
    }

    private boolean testFunction(IJavaElement element) {
        try {
            IType type = null;
            if (element instanceof ICompilationUnit) {
                type = (((ICompilationUnit) element)).findPrimaryType();
            } else if (element instanceof IOrdinaryClassFile) {
                type = (((IOrdinaryClassFile) element)).getType();
            } else if (element instanceof IType) {
                type = (IType) element;
            } else if (element instanceof IMember) {
                type = ((IMember) element).getDeclaringType();
            }
            if (type != null && type.exists()) {
                return hasAzureFunctionAnnotatedMethods(type);
            }
        } catch (CoreException e) {
            // ignore, return false
        }

        return false;
    }

    private boolean hasAzureFunctionAnnotatedMethods(IType type) throws CoreException {
        if (isTestSourceType(type)) {
            return false;
        }
        return EclipseFunctionStagingContributor.searchFunctionNameAnnotation(type).getResults().stream().anyMatch(t -> t.getElement() instanceof IMethod);
    }

    private boolean isTestSourceType(IType testType) {
        try {
            IClasspathEntry[] resolvedClasspath = testType.getJavaProject().getResolvedClasspath(true);
            final IPath resourcePath = testType.getResource().getFullPath();
            for (IClasspathEntry e : resolvedClasspath) {
                if (e.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
                    if (e.isTest()) {
                        if (e.getPath().isPrefixOf(resourcePath)) {
                            return true;
                        }
                    }
                }
            }
        } catch (JavaModelException e) {
            return false;
        }
        return false;
    }
}
