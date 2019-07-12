/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.intellij.telemetry

import com.intellij.ui.InplaceButton
import com.microsoft.azure.cosmosspark.common.JXHyperLinkWithUri
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitStorageType
import com.microsoft.azuretools.telemetrywrapper.EventType
import com.microsoft.azuretools.telemetrywrapper.EventUtil
import com.microsoft.intellij.ui.components.JsonEnvPropertiesField
import java.awt.Component
import java.awt.Container
import java.awt.event.*
import java.time.Instant
import java.time.format.DateTimeFormatter
import javax.swing.*

fun Container.getAllNamedComponents(): List<Component> {
    // Only if the container name is set will the container be added to result list
    return listOf(this).filter{ !it.name.isNullOrEmpty() } +
            components.filterIsInstance<Container>().flatMap { it.getAllNamedComponents() }
}

fun Container.addTelemetryListener(serviceName: String) {
    getAllNamedComponents().forEach { component ->
        when(component) {
            // WARNING: If class A is sub-class of class B, we should put matcher to class A in front of matcher to class B
            is JXHyperLinkWithUri -> component.addActionListener {
                createLogEvent(serviceName, "click-hyperlink", component.name)
            }
            is JsonEnvPropertiesField -> component.apply {
                button.addActionListener {
                    createLogEvent(serviceName, "click-button", name)
                }
                textField.addFocusListener(object: FocusAdapter() {
                    override fun focusGained(e: FocusEvent?) {
                        createLogEvent(serviceName, "gain-env-text-focus", name)
                        super.focusGained(e)
                    }
                    override fun focusLost(e: FocusEvent?) {
                        val props = mapOf("isEnvSet" to envs.isNotEmpty().toString())
                        createLogEvent(serviceName, "lost-env-text-focus", name, props)
                        super.focusLost(e)
                    }
                })
            }
            is JComboBox<*> -> component.addItemListener { itemEvent ->
                if (itemEvent?.stateChange == ItemEvent.SELECTED) {
                    when (component.name) {
                        "storageTypeComboBox" -> {
                            val props = mapOf("selectedItem" to (itemEvent.item as SparkSubmitStorageType).description)
                            createLogEvent(serviceName, "change-combo-box-selection", component.name, props)
                        }
                        else -> createLogEvent(serviceName, "change-combo-box-selection", component.name)
                    }
                }
            }
            is JTextField -> component.addFocusListener(object: FocusAdapter() {
                override fun focusGained(e: FocusEvent?) {
                    createLogEvent(serviceName, "gain-text-focus", component.name)
                    super.focusGained(e)
                }
                override fun focusLost(e: FocusEvent?) {
                    createLogEvent(serviceName, "lost-text-focus", component.name)
                    super.focusLost(e)
                }
            })
            is JTable -> component.addFocusListener(object: FocusAdapter() {
                override fun focusGained(e: FocusEvent?) {
                    createLogEvent(serviceName, "gain-table-focus", component.name)
                    super.focusGained(e)
                }
                override fun focusLost(e: FocusEvent?) {
                    createLogEvent(serviceName, "lost-table-focus", component.name)
                    super.focusLost(e)
                }
            })
            is JCheckBox -> component.addActionListener {
                val props = mapOf("isSelected" to component.isSelected.toString())
                createLogEvent(serviceName, "check-check-box", component.name, props)
            }
            is InplaceButton -> component.addMouseListener(object: MouseAdapter() {
                override fun mousePressed(e: MouseEvent?) {
                    createLogEvent(serviceName, "click-button", component.name)
                }
            })
            is JRadioButton -> component.addActionListener {
                val props = mapOf("isSelected" to component.isSelected.toString())
                createLogEvent(serviceName, "select-radio-button", component.name, props)
            }
            is JButton -> component.addActionListener {
                createLogEvent(serviceName, "click-button", component.name)
            }
            else -> {}
        }
    }
}

fun createLogEvent(serviceName: String, operationName: String, componentName: String, additionalProps: Map<String, String>? = null) {
    val currentUtcTime = DateTimeFormatter.ISO_INSTANT.format(Instant.now()).toString()
    val props = mutableMapOf("componentName" to componentName, "operationTime" to currentUtcTime).apply {
        if (additionalProps != null) {
            putAll(additionalProps)
        }
    }
    // If you need to debug telemetry feature, please uncomment the following line
    // println("""serviceName: $serviceName    operationName: $operationName   properties: $props""")

    EventUtil.logEvent(EventType.info, serviceName, operationName, props, null)
}