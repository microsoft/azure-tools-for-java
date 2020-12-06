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

using System.Collections.Generic;
using JetBrains.Annotations;
using JetBrains.Application.UI.Controls.BulbMenu.Anchors;
using JetBrains.Application.UI.Controls.BulbMenu.Items;
using JetBrains.ProjectModel;
using JetBrains.ReSharper.Azure.Daemon.FunctionApp;
using JetBrains.ReSharper.Azure.Localization;
using JetBrains.ReSharper.Azure.Psi.FunctionApp;
using JetBrains.ReSharper.Host.Features.RunMarkers;
using JetBrains.ReSharper.Resources.Shell;
using JetBrains.TextControl.DocumentMarkup;
using JetBrains.UI.Icons;
using JetBrains.UI.RichText;
using JetBrains.UI.ThemedIcons;
using JetBrains.Util;
using JetBrains.Util.Logging;

namespace JetBrains.ReSharper.Azure.Daemon.RunMarkers
{
    public abstract class FunctionAppRunMarkerGutterMark : IconGutterMarkType
    {
        private static readonly ILogger ourLogger = Logger.GetLogger<FunctionAppRunMarkerGutterMark>();

        public override IAnchor Priority => BulbMenuAnchors.PermanentBackgroundItems;

        protected FunctionAppRunMarkerGutterMark([NotNull] IconId iconId) : base(iconId)
        {
        }

        public override IEnumerable<BulbMenuItem> GetBulbMenuItems(IHighlighter highlighter)
        {
            if (!(highlighter.UserData is RunMarkerHighlighting runMarker)) yield break;

            var solution = Shell.Instance.GetComponent<SolutionsManager>().Solution;
            if (solution == null) yield break;

            if (runMarker.AttributeId != FunctionAppRunMarkerAttributeIds.FUNCTION_APP_RUN_METHOD_MARKER_ID)
                yield break;

            foreach (var item in GetRunMethodItems(solution, runMarker))
            {
                yield return item;
            }
        }

        private IEnumerable<BulbMenuItem> GetRunMethodItems(ISolution solution,
            [NotNull] RunMarkerHighlighting runMarker)
        {
            var functionAppDaemonHost = solution.GetComponent<FunctionAppDaemonHost>();
            var javaPropertiesLoader = solution.GetComponent<JavaPropertiesLoader>();

            var methodName = runMarker.Method.ShortName;
            var functionName = FunctionAppFinder.GetFunctionNameFromMethod(runMarker.Method) ??
                               runMarker.Method.ShortName;

            var projectFilePath =
                runMarker.Project.ProjectFileLocation.NormalizeSeparators(FileSystemPathEx.SeparatorStyle.Unix);

            yield return new BulbMenuItem(
                new ExecutableItem(() => { functionAppDaemonHost.RunFunctionApp(methodName, functionName, projectFilePath); }),
                new RichText(javaPropertiesLoader.GetLocalizedString("gutter.function_app.run", functionName)),
                RunMarkersThemedIcons.RunThis.Id,
                BulbMenuAnchors.PermanentBackgroundItems);

            yield return new BulbMenuItem(
                new ExecutableItem(() => { functionAppDaemonHost.DebugFunctionApp(methodName, functionName, projectFilePath); }),
                new RichText(javaPropertiesLoader.GetLocalizedString("gutter.function_app.debug", functionName)),
                RunMarkersThemedIcons.DebugThis.Id,
                BulbMenuAnchors.PermanentBackgroundItems);

            yield return new BulbMenuItem(
                new ExecutableItem(() => { functionAppDaemonHost.TriggerFunctionApp(methodName, functionName, projectFilePath); }),
                new RichText(javaPropertiesLoader.GetLocalizedString("gutter.function_app.trigger", functionName)),
                FunctionAppRunMarkersThemedIcons.Trigger.Id,
                BulbMenuAnchors.PermanentBackgroundItems);
        }
    }

    public class FunctionAppMethodRunMarkerGutterMark : FunctionAppRunMarkerGutterMark
    {
        public FunctionAppMethodRunMarkerGutterMark() : base(FunctionAppRunMarkersThemedIcons.RunFunctionApp.Id)
        {
        }
    }
}
