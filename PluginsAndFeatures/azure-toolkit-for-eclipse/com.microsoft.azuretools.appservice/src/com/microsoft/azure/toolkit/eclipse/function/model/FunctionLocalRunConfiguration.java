/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.eclipse.function.model;

public class FunctionLocalRunConfiguration {
	private String projectName;
	private String functionCliPath;
	private String localSettingsJsonPath;

	public String getProjectName() {
		return projectName;
	}

	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}

	public String getFunctionCliPath() {
		return functionCliPath;
	}

	public void setFunctionCliPath(String functionCliPath) {
		this.functionCliPath = functionCliPath;
	}

	public String getLocalSettingsJsonPath() {
		return localSettingsJsonPath;
	}

	public void setLocalSettingsJsonPath(String localSettingsJsonPath) {
		this.localSettingsJsonPath = localSettingsJsonPath;
	}
}
