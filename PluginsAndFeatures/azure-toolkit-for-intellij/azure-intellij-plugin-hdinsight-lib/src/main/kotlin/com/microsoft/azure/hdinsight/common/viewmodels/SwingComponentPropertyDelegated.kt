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

package com.microsoft.azure.hdinsight.common.viewmodels

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.ComponentWithBrowseButton
import com.microsoft.azure.hdinsight.common.logger.ILogger
import com.microsoft.azure.hdinsight.spark.ui.ImmutableComboBoxModel
import javax.swing.JComboBox
import javax.swing.SwingUtilities
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

abstract class SwingComponentPropertyDelegated<T>: ILogger, ReadWriteProperty<Any?, T>  {
    override operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        val application = ApplicationManager.getApplication()

        if (application == null) {
            assert(SwingUtilities.isEventDispatchThread())
        } else {
            application.assertIsDispatchThread()
        }

        setValueInDispatch(thisRef, property, value)
    }

    abstract fun setValueInDispatch(ref: Any?, property: KProperty<*>, v: T)
}

inline fun <T> swingPropertyDelegated(crossinline getter: (property: KProperty<*>) -> T,
                                      crossinline setterInDispatch: (property: KProperty<*>, newValue: T) -> Unit)
        : ReadWriteProperty<Any?, T> = object: SwingComponentPropertyDelegated<T>(){
    override fun getValue(thisRef: Any?, property: KProperty<*>): T = getter(property)

    override fun setValueInDispatch(ref: Any?, property: KProperty<*>, v: T) {
        setterInDispatch(property, v)
    }
}

class ComponentWithBrowseButtonEnabledDelegated(private val componentWithBrowseButton: ComponentWithBrowseButton<*>)
    : SwingComponentPropertyDelegated<Boolean>() {
    override operator fun getValue(thisRef: Any?, property: KProperty<*>): Boolean {
        return componentWithBrowseButton.button.isEnabled
    }

    override fun setValueInDispatch(ref: Any?, property: KProperty<*>, v: Boolean) {
        componentWithBrowseButton.setButtonEnabled(v)
    }
}

class ComboBoxSelectionDelegated<T>(private val comboBox: JComboBox<T>): SwingComponentPropertyDelegated<T>() {
    override operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        @Suppress("UNCHECKED_CAST")
        return comboBox.selectedItem as T
    }

    override fun setValueInDispatch(ref: Any?, property: KProperty<*>, v: T) {
        comboBox.model.selectedItem = v
    }
}

class ImmutableComboBoxModelDelegated<T>(private val comboBox: JComboBox<T>) {
    operator fun getValue(ref: Any?, property: KProperty<*>): ImmutableComboBoxModel<T> {
        return comboBox.model as ImmutableComboBoxModel<T>
    }

    operator fun setValue(ref: Any?, property: KProperty<*>, v: ImmutableComboBoxModel<T>) {
        if (comboBox.model != v) {
            comboBox.model = v
        }
    }
}

