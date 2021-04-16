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

using JetBrains.Application.BuildScript.Application.Zones;
using JetBrains.Application.UI.Options;
using JetBrains.Application.UI.Options.OptionsDialog;
using JetBrains.IDE.UI;
using JetBrains.Lifetimes;
using JetBrains.ProjectModel;
using JetBrains.ReSharper.Azure.Intellisense.FunctionApp.LiveTemplates.Scope;
using JetBrains.ReSharper.Feature.Services.LiveTemplates.Scope;
using JetBrains.ReSharper.Feature.Services.LiveTemplates.Settings;
using JetBrains.ReSharper.Feature.Services.Resources;
using JetBrains.ReSharper.LiveTemplates.UI;
using JetBrains.Rider.Model;

namespace JetBrains.ReSharper.Azure.Intellisense.FunctionApp.LiveTemplates
{
    [ZoneMarker(typeof(IRiderModelZone))]
    [OptionsPage("RiderAzureCSharpFileTemplatesSettings", "Azure (C#)", typeof(ServicesThemedIcons.ScopeGlobal))]
    public class RiderAzureCSharpFileTemplatesOptionPage : RiderFileTemplatesOptionPageBase
    {
        public RiderAzureCSharpFileTemplatesOptionPage(Lifetime lifetime, OptionsPageContext optionsPageContext, OptionsSettingsSmartContext optionsSettingsSmartContext, 
            StoredTemplatesProvider storedTemplatesProvider, AzureCSharpProjectScopeCategoryUIProvider uiProvider, 
            ScopeCategoryManager scopeCategoryManager, TemplatesUIFactory uiFactory, IconHostBase iconHostBase, IDialogHost dialogHost) : 
            base(lifetime, uiProvider, optionsPageContext, optionsSettingsSmartContext, storedTemplatesProvider, scopeCategoryManager, uiFactory, iconHostBase, dialogHost, CSharpProjectFileType.Name)
        {
        }
    }
}