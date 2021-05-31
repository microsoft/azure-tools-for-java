// Copyright (c) 2021 JetBrains s.r.o.
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

using System.Collections.Generic;
using JetBrains.Application;
using JetBrains.Rider.Backend.Features.ProjectModel.ProjectTemplates.DotNetExtensions;
using Microsoft.TemplateEngine.Edge.Settings;

namespace JetBrains.ReSharper.Azure.Intellisense.FunctionApp.ProjectTemplates.DotNetExtensions.Groups
{
    [ShellComponent]
    public class FunctionAppRuntimeGroupProvider : IDotNetTemplateGroupProvider
    {
        public int Priority => 40;
        
        public IReadOnlyCollection<DotNetTemplateGroup> Get()
        {
            return new[] {new FunctionAppRuntimeGroup()};
        }

        private class FunctionAppRuntimeGroup : DotNetTemplateGroup
        {
            public FunctionAppRuntimeGroup() : base("Functions runtime", null)
            {
            }
            
            protected override bool ShowIfSingleTemplate => false;

            protected override string GetOption(TemplateInfo info)
            {
                if (info.GroupIdentity == "Microsoft.AzureFunctions.ProjectTemplates")
                {
                    // Isolated worker known template identities:
                    // * Microsoft.AzureFunctions.ProjectTemplate.CSharp.Isolated.3.x
                    // * Microsoft.AzureFunctions.ProjectTemplate.FSharp.Isolated.3.x
                    if (info.Identity == "Microsoft.AzureFunctions.ProjectTemplate.CSharp.Isolated.3.x" || 
                        info.Identity == "Microsoft.AzureFunctions.ProjectTemplate.FSharp.Isolated.3.x")
                    {
                        return "Isolated worker (.NET 5+)";
                    }
                    
                    // Default worker known template identities:
                    // * Microsoft.AzureFunctions.ProjectTemplate.CSharp.3.x
                    // * Microsoft.AzureFunctions.ProjectTemplate.FSharp.3.x
                    if (info.Identity == "Microsoft.AzureFunctions.ProjectTemplate.CSharp.3.x" || 
                        info.Identity == "Microsoft.AzureFunctions.ProjectTemplate.FSharp.3.x")
                    {
                        return "Default worker";
                    }
                }
                
                return null;
            }
        }
    }
}