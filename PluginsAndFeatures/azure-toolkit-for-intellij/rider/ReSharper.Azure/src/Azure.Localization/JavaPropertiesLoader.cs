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

using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Reflection;
using System.Text.RegularExpressions;
using JetBrains.ProjectModel;
using JetBrains.Util;

namespace JetBrains.ReSharper.Azure.Localization
{
    /// <summary>
    /// Simple loader for Java .properties files with localized strings.
    /// Currently, this class is used to get string constants that persists in Java code in localized_messages.properties file.
    /// We would like to use same localized strings to share values between IDEA and R#.
    /// Note: the original .properties file is added to Azure.Localization project as a static resource.
    /// </summary>
    [SolutionComponent]
    public class JavaPropertiesLoader
    {
        private readonly IDictionary<string, string> myLocalizedStrings = new Dictionary<string, string>();

        private readonly object myLock = new object();

        /// <summary>
        /// Get value by key from "RiderAzureMessages.properties" Java file.
        /// </summary>
        /// <exception cref="T:System.Collections.Generic.KeyNotFoundException">When key is not found.</exception>
        /// <param name="key">Key to search for.</param>
        /// <param name="values">String values to pass to localized string if any.</param>
        /// <returns>Value from the "localized_messages.properties" file associated with a provided key.</returns>
        public string GetLocalizedString(string key, params string[] values)
        {
            lock (myLock)
            {
                if (myLocalizedStrings.IsNullOrEmpty())
                {
                    var assembly = Assembly.GetExecutingAssembly();
                    var resourceFullName = assembly.GetManifestResourceNames()
                        .Single(name => name.EndsWith("RiderAzureMessages.properties"));

                    using (var stream = assembly.GetManifestResourceStream(resourceFullName))
                    {
                        Load(stream);
                    }
                }

                var stringValue = myLocalizedStrings[key];
                ValidateStringParametersCount(stringValue, values);

                return string.Format(stringValue, values);
            }
        }

        private void Load(Stream stream)
        {
            if (stream.Length == 0) return;
            if (!stream.CanRead) throw new FileLoadException("Unable to read .properties file");

            using (var reader = new StreamReader(stream))
            {
                string line;
                while ((line = reader.ReadLine()?.Trim()) != null)
                {
                    if (line.Length == 0 || line.StartsWith("#") || !line.Contains("=")) continue;

                    // Consider all "="'s after the first match as the part of a value string.
                    var keyValuePair = line.Split('=');
                    myLocalizedStrings[keyValuePair[0]] = string.Join("", keyValuePair.Skip(1)).Replace("''", "'");
                }
            }
        }

        private void ValidateStringParametersCount(string stringValue, string[] parameters)
        {
            var stringParameterRegex = new Regex(@"({\d+})");
            var parametersMatches = stringParameterRegex.Matches(stringValue);
            if (parametersMatches.Count != parameters.Length)
                throw new Exception(
                    $"Mismatch number of parameters provided for a string value: {stringValue}, parameters: [{parameters.Join(", ")}]");
        }
    }
}
