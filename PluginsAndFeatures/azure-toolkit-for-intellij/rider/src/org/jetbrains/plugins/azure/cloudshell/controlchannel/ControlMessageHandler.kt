package org.jetbrains.plugins.azure.cloudshell.controlchannel

interface ControlMessageHandler {
    fun handle(jsonControlMessage: String)
}