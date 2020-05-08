/**
 * Copyright (c) 2020 JetBrains s.r.o.
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

import com.intellij.openapi.ui.ComboBox
import com.jetbrains.rider.test.asserts.shouldBe
import com.jetbrains.rider.test.asserts.shouldBeEmpty
import com.jetbrains.rider.test.asserts.shouldBeNull
import com.jetbrains.rider.test.asserts.shouldNotBeNull
import org.testng.annotations.Test

class ComboBoxExtensionsTest {

    //region Get Selected Value

    @Test
    fun testGetSelectedValue_ValueIsSet() {
        val comboBox = ComboBox<String>().apply {
            addItem("one")
            addItem("two")
            addItem("three")
            selectedItem = "two"
        }

        val selectedValue = comboBox.getSelectedValue()

        selectedValue.shouldNotBeNull()
        selectedValue.shouldBe("two")
    }

    @Test
    fun testGetSelectedValue_SelectedIsNotChanged() {
        val comboBox = ComboBox<String>().apply {
            addItem("one")
            addItem("two")
            addItem("three")
        }

        val selectedValue = comboBox.getSelectedValue()

        selectedValue.shouldNotBeNull()
        selectedValue.shouldBe("one")
    }

    @Test
    fun testGetSelectedValue_SelectedIsNotSet() {
        val comboBox = ComboBox<String>().apply {
            addItem("one")
            addItem("two")
            addItem("three")
        }

        comboBox.selectedItem = null

        val selectedValue = comboBox.getSelectedValue()

        selectedValue.shouldBeNull()
    }

    @Test
    fun testGetSelectedValue_EmptyList() {
        val comboBox = ComboBox<String>()

        val selectedValue = comboBox.getSelectedValue()
        selectedValue.shouldBeNull()
    }

    //endregion Get Selected Value

    //region Get All Items

    @Test
    fun testGetAllItems_ItemsAreSet() {
        val comboBox = ComboBox<String>().apply {
            addItem("one")
            addItem("two")
            addItem("three")
        }

        val items = comboBox.getAllItems()

        items.size.shouldBe(3)
        items[0].shouldBe("one")
        items[1].shouldBe("two")
        items[2].shouldBe("three")
    }

    @Test
    fun testGetAllItems_EmptyList() {
        val comboBox = ComboBox<String>()
        val items = comboBox.getAllItems()

        items.shouldBeEmpty()
    }

    //endregion Get All Items

    //region Fill ComboBox

    @Test
    fun testFillComboBox_Elements() {
        val comboBox = ComboBox<String>()
        comboBox.fillComboBox(listOf("one", "two", "three"))

        comboBox.model.size.shouldBe(3)
        comboBox.selectedItem.shouldBe("one")

        comboBox.getItemAt(0).shouldBe("one")
        comboBox.getItemAt(1).shouldBe("two")
        comboBox.getItemAt(2).shouldBe("three")
    }

    @Test
    fun testFillComboBox_NoElements() {
        val comboBox = ComboBox<String>()
        comboBox.fillComboBox(emptyList())

        comboBox.model.size.shouldBe(0)
        comboBox.selectedItem.shouldBeNull()
    }

    @Test
    fun testFillComboBox_DefaultElement_SetToExisting() {
        val comboBox = ComboBox<String>()
        comboBox.fillComboBox(listOf("one", "two", "three"), "two")

        comboBox.model.size.shouldBe(3)
        comboBox.selectedItem.shouldBe("two")
    }

    @Test
    fun testFillComboBox_DefaultElement_SetToNonExisting() {
        val comboBox = ComboBox<String>()
        comboBox.fillComboBox(listOf("one", "two", "three"), "four")

        comboBox.model.size.shouldBe(3)
        comboBox.selectedItem.shouldBe("one")
    }

    //endregion Fill ComboBox
}
