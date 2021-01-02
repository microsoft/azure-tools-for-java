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
using JetBrains.Annotations;
using JetBrains.ProjectModel;
using JetBrains.ProjectModel.Assemblies.Interfaces;
using JetBrains.ProjectModel.MSBuild;
using JetBrains.ProjectModel.Properties;
using JetBrains.ProjectModel.Properties.Managed;
using JetBrains.Rider.Model;
using JetBrains.Util;
using JetBrains.Util.Dotnet.TargetFrameworkIds;

namespace JetBrains.ReSharper.Azure.Project.FunctionApp
{
    public static class FunctionAppProjectDetector
    {
        private static readonly NugetId ExpectedPackageForNet50 = new NugetId("Microsoft.Azure.Functions.Worker");
        private static readonly NugetId ExpectedPackageForOlder = new NugetId("Microsoft.NET.Sdk.Functions");
        
        public static List<ProjectOutput> GetAzureFunctionsCompatibleProjectOutputs(
            [NotNull] IProject project, 
            [CanBeNull] out string problems, 
            [CanBeNull] ILogger logger = null)
        {
            problems = null;
            var projectOutputs = new List<ProjectOutput>();
      
            foreach (var tfm in project.TargetFrameworkIds)
            {
                if (!IsAzureFunctionsProject(project, tfm, out problems, logger))
                {
                    logger?.Trace($"Configuration for target framework does not have \"AzureFunctionsVersion\" property set: ${tfm.PresentableString}");
                    continue;
                }

                var configuration = project.ProjectProperties.TryGetConfiguration<IManagedProjectConfiguration>(tfm);
                if (configuration == null || (configuration.OutputType != ProjectOutputType.LIBRARY && configuration.OutputType != ProjectOutputType.CONSOLE_EXE))
                {
                    logger?.Trace("Project OutputType = {0}, skip configuration", configuration?.OutputType);
                    continue;
                }

                var frameworkName = tfm.PresentableString;
                var projectOutputPath = project.GetOutputFilePath(tfm);
                projectOutputs.Add(new ProjectOutput(frameworkName,
                    projectOutputPath.NormalizeSeparators(FileSystemPathEx.SeparatorStyle.Unix),
                    new List<string>(),
                    projectOutputPath.Directory.NormalizeSeparators(FileSystemPathEx.SeparatorStyle.Unix),
                    string.Empty));
            }

            return projectOutputs;
        }
        
        public static bool IsAzureFunctionsProject([NotNull] IProject project)
        {
            foreach (var tfm in project.TargetFrameworkIds)
            {
                var configuration = project.ProjectProperties.TryGetConfiguration<IManagedProjectConfiguration>(tfm);
                if (configuration == null || (configuration.OutputType != ProjectOutputType.LIBRARY && configuration.OutputType != ProjectOutputType.CONSOLE_EXE)) return false;

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

            // 2) Check expected package reference.
            var hasExpectedPackageReference = HasFunctionsPackageReference(project, targetFrameworkId);

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
        
        private static bool HasFunctionsPackageReference(IProject project, TargetFrameworkId targetFrameworkId)
        {
            // .NET 5+ requires Microsoft.Azure.Functions.Worker
            if (targetFrameworkId.Version.Major >= 5)
                return project.GetPackagesReference(ExpectedPackageForNet50, targetFrameworkId) != null;
      
            // Other frameworks need Microsoft.NET.Sdk.Functions
            return project.GetPackagesReference(ExpectedPackageForOlder, targetFrameworkId) != null;
        }
    }
}
