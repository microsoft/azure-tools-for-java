package org.jetbrains.plugins.azure.cloudshell.rest

class CloudConsoleProvisionResult(val properties: Properties?) {
    class Properties(
            val provisioningState: String?,
            val uri: String?
    )
}