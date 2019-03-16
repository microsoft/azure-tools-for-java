/**
 * Copyright (c) 2019 JetBrains s.r.o.
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

package com.microsoft.azuretools.core.mvp.model.functionapp.functions.rest

class FunctionAppServiceFunctions(val value: List<Value>?) {

    class Value(val id: String?,
                val name: String?,
                val type: String?,
                val location: String?,
                val properties: Properties?)

    class Properties(val name: String?,
                     val function_app_id: String?,
                     val config: Config?,
                     val files: Files?,
                     val testData: String?)

    class Config(val generatedBy: String?,
                 val configurationSource: String?,
                 val bindings: List<Binding>?,
                 val disabled: String?,
                 val scriptFile: String?,
                 val entryPoint: String?)

    class Binding(val type: String?,
                  val methods: List<String>?,
                  val authLevel: String?,
                  val name: String?)

    class Files
}
