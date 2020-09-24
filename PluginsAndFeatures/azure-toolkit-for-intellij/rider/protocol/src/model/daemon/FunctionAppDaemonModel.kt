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

package model.daemon

import com.jetbrains.rd.generator.nova.*
import com.jetbrains.rd.generator.nova.PredefinedType.string
import com.jetbrains.rd.generator.nova.csharp.CSharp50Generator
import com.jetbrains.rd.generator.nova.kotlin.Kotlin11Generator
import com.jetbrains.rd.generator.nova.util.syspropertyOrInvalid
import com.jetbrains.rider.model.nova.ide.IdeRoot
import com.jetbrains.rider.model.nova.ide.SolutionModel
import java.io.File

object DaemonKotlinGenerator : ExternalGenerator(
        Kotlin11Generator(FlowTransform.AsIs, "com.jetbrains.rider.model", File(syspropertyOrInvalid("ktGeneratedOutput"))),
        IdeRoot)

object DaemonCSharpGenerator : ExternalGenerator(
        CSharp50Generator(FlowTransform.Reversed, "JetBrains.Rider.Model", File(syspropertyOrInvalid("csDaemonGeneratedOutput"))),
        IdeRoot)

@Suppress("unused")
object FunctionAppDaemonModel : Ext(SolutionModel.Solution) {

    private val FunctionAppRequest = structdef {
        field("methodName", string)
        field("functionName", string)
        field("projectFilePath", string)
    }

    init {
        setting(GeneratorBase.AcceptsGenerator) { generator ->
            generator == DaemonKotlinGenerator.generator ||
                    generator == DaemonCSharpGenerator.generator
        }

        sink("runFunctionApp", FunctionAppRequest)
                .doc("Signal from backend to run a Function App locally.")

        sink("debugFunctionApp", FunctionAppRequest)
                .doc("Signal from backend to debug a Function App locally.")

        sink("triggerFunctionApp", FunctionAppRequest)
                .doc("Signal from backend to trigger a Function App.")
    }
}
