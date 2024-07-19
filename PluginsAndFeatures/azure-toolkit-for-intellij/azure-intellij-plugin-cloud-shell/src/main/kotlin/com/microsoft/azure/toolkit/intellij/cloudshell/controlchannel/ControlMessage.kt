/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.cloudshell.controlchannel

import kotlinx.serialization.Serializable

@Serializable
data class ControlMessage(
    val audience: String,
    val url: String?,
    val fileUri : String?
)
