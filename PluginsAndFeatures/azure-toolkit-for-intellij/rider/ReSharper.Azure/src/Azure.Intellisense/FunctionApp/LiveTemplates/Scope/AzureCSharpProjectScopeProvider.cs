// Copyright (c) 2020 JetBrains s.r.o.
//
// All rights reserved.
//
// MIT License
//
// Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
// documentation files (the "Software"), to deal in the Software without restriction, including without limitation
// the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
// to permit persons to whom the Software is furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all copies or substantial portions of
// the Software.
//
// THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
// THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
// TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.

using System;
using System.Collections.Generic;
using JetBrains.Application;
using JetBrains.ProjectModel;
using JetBrains.ProjectModel.Properties;
using JetBrains.ReSharper.Feature.Services.LiveTemplates.Scope;

namespace JetBrains.ReSharper.Azure.Intellisense.FunctionApp.LiveTemplates.Scope
{
    public class InAzureFunctionsCSharpProject : InAzureFunctionsProject
    {
        private static readonly Guid DefaultGuid = new Guid("CF6B0FD8-3787-41DE-AD81-D0B5AE9CBFA3");
    
        public override Guid GetDefaultUID() => DefaultGuid;
        public override string PresentableShortName => "Azure Functions (C#) projects";
    }

    [ShellComponent]
    public class AzureCSharpProjectScopeProvider : AzureProjectScopeProvider
    {
        public AzureCSharpProjectScopeProvider()
        {
            Creators.Add(TryToCreate<InAzureFunctionsCSharpProject>);
        }

        protected override IEnumerable<ITemplateScopePoint> GetLanguageSpecificScopePoints(IProject project)
        {
            var language = project.ProjectProperties.DefaultLanguage;
            if (language == ProjectLanguage.CSHARP) yield return new InAzureFunctionsCSharpProject();
        }
    }
}