/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.eclipse.function.launch.local;

import org.eclipse.core.resources.IResource;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.ui.ILaunchShortcut2;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;

public class FunctionLocalRunShortcut implements ILaunchShortcut2 {

    @Override
    public void launch(ISelection arg0, String arg1) {

    }

    @Override
    public void launch(IEditorPart arg0, String arg1) {
    }

    @Override
    public ILaunchConfiguration[] getLaunchConfigurations(ISelection selection) {
        return new ILaunchConfiguration[0];
    }

    @Override
    public ILaunchConfiguration[] getLaunchConfigurations(IEditorPart editorpart) {
        return new ILaunchConfiguration[0];
    }

    @Override
    public IResource getLaunchableResource(ISelection selection) {
        return null;
    }

    @Override
    public IResource getLaunchableResource(IEditorPart editorpart) {
        return null;
    }
}
