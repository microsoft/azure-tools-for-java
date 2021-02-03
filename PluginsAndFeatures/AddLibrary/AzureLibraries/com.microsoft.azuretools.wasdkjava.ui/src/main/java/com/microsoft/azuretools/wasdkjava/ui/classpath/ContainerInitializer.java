/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.wasdkjava.ui.classpath;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

/**
 * Container Initializer class.
 *
 */
public class ContainerInitializer extends ClasspathContainerInitializer {

    @Override
    public void initialize(IPath path, IJavaProject project) throws CoreException {
        ClasspathContainer container = new ClasspathContainer(path);

        JavaCore.setClasspathContainer(path, new IJavaProject[] {project}, new IClasspathContainer[] {container},
                null);
    }

}
