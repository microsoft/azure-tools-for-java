package org.jetbrains.plugins.azure.cloudshell.rest

class CloudConsoleUserSettings(val properties : Properties?) {
    class Properties (
            val preferredOsType : String?,
            val preferredLocation : String?,
            val storageProfile : StorageProfile?,
            val terminalSettings : TerminalSettings?
    )

    class StorageProfile (
            val storageAccountResourceId : String?,
            val fileShareName : String?,
            val diskSizeInGB : Int?
    )

    class TerminalSettings (
            val fontSize : String?,
            val fontStyle : String?
    )
}