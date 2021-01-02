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

using System.Collections.Generic;
using JetBrains.ProjectModel;
using JetBrains.ReSharper.Azure.Project.FunctionApp;
using JetBrains.ReSharper.Host.Features.ProjectModel.RunnableProjectsHost.RunnableProjectProviders;
using JetBrains.Rider.Model;
using JetBrains.Util;

namespace JetBrains.ReSharper.Azure.Project.RunnableProjectsHost.RunnableProjectProviders
{
    [SolutionComponent]
    public class AzureFunctionsRunnableProjectProvider : IRunnableProjectProvider
    {
        private readonly ILogger _logger;

        public AzureFunctionsRunnableProjectProvider(ILogger logger)
        {
            _logger = logger;
        }

        public RunnableProject CreateRunnableProject(IProject project, string name, string fullName, IconModel icon)
        {
            if (!project.IsDotNetCoreProject())
            {
                _logger.Trace("Project is not .NET Core SDK project, return null");
                return null;
            }

            var projectOutputs = FunctionAppProjectDetector.GetAzureFunctionsCompatibleProjectOutputs(
                project, 
                out var problems, 
                _logger);

            if (projectOutputs.IsEmpty())
            {
                _logger.Trace("No project output was found, return null");
                return null;
            }
      
            _logger.Trace("AzureFunctionsRunnableProjectProvider returned RunnableProject {0}", fullName);

            return new RunnableProject(name, fullName,
                project.ProjectFileLocation.NormalizeSeparators(FileSystemPathEx.SeparatorStyle.Unix),
                RunnableProjectKind.AzureFunctions,
                projectOutputs, new List<EnvironmentVariable>(), problems, new List<CustomAttribute>());
        }

        public IEnumerable<RunnableProjectKind> HiddenRunnableProjectKinds { get; } =
            EmptyList<RunnableProjectKind>.Instance;
    }
}