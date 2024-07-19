/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.cloudshell.rest

import kotlinx.serialization.Serializable

@Serializable
data class CloudConsoleUserSettings(
    val properties: CloudConsoleUserSettingProperties?
)

@Serializable
data class CloudConsoleUserSettingProperties(
    val preferredOsType: String?,
    val preferredLocation: String?,
    val storageProfile: CloudConsoleUserSettingStorageProfile?,
    val terminalSettings: CloudConsoleUserSettingTerminalSettings?
)

@Serializable
data class CloudConsoleUserSettingStorageProfile(
    val storageAccountResourceId: String?,
    val fileShareName: String?,
    val diskSizeInGB: Int?
)

@Serializable
data class CloudConsoleUserSettingTerminalSettings(
    val fontSize: String?,
    val fontStyle: String?
)