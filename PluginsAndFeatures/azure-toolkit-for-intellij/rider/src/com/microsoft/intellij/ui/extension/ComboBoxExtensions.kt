/**
 * Copyright (c) 2018-2019 JetBrains s.r.o.
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.intellij.ui.extension

import com.intellij.ui.ListCellRendererWrapper
import javax.swing.Icon
import javax.swing.JComboBox
import javax.swing.JList

fun <T>JComboBox<T>.getSelectedValue(): T? {
    val index = this.selectedIndex
    if (index == -1) return null
    return getItemAt(index)
}

fun <T>JComboBox<T>.getAllItems() = List(itemCount) { index -> getItemAt(index) }

fun <T>JComboBox<T>.fillComboBox(elements: List<T>, defaultElement: T? = null) {
    fillComboBox(
            elements = elements,
            defaultComparator = { element: T -> element != null && defaultElement != null && element == defaultElement }
    )
}

fun <T>JComboBox<T>.fillComboBox(elements: List<T>, defaultComparator: (T) -> Boolean) {
    try {
        removeAllItems()
    } catch(e: Throwable) {
        // ExpandableStringEnum<T> equals throw NPE when comparing with null
        // TODO: make a PR https://github.com/Azure/azure-sdk-for-java/
    }

    elements.forEach {
        addItem(it)
        if (defaultComparator(it))
            this.selectedItem = it
    }
}

fun <T>JComboBox<T>.setDefaultRenderer(errorMessage: String,
                                       icon: Icon? = null,
                                       getValueString: (T) -> String) {

    this.renderer = object : ListCellRendererWrapper<T>() {
        override fun customize(list: JList<*>, value: T?, index: Int, isSelected: Boolean, cellHasFocus: Boolean) {
            if (value == null) {
                setText(errorMessage)
                return
            }
            setText(getValueString(value))

            if (icon != null) setIcon(icon)
        }
    }
}
