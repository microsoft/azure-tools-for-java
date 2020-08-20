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

using JetBrains.Annotations;
using JetBrains.ProjectModel;
using JetBrains.ProjectModel.Assemblies.Interfaces;
using JetBrains.ProjectModel.MSBuild;
using JetBrains.ProjectModel.Properties;
using JetBrains.ProjectModel.Properties.Managed;
using JetBrains.Util;
using JetBrains.Util.Dotnet.TargetFrameworkIds;

namespace JetBrains.ReSharper.Azure.Project.FunctionApp
{
    public static class FunctionAppProjectDetector
    {
        // TODO: Migrate [AzureFunctionsProjectDetector] from Rider to plugin codebase if possible.
        // TODO: Grabbed it from ReSharper backend to handle the issue with moved [AzureFunctionsProjectDetector] class into
        //       [JetBrains.ReSharper.Host.Features.ProjectModel.Azure] namespace. Unable to fix until EAP 8 with updated
        //       assembly is released. So, copy the logic here to prevent blocking plugin release.
        public static bool IsAzureFunctionsProject([NotNull] IProject project)
        {
            foreach (var tfm in project.TargetFrameworkIds)
            {
                var configuration = project.ProjectProperties.TryGetConfiguration<IManagedProjectConfiguration>(tfm);
                if (configuration == null || configuration.OutputType != ProjectOutputType.LIBRARY) return false;

                if (IsAzureFunctionsProject(project, tfm, out _, null))
                {
                    return true;
                }
            }

            return false;
        }

        private static bool IsAzureFunctionsProject(IProject project, TargetFrameworkId targetFrameworkId,
            out string problems, [CanBeNull] ILogger logger)
        {
            // Support .NET Core/Standard, NetCoreApp, NetFx
            if (!(targetFrameworkId.IsNetCore ||
                  targetFrameworkId.IsNetStandard ||
                  targetFrameworkId.IsNetCoreApp ||
                  targetFrameworkId.IsNetFramework))
            {
                logger?.Trace(
                    $"Target framework not supported by Azure Functions: ${targetFrameworkId.PresentableString}");
                problems = null;
                return false;
            }

            // 1) Check MSBuild properties. When property is defined but is empty, this will yield false.
            var hasMsBuildProperty = !string.IsNullOrEmpty(project
                .GetRequestedProjectProperties(MSBuildProjectUtil.AzureFunctionsVersion)
                .FirstNotNull());

            // 2) Check package references. If Microsoft.NET.Sdk.Functions is referenced, we're good.
            var hasExpectedPackageReference =
                project.GetPackagesReference(new NugetId("Microsoft.NET.Sdk.Functions"), targetFrameworkId) != null;

            // 3) Check existence of host.json in the project
            var hasHostJsonFile = project
                .GetSubItems("host.json")
                .Any();

            // Build problem description
            if (!hasHostJsonFile)
            {
                problems =
                    "Consider adding missing host.json file required by Azure Functions runtime to your project.";
            }
            else
            {
                problems = null;
            }

            return hasMsBuildProperty || hasExpectedPackageReference || hasHostJsonFile;
        }
    }
}
