// Copyright (c) 2020 JetBrains s.r.o.
// <p/>
// All rights reserved.
// <p/>
// MIT License
// <p/>
// Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
// documentation files (the "Software"), to deal in the Software without restriction, including without limitation
// the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
// to permit persons to whom the Software is furnished to do so, subject to the following conditions:
// <p/>
// The above copyright notice and this permission notice shall be included in all copies or substantial portions of
// the Software.
// <p/>
// THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
// THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
// TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.

using System.Linq;
using JetBrains.Annotations;
using JetBrains.Metadata.Reader.API;
using JetBrains.Metadata.Reader.Impl;
using JetBrains.Rd.Base;
using JetBrains.ReSharper.Psi;
using JetBrains.Util;
using JetBrains.Util.Logging;

namespace JetBrains.ReSharper.Azure.Psi.FunctionApp
{
    public static class FunctionAppFinder
    {
        private static ILogger ourLogger = Logger.GetLogger(typeof(FunctionAppFinder));
        
        static readonly IClrTypeName FunctionNameAttributeTypeName =
            new ClrTypeName("Microsoft.Azure.WebJobs.FunctionNameAttribute");

        static readonly IClrTypeName TimerTriggerAttributeTypeName =
            new ClrTypeName("Microsoft.Azure.WebJobs.TimerTriggerAttribute");

        /// <summary>
        /// Get Function Name from Attribute for a provided method or null if attribute is missing
        /// </summary>
        /// <param name="method">A Method instance to check.</param>
        /// <returns>Function App name string value.</returns>
        [CanBeNull]
        public static string GetFunctionNameFromMethod([CanBeNull] IMethod method)
        {
            if (method == null) return null;

            var functionAttribute = GetFunctionNameAttribute(method);
            if (functionAttribute == null) return null;

            var functionParameters = functionAttribute.PositionParameters().ToArray();
            if (functionParameters.Length < 1)
            {
                ourLogger.Warn($"Insufficient number of parameters in '{functionAttribute.GetAttributeShortName()}' attribute to get a Function name.");
                return null;
            }

            var functionNameParameter = functionParameters.First();

            if (functionNameParameter == null || !functionNameParameter.ConstantValue.IsString())
            {
                ourLogger.Error(
                    $"Unable to get a Function name from '{method.ShortName}' method attribute parameter: '{functionNameParameter.PrintToString()}'.");
                return null;
            }

            return functionNameParameter.ConstantValue.Value?.ToString();
        }

        /// <summary>
        /// Check whether a method define a Function App that can be run.
        /// Reference link: https://docs.microsoft.com/en-us/azure/azure-functions/functions-dotnet-class-library#methods-recognized-as-functions
        /// </summary>
        /// <param name="method">A Method instance to check.</param>
        /// <returns>Flag whether provided method can be considered as a method to start a Function App of any type.</returns>
        public static bool IsSuitableFunctionAppMethod([CanBeNull] IMethod method)
        {
            return method != null &&
                   method.GetAccessRights() == AccessRights.PUBLIC &&
                   method.IsStatic &&
                   GetFunctionNameAttribute(method) != null;
        }

        /// <summary>
        /// Check whether declared type is a Function App Timer Trigger type.
        /// </summary>
        /// <param name="typeElement">A type element to check.</param>
        /// <returns>Flag whether type element match Function App Timer Trigger attribute type.</returns>
        public static bool IsTimerTriggerAttribute([NotNull] ITypeElement typeElement)
        {
            return typeElement.GetClrName().Equals(TimerTriggerAttributeTypeName);
        }

        [CanBeNull]
        private static IAttributeInstance GetFunctionNameAttribute([NotNull] IMethod method)
        {
            var functionAttributes = method.GetAttributeInstances(FunctionNameAttributeTypeName, false);

            if (functionAttributes.IsEmpty())
            {
                if (ourLogger.IsTraceEnabled())
                {
                    ourLogger.Trace(
                        $"Unable to get a proper function name from a method '{method.ShortName}' that has more then one [Function] attribute.");
                }

                return null;
            }

            if (functionAttributes.Count > 1)
            {
                ourLogger.Info(
                    $"Found more then one FunctionName attribute from a method '{method.ShortName}'. Return the first match.");
            }

            return functionAttributes.First();
        }
    }
}
