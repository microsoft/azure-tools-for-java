/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.cloudshell.rest

import kotlinx.serialization.Serializable

@Serializable
data class CloudConsoleProvisionTerminalResult(
    val socketUri: String?
)
