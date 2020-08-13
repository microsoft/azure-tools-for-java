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

package org.jetbrains.plugins.azure

import com.jetbrains.rider.test.asserts.shouldBe
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

class RiderAzureUtilsTest {

    class IsValidUrlTest {

        @DataProvider(name = "urlsToTest")
        fun urlToValid() = arrayOf(
                arrayOf("http://localhost:5001/", true),
                arrayOf("https://localhost:5001/", true),
                arrayOf("https://localhost:5001/path", true),
                arrayOf("https://localhost:5001/path/", true),
                arrayOf("https://localhost:5001/path/path?query=something", true),
                arrayOf("ftp://localhost:5001/", true),
                arrayOf("localhost:5001", false),
                arrayOf("acmecorp", false),
                arrayOf("acmecorp.onmicrosoft", false),
                arrayOf("", false),
                arrayOf(null, false),
                arrayOf("banana apple", false)
        )

        @Test(dataProvider = "urlsToTest")
        fun testUrlShouldBeValid(url: String?, expected: Boolean) {
            url.isValidUrl()
                    .shouldBe(expected)
        }

    }

    class IsValidGuidTest {

        @DataProvider(name = "guidsToTest")
        fun urlToValid() = arrayOf(
                arrayOf("810f08fd-6cc2-4d4a-be8b-9d61a08a83ef", true),
                arrayOf("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa", true),
                arrayOf("zzzzzzzz-zzzz-zzzz-zzzz-zzzzzzzzzzzz", false),
                arrayOf("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", false),
                arrayOf("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa", false),
                arrayOf("aaaaaaaa", false),
                arrayOf("", false),
                arrayOf(null, false),
                arrayOf("banana apple", false)
        )

        @Test(dataProvider = "guidsToTest")
        fun testGuidShouldBeValid(guid: String?, expected: Boolean) {
            guid.isValidGuid()
                    .shouldBe(expected)
        }

    }
}