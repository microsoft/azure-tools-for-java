package org.jetbrains.plugins.azure.cloudshell.rest

class CloudConsoleProvisionParameters(var properties : Properties?) {
    class Properties (
            val osType : String?
    )
}

