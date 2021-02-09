/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.container.testers;

import java.nio.file.Paths;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IProject;

import com.microsoft.azuretools.container.Constant;
import com.microsoft.azuretools.core.utils.PluginUtil;

public class DockerizedTester extends PropertyTester {

    @Override
    public boolean test(Object arg0, String arg1, Object[] arg2, Object arg3) {
        IProject project = PluginUtil.getSelectedProject();
        return project.exists()
                && Paths.get(project.getLocation().toString(), Constant.DOCKERFILE_FOLDER, Constant.DOCKERFILE_NAME)
                        .toFile().exists();
    }
}
