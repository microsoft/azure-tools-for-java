/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.eclipse.common;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.jface.resource.ImageDescriptor;

import com.microsoft.applicationinsights.core.dependencies.apachecommons.lang3.StringUtils;
import com.microsoft.azuretools.azureexplorer.Activator;

public class AzureIcons {
	public static final String FILE_EXTENSION_ICON_PREFIX = "file/";

	public static ImageDescriptor getIcon(String input) {
		if (StringUtils.startsWith(input, FILE_EXTENSION_ICON_PREFIX)) {
			final String fileExtension = StringUtils.removeStart(input, FILE_EXTENSION_ICON_PREFIX);
			return ImageDescriptor
					.createFromImageData(org.eclipse.swt.program.Program.findProgram(fileExtension).getImageData());
		}
		final String extension = FilenameUtils.getExtension(input);
		final String iconPath = StringUtils.equals(extension, "svg")
				? FilenameUtils.removeExtension(input) + FilenameUtils.EXTENSION_SEPARATOR + "png"
				: input;
		return Activator.getImageDescriptor(iconPath);
	}
}
