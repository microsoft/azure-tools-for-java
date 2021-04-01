/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Copyright (c) 2020-2021 JetBrains s.r.o.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package org.jetbrains.icons

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity

internal class RiderIconsPatcherRegistrar : StartupActivity {

    override fun runActivity(project: Project) {
        RiderIconsPatcher.install()
    }
}