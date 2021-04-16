﻿// Copyright (c) 2020 JetBrains s.r.o.
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
using JetBrains.ReSharper.Azure.Project.FunctionApp;
using JetBrains.ReSharper.Feature.Services.LiveTemplates.Context;
using JetBrains.ReSharper.Feature.Services.LiveTemplates.Scope;

namespace JetBrains.ReSharper.Azure.Intellisense.FunctionApp.LiveTemplates.Scope
{
    public class InAzureFunctionsProject : InAnyProject
    {
        private static readonly Guid DefaultGuid = new Guid("41A8B8D2-2DE7-43F8-A812-220E5BC95BEB");
    
        public override Guid GetDefaultUID() => DefaultGuid;
        public override string PresentableShortName => "Azure Functions projects";
    }
    
    [ShellComponent]
    public class AzureProjectScopeProvider : ScopeProvider
    {
        public AzureProjectScopeProvider()
        {
            Creators.Add(TryToCreate<InAzureFunctionsProject>);
        }

        public override IEnumerable<ITemplateScopePoint> ProvideScopePoints(TemplateAcceptanceContext context)
        {
            var project = context.GetProject();
            if (project == null) yield break;
            if (FunctionAppProjectDetector.IsAzureFunctionsProject(project)) 
            {
                yield return new InAzureFunctionsProject();
        
                foreach (var scope in GetLanguageSpecificScopePoints(project)) 
                    yield return scope;
            }
        }

        protected virtual IEnumerable<ITemplateScopePoint> GetLanguageSpecificScopePoints(IProject project)
        {
            yield break;
        }
    }
}